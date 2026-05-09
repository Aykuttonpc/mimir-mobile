package com.aykutcincik.mimir.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Sprint #3-4 navigation state-machine. Compose Navigation kütüphanesine geçişi ileride değerlendir.
 */
sealed interface Screen {
    data object Login : Screen
    data object Register : Screen
    data class EmailSent(val email: String) : Screen
    data class Pending(val username: String) : Screen
    data class Home(val username: String, val isAdmin: Boolean, val accessToken: String, val userId: String) : Screen
    data class Admin(val accessToken: String, val username: String, val isAdmin: Boolean, val userId: String) : Screen
    data class ChangePassword(val accessToken: String, val username: String, val isAdmin: Boolean, val userId: String) : Screen
    data class ChatList(val accessToken: String, val username: String, val isAdmin: Boolean, val userId: String) : Screen
    data class NewChat(val accessToken: String, val username: String, val isAdmin: Boolean, val userId: String) : Screen
    data class Chat(val accessToken: String, val currentUserId: String, val peerUserId: String, val peerUsername: String, val username: String, val isAdmin: Boolean) : Screen
}

@Composable
fun MimirApp() {
    var screen: Screen by remember { mutableStateOf<Screen>(Screen.Login) }

    when (val s = screen) {
        is Screen.Login -> LoginScreen(
            onLoggedIn = { auth ->
                // JWT'den userId çıkar — sub claim middle base64 segment
                val userId = extractSubFromJwt(auth.accessToken) ?: ""
                screen = Screen.Home(auth.username, auth.isAdmin, auth.accessToken, userId)
            },
            onAccountPending = { username -> screen = Screen.Pending(username) },
            onGoToRegister = { screen = Screen.Register },
        )
        is Screen.Register -> RegisterScreen(
            onRegistered = { email -> screen = Screen.EmailSent(email) },
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
                { screen = Screen.Admin(s.accessToken, s.username, s.isAdmin, s.userId) }
            } else null,
            onChangePassword = { screen = Screen.ChangePassword(s.accessToken, s.username, s.isAdmin, s.userId) },
            onOpenMessages = { screen = Screen.ChatList(s.accessToken, s.username, s.isAdmin, s.userId) },
        )
        is Screen.Admin -> AdminScreen(
            accessToken = s.accessToken,
            onBack = { screen = Screen.Home(s.username, s.isAdmin, s.accessToken, s.userId) },
        )
        is Screen.ChangePassword -> ChangePasswordScreen(
            accessToken = s.accessToken,
            onBack = { screen = Screen.Home(s.username, s.isAdmin, s.accessToken, s.userId) },
            onSuccessRequireRelogin = { screen = Screen.Login },
        )
        is Screen.ChatList -> ChatListScreen(
            accessToken = s.accessToken,
            onBack = { screen = Screen.Home(s.username, s.isAdmin, s.accessToken, s.userId) },
            onOpenChat = { peerId, peerUsername ->
                screen = Screen.Chat(s.accessToken, s.userId, peerId, peerUsername, s.username, s.isAdmin)
            },
            onNewChat = { screen = Screen.NewChat(s.accessToken, s.username, s.isAdmin, s.userId) },
        )
        is Screen.NewChat -> NewChatScreen(
            accessToken = s.accessToken,
            onBack = { screen = Screen.ChatList(s.accessToken, s.username, s.isAdmin, s.userId) },
            onPickUser = { peerId, peerUsername ->
                screen = Screen.Chat(s.accessToken, s.userId, peerId, peerUsername, s.username, s.isAdmin)
            },
        )
        is Screen.Chat -> ChatScreen(
            accessToken = s.accessToken,
            currentUserId = s.currentUserId,
            peerUserId = s.peerUserId,
            peerUsername = s.peerUsername,
            onBack = {
                screen = Screen.ChatList(s.accessToken, s.username, s.isAdmin, s.currentUserId)
            },
        )
    }
}

/**
 * JWT payload'dan `sub` claim çıkar. JWT format: `header.payload.signature` — payload base64-url.
 * Sprint #5'te düzgün JWT parse library'si eklenebilir.
 */
private fun extractSubFromJwt(jwt: String): String? = runCatching {
    val parts = jwt.split('.')
    if (parts.size < 2) return@runCatching null
    val payload = parts[1]
    // base64-url → base64 standard + padding
    val padded = payload.replace('-', '+').replace('_', '/').let {
        val rem = it.length % 4
        if (rem == 0) it else it + "=".repeat(4 - rem)
    }
    val decoded = String(java.util.Base64.getDecoder().decode(padded))
    // "sub":"<uuid>" — basit regex (kotlinx.serialization JsonObject'le yapılabilir, MVP için basit)
    Regex(""""sub"\s*:\s*"([^"]+)"""").find(decoded)?.groupValues?.get(1)
}.getOrNull()
