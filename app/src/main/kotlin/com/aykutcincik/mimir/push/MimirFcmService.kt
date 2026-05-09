package com.aykutcincik.mimir.push

import android.util.Log
import com.aykutcincik.mimir.Apis
import com.aykutcincik.mimir.data.DataStoreTokenStorage
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// ADR-017: FCM signal-only push. onMessageReceived → bildirim. onNewToken → backend kayıt.
class MimirFcmService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val type = data["type"] ?: return
        if (type != "newMessage") return

        val senderUserId = data["senderUserId"] ?: return
        Notifications.showNewMessage(applicationContext, senderUserId)
    }

    override fun onNewToken(token: String) {
        // FCM token rotate oldu (app reinstall, data clear, vs). Backend'e bildir.
        // Login state yoksa kayıt edemeyiz; kullanıcı login olunca MimirApp tekrar register eder.
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
