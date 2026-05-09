package com.aykutcincik.mimir.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aykutcincik.mimir.data.ApiResult
import com.aykutcincik.mimir.data.AuthResponse
import com.aykutcincik.mimir.data.LoginRequest
import com.aykutcincik.mimir.ui.components.MimirPrimaryButton
import com.aykutcincik.mimir.ui.components.MimirTextField
import com.aykutcincik.mimir.ui.components.MimirTextLink
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoggedIn: (AuthResponse) -> Unit,
    onAccountPending: (username: String) -> Unit,
    onGoToRegister: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val api = remember { com.aykutcincik.mimir.Apis.mimir() }

    var usernameOrEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(80.dp))

        Text(
            text = "Mimir",
            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Bilgelik kuyusunun başı",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(56.dp))

        MimirTextField(
            value = usernameOrEmail,
            onValueChange = { usernameOrEmail = it; errorText = null },
            label = "Kullanıcı adı veya e-posta",
            keyboardType = KeyboardType.Email,
            enabled = !loading,
        )

        Spacer(Modifier.height(14.dp))

        MimirTextField(
            value = password,
            onValueChange = { password = it; errorText = null },
            label = "Şifre",
            isPassword = true,
            enabled = !loading,
        )

        if (errorText != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = errorText!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(Modifier.height(28.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            MimirPrimaryButton(
                text = if (loading) "Giriliyor..." else "Giriş yap",
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading,
                onClick = {
                    if (usernameOrEmail.isBlank() || password.isBlank()) {
                        errorText = "Tüm alanları doldur."
                        return@MimirPrimaryButton
                    }
                    loading = true
                    errorText = null
                    scope.launch {
                        when (val r = api.login(LoginRequest(usernameOrEmail.trim(), password))) {
                            is ApiResult.Success -> onLoggedIn(r.value)
                            is ApiResult.Error -> {
                                errorText = when (r.errorKey) {
                                    "invalid_credentials" -> "Kullanıcı adı veya şifre hatalı."
                                    "account_not_active" -> { onAccountPending(usernameOrEmail); null }
                                    else -> "Giriş başarısız (HTTP ${r.code}${r.errorKey?.let { " — $it" } ?: ""})."
                                }
                            }
                            is ApiResult.Failure -> {
                                errorText = "Bağlantı hatası: ${r.cause.message}"
                            }
                        }
                        loading = false
                    }
                },
            )
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
        MimirTextLink(text = "Davet token'ım var, kayıt ol", onClick = onGoToRegister)

        Spacer(Modifier.height(40.dp))
    }
}
