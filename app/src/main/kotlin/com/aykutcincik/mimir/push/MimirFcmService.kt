package com.aykutcincik.mimir.push

import android.util.Log
import com.aykutcincik.mimir.Apis
import com.aykutcincik.mimir.data.ApiResult
import com.aykutcincik.mimir.data.DataStoreTokenStorage
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// ADR-017: FCM signal-only push. 2-aşamalı bildirim:
//   1. FCM payload'dan title (senderUsername) ile hızlı bildirim göster
//   2. Mimir API'den son mesaj içeriğini çek, aynı bildirim id'si ile zenginleştir
class MimirFcmService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val type = data["type"] ?: return

        if (type == "callOffer") {
            val callerUserId = data["callerUserId"] ?: return
            val callerUsername = data["callerUsername"] ?: ""
            val sdpOffer = data["sdpOffer"] ?: ""

            // FCM payload'daki SDP offer'ı CallManager'a doğrudan enjekte et —
            // SignalR connection olmasa bile state Incoming'e geçer, app uyandığında
            // CallScreen otomatik açılır.
            if (sdpOffer.isNotBlank()) {
                com.aykutcincik.mimir.call.CallManager.injectIncomingOffer(
                    applicationContext, callerUserId, callerUsername, sdpOffer
                )
            }

            // Notification — CallManager Incoming/Connecting/Connected ise tekrar gösterme
            val callState = com.aykutcincik.mimir.call.CallManager.state.value
            val isAlreadyHandling = callState is com.aykutcincik.mimir.call.CallManager.CallState.Incoming ||
                    callState is com.aykutcincik.mimir.call.CallManager.CallState.Connecting ||
                    callState is com.aykutcincik.mimir.call.CallManager.CallState.Connected
            if (!isAlreadyHandling || sdpOffer.isBlank()) {
                Notifications.showIncomingCall(applicationContext, callerUserId, callerUsername)
            }
            return
        }

        if (type != "newMessage") return

        val senderUserId = data["senderUserId"] ?: return
        val senderUsername = data["senderUsername"] ?: ""

        // 1. Hızlı bildirim — username var, içerik henüz yok
        Notifications.showNewMessage(applicationContext, senderUserId, senderUsername, contentPreview = null)

        // 2. API'den son mesajı çek, bildirimi zenginleştir
        scope.launch {
            val storage = DataStoreTokenStorage(applicationContext)
            val saved = storage.load() ?: return@launch
            val api = Apis.messaging(saved.accessToken)
            val result = runCatching { api.messagesWith(senderUserId, limit = 1) }.getOrNull()
            if (result is ApiResult.Success) {
                // messagesWith kronolojik (eski → yeni) reverse dönüyor; son mesaj listede last()
                val last = result.value.lastOrNull()?.content
                if (!last.isNullOrBlank()) {
                    Notifications.showNewMessage(applicationContext, senderUserId, senderUsername, last)
                }
            }
        }
    }

    override fun onNewToken(token: String) {
        scope.launch {
            val storage = DataStoreTokenStorage(applicationContext)
            val saved = storage.load() ?: return@launch
            runCatching {
                Apis.push(saved.accessToken).registerDevice(token)
            }.onFailure {
                Log.w("MimirFcm", "registerDevice fail", it)
            }
        }
    }
}
