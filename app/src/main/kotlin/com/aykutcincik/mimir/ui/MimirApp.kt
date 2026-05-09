package com.aykutcincik.mimir.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Sprint #3 iskelet — minimal navigation state-machine.
 * Sprint sonu öncesi Navigation Compose'a taşı.
 */
sealed interface Screen {
    data object Login : Screen
    data object Register : Screen
    data class EmailSent(val email: String) : Screen
    data class Pending(val username: String) : Screen
    data class Home(val username: String, val isAdmin: Boolean, val accessToken: String) : Screen
    data class Admin(val accessToken: String, val username: String, val isAdmin: Boolean) : Screen
    data class ChangePassword(val accessToken: String, val username: String, val isAdmin: Boolean) : Screen
}

@Composable
fun MimirApp() {
    var screen: Screen by remember { mutableStateOf<Screen>(Screen.Login) }

    when (val s = screen) {
        is Screen.Login -> LoginScreen(
            onLoggedIn = { auth ->
                screen = Screen.Home(auth.username, auth.isAdmin, auth.accessToken)
            },
            onAccountPending = { username ->
                screen = Screen.Pending(username)
            },
            onGoToRegister = { screen = Screen.Register },
        )
        is Screen.Register -> RegisterScreen(
            onRegistered = { email ->
                screen = Screen.EmailSent(email)
            },
            onBackToLogin = { screen = Screen.Login },
        )
        is Screen.EmailSent -> EmailSentScreen(
            email = s.email,
            onBackToLogin = { screen = Screen.Login },
        )
        is Screen.Pending -> PendingScreen(
            username = s.username,
            onBack = { screen = Screen.Login },
        )
        is Screen.Home -> HomeScreen(
            username = s.username,
            isAdmin = s.isAdmin,
            onLogout = { screen = Screen.Login },
            onOpenAdmin = if (s.isAdmin) {
                { screen = Screen.Admin(s.accessToken, s.username, s.isAdmin) }
            } else null,
            onChangePassword = { screen = Screen.ChangePassword(s.accessToken, s.username, s.isAdmin) },
        )
        is Screen.Admin -> AdminScreen(
            accessToken = s.accessToken,
            onBack = { screen = Screen.Home(s.username, s.isAdmin, s.accessToken) },
        )
        is Screen.ChangePassword -> ChangePasswordScreen(
            accessToken = s.accessToken,
            onBack = { screen = Screen.Home(s.username, s.isAdmin, s.accessToken) },
            onSuccessRequireRelogin = { screen = Screen.Login },
        )
    }
}
