/*
 * Adapted from GetStream/webrtc-in-jetpack-compose (Apache 2.0)
 * Original: io.getstream.webrtc.sample.compose.webrtc.sessions.WebRtcSessionManagerImpl
 *
 * Mimir farkları:
 *  - Audio-only (video yok — Sprint #14'te eklenir)
 *  - 1-1 peer-bound (room değil)
 *  - State machine (Idle/Outgoing/Incoming/Connecting/Connected/Ended)
 *  - Bizim TURN credentials API'sinden ICE servers fetch
 *  - SignalingCommand'a peerId tracking eklenmiş
 */
package com.aykutcincik.mimir.call.webrtc.session

import android.content.Context
import android.media.AudioManager
import androidx.core.content.getSystemService
import com.aykutcincik.mimir.Apis
import com.aykutcincik.mimir.call.webrtc.peer.StreamPeerConnection
import com.aykutcincik.mimir.call.webrtc.peer.StreamPeerConnectionFactory
import com.aykutcincik.mimir.call.webrtc.peer.StreamPeerType
import com.aykutcincik.mimir.call.webrtc.signaling.SignalingClient
import com.aykutcincik.mimir.call.webrtc.signaling.SignalingCommand
import com.aykutcincik.mimir.call.webrtc.signaling.SignalingEvent
import com.aykutcincik.mimir.call.webrtc.utils.taggedLogger
import com.aykutcincik.mimir.data.ApiResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import java.util.UUID

private const val ICE_SEPARATOR = '|'

/**
 * Tek bir 1-1 sesli arama oturumu. CallManager bunu wrap eder + lifecycle yönetir.
 */
class CallSession(
    private val context: Context,
    private val signalingClient: SignalingClient,
    private val peerConnectionFactory: StreamPeerConnectionFactory,
    private val myUserId: String,
    private val accessToken: String,
) {

    sealed interface State {
        data object Idle : State
        data class Outgoing(val peerId: String, val peerUsername: String) : State
        data class Incoming(val peerId: String, val peerUsername: String, val sdpOffer: String) : State
        data class Connecting(val peerId: String, val peerUsername: String) : State
        data class Connected(val peerId: String, val peerUsername: String, val startedAt: Long) : State
        data class Ended(val reason: String) : State
    }

    private val logger by taggedLogger("Mimir.CallSession")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var peerConnection: StreamPeerConnection? = null
    private var pendingPeerId: String? = null
    private var iceServersLoaded = false
    private var outgoingTimeoutJob: Job? = null

    // Audio-only constraints — OfferToReceiveAudio true, video false
    private val mediaConstraints = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
    }

    private val audioConstraints: MediaConstraints by lazy {
        MediaConstraints().apply {
            optional.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            optional.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            optional.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
            optional.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
            optional.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
        }
    }

    init {
        // Inbound signaling event'leri dinle
        scope.launch {
            signalingClient.incoming.collect { ev -> handleIncomingSignal(ev) }
        }
    }

    // ──────────────────── Public API ────────────────────

    fun startOutgoing(peerId: String, peerUsername: String) {
        if (_state.value !is State.Idle && _state.value !is State.Ended) {
            logger.w { "startOutgoing skip — state=${_state.value::class.simpleName}" }
            return
        }
        _state.value = State.Outgoing(peerId, peerUsername)
        scope.launch { doOutgoing(peerId) }
    }

    fun acceptIncoming() {
        val incoming = _state.value as? State.Incoming ?: return
        _state.value = State.Connecting(incoming.peerId, incoming.peerUsername)
        scope.launch { doAnswer(incoming.peerId, incoming.sdpOffer) }
    }

    fun rejectIncoming() {
        val incoming = _state.value as? State.Incoming ?: return
        scope.launch {
            runCatching { signalingClient.send(SignalingCommand.REJECT, incoming.peerId, "") }
            end("Reddedildi")
        }
    }

    fun hangup() {
        val peerId = currentPeerId() ?: run { end("Bitti"); return }
        scope.launch {
            runCatching { signalingClient.send(SignalingCommand.END, peerId, "") }
            end("Bitti")
        }
    }

    fun setMuted(muted: Boolean) {
        runCatching { localAudioTrack?.setEnabled(!muted) }
    }

    /** FCM üzerinden gelen offer'ı manuel inject (app dead'ken). */
    fun injectIncomingOffer(callerId: String, callerUsername: String, sdpOffer: String) {
        if (_state.value !is State.Idle && _state.value !is State.Ended) {
            // Meşgul — yeni offer'ı sessizce yok say, glare resolver yetersiz şu an
            logger.w { "inject skip — busy" }
            return
        }
        _state.value = State.Incoming(callerId, callerUsername, sdpOffer)
    }

    fun dispose() {
        scope.launch { cleanup() }
        scope.cancel()
    }

    // ──────────────────── Internal flow ────────────────────

    private suspend fun doOutgoing(peerId: String) {
        try {
            val iceServers = ensureIceServers()
            val pc = createPeer(iceServers, peerId, StreamPeerType.PUBLISHER)
            peerConnection = pc

            attachLocalAudio(pc)
            setupAudioRouting()

            val offer = pc.createOffer().getOrThrow()
            pc.setLocalDescription(offer).getOrThrow()
            signalingClient.send(SignalingCommand.OFFER, peerId, offer.description)

            startOutgoingTimeout(peerId)
        } catch (e: Exception) {
            logger.e { "outgoing fail: ${e.message}" }
            end("Hata: ${e.message?.take(60) ?: e.javaClass.simpleName}")
        }
    }

    private suspend fun doAnswer(peerId: String, sdpOffer: String) {
        try {
            val iceServers = ensureIceServers()
            val pc = createPeer(iceServers, peerId, StreamPeerType.SUBSCRIBER)
            peerConnection = pc

            attachLocalAudio(pc)
            setupAudioRouting()

            pc.setRemoteDescription(SessionDescription(SessionDescription.Type.OFFER, sdpOffer)).getOrThrow()
            val answer = pc.createAnswer().getOrThrow()
            pc.setLocalDescription(answer).getOrThrow()
            signalingClient.send(SignalingCommand.ANSWER, peerId, answer.description)
        } catch (e: Exception) {
            logger.e { "answer fail: ${e.message}" }
            runCatching { signalingClient.send(SignalingCommand.REJECT, peerId, "") }
            end("Hata: ${e.message?.take(60) ?: e.javaClass.simpleName}")
        }
    }

    private fun startOutgoingTimeout(peerId: String) {
        outgoingTimeoutJob?.cancel()
        outgoingTimeoutJob = scope.launch {
            delay(30_000L)
            val s = _state.value
            if (s is State.Outgoing && s.peerId == peerId) {
                runCatching { signalingClient.send(SignalingCommand.END, peerId, "") }
                end("Cevap yok")
            }
        }
    }

    private suspend fun handleIncomingSignal(ev: SignalingEvent) {
        when (ev.command) {
            SignalingCommand.OFFER -> handleRemoteOffer(ev.peerId, ev.callerUsername, ev.payload)
            SignalingCommand.ANSWER -> handleRemoteAnswer(ev.peerId, ev.payload)
            SignalingCommand.ICE -> handleRemoteIce(ev.peerId, ev.payload)
            SignalingCommand.REJECT -> {
                val s = _state.value
                if (s is State.Outgoing && s.peerId == ev.peerId) end("Karşı taraf reddetti")
            }
            SignalingCommand.END -> {
                val s = _state.value
                val peerId = currentPeerId()
                if (peerId == ev.peerId) end("Karşı taraf kapattı")
            }
        }
    }

    private fun handleRemoteOffer(callerId: String, callerUsername: String, sdpOffer: String) {
        when (val s = _state.value) {
            is State.Idle, is State.Ended ->
                _state.value = State.Incoming(callerId, callerUsername, sdpOffer)
            is State.Outgoing -> {
                // Glare resolution — küçük ID kazanır
                if (callerId < myUserId) {
                    logger.i { "glare: peer wins, accepting incoming" }
                    scope.launch {
                        runCatching { signalingClient.send(SignalingCommand.END, s.peerId, "") }
                        cleanup()
                        _state.value = State.Incoming(callerId, callerUsername, sdpOffer)
                    }
                } else {
                    logger.i { "glare: we win, rejecting peer" }
                    scope.launch { runCatching { signalingClient.send(SignalingCommand.REJECT, callerId, "") } }
                }
            }
            else -> scope.launch { runCatching { signalingClient.send(SignalingCommand.REJECT, callerId, "") } }
        }
    }

    private suspend fun handleRemoteAnswer(answererId: String, sdpAnswer: String) {
        val outgoing = _state.value as? State.Outgoing ?: return
        if (outgoing.peerId != answererId) return
        outgoingTimeoutJob?.cancel()
        _state.value = State.Connecting(outgoing.peerId, outgoing.peerUsername)
        peerConnection?.setRemoteDescription(SessionDescription(SessionDescription.Type.ANSWER, sdpAnswer))
    }

    private suspend fun handleRemoteIce(fromUserId: String, candidatePayload: String) {
        val parts = candidatePayload.split(ICE_SEPARATOR)
        if (parts.size != 3) return
        val candidate = IceCandidate(parts[0], parts[1].toIntOrNull() ?: 0, parts[2])
        peerConnection?.addIceCandidate(candidate)
    }

    // ──────────────────── WebRTC plumbing ────────────────────

    private suspend fun ensureIceServers(): List<PeerConnection.IceServer> {
        // İlk kez TURN creds fetch — sonraki çağrılarda cache'lenir (token süresi içinde geçerli).
        val r = Apis.call(accessToken).getTurnCredentials()
        if (r is ApiResult.Success && r.value.urls.isNotEmpty()) {
            iceServersLoaded = true
            return r.value.urls.map { url ->
                PeerConnection.IceServer.builder(url)
                    .setUsername(r.value.username)
                    .setPassword(r.value.credential)
                    .createIceServer()
            }
        }
        logger.w { "TURN creds unavailable — Google STUN fallback" }
        return listOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
    }

    private fun createPeer(
        iceServers: List<PeerConnection.IceServer>,
        peerId: String,
        type: StreamPeerType,
    ): StreamPeerConnection {
        val cfg = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        return peerConnectionFactory.makePeerConnection(
            coroutineScope = scope,
            configuration = cfg,
            type = type,
            mediaConstraints = mediaConstraints,
            onIceCandidateRequest = { iceCandidate, _ ->
                val payload = "${iceCandidate.sdpMid}$ICE_SEPARATOR${iceCandidate.sdpMLineIndex}$ICE_SEPARATOR${iceCandidate.sdp}"
                scope.launch { runCatching { signalingClient.send(SignalingCommand.ICE, peerId, payload) } }
            },
            onAudioTrack = {
                logger.i { "remote audio track attached" }
                onIceCallbackConnected()
            },
        )
    }

    private fun attachLocalAudio(pc: StreamPeerConnection) {
        val source = peerConnectionFactory.makeAudioSource(audioConstraints)
        audioSource = source
        val track = peerConnectionFactory.makeAudioTrack(source, "audio${UUID.randomUUID()}")
        track.setEnabled(true)
        localAudioTrack = track
        pc.connection.addTrack(track, listOf("stream0"))
    }

    /**
     * StreamPeerConnection ICE state CONNECTED olunca tetiklensin diye buraya hook.
     * Şu an audio track add edilince çağrılıyor (proxy). State Connected'a geçer.
     */
    private fun onIceCallbackConnected() {
        val cur = _state.value
        val (pid, uname) = when (cur) {
            is State.Connecting -> cur.peerId to cur.peerUsername
            is State.Outgoing -> cur.peerId to cur.peerUsername
            else -> return
        }
        _state.value = State.Connected(pid, uname, System.currentTimeMillis())
    }

    private fun setupAudioRouting() {
        val am = context.getSystemService<AudioManager>() ?: return
        am.mode = AudioManager.MODE_IN_COMMUNICATION
        am.isSpeakerphoneOn = true   // Default speakerphone; bluetooth/earpiece switch sonraki sprint
    }

    private fun end(reason: String) {
        scope.launch {
            cleanup()
            _state.value = State.Ended(reason)
            delay(2000)
            if (_state.value is State.Ended) _state.value = State.Idle
        }
    }

    private fun cleanup() {
        outgoingTimeoutJob?.cancel()
        outgoingTimeoutJob = null
        runCatching { localAudioTrack?.dispose() }
        runCatching { audioSource?.dispose() }
        localAudioTrack = null
        audioSource = null
        runCatching { peerConnection?.connection?.close() }
        peerConnection = null
        runCatching {
            context.getSystemService<AudioManager>()?.let {
                it.mode = AudioManager.MODE_NORMAL
                it.isSpeakerphoneOn = false
            }
        }
    }

    private fun currentPeerId(): String? = when (val s = _state.value) {
        is State.Outgoing -> s.peerId
        is State.Incoming -> s.peerId
        is State.Connecting -> s.peerId
        is State.Connected -> s.peerId
        else -> null
    }
}
