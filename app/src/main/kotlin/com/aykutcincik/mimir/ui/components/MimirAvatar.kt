package com.aykutcincik.mimir.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Username initial avatar. Tutarlı renk hash'i — aynı kullanıcı her yerde aynı renk.
 * @param ring true → primary renkte ince halka (örn. profile own avatar)
 * @param online true → sağ-altta yeşil dot (pulse animasyonu ile)
 */
@Composable
fun MimirAvatar(
    username: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    ring: Boolean = false,
    online: Boolean = false,
) {
    val initial = username.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val palette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
    )
    val baseColor: Color = palette[(username.hashCode() and 0x7fffffff) % palette.size]
    val gradient = Brush.linearGradient(
        colors = listOf(baseColor, baseColor.copy(alpha = 0.7f)),
    )

    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .let { if (ring) it.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else it }
                .background(gradient),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
        }

        if (online) {
            val transition = rememberInfiniteTransition(label = "online-pulse")
            val pulseScale by transition.animateFloat(
                initialValue = 0.85f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1100, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "scale",
            )
            val dotSize = (size.value * 0.28f).dp.coerceAtLeast(10.dp)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(dotSize)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .background(Color(0xFF4CAF50)),
            )
        }
    }
}
