package com.aykutcincik.mimir.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.aykutcincik.mimir.ui.components.MimirPrimaryButton
import com.aykutcincik.mimir.ui.components.MimirTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aykutcincik.mimir.Apis
import com.aykutcincik.mimir.data.ApiResult
import kotlinx.coroutines.launch

@Composable
fun AddFriendScreen(
    accessToken: String,
    onBack: () -> Unit,
    onRequestSent: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val api = remember(accessToken) { Apis.friends(accessToken) }

    var key by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var successText by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { MimirTopBar(title = "Arkadaş Ekle", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                text = "Arkadaşlık Anahtarı",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Eklemek istediğin kişinin sana paylaştığı 12 karakterli anahtarı yapıştır. Onun onayından sonra mesajlaşabilirsin.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = key,
                onValueChange = { key = it.trim(); errorText = null; successText = null },
                label = { Text("Arkadaşlık anahtarı") },
                singleLine = true,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
            )

            if (errorText != null) {
                Spacer(Modifier.height(12.dp))
                Text(errorText!!, color = MaterialTheme.colorScheme.error)
            }
            if (successText != null) {
                Spacer(Modifier.height(12.dp))
                Text(successText!!, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(24.dp))
            MimirPrimaryButton(
                text = if (loading) "Gönderiliyor..." else "İstek Gönder",
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading,
                onClick = {
                    val k = key.trim()
                    if (k.length < 6) { errorText = "Geçerli bir anahtar gir."; return@MimirPrimaryButton }
                    loading = true
                    errorText = null
                    successText = null
                    scope.launch {
                        when (val r = api.sendRequest(k)) {
                            is ApiResult.Success -> {
                                successText = "İstek gönderildi: @${r.value.otherUsername} (onayını bekliyor)"
                                key = ""
                                kotlinx.coroutines.delay(1200)
                                onRequestSent()
                            }
                            is ApiResult.Error -> {
                                errorText = when (r.errorKey) {
                                    "friend_key_not_found" -> "Bu anahtara sahip kullanıcı bulunamadı."
                                    "cannot_friend_self" -> "Kendi anahtarınla istek gönderemezsin."
                                    "already_friends" -> "Bu kişi zaten arkadaşın."
                                    "request_already_pending" -> "Zaten bekleyen bir isteğin var."
                                    else -> "İstek gönderilemedi (HTTP ${r.code}${r.errorKey?.let { " — $it" } ?: ""})."
                                }
                            }
                            is ApiResult.Failure -> errorText = "Bağlantı hatası: ${r.cause.message}"
                        }
                        loading = false
                    }
                },
            )
        }
    }
}
