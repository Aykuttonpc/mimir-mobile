package com.aykutcincik.mimir.push

import android.util.Log
import com.aykutcincik.mimir.Apis
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// ADR-017: FCM token register/unregister helper'ı. MimirApp login/logout
// path'lerinden çağrılır. Token rotate olursa MimirFcmService.onNewToken kayıt eder.
object PushRegistrar {

    suspend fun fetchToken(): String? = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            cont.resume(if (task.isSuccessful) task.result else null)
        }
    }

    suspend fun register(accessToken: String) {
        val token = fetchToken() ?: run {
            Log.w(TAG, "FCM token alınamadı — registerDevice atlandı")
            return
        }
        runCatching {
            Apis.push(accessToken).registerDevice(token)
        }.onFailure {
            Log.w(TAG, "registerDevice fail", it)
        }
    }

    suspend fun unregister(accessToken: String) {
        val token = fetchToken() ?: return
        runCatching {
            Apis.push(accessToken).unregisterDevice(token)
        }.onFailure {
            Log.w(TAG, "unregisterDevice fail", it)
        }
    }

    private const val TAG = "PushRegistrar"
}
