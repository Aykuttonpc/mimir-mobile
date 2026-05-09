package com.aykutcincik.mimir.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

// List item enter — slide-in from below + fade. Stagger için index parametresi.
@Composable
fun AnimatedListItem(
    visible: Boolean = true,
    delayMillis: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = tween(300, delayMillis = delayMillis, easing = LinearOutSlowInEasing),
        ) + fadeIn(animationSpec = tween(300, delayMillis = delayMillis)),
        exit = slideOutVertically(targetOffsetY = { -it / 4 }) + fadeOut(),
        modifier = modifier,
        content = content,
    )
}

// Ekran geçişi için yatay slide. forward = sağdan gelir, backward = soldan.
fun screenEnter(forward: Boolean = true): EnterTransition =
    slideInHorizontally(
        initialOffsetX = { full -> if (forward) full else -full },
        animationSpec = tween(280, easing = LinearOutSlowInEasing),
    ) + fadeIn(animationSpec = tween(280))

fun screenExit(forward: Boolean = true): ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { full -> if (forward) -full / 4 else full / 4 },
        animationSpec = tween(220),
    ) + fadeOut(animationSpec = tween(220))

// Mount sonrası tetiklenir — list item'ların animate olarak görünmesi için.
@Composable
fun rememberMountedState(): MutableState<Boolean> {
    val state = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { state.value = true }
    return state
}

// Bubble enter — chat mesajları için spring scale + fade
@Composable
fun BubbleEnter(
    visible: Boolean,
    fromMe: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            initialOffsetX = { full -> if (fromMe) full / 8 else -full / 8 },
            animationSpec = spring(dampingRatio = 0.7f, stiffness = 350f),
        ) + fadeIn(tween(220)) + expandVertically(animationSpec = tween(220), expandFrom = androidx.compose.ui.Alignment.Top),
        exit = fadeOut(tween(120)) + shrinkVertically(),
        modifier = modifier,
        content = content,
    )
}
