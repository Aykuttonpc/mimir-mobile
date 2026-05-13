/*
 * Port of GetStream/webrtc-in-jetpack-compose (Apache 2.0)
 * Original: io.getstream.webrtc.sample.compose.webrtc.peer.StreamPeerConnection
 *
 * Bizim port: logger değişti (TaggedLogger), audio-only kullanım (video lambdaları no-op),
 * package paths Mimir'e adapt edildi.
 */
package com.aykutcincik.mimir.call.webrtc.peer

import com.aykutcincik.mimir.call.webrtc.utils.addRtcIceCandidate
import com.aykutcincik.mimir.call.webrtc.utils.createValue
import com.aykutcincik.mimir.call.webrtc.utils.mungeCodecs
import com.aykutcincik.mimir.call.webrtc.utils.setValue
import com.aykutcincik.mimir.call.webrtc.utils.stringify
import com.aykutcincik.mimir.call.webrtc.utils.taggedLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.webrtc.CandidatePairChangeEvent
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.IceCandidateErrorEvent
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RTCStatsReport
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SessionDescription

/**
 * Wrapper around the WebRTC connection. Manages SDP exchange + ICE candidate buffering.
 */
class StreamPeerConnection(
    private val coroutineScope: CoroutineScope,
    private val type: StreamPeerType,
    private val mediaConstraints: MediaConstraints,
    private val onStreamAdded: ((MediaStream) -> Unit)? = null,
    private val onNegotiationNeeded: ((StreamPeerConnection, StreamPeerType) -> Unit)? = null,
    private val onIceCandidate: ((IceCandidate, StreamPeerType) -> Unit)? = null,
    private val onAudioTrack: (() -> Unit)? = null,
) : PeerConnection.Observer {

    private val typeTag = type.stringify()
    private val logger by taggedLogger("Mimir.Peer.$typeTag")

    lateinit var connection: PeerConnection
        private set

    private var statsJob: Job? = null
    private val pendingIceMutex = Mutex()
    private val pendingIceCandidates = mutableListOf<IceCandidate>()
    private val statsFlow: MutableStateFlow<RTCStatsReport?> = MutableStateFlow(null)

    fun initialize(peerConnection: PeerConnection) {
        this.connection = peerConnection
        logger.d { "initialize" }
    }

    suspend fun createOffer(): Result<SessionDescription> =
        createValue { connection.createOffer(it, mediaConstraints) }

    suspend fun createAnswer(): Result<SessionDescription> =
        createValue { connection.createAnswer(it, mediaConstraints) }

    suspend fun setRemoteDescription(sessionDescription: SessionDescription): Result<Unit> {
        val munged = SessionDescription(sessionDescription.type, sessionDescription.description.mungeCodecs())
        return setValue { connection.setRemoteDescription(it, munged) }.also {
            pendingIceMutex.withLock {
                pendingIceCandidates.forEach { ice ->
                    connection.addRtcIceCandidate(ice)
                }
                pendingIceCandidates.clear()
            }
        }
    }

    suspend fun setLocalDescription(sessionDescription: SessionDescription): Result<Unit> {
        val munged = SessionDescription(sessionDescription.type, sessionDescription.description.mungeCodecs())
        return setValue { connection.setLocalDescription(it, munged) }
    }

    suspend fun addIceCandidate(iceCandidate: IceCandidate): Result<Unit> {
        if (connection.remoteDescription == null) {
            logger.w { "addIceCandidate postponed (no remoteDescription)" }
            pendingIceMutex.withLock { pendingIceCandidates.add(iceCandidate) }
            return Result.failure(RuntimeException("RemoteDescription is not set"))
        }
        return connection.addRtcIceCandidate(iceCandidate)
    }

    fun getStats(): StateFlow<RTCStatsReport?> = statsFlow

    private fun observeStats() = coroutineScope.launch {
        while (isActive) {
            delay(10_000L)
            try {
                connection.getStats { statsFlow.value = it }
            } catch (_: Exception) {}
        }
    }

    // ─── Observer callbacks ───
    override fun onIceCandidate(candidate: IceCandidate?) {
        if (candidate == null) return
        onIceCandidate?.invoke(candidate, type)
    }

    override fun onAddStream(stream: MediaStream?) {
        if (stream != null) onStreamAdded?.invoke(stream)
    }

    override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
        mediaStreams?.forEach { mediaStream ->
            mediaStream.audioTracks?.forEach { remoteAudioTrack ->
                logger.i { "remote audio track added: ${remoteAudioTrack.stringify()}" }
                remoteAudioTrack.setEnabled(true)
            }
            onStreamAdded?.invoke(mediaStream)
        }
        onAudioTrack?.invoke()
    }

    override fun onRenegotiationNeeded() {
        onNegotiationNeeded?.invoke(this, type)
    }

    override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
        logger.i { "ICE: $newState" }
        when (newState) {
            PeerConnection.IceConnectionState.CLOSED,
            PeerConnection.IceConnectionState.FAILED,
            PeerConnection.IceConnectionState.DISCONNECTED -> statsJob?.cancel()
            PeerConnection.IceConnectionState.CONNECTED -> statsJob = observeStats()
            else -> Unit
        }
    }

    override fun onRemoveStream(stream: MediaStream?) = Unit
    override fun onRemoveTrack(receiver: RtpReceiver?) = Unit
    override fun onSignalingChange(newState: PeerConnection.SignalingState?) = Unit
    override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
    override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) = Unit
    override fun onIceCandidatesRemoved(iceCandidates: Array<out IceCandidate>?) = Unit
    override fun onIceCandidateError(event: IceCandidateErrorEvent?) {
        if (event != null) logger.e { "ice candidate error: ${event.stringify()}" }
    }
    override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
        logger.i { "conn state: $newState" }
    }
    override fun onSelectedCandidatePairChanged(event: CandidatePairChangeEvent?) = Unit
    override fun onDataChannel(channel: DataChannel?) = Unit
    override fun onTrack(transceiver: RtpTransceiver?) {
        logger.i { "onTrack: kind=${transceiver?.receiver?.track()?.kind()}" }
    }

    override fun toString() = "StreamPeerConnection(type=$typeTag)"
}
