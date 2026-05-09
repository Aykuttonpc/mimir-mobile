package com.aykutcincik.mimir.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class MimirTab(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val badge: Int = 0,
)

@Composable
fun MimirBottomBar(
    tabs: List<MimirTab>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                BottomTabItem(
                    tab = tab,
                    selected = tab.key == selectedKey,
                    onClick = { onSelect(tab.key) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomTabItem(
    tab: MimirTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val tint = if (selected) cs.onPrimaryContainer else cs.onSurfaceVariant
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.0f else 0.92f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 600f),
        label = "tab-scale",
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Pill background when selected (Material 3 style)
        Box(
            modifier = Modifier
                .height(32.dp)
                .padding(horizontal = 14.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (selected) cs.primaryContainer else androidx.compose.ui.graphics.Color.Transparent)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box {
                Icon(
                    tab.icon,
                    contentDescription = tab.label,
                    tint = tint,
                    modifier = Modifier.size(22.dp).scale(iconScale),
                )
                if (tab.badge > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(cs.error),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (tab.badge > 9) "9+" else tab.badge.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = cs.onError,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = tint,
        )
    }
}
