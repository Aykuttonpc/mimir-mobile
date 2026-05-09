package com.aykutcincik.mimir.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aykutcincik.mimir.data.ApiResult
import com.aykutcincik.mimir.data.RegisterRequest
import com.aykutcincik.mimir.ui.components.MimirPrimaryButton
import com.aykutcincik.mimir.ui.components.MimirTextField
import com.aykutcincik.mimir.ui.components.MimirTextLink
import com.aykutcincik.mimir.ui.components.MimirTopBar
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

    Scaffold(topBar = { MimirTopBar(title = "Kayıt ol", onBack = onBackToLogin) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Mimir kapalı bir ağ.\nDavet linkin yoksa kayıt olamazsın.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            MimirTextField(
                value = inviteToken,
                onValueChange = { inviteToken = it.trim(); errorText = null },
                label = "Davet token'ı *",
                enabled = !loading,
            )
            MimirTextField(
                value = email,
                onValueChange = { email = it.trim(); errorText = null },
                label = "E-posta *",
                keyboardType = KeyboardType.Email,
                enabled = !loading,
            )
            MimirTextField(
                value = username,
                onValueChange = { username = it.trim(); errorText = null },
                label = "Kullanıcı adı * (3-50, harf/rakam/_)",
                enabled = !loading,
            )
            MimirTextField(
                value = password,
                onValueChange = { password = it; errorText = null },
                label = "Şifre * (en az 8 karakter)",
                isPassword = true,
                enabled = !loading,
            )
            MimirTextField(
                value = passwordConfirm,
                onValueChange = { passwordConfirm = it; errorText = null },
                label = "Şifre (tekrar) *",
                isPassword = true,
                enabled = !loading,
            )
            MimirTextField(
                value = phone,
                onValueChange = { phone = it.trim(); errorText = null },
                label = "Telefon (opsiyonel)",
                keyboardType = KeyboardType.Phone,
                enabled = !loading,
            )

            if (errorText != null) {
                Text(
                    text = errorText!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(Modifier.height(8.dp))

            MimirPrimaryButton(
                text = if (loading) "Kayıt yapılıyor..." else "Kayıt ol",
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading,
                onClick = {
                    val err = validate(inviteToken, email, username, password, passwordConfirm)
                    if (err != null) { errorText = err; return@MimirPrimaryButton }
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
                            is ApiResult.Failure -> errorText = "Bağlantı hatası: ${r.cause.message}"
                        }
                        loading = false
                    }
                },
            )

            MimirTextLink(text = "Hesabın var mı? Giriş yap", onClick = onBackToLogin)

            Spacer(Modifier.height(40.dp))
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
