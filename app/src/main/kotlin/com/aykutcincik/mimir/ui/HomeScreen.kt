package com.aykutcincik.mimir.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Iskelet placeholder. Sprint #3 sonu / Sprint #4 başlangıcı: feed + DM buradan dallanır.
 */
@Composable
fun HomeScreen(
    username: String,
    isAdmin: Boolean,
    onLogout: () -> Unit,
    onOpenAdmin: (() -> Unit)? = null,
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
                label = { Text("Admin Paneli") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
            )
        }
        Spacer(Modifier.height(40.dp))
        Text(
            text = "Feed + DM yakında.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))
        if (isAdmin && onOpenAdmin != null) {
            Button(onClick = onOpenAdmin) { Text("Admin Paneli") }
            Spacer(Modifier.height(12.dp))
        }
        OutlinedButton(onClick = onLogout) { Text("Çıkış") }
    }
}
