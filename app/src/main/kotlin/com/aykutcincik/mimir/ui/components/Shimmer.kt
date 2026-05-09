package com.aykutcincik.mimir.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "translate",
    )
    val cs = MaterialTheme.colorScheme
    return Brush.linearGradient(
        colors = listOf(
            cs.surfaceVariant.copy(alpha = 0.6f),
            cs.surface.copy(alpha = 0.95f),
            cs.surfaceVariant.copy(alpha = 0.6f),
        ),
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim),
    )
}

@Composable
fun ShimmerLine(width: Dp = 200.dp, height: Dp = 14.dp) {
    Box(
        modifier = Modifier
            .height(height)
            .clip(MaterialTheme.shapes.small)
            .background(shimmerBrush())
            .let { if (width == Dp.Unspecified) it.fillMaxWidth() else it.size(width = width, height = height) },
    )
}

@Composable
fun ShimmerCircle(size: Dp = 44.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(shimmerBrush()),
    )
}

@Composable
fun ShimmerConversationCard() {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth().height(72.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            androidx.compose.foundation.layout.Spacer(Modifier.size(14.dp))
            ShimmerCircle(size = 48.dp)
            androidx.compose.foundation.layout.Spacer(Modifier.size(14.dp))
            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                ShimmerLine(width = 120.dp, height = 14.dp)
                androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
                ShimmerLine(width = 200.dp, height = 12.dp)
            }
            androidx.compose.foundation.layout.Spacer(Modifier.size(14.dp))
        }
    }
}
