package com.aykutcincik.mimir.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aykutcincik.mimir.data.ApiResult
import com.aykutcincik.mimir.ui.components.MimirPrimaryButton
import com.aykutcincik.mimir.ui.components.MimirTextField
import com.aykutcincik.mimir.ui.components.MimirTopBar
import kotlinx.coroutines.launch

@Composable
fun ChangePasswordScreen(
    accessToken: String,
    onBack: () -> Unit,
    onSuccessRequireRelogin: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val api = remember { com.aykutcincik.mimir.Apis.mimir() }

    var currentPwd by remember { mutableStateOf("") }
    var newPwd by remember { mutableStateOf("") }
    var newPwdConfirm by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var successText by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { MimirTopBar(title = "Şifre değiştir", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            MimirTextField(
                value = currentPwd,
                onValueChange = { currentPwd = it; errorText = null },
                label = "Mevcut şifre",
                isPassword = true,
                enabled = !loading,
            )
            MimirTextField(
                value = newPwd,
                onValueChange = { newPwd = it; errorText = null },
                label = "Yeni şifre (en az 8 karakter)",
                isPassword = true,
                enabled = !loading,
            )
            MimirTextField(
                value = newPwdConfirm,
                onValueChange = { newPwdConfirm = it; errorText = null },
                label = "Yeni şifre (tekrar)",
                isPassword = true,
                enabled = !loading,
            )

            if (errorText != null) {
                Text(errorText!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            if (successText != null) {
                Text(successText!!, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(8.dp))
            MimirPrimaryButton(
                text = if (loading) "Değiştiriliyor..." else "Değiştir",
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading,
                onClick = {
                    when {
                        currentPwd.isBlank() || newPwd.isBlank() || newPwdConfirm.isBlank() ->
                            errorText = "Tüm alanları doldur."
                        newPwd.length < 8 -> errorText = "Yeni şifre en az 8 karakter olmalı."
                        newPwd != newPwdConfirm -> errorText = "Yeni şifreler eşleşmiyor."
                        newPwd == currentPwd -> errorText = "Yeni şifre eskisiyle aynı olamaz."
                        else -> {
                            loading = true
                            errorText = null
                            successText = null
                            scope.launch {
                                when (val r = api.changePassword(accessToken, currentPwd, newPwd)) {
                                    is ApiResult.Success -> {
                                        successText = "Şifre değiştirildi. Tekrar giriş yapman gerekiyor."
                                        currentPwd = ""; newPwd = ""; newPwdConfirm = ""
                                        kotlinx.coroutines.delay(1500)
                                        onSuccessRequireRelogin()
                                    }
                                    is ApiResult.Error -> {
                                        errorText = when (r.errorKey) {
                                            "current_password_incorrect" -> "Mevcut şifre yanlış."
                                            "new_password_same_as_current" -> "Yeni şifre eskisiyle aynı olamaz."
                                            else -> "İşlem başarısız (HTTP ${r.code}${r.errorKey?.let { " — $it" } ?: ""})."
                                        }
                                    }
                                    is ApiResult.Failure -> errorText = "Bağlantı hatası: ${r.cause.message}"
                                }
                                loading = false
                            }
                        }
                    }
                },
            )

            Text(
                text = "Şifre değişiminden sonra tüm cihazlarındaki oturumlar sonlanır.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
