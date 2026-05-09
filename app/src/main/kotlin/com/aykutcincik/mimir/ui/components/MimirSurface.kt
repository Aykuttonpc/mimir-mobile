package com.aykutcincik.mimir.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Yumuşak elevation'lı kart — list item, info paneli, vs.
@Composable
fun MimirCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        onClick = onClick ?: {},
        enabled = onClick != null,
    ) {
        Box(modifier = Modifier.padding(16.dp)) { content() }
    }
}

// Hero panel — ekranın üstüne büyük bir başlık + açıklama
@Composable
fun MimirHero(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 0.dp,
    ) {
        Box(modifier = Modifier.padding(20.dp)) {
            androidx.compose.foundation.layout.Column {
                androidx.compose.material3.Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                if (subtitle != null) {
                    androidx.compose.material3.Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                if (trailing != null) {
                    Box(modifier = Modifier.padding(top = 16.dp)) { trailing() }
                }
            }
        }
    }
}
