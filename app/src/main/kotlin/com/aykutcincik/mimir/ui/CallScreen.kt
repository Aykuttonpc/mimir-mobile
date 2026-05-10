package com.aykutcincik.mimir.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aykutcincik.mimir.call.CallManager
import com.aykutcincik.mimir.ui.components.MimirAvatar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Tek ekran tüm call state'lerini handle eder — Outgoing/Incoming/Connecting/Connected.
 * CallManager.state'i observe eder, state'e göre UI değiştirir.
 * onClose: Idle veya Ended state'e geçince çağrılır (parent dismisses).
 */
@Composable
fun CallScreen(accessToken: String, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by CallManager.state.collectAsState()

    // Idle / Ended → ekrandan çık
    LaunchedEffect(state) {
        if (state is CallManager.CallState.Idle) onClose()
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.background,
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient),
    ) {
        when (val s = state) {
            is CallManager.CallState.Outgoing -> CallContent(
                title = "@${s.peerUsername}",
                subtitle = "aranıyor…",
                username = s.peerUsername,
                pulsing = true,
                accessToken = accessToken,
                showAccept = false,
                onHangup = { scope.launch { CallManager.hangup() } },
            )
            is CallManager.CallState.Incoming -> CallContent(
                title = "@${s.peerUsername}",
                subtitle = "geliyor…",
                username = s.peerUsername,
                pulsing = true,
                accessToken = accessToken,
                showAccept = true,
                onAccept = { scope.launch { CallManager.acceptIncoming(ctx, accessToken) } },
                onHangup = { scope.launch { CallManager.rejectIncoming() } },
            )
            is CallManager.CallState.Connecting -> CallContent(
                title = "@${s.peerUsername}",
                subtitle = "bağlanıyor…",
                username = s.peerUsername,
                pulsing = true,
                accessToken = accessToken,
                showAccept = false,
                onHangup = { scope.launch { CallManager.hangup() } },
            )
            is CallManager.CallState.Connected -> {
                var elapsedMs by remember { mutableLongStateOf(0L) }
                LaunchedEffect(s.startedAt) {
                    while (true) {
                        elapsedMs = System.currentTimeMillis() - s.startedAt
                        delay(500)
                    }
                }
                val mins = elapsedMs / 60_000
                val secs = (elapsedMs / 1_000) % 60
                CallContent(
                    title = "@${s.peerUsername}",
                    subtitle = String.format("%02d:%02d", mins, secs),
                    username = s.peerUsername,
                    pulsing = false,
                    accessToken = accessToken,
                    showAccept = false,
                    showInCallActions = true,
                    onHangup = { scope.launch { CallManager.hangup() } },
                )
            }
            is CallManager.CallState.Ended -> CallContent(
                title = s.reason,
                subtitle = "kapandı",
                username = "?",
                pulsing = false,
                accessToken = accessToken,
                showAccept = false,
                onHangup = onClose,
            )
            is CallManager.CallState.Idle -> {}
        }
    }
}

@Composable
private fun CallContent(
    title: String,
    subtitle: String,
    username: String,
    pulsing: Boolean,
    accessToken: String,
    showAccept: Boolean,
    showInCallActions: Boolean = false,
    onAccept: (() -> Unit)? = null,
    onHangup: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(80.dp))

        // Pulsing avatar
        Box(contentAlignment = Alignment.Center) {
            if (pulsing) {
                val transition = rememberInfiniteTransition(label = "call-pulse")
                val pulseScale by transition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.4f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "pulse",
                )
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f)),
                )
            }
            MimirAvatar(username = username, size = 140.dp)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
        }

        // Bottom action row
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = if (showAccept) Arrangement.SpaceEvenly else Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showInCallActions) {
                var muted by remember { mutableStateOf(false) }
                CircleActionButton(
                    icon = if (muted) Icons.Filled.MicOff else Icons.Filled.Mic,
                    bg = Color.White.copy(alpha = 0.2f),
                    fg = Color.White,
                    onClick = {
                        muted = !muted
                        CallManager.setMuted(muted)
                    },
                )
            }

            CircleActionButton(
                icon = Icons.Filled.CallEnd,
                bg = Color(0xFFE53935),
                fg = Color.White,
                size = 76.dp,
                onClick = onHangup,
            )

            if (showAccept && onAccept != null) {
                CircleActionButton(
                    icon = Icons.Filled.Call,
                    bg = Color(0xFF4CAF50),
                    fg = Color.White,
                    size = 76.dp,
                    onClick = onAccept,
                )
            }

            if (showInCallActions) {
                CircleActionButton(
                    icon = Icons.Filled.VolumeUp,
                    bg = Color.White.copy(alpha = 0.2f),
                    fg = Color.White,
                    onClick = { /* TODO: speaker toggle */ },
                )
            }
        }
    }
}

@Composable
private fun CircleActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bg: Color,
    fg: Color,
    size: androidx.compose.ui.unit.Dp = 60.dp,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(size),
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = bg, contentColor = fg),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(size * 0.45f))
    }
}
