package com.aykutcincik.mimir.realtime

import android.util.Log
import com.aykutcincik.mimir.data.CallAnsweredEvent
import com.aykutcincik.mimir.data.CallSimpleEvent
import com.aykutcincik.mimir.data.ConversationMemberAddedEvent
import com.aykutcincik.mimir.data.ConversationMemberRemovedEvent
import com.aykutcincik.mimir.data.ConversationReadEvent
import com.aykutcincik.mimir.data.ConversationRenamedEvent
import com.aykutcincik.mimir.data.IceCandidateEvent
import com.aykutcincik.mimir.data.IncomingCallEvent
import com.aykutcincik.mimir.data.MessageDeletedEvent
import com.aykutcincik.mimir.data.MessageDto
import com.aykutcincik.mimir.data.MessageEditedEvent
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
        data class Edited(val event: MessageEditedEvent) : RealtimeEvent
        data class Deleted(val event: MessageDeletedEvent) : RealtimeEvent
        data class Typing(val event: TypingEvent) : RealtimeEvent
        data class Presence(val event: PresenceChangedEvent) : RealtimeEvent
        // Sprint #14 — conversation events
        data class ConversationRead(val event: ConversationReadEvent) : RealtimeEvent
        data class MemberAdded(val event: ConversationMemberAddedEvent) : RealtimeEvent
        data class MemberRemoved(val event: ConversationMemberRemovedEvent) : RealtimeEvent
        data class Renamed(val event: ConversationRenamedEvent) : RealtimeEvent
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

        // Sprint #14 — conversation lifecycle events
        conn.on("ConversationRead", { ev ->
            _events.tryEmit(RealtimeEvent.ConversationRead(ev))
        }, ConversationReadEvent::class.java)

        conn.on("ConversationMemberAdded", { ev ->
            _events.tryEmit(RealtimeEvent.MemberAdded(ev))
        }, ConversationMemberAddedEvent::class.java)

        conn.on("ConversationMemberRemoved", { ev ->
            _events.tryEmit(RealtimeEvent.MemberRemoved(ev))
        }, ConversationMemberRemovedEvent::class.java)

        conn.on("ConversationRenamed", { ev ->
            _events.tryEmit(RealtimeEvent.Renamed(ev))
        }, ConversationRenamedEvent::class.java)

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

    // SignalR Java client'ında `send()` Completable döner — subscribe edilmesi şart,
    // yoksa lazy ve hiç tetiklenmez. `invoke(Void::class.java, ...)` Single döner;
    // `.blockingGet()` await + exception throw (HubException backend tarafından
    // fırlatılırsa mobile'da yakalanır).

    // Sprint #14 — conversation-scoped real-time
    suspend fun sendMessage(conversationId: String, plaintext: String) = withContext(Dispatchers.IO) {
        try { hub?.invoke(Void::class.java, "SendMessage", conversationId, plaintext)?.blockingGet() }
        catch (e: Exception) { Log.e(TAG, "sendMessage fail: ${e.message}") }
    }

    suspend fun markConversationRead(conversationId: String) = withContext(Dispatchers.IO) {
        try { hub?.invoke(Void::class.java, "MarkConversationRead", conversationId)?.blockingGet() } catch (_: Exception) {}
    }

    suspend fun sendTyping(conversationId: String, isTyping: Boolean) = withContext(Dispatchers.IO) {
        try { hub?.invoke(Void::class.java, "Typing", conversationId, isTyping)?.blockingGet() } catch (_: Exception) {}
    }

    // Sprint #12 — call signaling
    suspend fun offerCall(toUserId: String, sdpOffer: String) = withContext(Dispatchers.IO) {
        try { hub?.invoke(Void::class.java, "OfferCall", toUserId, sdpOffer)?.blockingGet() }
        catch (e: Exception) { Log.e(TAG, "offerCall fail: ${e.message}") }
    }
    suspend fun answerCall(toUserId: String, sdpAnswer: String) = withContext(Dispatchers.IO) {
        try { hub?.invoke(Void::class.java, "AnswerCall", toUserId, sdpAnswer)?.blockingGet() }
        catch (e: Exception) { Log.e(TAG, "answerCall fail: ${e.message}") }
    }
    suspend fun sendIceCandidate(toUserId: String, candidate: String) = withContext(Dispatchers.IO) {
        try { hub?.invoke(Void::class.java, "SendIceCandidate", toUserId, candidate)?.blockingGet() } catch (_: Exception) {}
    }
    suspend fun rejectCall(toUserId: String) = withContext(Dispatchers.IO) {
        try { hub?.invoke(Void::class.java, "RejectCall", toUserId)?.blockingGet() } catch (_: Exception) {}
    }
    suspend fun endCall(toUserId: String) = withContext(Dispatchers.IO) {
        try { hub?.invoke(Void::class.java, "EndCall", toUserId)?.blockingGet() } catch (_: Exception) {}
    }

    val isConnected: Boolean
        get() = hub?.connectionState == HubConnectionState.CONNECTED

    companion object {
        private const val TAG = "RealtimeClient"
    }
}
