package com.aykutcincik.mimir.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import com.aykutcincik.mimir.Apis
import com.aykutcincik.mimir.data.ApiResult
import com.aykutcincik.mimir.data.FriendDto
import com.aykutcincik.mimir.ui.components.MimirAvatar
import kotlinx.coroutines.launch

/**
 * Sprint #14: 2-aşamalı grup oluşturma sihirbazı — GetStream `AddGroupChannelScreen` pattern.
 * 1) SELECT_FRIENDS — multi-select arkadaş listesi (chip preview üstte)
 * 2) ENTER_NAME — grup adı input + üye preview
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    accessToken: String,
    onBack: () -> Unit,
    onCreated: (conversationId: String, name: String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val friendsApi = remember(accessToken) { Apis.friends(accessToken) }
    val messagingApi = remember(accessToken) { Apis.messaging(accessToken) }

    var step by remember { mutableStateOf(Step.SelectFriends) }
    val friends = remember { mutableStateListOf<FriendDto>() }
    val selectedIds = remember { mutableStateListOf<String>() }
    var name by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var creating by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        when (val r = friendsApi.listFriends()) {
            is ApiResult.Success -> { friends.clear(); friends.addAll(r.value) }
            is ApiResult.Error -> errorText = "Arkadaş listesi alınamadı (HTTP ${r.code})"
            is ApiResult.Failure -> errorText = "Bağlantı hatası: ${r.cause.message}"
        }
        loading = false
    }

    BackHandler {
        if (step == Step.EnterName) step = Step.SelectFriends else onBack()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        when (step) {
                            Step.SelectFriends -> "Üye seç (${selectedIds.size})"
                            Step.EnterName -> "Grup adı"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (step == Step.EnterName) step = Step.SelectFriends else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            when (step) {
                Step.SelectFriends -> if (selectedIds.isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        onClick = { step = Step.EnterName },
                        icon = { Icon(Icons.Default.Check, contentDescription = null) },
                        text = { Text("İleri") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Step.EnterName -> if (name.trim().isNotEmpty() && selectedIds.isNotEmpty() && !creating) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            creating = true
                            errorText = null
                            scope.launch {
                                val trimmed = name.trim()
                                when (val r = messagingApi.createGroup(trimmed, selectedIds.toList())) {
                                    is ApiResult.Success -> onCreated(r.value.id, trimmed)
                                    is ApiResult.Error -> errorText = "Oluşturulamadı (HTTP ${r.code}) ${r.errorKey ?: ""}"
                                    is ApiResult.Failure -> errorText = "Bağlantı hatası: ${r.cause.message}"
                                }
                                creating = false
                            }
                        },
                        icon = {
                            if (creating) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                            else Icon(Icons.Default.Check, contentDescription = null)
                        },
                        text = { Text("Grubu oluştur") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (errorText != null) {
                Text(
                    text = errorText!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
            }
            if (selectedIds.isNotEmpty()) {
                SelectedChipRow(
                    selected = selectedIds.mapNotNull { id -> friends.firstOrNull { it.userId == id } },
                    onRemove = { id -> selectedIds.remove(id) },
                )
                HorizontalDivider()
            }
            when (step) {
                Step.SelectFriends -> SelectFriendsList(
                    friends = friends,
                    loading = loading,
                    selectedIds = selectedIds,
                    onToggle = { id ->
                        if (selectedIds.contains(id)) selectedIds.remove(id)
                        else if (selectedIds.size < 49) selectedIds.add(id)
                    },
                )
                Step.EnterName -> {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { if (it.length <= 100) name = it },
                        placeholder = { Text("Örn. \"Eskişehir Eski Dostlar\"") },
                        label = { Text("Grup adı") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    )
                    Text(
                        text = "${selectedIds.size + 1} üye (sen dahil)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}

private enum class Step { SelectFriends, EnterName }

@Composable
private fun SelectedChipRow(
    selected: List<FriendDto>,
    onRemove: (String) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(selected, key = { it.userId }) { f ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.clickable { onRemove(f.userId) },
            ) {
                Row(
                    modifier = Modifier.padding(start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MimirAvatar(username = f.username, size = 24.dp)
                    Spacer(Modifier.size(6.dp))
                    Text("@${f.username}", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.size(6.dp))
                    Icon(Icons.Default.Close, contentDescription = "Kaldır", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SelectFriendsList(
    friends: List<FriendDto>,
    loading: Boolean,
    selectedIds: List<String>,
    onToggle: (String) -> Unit,
) {
    if (loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (friends.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "Önce arkadaş eklemen gerek",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(friends, key = { it.userId }) { f ->
            val isSelected = selectedIds.contains(f.userId)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                shadowElevation = 1.dp,
                onClick = { onToggle(f.userId) },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MimirAvatar(username = f.username, size = 42.dp, online = f.isOnline)
                    Spacer(Modifier.size(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("@${f.username}", style = MaterialTheme.typography.titleMedium)
                        if (f.isOnline) {
                            Text(
                                text = "çevrimiçi",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                    }
                }
            }
        }
    }
}
