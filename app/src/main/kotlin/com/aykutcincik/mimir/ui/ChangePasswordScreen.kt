package com.aykutcincik.mimir.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.aykutcincik.mimir.data.ApiResult
import com.aykutcincik.mimir.data.MimirApi
import kotlinx.coroutines.launch

@Composable
fun ChangePasswordScreen(
    accessToken: String,
    onBack: () -> Unit,
    onSuccessRequireRelogin: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val api = remember { MimirApi() }

    var currentPwd by remember { mutableStateOf("") }
    var newPwd by remember { mutableStateOf("") }
    var newPwdConfirm by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var successText by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, enabled = !loading) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
            }
            Text(
                text = "Şifre değiştir",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            )
        }
        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = currentPwd,
                onValueChange = { currentPwd = it; errorText = null },
                label = { Text("Mevcut şifre") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = newPwd,
                onValueChange = { newPwd = it; errorText = null },
                label = { Text("Yeni şifre (en az 8 karakter)") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = newPwdConfirm,
                onValueChange = { newPwdConfirm = it; errorText = null },
                label = { Text("Yeni şifre (tekrar)") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
            )

            if (errorText != null) {
                Text(errorText!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            if (successText != null) {
                Text(successText!!, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            }

            Button(
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
                                    is ApiResult.Failure -> {
                                        errorText = "Bağlantı hatası: ${r.cause.message}"
                                    }
                                }
                                loading = false
                            }
                        }
                    }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (loading) CircularProgressIndicator(modifier = Modifier.height(20.dp))
                else Text("Değiştir")
            }

            Text(
                text = "Şifre değişiminden sonra tüm cihazlarındaki oturumlar sonlanır — yeni şifreyle tekrar login olman gerekir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
