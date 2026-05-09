package com.aykutcincik.mimir.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.aykutcincik.mimir.ui.components.MimirPrimaryButton
import com.aykutcincik.mimir.ui.components.MimirSecondaryButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aykutcincik.mimir.update.ApkInstaller
import kotlinx.coroutines.launch

@Composable
fun ForceUpdateScreen(
    currentVersion: String,
    minVersion: String,
    downloadUrl: String,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val installer = remember { ApkInstaller(ctx) }

    var phase: UpdatePhase by remember { mutableStateOf(UpdatePhase.Idle) }
    var permissionRequested by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.Upgrade,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Yeni sürüm gerekli",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Mevcut: $currentVersion\nGerekli: en az $minVersion",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        when (phase) {
            UpdatePhase.Idle -> {
                if (downloadUrl.isBlank()) {
                    Text(
                        text = "İndirme bağlantısı tanımlı değil. Admin'le iletişime geç.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    MimirPrimaryButton(
                        text = "Güncelle",
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Download, contentDescription = null); Spacer(Modifier.size(8.dp)) },
                        onClick = {
                            if (!installer.isInstallPermitted()) {
                                installer.openInstallPermissionSettings()
                                permissionRequested = true
                                Toast.makeText(
                                    ctx,
                                    "İzin verdikten sonra Güncelle'ye tekrar bas",
                                    Toast.LENGTH_LONG
                                ).show()
                                return@MimirPrimaryButton
                            }
                            phase = UpdatePhase.Downloading
                            scope.launch {
                                val uri = installer.downloadApk(downloadUrl, minVersion)
                                if (uri != null) {
                                    phase = UpdatePhase.ReadyToInstall
                                    installer.installApk(uri)
                                } else {
                                    phase = UpdatePhase.Failed
                                }
                            }
                        },
                    )

                    if (permissionRequested) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "İzin verdiyseniz buraya tekrar bas. Vermediyseniz Settings → Mimir → Bilinmeyen kaynaklara izin ver.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    MimirSecondaryButton(
                        text = "Kurulum İzinlerini Aç",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { installer.openInstallPermissionSettings() },
                    )
                }
            }
            UpdatePhase.Downloading -> {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "İndiriliyor… (bildirim çubuğunda ilerleme görünür)",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            UpdatePhase.ReadyToInstall -> {
                Text(
                    text = "İndirme tamamlandı. Sistem yükleyici açıldı — onaylayın.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            UpdatePhase.Failed -> {
                Text(
                    text = "İndirme başarısız oldu. Bağlantını kontrol et, tekrar dene.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                MimirSecondaryButton(
                    text = "Tekrar dene",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { phase = UpdatePhase.Idle },
                )
            }
        }
    }
}

private enum class UpdatePhase { Idle, Downloading, ReadyToInstall, Failed }
