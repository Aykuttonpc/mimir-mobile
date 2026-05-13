package com.aykutcincik.mimir.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aykutcincik.mimir.data.ApiResult
import com.aykutcincik.mimir.data.ConversationDto
import com.aykutcincik.mimir.ui.components.AnimatedListItem
import com.aykutcincik.mimir.ui.components.MimirAvatar
import com.aykutcincik.mimir.ui.components.MimirTopBar
import com.aykutcincik.mimir.ui.components.ShimmerConversationCard
import com.aykutcincik.mimir.ui.components.rememberMountedState
import kotlinx.coroutines.launch

@Composable
fun ChatListScreen(
    accessToken: String,
    onOpenChat: (conversationId: String, displayName: String, isGroup: Boolean) -> Unit,
    onNewChat: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val api = remember(accessToken) { com.aykutcincik.mimir.Apis.messaging(accessToken) }
    val mounted = rememberMountedState()

    val items = remember { mutableStateListOf<ConversationDto>() }
    var loading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }

    val filtered by remember(searchQuery) {
        derivedStateOf {
            val q = searchQuery.trim().lowercase()
            if (q.isBlank()) items.toList()
            else items.filter {
                it.displayName.lowercase().contains(q) ||
                (it.lastMessageContent?.lowercase()?.contains(q) == true)
            }
        }
    }

    suspend fun reload() {
        loading = true
        errorText = null
        when (val r = api.listConversations()) {
            is ApiResult.Success -> { items.clear(); items.addAll(r.value) }
            is ApiResult.Error -> errorText = "Liste alınamadı (HTTP ${r.code})"
            is ApiResult.Failure -> errorText = "Bağlantı hatası: ${r.cause.message}"
        }
        loading = false
    }

    LaunchedEffect(Unit) { reload() }

    Scaffold(
        topBar = {
            MimirTopBar(
                title = if (searchOpen) "" else "Mesajlar",
                actions = {
                    if (searchOpen) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Mesajlarda ara…") },
                            singleLine = true,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(width = 240.dp, height = 50.dp),
                            shape = MaterialTheme.shapes.large,
                            colors = OutlinedTextFieldDefaults.colors(),
                        )
                        IconButton(onClick = { searchQuery = ""; searchOpen = false }) {
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
                    onClick = onNewChat,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Yeni sohbet") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (errorText != null) {
                Text(
                    text = errorText!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(20.dp),
                )
            }
            if (items.isEmpty() && loading) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(6) { ShimmerConversationCard() }
                }
                return@Column
            }
            if (items.isEmpty() && !loading && errorText == null) {
                EmptyState()
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
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(filtered, key = { _, it -> it.id }) { idx, c ->
                    AnimatedListItem(visible = mounted.value, delayMillis = (idx * 30).coerceAtMost(300)) {
                        ConversationCard(
                            c = c,
                            onClick = { onOpenChat(c.id, c.displayName, c.isGroup) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationCard(c: ConversationDto, onClick: () -> Unit) {
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
            if (c.isGroup) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(28.dp),
                    )
                }
            } else {
                MimirAvatar(username = c.displayName, size = 48.dp)
            }
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (c.isGroup) c.displayName else "@${c.displayName}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val preview = c.lastMessageContent ?: "(mesaj yok)"
                Text(
                    text = if (c.lastMessageFromMe) "Sen: $preview" else preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (c.isGroup) {
                    Text(
                        text = "${c.memberCount} üye",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (c.unreadCount > 0) {
                Spacer(Modifier.size(8.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (c.unreadCount > 99) "99+" else c.unreadCount.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.ChatBubbleOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(72.dp),
            )
            Spacer(Modifier.size(16.dp))
            Text(
                text = "Henüz sohbet yok",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.size(4.dp))
            Text(
                text = "Yeni sohbet başlatmak için + butonu",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
