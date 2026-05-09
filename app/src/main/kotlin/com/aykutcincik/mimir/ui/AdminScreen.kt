package com.aykutcincik.mimir.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aykutcincik.mimir.data.AdminApi
import com.aykutcincik.mimir.data.ApiResult
import com.aykutcincik.mimir.data.ApprovalDecisionRequest
import com.aykutcincik.mimir.data.InvitationCreateRequest
import com.aykutcincik.mimir.data.InvitationCreateResponse
import com.aykutcincik.mimir.data.InvitationSummaryDto
import com.aykutcincik.mimir.data.PendingUserDto
import kotlinx.coroutines.launch

@Composable
fun AdminScreen(
    accessToken: String,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val api = remember(accessToken) { com.aykutcincik.mimir.Apis.admin(accessToken) }

    var note by remember { mutableStateOf("") }
    var expiryDaysText by remember { mutableStateOf("7") }
    var inviteLoading by remember { mutableStateOf(false) }
    var lastInvite by remember { mutableStateOf<InvitationCreateResponse?>(null) }
    var inviteError by remember { mutableStateOf<String?>(null) }

    val pending = remember { mutableStateListOf<PendingUserDto>() }
    var pendingLoading by remember { mutableStateOf(false) }
    var pendingError by remember { mutableStateOf<String?>(null) }
    val processingIds = remember { mutableStateListOf<String>() }

    val invitations = remember { mutableStateListOf<InvitationSummaryDto>() }
    var invitationsLoading by remember { mutableStateOf(false) }
    var invitationsError by remember { mutableStateOf<String?>(null) }
    val revokingIds = remember { mutableStateListOf<String>() }

    suspend fun reloadPending() {
        pendingLoading = true
        pendingError = null
        when (val r = api.listPending()) {
            is ApiResult.Success -> { pending.clear(); pending.addAll(r.value) }
            is ApiResult.Error -> { pendingError = "Liste alınamadı (HTTP ${r.code})" }
            is ApiResult.Failure -> { pendingError = "Bağlantı hatası: ${r.cause.message}" }
        }
        pendingLoading = false
    }

    suspend fun reloadInvitations() {
        invitationsLoading = true
        invitationsError = null
        when (val r = api.listInvitations()) {
            is ApiResult.Success -> { invitations.clear(); invitations.addAll(r.value) }
            is ApiResult.Error -> { invitationsError = "Davet listesi alınamadı (HTTP ${r.code})" }
            is ApiResult.Failure -> { invitationsError = "Bağlantı hatası: ${r.cause.message}" }
        }
        invitationsLoading = false
    }

    LaunchedEffect(Unit) { reloadPending(); reloadInvitations() }

    Scaffold(topBar = { MimirTopBar(title = "Admin Paneli", onBack = onBack) }) { padScaffold ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padScaffold)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        // ── Davet üret ──
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(0.dp))
                    Text("  Yeni Davet", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Not (kim için, opsiyonel)") },
                    singleLine = true,
                    enabled = !inviteLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = expiryDaysText,
                    onValueChange = { expiryDaysText = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("Geçerlilik (gün, 1-30)") },
                    singleLine = true,
                    enabled = !inviteLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (inviteError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(inviteError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val days = expiryDaysText.toIntOrNull()?.coerceIn(1, 30) ?: 7
                        inviteLoading = true
                        inviteError = null
                        scope.launch {
                            val req = InvitationCreateRequest(
                                note = note.ifBlank { null },
                                expiryDays = days,
                            )
                            when (val r = api.createInvitation(req)) {
                                is ApiResult.Success -> {
                                    lastInvite = r.value; note = ""
                                    reloadInvitations()  // listeye hemen yansısın
                                }
                                is ApiResult.Error -> { inviteError = "Hata (HTTP ${r.code}${r.errorKey?.let { " — $it" } ?: ""})" }
                                is ApiResult.Failure -> { inviteError = "Bağlantı hatası: ${r.cause.message}" }
                            }
                            inviteLoading = false
                        }
                    },
                    enabled = !inviteLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (inviteLoading) CircularProgressIndicator(modifier = Modifier.height(20.dp))
                    else Text("Davet Üret")
                }

                if (lastInvite != null) {
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Text("Son üretilen davet:", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = lastInvite!!.token,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Bitiş: ${lastInvite!!.expiresAt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row {
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(lastInvite!!.token))
                                Toast.makeText(ctx, "Token kopyalandı", Toast.LENGTH_SHORT).show()
                            },
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                            Text("  Kopyala")
                        }
                        Spacer(Modifier.height(0.dp))
                        OutlinedButton(
                            onClick = {
                                val msg = "Mimir davet token'ı:\n${lastInvite!!.token}\n\nUygulamayı aç → Davet token'ım var → token'ı yapıştır.\n(Geçerlilik: ${lastInvite!!.expiresAt})"
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, msg)
                                }
                                ctx.startActivity(Intent.createChooser(sendIntent, "Daveti paylaş"))
                            },
                            modifier = Modifier.padding(start = 8.dp),
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Text("  Paylaş")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Bekleyen Kullanıcılar ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Bekleyen Kullanıcılar (${pending.size})",
                style = MaterialTheme.typography.titleMedium,
            )
            IconButton(onClick = { scope.launch { reloadPending() } }, enabled = !pendingLoading) {
                if (pendingLoading) CircularProgressIndicator(modifier = Modifier.height(20.dp))
                else Icon(Icons.Default.Refresh, contentDescription = "Yenile")
            }
        }
        Spacer(Modifier.height(8.dp))

        if (pendingError != null) {
            Text(pendingError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        } else if (pending.isEmpty() && !pendingLoading) {
            Text(
                text = "Bekleyen kullanıcı yok.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            // LazyColumn iç içe scroll'da sorun çıkarabilir; küçük listeler için Column yeterli
            pending.forEach { user ->
                PendingUserRow(
                    user = user,
                    isProcessing = user.id in processingIds,
                    onApprove = {
                        processingIds.add(user.id)
                        scope.launch {
                            val r = api.decide(user.id, ApprovalDecisionRequest("approve"))
                            handleDecisionResult(ctx, r, user.username, "onaylandı") {
                                pending.removeAll { it.id == user.id }
                            }
                            processingIds.remove(user.id)
                        }
                    },
                    onReject = {
                        processingIds.add(user.id)
                        scope.launch {
                            val r = api.decide(user.id, ApprovalDecisionRequest("reject"))
                            handleDecisionResult(ctx, r, user.username, "reddedildi") {
                                pending.removeAll { it.id == user.id }
                            }
                            processingIds.remove(user.id)
                        }
                    },
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Davetlerim ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MailOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "  Davetlerim (${invitations.size})",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            IconButton(onClick = { scope.launch { reloadInvitations() } }, enabled = !invitationsLoading) {
                if (invitationsLoading) CircularProgressIndicator(modifier = Modifier.height(20.dp))
                else Icon(Icons.Default.Refresh, contentDescription = "Yenile")
            }
        }
        Spacer(Modifier.height(8.dp))

        if (invitationsError != null) {
            Text(invitationsError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        } else if (invitations.isEmpty() && !invitationsLoading) {
            Text(
                text = "Henüz davet üretilmemiş.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            invitations.forEach { inv ->
                InvitationRow(
                    inv = inv,
                    isRevoking = inv.id in revokingIds,
                    onRevoke = {
                        revokingIds.add(inv.id)
                        scope.launch {
                            val r = api.revokeInvitation(inv.id)
                            val msg = when (r) {
                                is ApiResult.Success -> "Davet iptal edildi"
                                is ApiResult.Error -> "İptal başarısız (${r.errorKey ?: "HTTP ${r.code}"})"
                                is ApiResult.Failure -> "Bağlantı hatası"
                            }
                            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                            revokingIds.remove(inv.id)
                            reloadInvitations()
                        }
                    },
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(24.dp))
    }
    }  // Scaffold close
}

@Composable
private fun InvitationRow(
    inv: InvitationSummaryDto,
    isRevoking: Boolean,
    onRevoke: () -> Unit,
) {
    val statusColor = when (inv.status) {
        "Active" -> MaterialTheme.colorScheme.primary
        "Used"   -> MaterialTheme.colorScheme.tertiary
        else     -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusLabel = when (inv.status) {
        "Active" -> "Aktif"
        "Used"   -> "Kullanıldı"
        "Expired"-> "Süresi dolmuş"
        else     -> inv.status
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.padding(end = 8.dp)) {
                Text(
                    text = inv.note?.takeIf { it.isNotBlank() } ?: "(notsuz)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Üretildi: ${inv.createdAt.take(19).replace('T', ' ')}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Bitiş: ${inv.expiresAt.take(19).replace('T', ' ')}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (inv.redeemedByUsername != null) {
                    Text(
                        text = "Kullanan: @${inv.redeemedByUsername} (${inv.redeemedAt?.take(10) ?: ""})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.weight(1f, fill = true))
            if (inv.status == "Active") {
                IconButton(onClick = onRevoke, enabled = !isRevoking) {
                    if (isRevoking) CircularProgressIndicator(modifier = Modifier.height(20.dp))
                    else Icon(Icons.Default.Delete, contentDescription = "İptal et", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun PendingUserRow(
    user: PendingUserDto,
    isProcessing: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("@${user.username}", style = MaterialTheme.typography.titleSmall)
            Text(user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            user.phone?.let {
                Text("📞 $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = "Kayıt: ${user.createdAt.take(19).replace('T', ' ')}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row {
                Button(
                    onClick = onApprove,
                    enabled = !isProcessing,
                    modifier = Modifier.fillMaxWidth(0.5f),
                ) {
                    if (isProcessing) CircularProgressIndicator(modifier = Modifier.height(16.dp))
                    else Text("Onayla")
                }
                Spacer(Modifier.height(0.dp))
                OutlinedButton(
                    onClick = onReject,
                    enabled = !isProcessing,
                    modifier = Modifier.padding(start = 8.dp).fillMaxWidth(),
                ) {
                    Text("Reddet")
                }
            }
        }
    }
}

private fun handleDecisionResult(
    ctx: android.content.Context,
    r: ApiResult<Unit>,
    username: String,
    actionLabel: String,
    onSuccess: () -> Unit,
) {
    val msg = when (r) {
        is ApiResult.Success -> "@$username $actionLabel".also { onSuccess() }
        is ApiResult.Error -> "İşlem başarısız (HTTP ${r.code})"
        is ApiResult.Failure -> "Bağlantı hatası"
    }
    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
}
