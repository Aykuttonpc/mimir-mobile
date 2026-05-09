package com.aykutcincik.mimir.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.aykutcincik.mimir.Apis
import com.aykutcincik.mimir.data.ApiResult
import com.aykutcincik.mimir.data.MeDto
import com.aykutcincik.mimir.ui.components.MimirAvatar
import com.aykutcincik.mimir.ui.components.MimirCard
import com.aykutcincik.mimir.ui.components.MimirGradientPanel
import com.aykutcincik.mimir.ui.components.MimirSecondaryButton
import com.aykutcincik.mimir.ui.components.MimirTopBar
import com.aykutcincik.mimir.ui.theme.ThemeMode
import com.aykutcincik.mimir.ui.theme.ThemePreference
import kotlinx.coroutines.launch

@Composable
fun ProfileTab(
    accessToken: String,
    isAdmin: Boolean,
    onChangePassword: () -> Unit,
    onOpenAdmin: (() -> Unit)?,
    onLogout: () -> Unit,
) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val api = remember(accessToken) { Apis.friends(accessToken) }
    val themePref = remember { ThemePreference(ctx) }
    val themeMode by themePref.mode.collectAsState(initial = ThemeMode.System)

    var me by remember { mutableStateOf<MeDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var regenerating by remember { mutableStateOf(false) }

    suspend fun reload() {
        loading = true
        errorText = null
        when (val r = api.me()) {
            is ApiResult.Success -> me = r.value
            is ApiResult.Error -> errorText = "Profil alınamadı (HTTP ${r.code})"
            is ApiResult.Failure -> errorText = "Bağlantı hatası: ${r.cause.message}"
        }
        loading = false
    }

    LaunchedEffect(Unit) { reload() }

    Scaffold(topBar = { MimirTopBar(title = "Profilim") }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (loading) {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }
            if (errorText != null) {
                Text(errorText!!, color = MaterialTheme.colorScheme.error)
                return@Column
            }
            val u = me ?: return@Column

            Spacer(Modifier.height(4.dp))

            // Profile header — gradient
            MimirGradientPanel(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MimirAvatar(username = u.username, size = 72.dp, ring = true, online = true)
                    Spacer(Modifier.size(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "@${u.username}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                        )
                        Text(
                            text = u.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                        if (u.isAdmin) {
                            Spacer(Modifier.height(6.dp))
                            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.tertiary) {
                                Text(
                                    text = "ADMIN",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Friend key
            MimirCard {
                Column {
                    Text("Arkadaşlık Anahtarın", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Bu anahtarı sadece tanıdıklarınla paylaş.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = u.friendKey ?: "(henüz üretilmedi)",
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MimirSecondaryButton(
                            text = "Kopyala",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                u.friendKey?.let {
                                    clipboard.setText(AnnotatedString(it))
                                    Toast.makeText(ctx, "Anahtar kopyalandı", Toast.LENGTH_SHORT).show()
                                }
                            },
                        )
                        MimirSecondaryButton(
                            text = "Paylaş",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                u.friendKey?.let { key ->
                                    val msg = "Mimir arkadaşlık anahtarım: $key\n\nMimir'i aç → Arkadaş Ekle → bu anahtarı yapıştır."
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, msg)
                                    }
                                    ctx.startActivity(Intent.createChooser(intent, "Anahtarı paylaş"))
                                }
                            },
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    MimirSecondaryButton(
                        text = if (regenerating) "Üretiliyor..." else "Yeni Anahtar Üret",
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !regenerating,
                        onClick = {
                            regenerating = true
                            scope.launch {
                                val r = api.regenerateFriendKey()
                                if (r is ApiResult.Success) {
                                    me = u.copy(friendKey = r.value.friendKey)
                                    Toast.makeText(ctx, "Yeni anahtar üretildi", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(ctx, "Hata", Toast.LENGTH_SHORT).show()
                                }
                                regenerating = false
                            }
                        },
                    )
                }
            }

            // Theme selector
            MimirCard {
                Column {
                    Text("Görünüm", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Tema modunu seç",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeChip("Sistem", Icons.Filled.AutoAwesome, themeMode == ThemeMode.System) {
                            scope.launch { themePref.set(ThemeMode.System) }
                        }
                        ThemeChip("Açık", Icons.Filled.LightMode, themeMode == ThemeMode.Light) {
                            scope.launch { themePref.set(ThemeMode.Light) }
                        }
                        ThemeChip("Koyu", Icons.Filled.DarkMode, themeMode == ThemeMode.Dark) {
                            scope.launch { themePref.set(ThemeMode.Dark) }
                        }
                    }
                }
            }

            // Actions
            MimirCard {
                Column {
                    Text("Hesap", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    if (onOpenAdmin != null) {
                        ActionRow(Icons.Filled.AdminPanelSettings, "Admin Paneli", onOpenAdmin)
                    }
                    ActionRow(Icons.Filled.Lock, "Şifre değiştir", onChangePassword)
                    ActionRow(
                        Icons.AutoMirrored.Filled.Logout,
                        "Çıkış",
                        onLogout,
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ThemeChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
        modifier = Modifier.weight(1f),
        shape = MaterialTheme.shapes.medium,
    )
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = Color.Transparent,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = tint)
            Spacer(Modifier.size(14.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, color = tint)
        }
    }
}
