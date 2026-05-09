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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aykutcincik.mimir.Apis
import com.aykutcincik.mimir.data.ApiResult
import com.aykutcincik.mimir.data.MeDto
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeScreen(
    accessToken: String,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val api = remember(accessToken) { Apis.friends(accessToken) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profilim") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }
            if (errorText != null) {
                Text(errorText!!, color = MaterialTheme.colorScheme.error)
                return@Column
            }
            val u = me ?: return@Column

            Text(
                text = "@${u.username}",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(u.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            u.phone?.let {
                Text("📞 $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (u.isAdmin) {
                Spacer(Modifier.height(4.dp))
                Text("Admin", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
            }
            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            // Friend key card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Arkadaşlık Anahtarın",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Bu anahtarı sadece tanıdıklarınla paylaş. Onlar Mimir'e girer + bu anahtarla arkadaş isteği gönderir.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .padding(12.dp),
                    ) {
                        Text(
                            text = u.friendKey ?: "(henüz üretilmedi)",
                            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row {
                        OutlinedButton(
                            onClick = {
                                u.friendKey?.let {
                                    clipboard.setText(AnnotatedString(it))
                                    Toast.makeText(ctx, "Anahtar kopyalandı", Toast.LENGTH_SHORT).show()
                                }
                            },
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("  Kopyala")
                        }
                        Spacer(Modifier.size(8.dp))
                        OutlinedButton(
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
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("  Paylaş")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
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
                        enabled = !regenerating,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (regenerating) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("  Yeni Anahtar Üret")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Eski anahtar iptal olur. Önceden paylaştığın kişiler artık eski anahtarla istek gönderemez.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
