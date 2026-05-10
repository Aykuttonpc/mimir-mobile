package com.aykutcincik.mimir.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PeopleOutline
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aykutcincik.mimir.Apis
import com.aykutcincik.mimir.data.ApiResult
import com.aykutcincik.mimir.data.FriendDto
import com.aykutcincik.mimir.realtime.RealtimeClient
import com.aykutcincik.mimir.ui.components.AnimatedListItem
import com.aykutcincik.mimir.ui.components.MimirAvatar
import com.aykutcincik.mimir.ui.components.MimirTopBar
import com.aykutcincik.mimir.ui.components.rememberMountedState
import com.aykutcincik.mimir.util.PresenceFormat
import kotlinx.coroutines.launch

@Composable
fun FriendsScreen(
    accessToken: String,
    onAddFriend: () -> Unit,
    onOpenChat: (peerId: String, peerUsername: String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val api = remember(accessToken) { Apis.friends(accessToken) }
    val realtime = remember(accessToken) { RealtimeClient(accessToken) }
    val mounted = rememberMountedState()

    val items = remember { mutableStateListOf<FriendDto>() }
    var loading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }

    suspend fun reload() {
        loading = true
        errorText = null
        when (val r = api.listFriends()) {
            is ApiResult.Success -> { items.clear(); items.addAll(r.value) }
            is ApiResult.Error -> errorText = "Liste alınamadı (HTTP ${r.code})"
            is ApiResult.Failure -> errorText = "Bağlantı hatası: ${r.cause.message}"
        }
        loading = false
    }

    LaunchedEffect(Unit) {
        reload()
        realtime.start()
        realtime.events.collect { ev ->
            if (ev is RealtimeClient.RealtimeEvent.Presence) {
                val idx = items.indexOfFirst { it.userId == ev.event.userId }
                if (idx >= 0) {
                    items[idx] = items[idx].copy(
                        isOnline = ev.event.online,
                        lastSeenAt = ev.event.lastSeenAt ?: items[idx].lastSeenAt,
                    )
                }
            }
        }
    }

    val filtered by remember(searchQuery) {
        derivedStateOf {
            val q = searchQuery.trim().lowercase()
            if (q.isBlank()) items.toList()
            else items.filter { it.username.lowercase().contains(q) }
        }
    }

    Scaffold(
        topBar = {
            MimirTopBar(
                title = if (searchOpen) "" else "Arkadaşlarım",
                actions = {
                    if (searchOpen) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Arkadaş ara…") },
                            singleLine = true,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(width = 240.dp, height = 50.dp),
                            shape = MaterialTheme.shapes.large,
                            colors = OutlinedTextFieldDefaults.colors(),
                        )
                        IconButton(onClick = {
                            searchQuery = ""; searchOpen = false
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat")
                        }
                    } else {
                        IconButton(onClick = { searchOpen = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Ara")
                        }
                        IconButton(onClick = { scope.launch { reload() } }, enabled = !loading) {
                            if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            else Icon(Icons.Default.Refresh, contentDescription = "Yenile")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (!searchOpen) {
                ExtendedFloatingActionButton(
                    onClick = onAddFriend,
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    text = { Text("Ekle") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (errorText != null) {
                Text(errorText!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(20.dp))
            }
            if (items.isEmpty() && !loading && errorText == null) {
                EmptyFriends()
                return@Column
            }
            if (filtered.isEmpty() && searchQuery.isNotBlank()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "\"$searchQuery\" bulunamadı",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                return@Column
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(filtered, key = { _, it -> it.userId }) { idx, f ->
                    AnimatedListItem(visible = mounted.value, delayMillis = (idx * 30).coerceAtMost(300)) {
                        FriendCard(f = f, onClick = { onOpenChat(f.userId, f.username) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendCard(f: FriendDto, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MimirAvatar(username = f.username, size = 48.dp, online = f.isOnline)
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("@${f.username}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = PresenceFormat.statusLabel(f.isOnline, f.lastSeenAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (f.isOnline) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyFriends() {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.PeopleOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(72.dp),
            )
            Spacer(Modifier.size(16.dp))
            Text("Henüz arkadaşın yok", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(4.dp))
            Text(
                text = "Anahtar paylaşıp birini ekle",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
