package com.aykutcincik.mimir.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.aykutcincik.mimir.data.ApiResult
import com.aykutcincik.mimir.data.MimirApi
import com.aykutcincik.mimir.data.RegisterRequest
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onRegistered: (email: String) -> Unit,
    onBackToLogin: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val api = remember { com.aykutcincik.mimir.Apis.mimir() }

    var inviteToken by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        Text(
            text = "Kayıt ol",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Mimir kapalı bir ağ. Davet linkin yoksa kayıt olamazsın.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = inviteToken,
            onValueChange = { inviteToken = it.trim(); errorText = null },
            label = { Text("Davet token'ı *") },
            singleLine = true,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it.trim(); errorText = null },
            label = { Text("E-posta *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it.trim(); errorText = null },
            label = { Text("Kullanıcı adı * (3-50, harf/rakam/_)") },
            singleLine = true,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorText = null },
            label = { Text("Şifre * (en az 8 karakter)") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = passwordConfirm,
            onValueChange = { passwordConfirm = it; errorText = null },
            label = { Text("Şifre (tekrar) *") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it.trim(); errorText = null },
            label = { Text("Telefon (opsiyonel)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
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
                val err = validate(inviteToken, email, username, password, passwordConfirm)
                if (err != null) {
                    errorText = err
                    return@Button
                }
                loading = true
                errorText = null
                scope.launch {
                    val req = RegisterRequest(
                        invitationToken = inviteToken,
                        email = email,
                        username = username,
                        password = password,
                        phone = phone.ifBlank { null },
                    )
                    when (val r = api.register(req)) {
                        is ApiResult.Success -> onRegistered(email)
                        is ApiResult.Error -> {
                            errorText = when (r.errorKey) {
                                "invalid_or_expired_invitation" -> "Davet token'ı geçersiz veya süresi dolmuş."
                                "registration_failed" -> "Bu e-posta veya kullanıcı adı zaten kayıtlı."
                                else -> "Kayıt başarısız (HTTP ${r.code}${r.errorKey?.let { " — $it" } ?: ""})."
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
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            } else {
                Text("Kayıt ol")
            }
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBackToLogin, enabled = !loading) {
            Text("Hesabın var mı? Giriş yap")
        }
    }
}

private fun validate(
    invite: String,
    email: String,
    username: String,
    password: String,
    passwordConfirm: String,
): String? {
    if (invite.isBlank()) return "Davet token'ı boş olamaz."
    if (email.isBlank() || !email.contains("@") || !email.contains("."))
        return "Geçerli bir e-posta gir."
    if (username.length !in 3..50)
        return "Kullanıcı adı 3-50 karakter olmalı."
    if (!username.matches(Regex("^[a-zA-Z0-9_]+$")))
        return "Kullanıcı adı sadece harf/rakam/_ içerebilir."
    if (password.length < 8) return "Şifre en az 8 karakter olmalı."
    if (password != passwordConfirm) return "Şifreler eşleşmiyor."
    return null
}
