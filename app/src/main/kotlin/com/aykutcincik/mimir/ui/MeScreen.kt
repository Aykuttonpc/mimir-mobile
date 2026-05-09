package com.aykutcincik.mimir.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.aykutcincik.mimir.ui.components.MimirSecondaryButton
import com.aykutcincik.mimir.ui.components.MimirTopBar
import com.aykutcincik.mimir.ui.theme.ThemeMode
import com.aykutcincik.mimir.ui.theme.ThemePreference
import kotlinx.coroutines.launch

@Composable
fun MeScreen(
    accessToken: String,
    onBack: () -> Unit,
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

    Scaffold(topBar = { MimirTopBar(title = "Profilim", onBack = onBack) }) { padding ->
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

            // Profile header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MimirAvatar(username = u.username, size = 72.dp)
                    Spacer(Modifier.size(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "@${u.username}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = u.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        if (u.isAdmin) {
                            Spacer(Modifier.height(6.dp))
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.tertiary,
                            ) {
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
                    Text(
                        text = "Arkadaşlık Anahtarın",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
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

            // Theme selector ⭐
            MimirCard {
                Column {
                    Text(
                        text = "Görünüm",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Tema modunu seç",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeChip(
                            label = "Sistem",
                            icon = Icons.Filled.AutoAwesome,
                            selected = themeMode == ThemeMode.System,
                            onClick = { scope.launch { themePref.set(ThemeMode.System) } },
                        )
                        ThemeChip(
                            label = "Açık",
                            icon = Icons.Filled.LightMode,
                            selected = themeMode == ThemeMode.Light,
                            onClick = { scope.launch { themePref.set(ThemeMode.Light) } },
                        )
                        ThemeChip(
                            label = "Koyu",
                            icon = Icons.Filled.DarkMode,
                            selected = themeMode == ThemeMode.Dark,
                            onClick = { scope.launch { themePref.set(ThemeMode.Dark) } },
                        )
                    }
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
