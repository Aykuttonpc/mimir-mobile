package com.aykutcincik.mimir.realtime

import android.util.Log
import com.aykutcincik.mimir.data.MessageDto
import com.aykutcincik.mimir.data.MessageReadEvent
import com.aykutcincik.mimir.data.MimirApi
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

    val isConnected: Boolean
        get() = hub?.connectionState == HubConnectionState.CONNECTED

    companion object {
        private const val TAG = "RealtimeClient"
    }
}
