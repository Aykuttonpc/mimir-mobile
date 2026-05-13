package com.aykutcincik.mimir.call.webrtc.signaling

import com.aykutcincik.mimir.call.webrtc.utils.taggedLogger
import com.aykutcincik.mimir.realtime.RealtimeClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Mimir SignalR DmHub'ı GetStream `SignalingClient` interface'ine adapt eder.
 *
 * Inbound: RealtimeClient.events'ten IncomingCall/CallAnswered/IceCandidate/CallRejected/CallEnded
 *          → SignalingEvent'e dönüştürüp `incoming` flow'a emit.
 * Outbound: send(OFFER/ANSWER/ICE/...) → realtime.offerCall/answerCall/sendIceCandidate/...
 *
 * Bonus: FCM payload üzerinden gelen offer'ı `injectIncomingOffer` ile aynı flow'a basıyor —
 * app dead'ken bile state machine tetiklenir.
 */
class MimirSignalingAdapter(
    private val realtime: RealtimeClient,
) : SignalingClient {

    private val logger by taggedLogger("Mimir.Signaling")
    private val scope = CoroutineScope(SupervisorJob() + EmptyCoroutineContext)

    private val _incoming = MutableSharedFlow<SignalingEvent>(replay = 0, extraBufferCapacity = 16)
    override val incoming: SharedFlow<SignalingEvent> = _incoming.asSharedFlow()

    private var collectJob: Job? = null

    init {
        start()
    }

    private fun start() {
        collectJob?.cancel()
        collectJob = scope.launch {
            realtime.events.collect { ev ->
                when (ev) {
                    is RealtimeClient.RealtimeEvent.IncomingCall ->
                        emit(SignalingCommand.OFFER, ev.event.callerId, ev.event.sdpOffer, ev.event.callerUsername)
                    is RealtimeClient.RealtimeEvent.CallAnswered ->
                        emit(SignalingCommand.ANSWER, ev.event.answererId, ev.event.sdpAnswer)
                    is RealtimeClient.RealtimeEvent.IceCandidate ->
                        emit(SignalingCommand.ICE, ev.event.fromUserId, ev.event.candidate)
                    is RealtimeClient.RealtimeEvent.CallRejected ->
                        emit(SignalingCommand.REJECT, ev.event.fromUserId, "")
                    is RealtimeClient.RealtimeEvent.CallEnded ->
                        emit(SignalingCommand.END, ev.event.fromUserId, "")
                    else -> {}
                }
            }
        }
    }

    /** FCM payload tarafından çağrılır — app dead'ken offer'ı state machine'e enjekte et. */
    fun injectIncomingOffer(callerId: String, callerUsername: String, sdpOffer: String) {
        scope.launch {
            emit(SignalingCommand.OFFER, callerId, sdpOffer, callerUsername)
        }
    }

    private suspend fun emit(command: SignalingCommand, peerId: String, payload: String, callerUsername: String = "") {
        _incoming.emit(SignalingEvent(command, peerId, payload, callerUsername))
        logger.d { "← $command from=$peerId (len=${payload.length})" }
    }

    override suspend fun send(command: SignalingCommand, peerId: String, payload: String) {
        logger.d { "→ $command to=$peerId (len=${payload.length})" }
        when (command) {
            SignalingCommand.OFFER -> realtime.offerCall(peerId, payload)
            SignalingCommand.ANSWER -> realtime.answerCall(peerId, payload)
            SignalingCommand.ICE -> realtime.sendIceCandidate(peerId, payload)
            SignalingCommand.REJECT -> realtime.rejectCall(peerId)
            SignalingCommand.END -> realtime.endCall(peerId)
        }
    }

    override val isConnected: Boolean
        get() = realtime.isConnected

    override fun dispose() {
        collectJob?.cancel()
        scope.cancel()
    }
}
