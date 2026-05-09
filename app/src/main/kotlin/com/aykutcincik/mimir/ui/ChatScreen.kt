package com.aykutcincik.mimir.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import com.aykutcincik.mimir.ui.components.BubbleEnter
import com.aykutcincik.mimir.ui.components.MimirAvatar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.aykutcincik.mimir.data.ApiResult
import com.aykutcincik.mimir.data.MessageDto
import com.aykutcincik.mimir.data.MessagingApi
import com.aykutcincik.mimir.realtime.RealtimeClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TYPING_PAUSE_MS = 2500L  // 2.5sn input pause → typing=false

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    accessToken: String,
    currentUserId: String,
    peerUserId: String,
    peerUsername: String,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val api = remember(accessToken) { com.aykutcincik.mimir.Apis.messaging(accessToken) }
    val realtime = remember(accessToken) { RealtimeClient(accessToken) }

    val messages = remember { mutableStateListOf<MessageDto>() }
    val seenIds = remember { mutableSetOf<String>() }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var initialLoading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var connected by remember { mutableStateOf(false) }
    var peerTyping by remember { mutableStateOf(false) }
    var typingResetJob by remember { mutableStateOf<Job?>(null) }
    var lastTypingSent by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Mesaj action menü state
    var actionFor by remember { mutableStateOf<MessageDto?>(null) }
    var editingMessage by remember { mutableStateOf<MessageDto?>(null) }
    var editText by remember { mutableStateOf("") }
    var deletingMessage by remember { mutableStateOf<MessageDto?>(null) }

    // İlk yükleme + SignalR connect + event collect
    LaunchedEffect(peerUserId) {
        when (val r = api.messagesWith(peerUserId, limit = 50)) {
            is ApiResult.Success -> {
                messages.clear()
                seenIds.clear()
                messages.addAll(r.value)
                seenIds.addAll(r.value.map { it.id })
                r.value.filter { it.senderId == peerUserId && it.readAt == null }
                    .forEach { api.markAsRead(it.id) }
            }
            is ApiResult.Error -> errorText = "Mesajlar alınamadı (HTTP ${r.code})"
            is ApiResult.Failure -> errorText = "Bağlantı hatası: ${r.cause.message}"
        }
        initialLoading = false

        realtime.start()

        realtime.events.collect { ev ->
            when (ev) {
                is RealtimeClient.RealtimeEvent.Connected -> connected = true
                is RealtimeClient.RealtimeEvent.Disconnected -> connected = false
                is RealtimeClient.RealtimeEvent.Received -> {
                    val m = ev.msg
                    if ((m.senderId == peerUserId || m.recipientId == peerUserId) && m.id !in seenIds) {
                        seenIds.add(m.id)
                        messages.add(m)
                        if (m.senderId == peerUserId && m.readAt == null) {
                            scope.launch { api.markAsRead(m.id) }
                        }
                    }
                }
                is RealtimeClient.RealtimeEvent.Sent -> {
                    val m = ev.msg
                    if (m.recipientId == peerUserId && m.id !in seenIds) {
                        seenIds.add(m.id)
                        messages.add(m)
                    }
                }
                is RealtimeClient.RealtimeEvent.Read -> {
                    val idx = messages.indexOfFirst { it.id == ev.event.messageId }
                    if (idx >= 0) messages[idx] = messages[idx].copy(readAt = ev.event.readAt)
                }
                is RealtimeClient.RealtimeEvent.Edited -> {
                    val idx = messages.indexOfFirst { it.id == ev.event.messageId }
                    if (idx >= 0) messages[idx] = messages[idx].copy(
                        content = ev.event.content,
                        editedAt = ev.event.editedAt,
                    )
                }
                is RealtimeClient.RealtimeEvent.Deleted -> {
                    val idx = messages.indexOfFirst { it.id == ev.event.messageId }
                    if (idx >= 0) messages.removeAt(idx)
                    seenIds.remove(ev.event.messageId)
                }
                is RealtimeClient.RealtimeEvent.Typing -> {
                    if (ev.event.fromUserId == peerUserId) {
                        peerTyping = ev.event.isTyping
                    }
                }
                is RealtimeClient.RealtimeEvent.Error -> {}
            }
        }
    }

    DisposableEffect(realtime) {
        onDispose {
            scope.launch {
                if (lastTypingSent) realtime.sendTyping(peerUserId, false)
                realtime.stop()
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    // Typing send debounce
    fun handleInputChange(newValue: String) {
        input = newValue
        val nonEmpty = newValue.isNotBlank()
        if (nonEmpty && !lastTypingSent) {
            scope.launch { realtime.sendTyping(peerUserId, true) }
            lastTypingSent = true
        }
        typingResetJob?.cancel()
        typingResetJob = scope.launch {
            delay(TYPING_PAUSE_MS)
            if (lastTypingSent) {
                realtime.sendTyping(peerUserId, false)
                lastTypingSent = false
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MimirAvatar(username = peerUsername, size = 36.dp)
                        Spacer(Modifier.size(10.dp))
                        Column {
                            Text("@$peerUsername", style = MaterialTheme.typography.titleMedium)
                            val statusLine = when {
                                peerTyping -> "yazıyor…"
                                connected -> "● bağlı"
                                else -> "○ bağlanıyor…"
                            }
                            Text(
                                text = statusLine,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (connected || peerTyping) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
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
                        MessageBubble(
                            m = m,
                            isMine = m.senderId == currentUserId,
                            menuOpen = actionFor?.id == m.id,
                            onLongPress = { if (m.senderId == currentUserId) actionFor = m },
                            onDismissMenu = { actionFor = null },
                            onEdit = {
                                editingMessage = m
                                editText = m.content
                                actionFor = null
                            },
                            onDelete = {
                                deletingMessage = m
                                actionFor = null
                            },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = ::handleInputChange,
                    placeholder = { Text("Mesaj yaz…") },
                    keyboardOptions = KeyboardOptions.Default,
                    modifier = Modifier.weight(1f),
                    enabled = !sending,
                    maxLines = 4,
                    shape = MaterialTheme.shapes.extraLarge,
                )
                Spacer(Modifier.size(10.dp))
                FilledIconButton(
                    onClick = {
                        val content = input.trim()
                        if (content.isBlank() || sending) return@FilledIconButton
                        sending = true
                        scope.launch {
                            when (val r = api.sendMessage(peerUserId, content)) {
                                is ApiResult.Success -> {
                                    if (r.value.id !in seenIds) {
                                        seenIds.add(r.value.id)
                                        messages.add(r.value)
                                    }
                                    input = ""
                                    if (lastTypingSent) {
                                        realtime.sendTyping(peerUserId, false)
                                        lastTypingSent = false
                                    }
                                }
                                is ApiResult.Error -> errorText = "Gönderilemedi (HTTP ${r.code})"
                                is ApiResult.Failure -> errorText = "Bağlantı hatası: ${r.cause.message}"
                            }
                            sending = false
                        }
                    },
                    enabled = !sending && input.isNotBlank(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier.size(48.dp),
                ) {
                    if (sending) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gönder")
                }
            }
        }
    }

    // Edit dialog
    val editing = editingMessage
    if (editing != null) {
        AlertDialog(
            onDismissRequest = { editingMessage = null },
            title = { Text("Mesajı düzenle") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 6,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val content = editText.trim()
                        if (content.isBlank() || content == editing.content) {
                            editingMessage = null; return@TextButton
                        }
                        scope.launch {
                            when (val r = api.editMessage(editing.id, content)) {
                                is ApiResult.Success -> {
                                    val idx = messages.indexOfFirst { it.id == editing.id }
                                    if (idx >= 0) messages[idx] = messages[idx].copy(
                                        content = content,
                                        editedAt = java.time.Instant.now().toString(),
                                    )
                                    editingMessage = null
                                }
                                is ApiResult.Error -> errorText = "Düzenleme başarısız (HTTP ${r.code})"
                                is ApiResult.Failure -> errorText = "Bağlantı hatası: ${r.cause.message}"
                            }
                        }
                    },
                ) { Text("Kaydet") }
            },
            dismissButton = {
                TextButton(onClick = { editingMessage = null }) { Text("İptal") }
            },
        )
    }

    // Delete onay
    val deleting = deletingMessage
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { deletingMessage = null },
            title = { Text("Mesajı sil") },
            text = { Text("Bu mesajı silmek istediğine emin misin? Geri alınamaz.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            when (val r = api.deleteMessage(deleting.id)) {
                                is ApiResult.Success -> {
                                    messages.removeAll { it.id == deleting.id }
                                    seenIds.remove(deleting.id)
                                    deletingMessage = null
                                }
                                is ApiResult.Error -> errorText = "Silme başarısız (HTTP ${r.code})"
                                is ApiResult.Failure -> errorText = "Bağlantı hatası: ${r.cause.message}"
                            }
                        }
                    },
                ) { Text("Sil") }
            },
            dismissButton = {
                TextButton(onClick = { deletingMessage = null }) { Text("İptal") }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    m: MessageDto,
    isMine: Boolean,
    menuOpen: Boolean,
    onLongPress: () -> Unit,
    onDismissMenu: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val bg = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val align = if (isMine) Alignment.End else Alignment.Start
    val shape = RoundedCornerShape(
        topStart = 16.dp, topEnd = 16.dp,
        bottomStart = if (isMine) 16.dp else 4.dp,
        bottomEnd = if (isMine) 4.dp else 16.dp,
    )
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = align) {
        Box {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(shape)
                    .background(bg)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = onLongPress,
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Column {
                    Text(
                        text = m.content,
                        color = fg,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (m.editedAt != null) {
                        Text(
                            text = "düzenlendi",
                            style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                            color = fg.copy(alpha = 0.7f),
                        )
                    }
                }
            }
            if (isMine) {
                DropdownMenu(expanded = menuOpen, onDismissRequest = onDismissMenu) {
                    DropdownMenuItem(text = { Text("Düzenle") }, onClick = onEdit)
                    DropdownMenuItem(text = { Text("Sil") }, onClick = onDelete)
                }
            }
        }
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
