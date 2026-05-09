package com.aykutcincik.mimir.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Username'in ilk harfi + tutarlı renk hash'i. Avatar resmi yok şu an.
@Composable
fun MimirAvatar(
    username: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val initial = username.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val palette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
    )
    val bg: Color = palette[(username.hashCode() and 0x7fffffff) % palette.size]

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.surface,
        )
    }
}
