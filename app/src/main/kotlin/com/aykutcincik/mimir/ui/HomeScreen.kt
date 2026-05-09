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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.aykutcincik.mimir.ui.components.AnimatedListItem
import com.aykutcincik.mimir.ui.components.MimirAvatar
import com.aykutcincik.mimir.ui.components.MimirHeroGradient
import com.aykutcincik.mimir.ui.components.MimirSecondaryButton
import com.aykutcincik.mimir.ui.components.MimirTextLink
import com.aykutcincik.mimir.ui.components.MimirTopBar
import com.aykutcincik.mimir.ui.components.rememberMountedState

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
    val mounted = rememberMountedState()

    LaunchedEffect(Unit) {
        when (val r = api.listPendingRequests()) {
            is ApiResult.Success -> pendingCount = r.value.count { it.direction == "Incoming" }
            else -> {}
        }
    }

    Scaffold(topBar = { MimirTopBar(title = "Mimir") }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // Gradient hero with avatar
            MimirHeroGradient(
                title = "Hoş geldin",
                subtitle = "@$username",
                trailing = { MimirAvatar(username = username, size = 56.dp, ring = true) },
            )

            Spacer(Modifier.height(28.dp))

            val tiles = buildList {
                add(Triple(Icons.AutoMirrored.Filled.Chat, "Mesajlar", onOpenMessages))
                add(Triple(Icons.Filled.People, "Arkadaşlarım", onOpenFriends))
                add(Triple(Icons.Filled.Notifications, "İstekler", onOpenRequests))
                add(Triple(Icons.Filled.Person, "Profilim", onOpenMe))
                if (isAdmin && onOpenAdmin != null) add(Triple(Icons.Filled.AdminPanelSettings, "Admin Paneli", onOpenAdmin))
            }

            tiles.forEachIndexed { i, (icon, label, action) ->
                AnimatedListItem(visible = mounted.value, delayMillis = i * 60) {
                    HomeTile(
                        icon = icon,
                        label = label,
                        badge = if (label == "İstekler") pendingCount else 0,
                        onClick = action,
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MimirTextLink(text = "Şifre değiştir", onClick = onChangePassword)
                MimirSecondaryButton(text = "Çıkış", onClick = onLogout)
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun HomeTile(
    icon: ImageVector,
    label: String,
    badge: Int = 0,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.size(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (badge > 0) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (badge > 99) "99+" else badge.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onError,
                    )
                }
            }
        }
    }
}
