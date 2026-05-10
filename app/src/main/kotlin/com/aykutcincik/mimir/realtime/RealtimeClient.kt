package com.aykutcincik.mimir.realtime

import android.util.Log
import com.aykutcincik.mimir.data.CallAnsweredEvent
import com.aykutcincik.mimir.data.CallSimpleEvent
import com.aykutcincik.mimir.data.IceCandidateEvent
import com.aykutcincik.mimir.data.IncomingCallEvent
import com.aykutcincik.mimir.data.MessageDeletedEvent
import com.aykutcincik.mimir.data.MessageDto
import com.aykutcincik.mimir.data.MessageEditedEvent
import com.aykutcincik.mimir.data.MessageReadEvent
import com.aykutcincik.mimir.data.MimirApi
import com.aykutcincik.mimir.data.PresenceChangedEvent
import com.aykutcincik.mimir.data.TypingEvent
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

/**
 * SignalR DM hub client (T-037). Android-only şu an (KMP refactor Sprint #6).
 * Auth: query string `?access_token=<jwt>` (backend Program.cs'te `OnMessageReceived` handler bunu alır).
 *
 * UX: ChatScreen lifecycle'ında start/stop; events → SharedFlow.
 * Bağlantı koparsa otomatik reconnect (SignalR Java client built-in).
 */
class RealtimeClient(
    private val accessToken: String,
    baseUrl: String = MimirApi.DEFAULT_BASE_URL,
) {
    private val hubUrl = "${baseUrl.trimEnd('/')}/hubs/dm?access_token=$accessToken"
    private var hub: HubConnection? = null

    private val _events = MutableSharedFlow<RealtimeEvent>(replay = 0, extraBufferCapacity = 64)
    val events: SharedFlow<RealtimeEvent> = _events.asSharedFlow()

    sealed interface RealtimeEvent {
        data class Received(val msg: MessageDto) : RealtimeEvent
        data class Sent(val msg: MessageDto) : RealtimeEvent
        data class Read(val event: MessageReadEvent) : RealtimeEvent
        data class Edited(val event: MessageEditedEvent) : RealtimeEvent
        data class Deleted(val event: MessageDeletedEvent) : RealtimeEvent
        data class Typing(val event: TypingEvent) : RealtimeEvent
        data class Presence(val event: PresenceChangedEvent) : RealtimeEvent
        // Sprint #12 voice call signaling
        data class IncomingCall(val event: IncomingCallEvent) : RealtimeEvent
        data class CallAnswered(val event: CallAnsweredEvent) : RealtimeEvent
        data class IceCandidate(val event: IceCandidateEvent) : RealtimeEvent
        data class CallRejected(val event: CallSimpleEvent) : RealtimeEvent
        data class CallEnded(val event: CallSimpleEvent) : RealtimeEvent
        data object Connected : RealtimeEvent
        data object Disconnected : RealtimeEvent
        data class Error(val cause: Throwable) : RealtimeEvent
    }

    suspend fun start(): Boolean = withContext(Dispatchers.IO) {
        if (hub?.connectionState == HubConnectionState.CONNECTED) return@withContext true

        val conn = try {
            HubConnectionBuilder.create(hubUrl).build()
        } catch (e: Exception) {
            Log.e(TAG, "Hub build failed", e)
            _events.tryEmit(RealtimeEvent.Error(e))
            return@withContext false
        }

        // Backend method names match (DmHub.cs)
        conn.on("MessageReceived", { dto ->
            _events.tryEmit(RealtimeEvent.Received(dto))
        }, MessageDto::class.java)

        conn.on("MessageSent", { dto ->
            _events.tryEmit(RealtimeEvent.Sent(dto))
        }, MessageDto::class.java)

        conn.on("MessageRead", { ev ->
            _events.tryEmit(RealtimeEvent.Read(ev))
        }, MessageReadEvent::class.java)

        conn.on("MessageEdited", { ev ->
            _events.tryEmit(RealtimeEvent.Edited(ev))
        }, MessageEditedEvent::class.java)

        conn.on("MessageDeleted", { ev ->
            _events.tryEmit(RealtimeEvent.Deleted(ev))
        }, MessageDeletedEvent::class.java)

        conn.on("Typing", { ev ->
            _events.tryEmit(RealtimeEvent.Typing(ev))
        }, TypingEvent::class.java)

        conn.on("PresenceChanged", { ev ->
            _events.tryEmit(RealtimeEvent.Presence(ev))
        }, PresenceChangedEvent::class.java)

        // Sprint #12 — call signaling events
        conn.on("IncomingCall", { ev ->
            _events.tryEmit(RealtimeEvent.IncomingCall(ev))
        }, IncomingCallEvent::class.java)

        conn.on("CallAnswered", { ev ->
            _events.tryEmit(RealtimeEvent.CallAnswered(ev))
        }, CallAnsweredEvent::class.java)

        conn.on("IceCandidate", { ev ->
            _events.tryEmit(RealtimeEvent.IceCandidate(ev))
        }, IceCandidateEvent::class.java)

        conn.on("CallRejected", { ev ->
            _events.tryEmit(RealtimeEvent.CallRejected(ev))
        }, CallSimpleEvent::class.java)

        conn.on("CallEnded", { ev ->
            _events.tryEmit(RealtimeEvent.CallEnded(ev))
        }, CallSimpleEvent::class.java)

        conn.onClosed { e ->
            Log.w(TAG, "Hub closed: ${e?.message}")
            _events.tryEmit(RealtimeEvent.Disconnected)
        }

        try {
            conn.start().blockingAwait()
            hub = conn
            _events.tryEmit(RealtimeEvent.Connected)
            Log.i(TAG, "Hub connected")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Hub start failed", e)
            _events.tryEmit(RealtimeEvent.Error(e))
            return@withContext false
        }
    }

    suspend fun stop() = withContext(Dispatchers.IO) {
        try { hub?.stop()?.blockingAwait() } catch (_: Exception) {}
        hub = null
    }

    /** Typing indicator gönder (T-033). Hata sessiz yutar — UX'i etkilemesin. */
    suspend fun sendTyping(toUserId: String, isTyping: Boolean) = withContext(Dispatchers.IO) {
        try { hub?.send("Typing", toUserId, isTyping) } catch (_: Exception) {}
    }

    // Sprint #12 — call signaling
    suspend fun offerCall(toUserId: String, sdpOffer: String) = withContext(Dispatchers.IO) {
        hub?.send("OfferCall", toUserId, sdpOffer)
    }
    suspend fun answerCall(toUserId: String, sdpAnswer: String) = withContext(Dispatchers.IO) {
        hub?.send("AnswerCall", toUserId, sdpAnswer)
    }
    suspend fun sendIceCandidate(toUserId: String, candidate: String) = withContext(Dispatchers.IO) {
        try { hub?.send("SendIceCandidate", toUserId, candidate) } catch (_: Exception) {}
    }
    suspend fun rejectCall(toUserId: String) = withContext(Dispatchers.IO) {
        try { hub?.send("RejectCall", toUserId) } catch (_: Exception) {}
    }
    suspend fun endCall(toUserId: String) = withContext(Dispatchers.IO) {
        try { hub?.send("EndCall", toUserId) } catch (_: Exception) {}
    }

    val isConnected: Boolean
        get() = hub?.connectionState == HubConnectionState.CONNECTED

    companion object {
        private const val TAG = "RealtimeClient"
    }
}
