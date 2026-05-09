package com.aykutcincik.mimir.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.aykutcincik.mimir.data.ApiResult
import com.aykutcincik.mimir.data.DataStoreTokenStorage
import com.aykutcincik.mimir.data.MimirApi
import com.aykutcincik.mimir.data.SavedAuth
import kotlinx.coroutines.launch

/**
 * Sprint #3-4-5 navigation state-machine. Compose Navigation kütüphanesine geçişi ileride değerlendir.
 * T-038: app açılışında DataStore'dan token restore + refresh dene.
 */
sealed interface Screen {
    data object Bootstrap : Screen          // T-038: token restore deneme
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
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val storage = remember { DataStoreTokenStorage(ctx) }
    val api = remember { MimirApi() }

    var screen: Screen by remember { mutableStateOf<Screen>(Screen.Bootstrap) }

    // T-038: app açılışında token restore + refresh
    LaunchedEffect(Unit) {
        val saved = storage.load()
        if (saved == null) {
            screen = Screen.Login
            return@LaunchedEffect
        }
        // Refresh dene — başarılıysa yeni access tokenla devam, başarısızsa Login'e
        when (val r = api.refresh(saved.refreshToken)) {
            is ApiResult.Success -> {
                storage.save(r.value, saved.userId)
                screen = Screen.Home(r.value.username, r.value.isAdmin, r.value.accessToken, saved.userId)
            }
            is ApiResult.Error, is ApiResult.Failure -> {
                storage.clear()
                screen = Screen.Login
            }
        }
    }

    when (val s = screen) {
        is Screen.Bootstrap -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is Screen.Login -> LoginScreen(
            onLoggedIn = { auth ->
                val userId = extractSubFromJwt(auth.accessToken) ?: ""
                scope.launch { storage.save(auth, userId) }
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
            onLogout = {
                scope.launch { storage.clear() }
                screen = Screen.Login
            },
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
            onSuccessRequireRelogin = {
                scope.launch { storage.clear() }
                screen = Screen.Login
            },
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
 */
private fun extractSubFromJwt(jwt: String): String? = runCatching {
    val parts = jwt.split('.')
    if (parts.size < 2) return@runCatching null
    val payload = parts[1]
    val padded = payload.replace('-', '+').replace('_', '/').let {
        val rem = it.length % 4
        if (rem == 0) it else it + "=".repeat(4 - rem)
    }
    val decoded = String(java.util.Base64.getDecoder().decode(padded))
    Regex(""""sub"\s*:\s*"([^"]+)"""").find(decoded)?.groupValues?.get(1)
}.getOrNull()
