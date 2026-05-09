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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import com.aykutcincik.mimir.data.ApiResult
import com.aykutcincik.mimir.data.MessageDto
import com.aykutcincik.mimir.data.MessagingApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val POLLING_INTERVAL_MS = 5000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    accessToken: String,
    currentUserId: String,
    peerUserId: String,
    peerUsername: String,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val api = remember(accessToken) { MessagingApi(accessToken) }

    val messages = remember { mutableStateListOf<MessageDto>() }
    val seenIds = remember { mutableSetOf<String>() }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var initialLoading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    suspend fun fetch(initial: Boolean = false) {
        when (val r = api.messagesWith(peerUserId, limit = 50)) {
            is ApiResult.Success -> {
                // Sıralama: server reverse → chronological (eski → yeni)
                val newOnes = r.value.filter { it.id !in seenIds }
                if (newOnes.isNotEmpty()) {
                    seenIds.addAll(newOnes.map { it.id })
                    if (initial) {
                        messages.clear()
                        messages.addAll(r.value)
                    } else {
                        messages.addAll(newOnes.filter { m -> messages.none { it.id == m.id } })
                    }
                    // Otomatik mark-as-read: peer'dan gelen unread mesajları
                    newOnes.filter { it.recipientId == currentUserId && it.readAt == null }
                        .forEach { api.markAsRead(it.id) }
                }
            }
            is ApiResult.Error -> { if (initial) errorText = "Mesajlar alınamadı (HTTP ${r.code})" }
            is ApiResult.Failure -> { if (initial) errorText = "Bağlantı hatası: ${r.cause.message}" }
        }
    }

    LaunchedEffect(peerUserId) {
        fetch(initial = true)
        initialLoading = false
        // Polling
        while (isActive) {
            delay(POLLING_INTERVAL_MS)
            fetch(initial = false)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("@$peerUsername") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (initialLoading) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (errorText != null) {
                Text(
                    text = errorText!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f).padding(horizontal = 8.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(messages, key = { it.id }) { m ->
                        MessageBubble(m, isMine = m.senderId == currentUserId)
                    }
                }
            }

            // Input bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Mesaj yaz…") },
                    modifier = Modifier.weight(1f),
                    enabled = !sending,
                    maxLines = 4,
                )
                Spacer(Modifier.size(8.dp))
                IconButton(
                    onClick = {
                        val content = input.trim()
                        if (content.isBlank() || sending) return@IconButton
                        sending = true
                        scope.launch {
                            when (val r = api.sendMessage(peerUserId, content)) {
                                is ApiResult.Success -> {
                                    if (r.value.id !in seenIds) {
                                        seenIds.add(r.value.id)
                                        messages.add(r.value)
                                    }
                                    input = ""
                                }
                                is ApiResult.Error -> errorText = "Gönderilemedi (HTTP ${r.code})"
                                is ApiResult.Failure -> errorText = "Bağlantı hatası: ${r.cause.message}"
                            }
                            sending = false
                        }
                    },
                    enabled = !sending && input.isNotBlank(),
                ) {
                    if (sending) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    else Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gönder")
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(m: MessageDto, isMine: Boolean) {
    val bg = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val align = if (isMine) Alignment.End else Alignment.Start
    val shape = RoundedCornerShape(
        topStart = 16.dp, topEnd = 16.dp,
        bottomStart = if (isMine) 16.dp else 4.dp,
        bottomEnd = if (isMine) 4.dp else 16.dp,
    )
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = align) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(bg)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = m.content,
                color = fg,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        // Read receipt: ben gönderdim + readAt set
        if (isMine && m.readAt != null) {
            Text(
                text = "okundu",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 8.dp, top = 2.dp),
            )
        }
    }
}
