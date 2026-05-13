package com.aykutcincik.mimir.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aykutcincik.mimir.Apis
import com.aykutcincik.mimir.data.ApiResult
import com.aykutcincik.mimir.data.ConversationDetailDto
import com.aykutcincik.mimir.data.ConversationMemberDto
import com.aykutcincik.mimir.data.FriendDto
import com.aykutcincik.mimir.ui.components.MimirAvatar
import kotlinx.coroutines.launch

/**
 * Sprint #14: Grup detayı — üye listesi, ad düzenleme, üye ekle/çıkar, gruptan ayrıl.
 * Sadece grup conversation'lar için açılır. Admin/Owner ek aksiyonlara sahip.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    accessToken: String,
    currentUserId: String,
    conversationId: String,
    onBack: () -> Unit,
    onLeft: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val msgApi = remember(accessToken) { Apis.messaging(accessToken) }
    val friendsApi = remember(accessToken) { Apis.friends(accessToken) }

    var detail by remember { mutableStateOf<ConversationDetailDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var renameOpen by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    var addOpen by remember { mutableStateOf(false) }
    var leaveConfirm by remember { mutableStateOf(false) }
    var removeConfirm by remember { mutableStateOf<ConversationMemberDto?>(null) }

    suspend fun reload() {
        loading = true
        when (val r = msgApi.getConversation(conversationId)) {
            is ApiResult.Success -> { detail = r.value; errorText = null }
            is ApiResult.Error -> errorText = "Detay alınamadı (HTTP ${r.code})"
            is ApiResult.Failure -> errorText = "Bağlantı hatası: ${r.cause.message}"
        }
        loading = false
    }

    LaunchedEffect(conversationId) { reload() }

    val myRole = detail?.members?.firstOrNull { it.userId == currentUserId }?.role
    val isAdmin = myRole == "Owner" || myRole == "Admin"

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Grup bilgisi", style = MaterialTheme.typography.titleMedium) },
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
        if (loading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        val d = detail ?: return@Scaffold

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (errorText != null) {
                Text(errorText!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }
            // Header — group avatar + name
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(56.dp),
                    )
                }
                Spacer(Modifier.size(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = d.name ?: "(adsız grup)",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (isAdmin) {
                        IconButton(onClick = {
                            renameText = d.name ?: ""
                            renameOpen = true
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Adı düzenle")
                        }
                    }
                }
                Text(
                    text = "${d.members.size} üye",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (isAdmin) {
                    ActionTile(
                        icon = Icons.Default.GroupAdd,
                        label = "Üye ekle",
                        onClick = { addOpen = true },
                        modifier = Modifier.weight(1f),
                    )
                }
                ActionTile(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    label = "Gruptan ayrıl",
                    onClick = { leaveConfirm = true },
                    danger = true,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.size(16.dp))

            // Members
            Text(
                text = "Üyeler",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(d.members, key = { it.userId }) { m ->
                    MemberRow(
                        m = m,
                        isMe = m.userId == currentUserId,
                        canRemove = isAdmin && m.userId != currentUserId && m.role != "Owner",
                        onRemove = { removeConfirm = m },
                    )
                }
            }
        }
    }

    // Rename dialog
    if (renameOpen) {
        AlertDialog(
            onDismissRequest = { renameOpen = false },
            title = { Text("Grubu yeniden adlandır") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { if (it.length <= 100) renameText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val n = renameText.trim()
                    if (n.isEmpty()) { renameOpen = false; return@TextButton }
                    scope.launch {
                        when (val r = msgApi.renameGroup(conversationId, n)) {
                            is ApiResult.Success -> { renameOpen = false; reload() }
                            is ApiResult.Error -> errorText = "Düzenlenemedi (HTTP ${r.code})"
                            is ApiResult.Failure -> errorText = "Bağlantı: ${r.cause.message}"
                        }
                    }
                }) { Text("Kaydet") }
            },
            dismissButton = {
                TextButton(onClick = { renameOpen = false }) { Text("İptal") }
            },
        )
    }

    // Add member sheet
    if (addOpen) {
        val existingIds = detail?.members?.map { it.userId }?.toSet() ?: emptySet()
        AddMemberDialog(
            accessToken = accessToken,
            existingIds = existingIds,
            onAdd = { userId ->
                scope.launch {
                    when (val r = msgApi.addMember(conversationId, userId)) {
                        is ApiResult.Success -> { addOpen = false; reload() }
                        is ApiResult.Error -> errorText = "Eklenemedi (HTTP ${r.code}) ${r.errorKey ?: ""}"
                        is ApiResult.Failure -> errorText = "Bağlantı: ${r.cause.message}"
                    }
                }
            },
            onDismiss = { addOpen = false },
        )
    }

    // Leave confirm
    if (leaveConfirm) {
        val isOwner = myRole == "Owner"
        AlertDialog(
            onDismissRequest = { leaveConfirm = false },
            title = { Text(if (isOwner) "Owner ayrılamaz" else "Gruptan ayrıl") },
            text = {
                Text(
                    if (isOwner) "Grup sahibi gruptan ayrılamaz. Önce başka bir üyeyi admin yapıp sahipliği devretmen gerekir (yakında)."
                    else "Bu gruptan ayrılmak istediğine emin misin? Tekrar eklenmek için bir admin'in seni davet etmesi gerekir."
                )
            },
            confirmButton = {
                if (!isOwner) TextButton(onClick = {
                    leaveConfirm = false
                    scope.launch {
                        when (val r = msgApi.removeMember(conversationId, currentUserId)) {
                            is ApiResult.Success -> onLeft()
                            is ApiResult.Error -> errorText = "Ayrılınamadı (HTTP ${r.code}) ${r.errorKey ?: ""}"
                            is ApiResult.Failure -> errorText = "Bağlantı: ${r.cause.message}"
                        }
                    }
                }) { Text("Ayrıl") }
                else TextButton(onClick = { leaveConfirm = false }) { Text("Tamam") }
            },
            dismissButton = if (!isOwner) {
                { TextButton(onClick = { leaveConfirm = false }) { Text("Vazgeç") } }
            } else null,
        )
    }

    // Remove member confirm
    val targetRemove = removeConfirm
    if (targetRemove != null) {
        AlertDialog(
            onDismissRequest = { removeConfirm = null },
            title = { Text("Üyeyi çıkar") },
            text = { Text("@${targetRemove.username} bu gruptan çıkarılsın mı?") },
            confirmButton = {
                TextButton(onClick = {
                    removeConfirm = null
                    scope.launch {
                        when (val r = msgApi.removeMember(conversationId, targetRemove.userId)) {
                            is ApiResult.Success -> reload()
                            is ApiResult.Error -> errorText = "Çıkarılamadı (HTTP ${r.code})"
                            is ApiResult.Failure -> errorText = "Bağlantı: ${r.cause.message}"
                        }
                    }
                }) { Text("Çıkar") }
            },
            dismissButton = {
                TextButton(onClick = { removeConfirm = null }) { Text("Vazgeç") }
            },
        )
    }
}

@Composable
private fun ActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = if (danger) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.primaryContainer,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (danger) MaterialTheme.colorScheme.onErrorContainer
                       else MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.size(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (danger) MaterialTheme.colorScheme.onErrorContainer
                        else MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun MemberRow(
    m: ConversationMemberDto,
    isMe: Boolean,
    canRemove: Boolean,
    onRemove: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MimirAvatar(username = m.username, size = 40.dp)
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "@${m.username}${if (isMe) " (sen)" else ""}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (m.role != "Member") {
                    Text(
                        text = m.role,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (canRemove) {
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.PersonRemove,
                        contentDescription = "Çıkar",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMemberDialog(
    accessToken: String,
    existingIds: Set<String>,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val friendsApi = remember(accessToken) { Apis.friends(accessToken) }
    var friends by remember { mutableStateOf<List<FriendDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        when (val r = friendsApi.listFriends()) {
            is ApiResult.Success -> friends = r.value.filter { it.userId !in existingIds }
            else -> {}
        }
        loading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Üye ekle") },
        text = {
            if (loading) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (friends.isEmpty()) {
                Text("Eklenebilecek arkadaş kalmadı.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(friends, key = { it.userId }) { f ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            onClick = { onAdd(f.userId) },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                MimirAvatar(username = f.username, size = 36.dp)
                                Spacer(Modifier.size(12.dp))
                                Text("@${f.username}")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Kapat") }
        },
    )
}
