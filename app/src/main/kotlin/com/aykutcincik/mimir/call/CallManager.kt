package com.aykutcincik.mimir.call

import android.content.Context
import android.util.Log
import com.aykutcincik.mimir.Apis
import com.aykutcincik.mimir.data.ApiResult
import com.aykutcincik.mimir.data.TurnCredentialsDto
import com.aykutcincik.mimir.realtime.RealtimeClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
 * Sprint #12 — WebRTC voice call orchestrator.
 *
 * Responsibilities:
 *  - PeerConnectionFactory init + audio track
 *  - SDP offer/answer exchange via SignalR
 *  - ICE candidate exchange
 *  - State machine (Idle → Outgoing → Connecting → Connected → Ended)
 *
 * Singleton (object) — uygulama lifecycle boyunca tek instance.
 * UI screens (Outgoing/Incoming/InCall) bunun state flow'unu observe eder.
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
    private var initialized = false
    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localAudioTrack: AudioTrack? = null
    private val eglBase: EglBase = EglBase.create()
    private var iceServers: List<PeerConnection.IceServer> = emptyList()
    private var realtime: RealtimeClient? = null
    private var collectJob: Job? = null
    private var pendingRemoteIceCandidates = mutableListOf<IceCandidate>()
    private var remoteSdpSet = false

    fun init(ctx: Context) {
        if (initialized) return
        val opts = PeerConnectionFactory.InitializationOptions.builder(ctx.applicationContext)
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

    /** RealtimeClient'i bağla — call signaling event'leri buradan izlenir. */
    fun bindRealtime(rt: RealtimeClient) {
        realtime = rt
        collectJob?.cancel()
        collectJob = scope.launch {
            rt.events.collect { ev ->
                when (ev) {
                    is RealtimeClient.RealtimeEvent.IncomingCall -> handleIncomingOffer(ev.event.callerId, ev.event.callerUsername, ev.event.sdpOffer)
                    is RealtimeClient.RealtimeEvent.CallAnswered -> handleAnswer(ev.event.sdpAnswer)
                    is RealtimeClient.RealtimeEvent.IceCandidate -> handleRemoteIce(ev.event.candidate)
                    is RealtimeClient.RealtimeEvent.CallRejected -> end("Karşı taraf reddetti")
                    is RealtimeClient.RealtimeEvent.CallEnded -> end("Karşı taraf kapattı")
                    else -> {}
                }
            }
        }
    }

    suspend fun startOutgoing(ctx: Context, peerId: String, peerUsername: String, accessToken: String) {
        try {
            Log.i(TAG, "startOutgoing[1]: init")
            init(ctx)

            Log.i(TAG, "startOutgoing[2]: ensureIceServers")
            ensureIceServers(accessToken)

            _state.value = CallState.Outgoing(peerId, peerUsername)

            Log.i(TAG, "startOutgoing[3]: createPeerConnection (ice=${iceServers.size})")
            val pc = createPeerConnection(peerId) ?: run { end("PC olusturulamadi"); return }
            peerConnection = pc

            Log.i(TAG, "startOutgoing[4]: addLocalAudio")
            addLocalAudio(pc)

            Log.i(TAG, "startOutgoing[5]: createOffer")
            val constraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            }
            val offer = createOffer(pc, constraints)
            if (offer == null) { end("Offer olusturulamadi"); return }

            Log.i(TAG, "startOutgoing[6]: setLocalSdp (sdpLen=${offer.description.length})")
            setLocalSdp(pc, offer)

            Log.i(TAG, "startOutgoing[7]: realtime.offerCall (rt=${realtime != null})")
            val rt = realtime
            if (rt == null) { end("realtime baglantisi yok"); return }
            rt.offerCall(peerId, offer.description)
            Log.i(TAG, "startOutgoing[8]: OfferCall SENT")
        } catch (e: Exception) {
            Log.e(TAG, "startOutgoing FAIL: ${e.message}", e)
            end("Hata: ${e.message?.take(60) ?: e.javaClass.simpleName}")
        }
    }

    /** UI'dan kabul tıklanınca — Incoming → Connecting + answer SDP gönder */
    suspend fun acceptIncoming(ctx: Context, accessToken: String) {
        val incoming = _state.value as? CallState.Incoming ?: return
        init(ctx)
        ensureIceServers(accessToken)

        _state.value = CallState.Connecting(incoming.peerId, incoming.peerUsername)

        val pc = createPeerConnection(incoming.peerId) ?: run { end("PC oluşturulamadı"); return }
        peerConnection = pc

        addLocalAudio(pc)
        setRemoteSdp(pc, SessionDescription(SessionDescription.Type.OFFER, incoming.sdpOffer))

        val answer = createAnswer(pc, MediaConstraints())
        if (answer == null) { end("Answer oluşturulamadı"); return }
        setLocalSdp(pc, answer)
        realtime?.answerCall(incoming.peerId, answer.description)
    }

    suspend fun rejectIncoming() {
        val incoming = _state.value as? CallState.Incoming ?: return
        realtime?.rejectCall(incoming.peerId)
        end("Reddedildi")
    }

    suspend fun hangup() {
        val peerId = currentPeerId() ?: return end("Bitti")
        realtime?.endCall(peerId)
        end("Bitti")
    }

    fun end(reason: String) {
        try { peerConnection?.dispose() } catch (_: Exception) {}
        peerConnection = null
        localAudioTrack = null
        pendingRemoteIceCandidates.clear()
        remoteSdpSet = false
        _state.value = CallState.Ended(reason)
        scope.launch {
            kotlinx.coroutines.delay(1500)
            if (_state.value is CallState.Ended) _state.value = CallState.Idle
        }
    }

    private fun currentPeerId(): String? = when (val s = _state.value) {
        is CallState.Outgoing -> s.peerId
        is CallState.Incoming -> s.peerId
        is CallState.Connecting -> s.peerId
        is CallState.Connected -> s.peerId
        else -> null
    }

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
            // Fallback: Google STUN sadece (TURN yok → simetrik NAT'larda fail olur)
            iceServers = listOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
            Log.w(TAG, "TURN credentials alınamadı, sadece Google STUN")
        }
    }

    private fun handleIncomingOffer(callerId: String, callerUsername: String, sdpOffer: String) {
        if (_state.value !is CallState.Idle) {
            // Meşgulken yeni arama — reddet
            scope.launch { realtime?.rejectCall(callerId) }
            return
        }
        _state.value = CallState.Incoming(callerId, callerUsername, sdpOffer)
    }

    private fun handleAnswer(sdpAnswer: String) {
        val pc = peerConnection ?: return
        val outgoing = _state.value as? CallState.Outgoing ?: return
        _state.value = CallState.Connecting(outgoing.peerId, outgoing.peerUsername)
        setRemoteSdp(pc, SessionDescription(SessionDescription.Type.ANSWER, sdpAnswer))
    }

    private fun handleRemoteIce(candidateJson: String) {
        val parts = candidateJson.split("|")
        if (parts.size != 3) return
        val candidate = IceCandidate(parts[0], parts[1].toIntOrNull() ?: 0, parts[2])
        val pc = peerConnection
        if (pc != null && remoteSdpSet) {
            pc.addIceCandidate(candidate)
        } else {
            pendingRemoteIceCandidates.add(candidate)
        }
    }

    private fun createPeerConnection(peerId: String): PeerConnection? {
        val cfg = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        return factory?.createPeerConnection(cfg, object : PeerConnection.Observer {
            override fun onSignalingChange(s: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(s: PeerConnection.IceConnectionState?) {
                Log.i(TAG, "ICE state: $s")
                when (s) {
                    PeerConnection.IceConnectionState.CONNECTED,
                    PeerConnection.IceConnectionState.COMPLETED -> {
                        val cur = _state.value
                        val (pid, uname) = when (cur) {
                            is CallState.Connecting -> cur.peerId to cur.peerUsername
                            is CallState.Outgoing -> cur.peerId to cur.peerUsername
                            else -> return
                        }
                        _state.value = CallState.Connected(pid, uname, System.currentTimeMillis())
                    }
                    PeerConnection.IceConnectionState.FAILED,
                    PeerConnection.IceConnectionState.CLOSED,
                    PeerConnection.IceConnectionState.DISCONNECTED -> end("Bağlantı kesildi")
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
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                Log.i(TAG, "Remote track added: ${receiver?.track()?.kind()}")
            }
        })
    }

    private fun addLocalAudio(pc: PeerConnection) {
        val factory = this.factory ?: return
        val audioConstraints = MediaConstraints()
        val source = factory.createAudioSource(audioConstraints)
        val track = factory.createAudioTrack("audio0", source)
        track.setEnabled(true)
        localAudioTrack = track
        pc.addTrack(track, listOf("stream0"))
    }

    fun setMuted(muted: Boolean) {
        localAudioTrack?.setEnabled(!muted)
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
                remoteSdpSet = true
                pendingRemoteIceCandidates.forEach { pc.addIceCandidate(it) }
                pendingRemoteIceCandidates.clear()
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
