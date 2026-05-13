/*
 * Port of GetStream/webrtc-in-jetpack-compose (Apache 2.0)
 * Original: io.getstream.webrtc.sample.compose.webrtc.peer.StreamPeerConnectionFactory
 *
 * Bizim port: SimulcastVideoEncoderFactory yerine HardwareVideoEncoderFactory direct,
 * audio device module hardware echo canceler + noise suppressor (audio-only kullanım).
 */
package com.aykutcincik.mimir.call.webrtc.peer

import android.content.Context
import android.os.Build
import com.aykutcincik.mimir.call.webrtc.utils.taggedLogger
import kotlinx.coroutines.CoroutineScope
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpTransceiver
import org.webrtc.audio.JavaAudioDeviceModule

class StreamPeerConnectionFactory(private val context: Context) {

    private val logger by taggedLogger("Mimir.PCFactory")
    private val audioLogger by taggedLogger("Mimir.AudioDeviceModule")

    val eglBaseContext: EglBase.Context by lazy { EglBase.create().eglBaseContext }

    private val videoDecoderFactory by lazy { DefaultVideoDecoderFactory(eglBaseContext) }
    private val videoEncoderFactory by lazy { DefaultVideoEncoderFactory(eglBaseContext, true, true) }

    /**
     * Varsayılan RTCConfiguration — STUN/TURN servers ICE candidate setup için.
     * CallSession bunu kendi config'iyle override edebilir (Mimir TURN credentials API).
     */
    val rtcConfig: PeerConnection.RTCConfiguration = PeerConnection.RTCConfiguration(
        arrayListOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
    ).apply {
        sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
    }

    private val factory: PeerConnectionFactory by lazy {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .createInitializationOptions()
        )
        PeerConnectionFactory.builder()
            .setVideoDecoderFactory(videoDecoderFactory)
            .setVideoEncoderFactory(videoEncoderFactory)
            .setAudioDeviceModule(
                JavaAudioDeviceModule.builder(context)
                    .setUseHardwareAcousticEchoCanceler(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    .setUseHardwareNoiseSuppressor(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    .setAudioRecordErrorCallback(object : JavaAudioDeviceModule.AudioRecordErrorCallback {
                        override fun onWebRtcAudioRecordInitError(msg: String?) = audioLogger.w { "rec init err: $msg" }
                        override fun onWebRtcAudioRecordStartError(c: JavaAudioDeviceModule.AudioRecordStartErrorCode?, m: String?) = audioLogger.w { "rec start err: $m" }
                        override fun onWebRtcAudioRecordError(m: String?) = audioLogger.w { "rec err: $m" }
                    })
                    .setAudioTrackErrorCallback(object : JavaAudioDeviceModule.AudioTrackErrorCallback {
                        override fun onWebRtcAudioTrackInitError(m: String?) = audioLogger.w { "trk init err: $m" }
                        override fun onWebRtcAudioTrackStartError(c: JavaAudioDeviceModule.AudioTrackStartErrorCode?, m: String?) = audioLogger.w { "trk start err: $m" }
                        override fun onWebRtcAudioTrackError(m: String?) = audioLogger.w { "trk err: $m" }
                    })
                    .createAudioDeviceModule()
                    .also {
                        it.setMicrophoneMute(false)
                        it.setSpeakerMute(false)
                    }
            )
            .createPeerConnectionFactory()
    }

    fun makePeerConnection(
        coroutineScope: CoroutineScope,
        configuration: PeerConnection.RTCConfiguration,
        type: StreamPeerType,
        mediaConstraints: MediaConstraints,
        onStreamAdded: ((MediaStream) -> Unit)? = null,
        onNegotiationNeeded: ((StreamPeerConnection, StreamPeerType) -> Unit)? = null,
        onIceCandidateRequest: ((IceCandidate, StreamPeerType) -> Unit)? = null,
        onAudioTrack: (() -> Unit)? = null,
    ): StreamPeerConnection {
        val wrapper = StreamPeerConnection(
            coroutineScope = coroutineScope,
            type = type,
            mediaConstraints = mediaConstraints,
            onStreamAdded = onStreamAdded,
            onNegotiationNeeded = onNegotiationNeeded,
            onIceCandidate = onIceCandidateRequest,
            onAudioTrack = onAudioTrack,
        )
        val pc = requireNotNull(factory.createPeerConnection(configuration, wrapper)) {
            "createPeerConnection returned null"
        }
        return wrapper.apply { initialize(pc) }
    }

    fun makeAudioSource(constraints: MediaConstraints = MediaConstraints()): AudioSource =
        factory.createAudioSource(constraints)

    fun makeAudioTrack(source: AudioSource, trackId: String): AudioTrack =
        factory.createAudioTrack(trackId, source)
}
