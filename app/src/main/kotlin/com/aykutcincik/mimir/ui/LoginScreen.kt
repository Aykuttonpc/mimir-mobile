package com.aykutcincik.mimir.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.aykutcincik.mimir.data.ApiResult
import com.aykutcincik.mimir.data.AuthResponse
import com.aykutcincik.mimir.data.LoginRequest
import com.aykutcincik.mimir.data.MimirApi
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

    LaunchedEffect(Unit) { /* ileride: token storage'tan auto-login */ }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Mimir",
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Bilgelik kuyusunun başı",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(40.dp))

        OutlinedTextField(
            value = usernameOrEmail,
            onValueChange = { usernameOrEmail = it; errorText = null },
            label = { Text("Kullanıcı adı veya e-posta") },
            singleLine = true,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(0.9f),
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorText = null },
            label = { Text("Şifre") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(0.9f),
        )

        if (errorText != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = errorText!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (usernameOrEmail.isBlank() || password.isBlank()) {
                    errorText = "Tüm alanları doldur."
                    return@Button
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
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(0.9f),
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            } else {
                Text("Giriş yap")
            }
        }

        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onGoToRegister, enabled = !loading) {
            Text("Davet token'ım var, kayıt ol")
        }
    }
}
