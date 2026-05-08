package com.aykutcincik.mimir.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Sprint #3 iskelet — minimal navigation state-machine.
 * Sprint sonu öncesi Navigation Compose'a taşı (T-022 detayı).
 */
sealed interface Screen {
    data object Login : Screen
    data class Pending(val username: String) : Screen
    data class Home(val username: String, val isAdmin: Boolean) : Screen
}

@Composable
fun MimirApp() {
    var screen: Screen by remember { mutableStateOf<Screen>(Screen.Login) }

    when (val s = screen) {
        is Screen.Login -> LoginScreen(
            onLoggedIn = { auth ->
                screen = Screen.Home(auth.username, auth.isAdmin)
            },
            onAccountPending = { username ->
                screen = Screen.Pending(username)
            }
        )
        is Screen.Pending -> PendingScreen(
            username = s.username,
            onBack = { screen = Screen.Login }
        )
        is Screen.Home -> HomeScreen(
            username = s.username,
            isAdmin = s.isAdmin,
            onLogout = { screen = Screen.Login }
        )
    }
}
