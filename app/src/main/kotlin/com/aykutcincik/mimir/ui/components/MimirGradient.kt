package com.aykutcincik.mimir.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Gradient panel — Mimir brand renklerinden seçim.
 * Login hero, Home hero, Me profile header gibi yerlerde kullanılır.
 */
@Composable
fun MimirGradientPanel(
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 28.dp,
    content: @Composable () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val brush = Brush.linearGradient(
        colors = listOf(
            cs.primary,
            cs.primary.copy(alpha = 0.85f),
            cs.tertiary.copy(alpha = 0.65f),
        ),
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)
            .padding(20.dp),
    ) { content() }
}

/**
 * Hero panel — büyük başlık + altyazı + opsiyonel trailing.
 * Gradient versiyonu Login/Home gibi marquee yerlerde.
 */
@Composable
fun MimirHeroGradient(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    MimirGradientPanel(modifier = modifier) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            if (trailing != null) trailing()
        }
    }
}
