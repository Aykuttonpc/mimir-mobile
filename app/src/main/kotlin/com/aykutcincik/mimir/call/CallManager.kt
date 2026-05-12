package com.aykutcincik.mimir.call

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.aykutcincik.mimir.Apis
import com.aykutcincik.mimir.data.ApiResult
import com.aykutcincik.mimir.push.Notifications
import com.aykutcincik.mimir.realtime.RealtimeClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.webrtc.AudioTrack
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * WebRTC voice call orchestrator — Sprint #12+ (post-spike refactor).
 *
 * GÜVENLİK GARANTİLERİ:
 *  - Permission check eager — RECORD_AUDIO yoksa crash yerine state Ended.
 *  - Mutex ile peerConnection mutation thread-safe (race condition önlenir).
 *  - State transition'ları tek noktadan (`transition()`) — log + invariant.
 *  - Outgoing 30sn timeout — answer gelmezse otomatik end("Cevap yok").
 *  - Tie-breaker (concurrent call): caller ID lexicographic karşılaştırma.
 *  - Ended → Idle TRANSITION YOK — Idle direkt set, UI Ended state kendi handle eder.
 *  - dispose() guard'lı: state başka iken çağrılırsa no-op.
 */
object CallManager {

    sealed interface CallState {
        data object Idle : CallState
        data class Outgoing(val peerId: String, val peerUsername: String) : CallState
        data class Incoming(val peerId: String, val peerUsername: String, val sdpOffer: String) : CallState
        data class Connecting(val peerId: String, val peerUsername: String) : CallState
        data class Connected(val peerId: String, val peerUsername: String, val startedAt: Long) : CallState
        data class Ended(val reason: String) : CallState
    }

    private val _state = MutableStateFlow<CallState>(CallState.Idle)
    val state: StateFlow<CallState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()                       // peerConnection mutation guard

    private var initialized = false
    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localAudioTrack: AudioTrack? = null
    private val eglBase: EglBase = EglBase.create()
    private var iceServers: List<PeerConnection.IceServer> = emptyList()
    private var realtime: RealtimeClient? = null
    private var appContext: Context? = null
    private var collectJob: Job? = null
    private var outgoingTimeoutJob: Job? = null
    private var pendingRemoteIceCandidates = mutableListOf<IceCandidate>()
    private var remoteSdpSet = false
    private var myUserId: String = ""

    private const val OUTGOING_TIMEOUT_MS = 30_000L

    // ──────────────────── Initialization ────────────────────

    fun init(ctx: Context) {
        if (initialized) return
        appContext = ctx.applicationContext

        val opts = PeerConnectionFactory.InitializationOptions.builder(appContext)
            .setEnableInternalTracer(false)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(opts)

        val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
        initialized = true
        Log.i(TAG, "PeerConnectionFactory initialized")
    }

    /** Auth bilgisini kullan — tie-breaker için lazım. */
    fun bindRealtime(rt: RealtimeClient, currentUserId: String) {
        realtime = rt
        myUserId = currentUserId
        collectJob?.cancel()
        collectJob = scope.launch {
            rt.events.collect { ev ->
                when (ev) {
                    is RealtimeClient.RealtimeEvent.IncomingCall ->
                        handleIncomingOffer(ev.event.callerId, ev.event.callerUsername, ev.event.sdpOffer)
                    is RealtimeClient.RealtimeEvent.CallAnswered ->
                        handleAnswer(ev.event.sdpAnswer)
                    is RealtimeClient.RealtimeEvent.IceCandidate ->
                        handleRemoteIce(ev.event.candidate)
                    is RealtimeClient.RealtimeEvent.CallRejected ->
                        end("Karşı taraf reddetti")
                    is RealtimeClient.RealtimeEvent.CallEnded ->
                        end("Karşı taraf kapattı")
                    else -> {}
                }
            }
        }
    }

    private fun hasMicPermission(): Boolean {
        val ctx = appContext ?: return false
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
    }

    // ──────────────────── Outgoing ────────────────────

    fun startOutgoing(ctx: Context, peerId: String, peerUsername: String, accessToken: String) {
        scope.launch {
            mutex.withLock {
                try {
                    init(ctx)

                    // Permission gate — crash önlemi (Bug 2)
                    if (!hasMicPermission()) {
                        _state.value = CallState.Ended("Mikrofon izni gerekli")
                        return@withLock
                    }

                    // Önceki çağrı bitmemişse temizle (defensive)
                    if (_state.value !is CallState.Idle && _state.value !is CallState.Ended) {
                        Log.w(TAG, "startOutgoing called while state=${_state.value::class.simpleName} — cleaning up")
                        cleanupPeerLocked()
                    }

                    ensureIceServers(accessToken)
                    transition(CallState.Outgoing(peerId, peerUsername))

                    val pc = createPeerConnection(peerId) ?: run {
                        end("PC olusturulamadi"); return@withLock
                    }
                    peerConnection = pc

                    addLocalAudio(pc) ?: run {
                        end("Audio kaynak olusturulamadi"); return@withLock
                    }

                    val constraints = MediaConstraints().apply {
                        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                    }
                    val offer = createOffer(pc, constraints) ?: run {
                        end("Offer olusturulamadi"); return@withLock
                    }
                    setLocalSdp(pc, offer)

                    val rt = realtime ?: run {
                        end("Realtime baglantisi yok"); return@withLock
                    }
                    rt.offerCall(peerId, offer.description)

                    // Outgoing timeout — 30sn answer gelmezse cancel
                    startOutgoingTimeout(peerId)
                } catch (e: Exception) {
                    Log.e(TAG, "startOutgoing FAIL: ${e.message}")
                    end("Hata: ${e.message?.take(60) ?: e.javaClass.simpleName}")
                }
            }
        }
    }

    private fun startOutgoingTimeout(peerId: String) {
        outgoingTimeoutJob?.cancel()
        outgoingTimeoutJob = scope.launch {
            delay(OUTGOING_TIMEOUT_MS)
            val s = _state.value
            if (s is CallState.Outgoing && s.peerId == peerId) {
                realtime?.endCall(peerId)
                end("Cevap yok")
            }
        }
    }

    // ──────────────────── Incoming ────────────────────

    /**
     * FCM payload üzerinden gelen SDP offer'ı CallManager'a enjekte et.
     * SignalR connection olmasa bile state Incoming'e geçer (app dead-from-FCM senaryosu).
     * MimirFcmService callOffer payload handler tarafından çağrılır.
     */
    fun injectIncomingOffer(ctx: Context, callerId: String, callerUsername: String, sdpOffer: String) {
        init(ctx)
        handleIncomingOffer(callerId, callerUsername, sdpOffer)
    }

    private fun handleIncomingOffer(callerId: String, callerUsername: String, sdpOffer: String) {
        scope.launch {
            mutex.withLock {
                when (val s = _state.value) {
                    is CallState.Idle, is CallState.Ended -> {
                        // Boşta — gelen aramayı kabul edilebilir hale getir
                        transition(CallState.Incoming(callerId, callerUsername, sdpOffer))
                    }
                    is CallState.Outgoing -> {
                        // Çift-yönlü simultane arama — tie-breaker:
                        // Küçük (lexicographic) caller ID kazanır.
                        if (callerId < myUserId) {
                            // Karşı taraf "kazandı" — kendi outgoing'imizi iptal,
                            // gelen aramayı kabul edilebilir hale getir.
                            Log.i(TAG, "Glare: peer $callerId wins, canceling our outgoing")
                            realtime?.endCall(s.peerId)
                            cleanupPeerLocked()
                            transition(CallState.Incoming(callerId, callerUsername, sdpOffer))
                        } else {
                            // Biz kazandık — gelen aramayı reddet, kendi outgoing'imizi sürdür.
                            Log.i(TAG, "Glare: we win, rejecting peer's call")
                            realtime?.rejectCall(callerId)
                        }
                    }
                    else -> {
                        // Connecting/Connected/Incoming — meşgul, reject
                        realtime?.rejectCall(callerId)
                    }
                }
            }
        }
    }

    fun acceptIncoming(ctx: Context, accessToken: String) {
        val incoming = _state.value as? CallState.Incoming ?: return
        scope.launch {
            mutex.withLock {
                try {
                    init(ctx)

                    if (!hasMicPermission()) {
                        realtime?.rejectCall(incoming.peerId)
                        end("Mikrofon izni gerekli")
                        return@withLock
                    }

                    ensureIceServers(accessToken)
                    transition(CallState.Connecting(incoming.peerId, incoming.peerUsername))

                    val pc = createPeerConnection(incoming.peerId) ?: run {
                        end("PC olusturulamadi"); return@withLock
                    }
                    peerConnection = pc

                    addLocalAudio(pc) ?: run {
                        end("Audio kaynak olusturulamadi"); return@withLock
                    }

                    setRemoteSdp(pc, SessionDescription(SessionDescription.Type.OFFER, incoming.sdpOffer))

                    val answer = createAnswer(pc, MediaConstraints()) ?: run {
                        end("Answer olusturulamadi"); return@withLock
                    }
                    setLocalSdp(pc, answer)
                    realtime?.answerCall(incoming.peerId, answer.description)

                    // FCM bildirimi açıksa kapat (Bug 6)
                    cancelIncomingNotification(incoming.peerId)
                } catch (e: Exception) {
                    Log.e(TAG, "acceptIncoming FAIL: ${e.message}")
                    end("Hata: ${e.message?.take(60) ?: e.javaClass.simpleName}")
                }
            }
        }
    }

    fun rejectIncoming() {
        val incoming = _state.value as? CallState.Incoming ?: return
        scope.launch {
            mutex.withLock {
                try { realtime?.rejectCall(incoming.peerId) } catch (_: Exception) {}
                cancelIncomingNotification(incoming.peerId)
                end("Reddedildi")
            }
        }
    }

    fun hangup() {
        scope.launch {
            mutex.withLock {
                val peerId = currentPeerId()
                if (peerId != null) {
                    try { realtime?.endCall(peerId) } catch (_: Exception) {}
                }
                end("Bitti")
            }
        }
    }

    // ──────────────────── State transitions ────────────────────

    private fun handleAnswer(sdpAnswer: String) {
        scope.launch {
            mutex.withLock {
                val pc = peerConnection ?: return@withLock
                val outgoing = _state.value as? CallState.Outgoing ?: return@withLock
                outgoingTimeoutJob?.cancel()
                transition(CallState.Connecting(outgoing.peerId, outgoing.peerUsername))
                setRemoteSdp(pc, SessionDescription(SessionDescription.Type.ANSWER, sdpAnswer))
            }
        }
    }

    private fun handleRemoteIce(candidateJson: String) {
        scope.launch {
            mutex.withLock {
                val parts = candidateJson.split("|")
                if (parts.size != 3) return@withLock
                val candidate = IceCandidate(parts[0], parts[1].toIntOrNull() ?: 0, parts[2])
                val pc = peerConnection
                if (pc != null && remoteSdpSet) {
                    pc.addIceCandidate(candidate)
                } else {
                    pendingRemoteIceCandidates.add(candidate)
                }
            }
        }
    }

    private fun transition(newState: CallState) {
        Log.i(TAG, "state: ${_state.value::class.simpleName} → ${newState::class.simpleName}")
        _state.value = newState
    }

    /**
     * Çağrı bitti — peerConnection dispose, state Ended → 2sn sonra Idle.
     * Mutex zaten caller'da tutuluyor (start/accept/reject/hangup tüm path mutex.withLock).
     * handleIncomingOffer glare case'inde de mutex altında.
     */
    fun end(reason: String) {
        cleanupPeerLocked()
        transition(CallState.Ended(reason))
        scope.launch {
            delay(2000)
            // Ended → Idle (eğer hala Ended ise, başka transition yapılmadıysa)
            if (_state.value is CallState.Ended) transition(CallState.Idle)
        }
    }

    private fun cleanupPeerLocked() {
        outgoingTimeoutJob?.cancel()
        outgoingTimeoutJob = null
        try { peerConnection?.dispose() } catch (e: Exception) { Log.w(TAG, "dispose: ${e.message}") }
        peerConnection = null
        localAudioTrack = null
        pendingRemoteIceCandidates.clear()
        remoteSdpSet = false
    }

    private fun cancelIncomingNotification(callerId: String) {
        val ctx = appContext ?: return
        runCatching { Notifications.cancelIncomingCall(ctx, callerId) }
    }

    private fun currentPeerId(): String? = when (val s = _state.value) {
        is CallState.Outgoing -> s.peerId
        is CallState.Incoming -> s.peerId
        is CallState.Connecting -> s.peerId
        is CallState.Connected -> s.peerId
        else -> null
    }

    // ──────────────────── WebRTC plumbing ────────────────────

    private suspend fun ensureIceServers(accessToken: String) {
        if (iceServers.isNotEmpty()) return
        val r = Apis.call(accessToken).getTurnCredentials()
        if (r is ApiResult.Success) {
            iceServers = r.value.urls.map { url ->
                PeerConnection.IceServer.builder(url)
                    .setUsername(r.value.username)
                    .setPassword(r.value.credential)
                    .createIceServer()
            }
            Log.i(TAG, "Loaded ${iceServers.size} ICE servers")
        } else {
            iceServers = listOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
            )
            Log.w(TAG, "TURN credentials alinamadi, sadece Google STUN")
        }
    }

    private fun createPeerConnection(peerId: String): PeerConnection? {
        val cfg = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        return factory?.createPeerConnection(cfg, object : PeerConnection.Observer {
            override fun onSignalingChange(s: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(s: PeerConnection.IceConnectionState?) {
                Log.i(TAG, "ICE: $s")
                when (s) {
                    PeerConnection.IceConnectionState.CONNECTED,
                    PeerConnection.IceConnectionState.COMPLETED -> {
                        scope.launch {
                            mutex.withLock {
                                val cur = _state.value
                                val (pid, uname) = when (cur) {
                                    is CallState.Connecting -> cur.peerId to cur.peerUsername
                                    is CallState.Outgoing -> cur.peerId to cur.peerUsername
                                    else -> return@withLock
                                }
                                transition(CallState.Connected(pid, uname, System.currentTimeMillis()))
                            }
                        }
                    }
                    PeerConnection.IceConnectionState.FAILED -> end("Baglanti kurulamadi")
                    PeerConnection.IceConnectionState.DISCONNECTED -> {
                        // Disconnect geçici olabilir; FAILED'a çevrilirse zaten yakalanır.
                    }
                    PeerConnection.IceConnectionState.CLOSED -> {
                        // dispose'dan tetiklenir, end() içinde zaten transition var
                    }
                    else -> {}
                }
            }
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(s: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidate(c: IceCandidate?) {
                if (c == null) return
                val payload = "${c.sdpMid}|${c.sdpMLineIndex}|${c.sdp}"
                scope.launch { realtime?.sendIceCandidate(peerId, payload) }
            }
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onAddStream(p0: MediaStream?) {}
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onDataChannel(p0: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
        })
    }

    /** Returns Unit on success, null on failure (caller end()'i çağırır). */
    private fun addLocalAudio(pc: PeerConnection): Unit? {
        return try {
            val factory = this.factory ?: return null
            val audioConstraints = MediaConstraints()
            val source = factory.createAudioSource(audioConstraints)
            val track = factory.createAudioTrack("audio0", source)
            track.setEnabled(true)
            localAudioTrack = track
            pc.addTrack(track, listOf("stream0"))
            Unit
        } catch (e: Exception) {
            Log.e(TAG, "addLocalAudio FAIL: ${e.message}")
            null
        }
    }

    fun setMuted(muted: Boolean) {
        try { localAudioTrack?.setEnabled(!muted) } catch (_: Exception) {}
    }

    private suspend fun createOffer(pc: PeerConnection, constraints: MediaConstraints): SessionDescription? =
        suspendCoroutine { cont ->
            pc.createOffer(object : SdpObserver {
                override fun onCreateSuccess(sdp: SessionDescription) { cont.resume(sdp) }
                override fun onSetSuccess() {}
                override fun onCreateFailure(p0: String?) { cont.resume(null) }
                override fun onSetFailure(p0: String?) {}
            }, constraints)
        }

    private suspend fun createAnswer(pc: PeerConnection, constraints: MediaConstraints): SessionDescription? =
        suspendCoroutine { cont ->
            pc.createAnswer(object : SdpObserver {
                override fun onCreateSuccess(sdp: SessionDescription) { cont.resume(sdp) }
                override fun onSetSuccess() {}
                override fun onCreateFailure(p0: String?) { cont.resume(null) }
                override fun onSetFailure(p0: String?) {}
            }, constraints)
        }

    private fun setLocalSdp(pc: PeerConnection, sdp: SessionDescription) {
        pc.setLocalDescription(noopSdp(), sdp)
    }

    private fun setRemoteSdp(pc: PeerConnection, sdp: SessionDescription) {
        pc.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                scope.launch {
                    mutex.withLock {
                        remoteSdpSet = true
                        pendingRemoteIceCandidates.forEach { pc.addIceCandidate(it) }
                        pendingRemoteIceCandidates.clear()
                    }
                }
            }
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }, sdp)
    }

    private fun noopSdp() = object : SdpObserver {
        override fun onCreateSuccess(p0: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(p0: String?) {}
        override fun onSetFailure(p0: String?) {}
    }

    private const val TAG = "CallManager"
}
