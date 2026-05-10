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

    const val CHANNEL_ID_CALLS = "mimir_calls"
    private const val CHANNEL_CALLS_NAME = "Sesli Aramalar"
    private const val CHANNEL_CALLS_DESC = "Gelen sesli arama bildirimleri"

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                    description = CHANNEL_DESC
                }
            )
        }
        if (nm.getNotificationChannel(CHANNEL_ID_CALLS) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID_CALLS, CHANNEL_CALLS_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                    description = CHANNEL_CALLS_DESC
                    setSound(
                        android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE),
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                }
            )
        }
    }

    fun showIncomingCall(ctx: Context, callerUserId: String, callerUsername: String) {
        ensureChannel(ctx)
        val openIntent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("incoming_call_from", callerUserId)
        }
        val pi = PendingIntent.getActivity(
            ctx, callerUserId.hashCode() xor 0x42, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID_CALLS)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle("Mimir gelen arama")
            .setContentText("@$callerUsername seni arıyor")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(pi, true)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        runCatching {
            NotificationManagerCompat.from(ctx).notify(callerUserId.hashCode() xor 0x42, notif)
        }
    }

    // ADR-017: 2-aşamalı bildirim. Title = senderUsername (FCM payload'dan, hızlı),
    // text = mesaj önizlemesi (mobile API'den çekildikten sonra update).
    // İçerik FCM'e gitmediği için Google sadece "Mimir push var" görür, mesajı görmez.
    fun showNewMessage(
        ctx: Context,
        senderUserId: String,
        senderUsername: String,
        contentPreview: String? = null,
    ) {
        ensureChannel(ctx)

        val openIntent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_PEER_USER_ID, senderUserId)
        }
        val pi = PendingIntent.getActivity(
            ctx, senderUserId.hashCode(), openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = senderUsername.ifBlank { "Mimir" }
        val text = contentPreview?.takeIf { it.isNotBlank() } ?: "Yeni mesajın var"

        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        // Aynı sender'dan tekrar gelirse aynı id ile üzerine yaz (spam kuyruğu yok).
        runCatching {
            NotificationManagerCompat.from(ctx).notify(senderUserId.hashCode(), notif)
        }
    }

    const val EXTRA_OPEN_PEER_USER_ID = "open_peer_user_id"
}
