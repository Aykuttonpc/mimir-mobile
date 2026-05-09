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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.aykutcincik.mimir.ui.components.MimirAvatar
import com.aykutcincik.mimir.ui.components.MimirTopBar
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
import com.aykutcincik.mimir.data.FriendRequestDto
import kotlinx.coroutines.launch

@Composable
fun FriendRequestsScreen(
    accessToken: String,
) {
    val scope = rememberCoroutineScope()
    val api = remember(accessToken) { Apis.friends(accessToken) }

    val requests = remember { mutableStateListOf<FriendRequestDto>() }
    var loading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val processingIds = remember { mutableStateListOf<String>() }

    suspend fun reload() {
        loading = true
        errorText = null
        when (val r = api.listPendingRequests()) {
            is ApiResult.Success -> { requests.clear(); requests.addAll(r.value) }
            is ApiResult.Error -> errorText = "Liste alınamadı (HTTP ${r.code})"
            is ApiResult.Failure -> errorText = "Bağlantı hatası: ${r.cause.message}"
        }
        loading = false
    }

    LaunchedEffect(Unit) { reload() }

    Scaffold(
        topBar = {
            MimirTopBar(
                title = "Arkadaşlık İstekleri",
                actions = {
                    IconButton(onClick = { scope.launch { reload() } }, enabled = !loading) {
                        if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        else Icon(Icons.Default.Refresh, contentDescription = "Yenile")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (errorText != null) {
                Text(errorText!!, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
            }
            if (requests.isEmpty() && !loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Bekleyen istek yok.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                requests.forEach { req ->
                    RequestRow(
                        req = req,
                        isProcessing = req.id in processingIds,
                        onAccept = {
                            processingIds.add(req.id)
                            scope.launch {
                                api.accept(req.id)
                                requests.removeAll { it.id == req.id }
                                processingIds.remove(req.id)
                            }
                        },
                        onReject = {
                            processingIds.add(req.id)
                            scope.launch {
                                api.reject(req.id)
                                requests.removeAll { it.id == req.id }
                                processingIds.remove(req.id)
                            }
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun RequestRow(
    req: FriendRequestDto,
    isProcessing: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    val incoming = req.direction == "Incoming"
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MimirAvatar(username = req.otherUsername, size = 44.dp)
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "@${req.otherUsername}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (incoming) "Sana arkadaşlık isteği gönderdi" else "İstek bekliyor (sen gönderdin)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (incoming) {
                Button(onClick = onAccept, enabled = !isProcessing, shape = MaterialTheme.shapes.medium) {
                    if (isProcessing) CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    else Text("Kabul")
                }
                Spacer(Modifier.size(4.dp))
                OutlinedButton(onClick = onReject, enabled = !isProcessing, shape = MaterialTheme.shapes.medium) { Text("Reddet") }
            }
        }
    }
}
