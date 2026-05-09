package com.aykutcincik.mimir.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Iskelet placeholder. Sprint #3-4: messaging entry, admin paneli, şifre değiştir, çıkış.
 * Sprint #4 sonu / Sprint #5: feed buradan dallanır.
 */
@Composable
fun HomeScreen(
    username: String,
    isAdmin: Boolean,
    onLogout: () -> Unit,
    onOpenAdmin: (() -> Unit)? = null,
    onChangePassword: () -> Unit,
    onOpenMessages: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Mimir'e hoş geldin",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "@$username",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        if (isAdmin) {
            Spacer(Modifier.height(8.dp))
            AssistChip(
                onClick = { onOpenAdmin?.invoke() },
                label = { Text("Admin") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
            )
        }
        Spacer(Modifier.height(40.dp))

        Button(
            onClick = onOpenMessages,
            modifier = Modifier.padding(vertical = 4.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
            Spacer(Modifier.height(0.dp))
            Text("  Mesajlar")
        }

        if (isAdmin && onOpenAdmin != null) {
            OutlinedButton(onClick = onOpenAdmin, modifier = Modifier.padding(vertical = 4.dp)) {
                Text("Admin Paneli")
            }
        }

        Spacer(Modifier.height(20.dp))
        TextButton(onClick = onChangePassword) { Text("Şifre değiştir") }
        Spacer(Modifier.height(4.dp))
        OutlinedButton(onClick = onLogout) { Text("Çıkış") }
    }
}
