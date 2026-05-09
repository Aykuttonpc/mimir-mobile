package com.aykutcincik.mimir.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aykutcincik.mimir.Apis
import com.aykutcincik.mimir.data.ApiResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    accessToken: String,
    username: String,
    isAdmin: Boolean,
    onLogout: () -> Unit,
    onOpenAdmin: (() -> Unit)? = null,
    onChangePassword: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenFriends: () -> Unit,
    onOpenRequests: () -> Unit,
    onOpenMe: () -> Unit,
) {
    var pendingCount by remember { mutableIntStateOf(0) }
    val api = remember(accessToken) { Apis.friends(accessToken) }

    LaunchedEffect(Unit) {
        when (val r = api.listPendingRequests()) {
            is ApiResult.Success -> pendingCount = r.value.count { it.direction == "Incoming" }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Mimir") })
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Hoş geldin",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium),
            )
            Text(
                text = "@$username",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(40.dp))

            HomeTile(Icons.AutoMirrored.Filled.Chat, "Mesajlar", onOpenMessages)
            Spacer(Modifier.height(12.dp))
            HomeTile(Icons.Filled.People, "Arkadaşlarım", onOpenFriends)
            Spacer(Modifier.height(12.dp))
            HomeTileWithBadge(Icons.Filled.Notifications, "İstekler", pendingCount, onOpenRequests)
            Spacer(Modifier.height(12.dp))
            HomeTile(Icons.Filled.Person, "Profilim", onOpenMe)

            if (isAdmin && onOpenAdmin != null) {
                Spacer(Modifier.height(12.dp))
                HomeTile(Icons.Filled.AdminPanelSettings, "Admin Paneli", onOpenAdmin)
            }

            Spacer(Modifier.height(32.dp))
            TextButton(onClick = onChangePassword) { Text("Şifre değiştir") }
            Spacer(Modifier.height(4.dp))
            OutlinedButton(onClick = onLogout) { Text("Çıkış") }
        }
    }
}

@Composable
private fun HomeTile(icon: ImageVector, label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.size(12.dp))
        Text(label)
    }
}

@Composable
private fun HomeTileWithBadge(icon: ImageVector, label: String, badgeCount: Int, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.size(12.dp))
        Text(label, modifier = Modifier.weight(1f))
        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onError,
                )
            }
        }
    }
}
