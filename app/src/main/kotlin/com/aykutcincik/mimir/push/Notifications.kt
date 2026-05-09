package com.aykutcincik.mimir.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aykutcincik.mimir.MainActivity

object Notifications {
    const val CHANNEL_ID = "mimir_messages"
    private const val CHANNEL_NAME = "Mesajlar"
    private const val CHANNEL_DESC = "Yeni DM bildirimleri"

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = CHANNEL_DESC
            }
        )
    }

    // ADR-017: signal-only — içerik FCM'den gelmediği için generic mesaj.
    // Tıklayınca MainActivity açılır, kullanıcı normal akışıyla ChatList → Chat'e gider.
    fun showNewMessage(ctx: Context, senderUserId: String) {
        ensureChannel(ctx)

        val openIntent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_PEER_USER_ID, senderUserId)
        }
        val pi = PendingIntent.getActivity(
            ctx, senderUserId.hashCode(), openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("Mimir")
            .setContentText("Yeni mesajın var")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        // Aynı sender'dan tekrar gelirse aynı id ile üzerine yaz (spam kuyruğu yok).
        runCatching {
            NotificationManagerCompat.from(ctx).notify(senderUserId.hashCode(), notif)
        }
        // SecurityException olabilir (POST_NOTIFICATIONS izni Android 13+ runtime).
        // Sessiz geç — kullanıcı uygulama açtığında zaten mesajları görecek.
    }

    const val EXTRA_OPEN_PEER_USER_ID = "open_peer_user_id"
}
