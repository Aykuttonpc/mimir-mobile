package com.aykutcincik.mimir.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * T-043: Sideload APK update — in-app download + install (Play Store yok).
 * 1. `isInstallPermitted` kontrol; gerekirse `openInstallPermissionSettings` ile kullanıcıyı yönlendir.
 * 2. `downloadApk(url, version)` — DownloadManager + notification.
 * 3. `installApk(uri)` — sistem installer dialog'u açılır.
 */
class ApkInstaller(private val context: Context) {

    private val downloadManager: DownloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    /** Android 8+ user "Bilinmeyen kaynaklara izin ver" onayı vermesi gerek. */
    fun isInstallPermitted(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()

    /** Settings → "Bilinmeyen uygulamalardan kuruluma izin ver" ekranına yönlendirir. */
    fun openInstallPermissionSettings() {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /** APK'yı external app-specific Downloads klasörüne indir. Tamamlanınca `Uri` döner, yoksa null. */
    suspend fun downloadApk(url: String, version: String): Uri? {
        val fileName = "mimir-$version.apk"
        // Eski dosyayı sil
        val target = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (target.exists()) target.delete()

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Mimir $version")
            .setDescription("Yeni sürüm indiriliyor…")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)

        val downloadId = downloadManager.enqueue(request)

        return suspendCancellableCoroutine { cont ->
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id != downloadId) return

                    val cursor = downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
                    val status = if (cursor.moveToFirst()) {
                        val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        cursor.getInt(statusIdx)
                    } else DownloadManager.STATUS_FAILED
                    cursor.close()

                    runCatching { context.unregisterReceiver(this) }

                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        val file = File(
                            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                            fileName
                        )
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        if (cont.isActive) cont.resume(uri)
                    } else {
                        if (cont.isActive) cont.resume(null)
                    }
                }
            }

            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

            cont.invokeOnCancellation {
                runCatching { context.unregisterReceiver(receiver) }
                downloadManager.remove(downloadId)
            }
        }
    }

    /** Sistemin "Bunu yüklemek istiyor musunuz?" dialog'unu açar. */
    fun installApk(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}
