package com.aykutcincik.mimir.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.aykutcincik.mimir.Apis
import com.aykutcincik.mimir.call.CallManager
import com.aykutcincik.mimir.data.ApiResult
import com.aykutcincik.mimir.data.DataStoreTokenStorage
import com.aykutcincik.mimir.push.PushRegistrar
import com.aykutcincik.mimir.realtime.RealtimeClient
import com.aykutcincik.mimir.ui.components.MimirBottomBar
import com.aykutcincik.mimir.ui.components.MimirTab
import com.aykutcincik.mimir.util.VersionGate
import kotlinx.coroutines.launch

sealed interface Screen {
    data object Bootstrap : Screen
    data class ForceUpdate(val current: String, val min: String, val downloadUrl: String) : Screen
    data object Login : Screen
    data object Register : Screen
    data class EmailSent(val email: String) : Screen
    data class Pending(val username: String) : Screen
    data class Authed(
        val accessToken: String,
        val username: String,
        val isAdmin: Boolean,
        val userId: String,
        val tab: AuthTab = AuthTab.Messages,
        val detail: AuthDetail? = null,
    ) : Screen
}

enum class AuthTab { Messages, Friends, Requests, Profile }

sealed interface AuthDetail {
    data class Chat(val peerUserId: String, val peerUsername: String) : AuthDetail
    data object AddFriend : AuthDetail
    data object Admin : AuthDetail
    data object ChangePassword : AuthDetail
    data object Call : AuthDetail   // Sprint #12 — fullscreen call UI (CallManager state'inden render)
}

@Composable
fun MimirApp() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val storage = remember { DataStoreTokenStorage(ctx) }
    val api = remember { Apis.mimir() }

    var screen: Screen by remember { mutableStateOf<Screen>(Screen.Bootstrap) }

    val gateTriggered by VersionGate.triggered.collectAsState()
    LaunchedEffect(gateTriggered) {
        if (gateTriggered && screen !is Screen.ForceUpdate) {
            storage.clear()
            val info = (api.appVersion("android") as? ApiResult.Success)?.value
            screen = Screen.ForceUpdate(
                current = com.aykutcincik.mimir.BuildConfig.VERSION_NAME,
                min = info?.minSupportedVersion ?: "0.0.0",
                downloadUrl = info?.downloadUrl ?: "",
            )
            VersionGate.reset()
        }
    }

    LaunchedEffect(Unit) {
        val versionResp = api.appVersion("android")
        if (versionResp is ApiResult.Success) {
            val current = com.aykutcincik.mimir.BuildConfig.VERSION_NAME
            val min = versionResp.value.minSupportedVersion
            if (versionLessThan(current, min)) {
                screen = Screen.ForceUpdate(current, min, versionResp.value.downloadUrl)
                return@LaunchedEffect
            }
        }

        val saved = storage.load()
        if (saved == null) {
            screen = Screen.Login
            return@LaunchedEffect
        }
        when (val r = api.refresh(saved.refreshToken)) {
            is ApiResult.Success -> {
                storage.save(r.value, saved.userId)
                PushRegistrar.register(r.value.accessToken)
                screen = Screen.Authed(r.value.accessToken, r.value.username, r.value.isAdmin, saved.userId)
            }
            is ApiResult.Error -> {
                if (r.code == 426) {
                    storage.clear()
                    val info = (api.appVersion("android") as? ApiResult.Success)?.value
                    screen = Screen.ForceUpdate(
                        current = com.aykutcincik.mimir.BuildConfig.VERSION_NAME,
                        min = info?.minSupportedVersion ?: "0.0.0",
                        downloadUrl = info?.downloadUrl ?: "",
                    )
                } else {
                    storage.clear()
                    screen = Screen.Login
                }
            }
            is ApiResult.Failure -> {
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
        is Screen.ForceUpdate -> ForceUpdateScreen(
            currentVersion = s.current,
            minVersion = s.min,
            downloadUrl = s.downloadUrl,
        )
        is Screen.Login -> LoginScreen(
            onLoggedIn = { auth ->
                val userId = extractSubFromJwt(auth.accessToken) ?: ""
                scope.launch {
                    storage.save(auth, userId)
                    PushRegistrar.register(auth.accessToken)
                }
                screen = Screen.Authed(auth.accessToken, auth.username, auth.isAdmin, userId)
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
        is Screen.Authed -> AuthedScaffold(
            state = s,
            onUpdate = { screen = it },
            onLogout = {
                scope.launch {
                    PushRegistrar.unregister(s.accessToken)
                    storage.clear()
                }
                screen = Screen.Login
            },
            onForceLogout = {
                scope.launch {
                    PushRegistrar.unregister(s.accessToken)
                    storage.clear()
                }
                screen = Screen.Login
            },
        )
    }
}

@Composable
private fun AuthedScaffold(
    state: Screen.Authed,
    onUpdate: (Screen.Authed) -> Unit,
    onLogout: () -> Unit,
    onForceLogout: () -> Unit,
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val friendsApi = remember(state.accessToken) { Apis.friends(state.accessToken) }
    var pendingCount by remember { mutableIntStateOf(0) }

    // Sprint #12 — uygulama içinde tek RealtimeClient + CallManager (Authed lifecycle).
    // ChatScreen kendi local RealtimeClient'ini açıyor; bu bağımsız bir uygulama-seviyesi
    // bağlantı sağlar (incoming call her zaman yakalanır, Chat dışında olsa bile).
    val appRealtime = remember(state.accessToken) { RealtimeClient(state.accessToken) }
    LaunchedEffect(state.accessToken) {
        CallManager.init(ctx)
        CallManager.bindRealtime(appRealtime)
        appRealtime.start()
    }
    val callState by CallManager.state.collectAsState()
    LaunchedEffect(callState) {
        // Incoming call → fullscreen call ekranını aç
        if (callState is CallManager.CallState.Incoming && state.detail != AuthDetail.Call) {
            onUpdate(state.copy(detail = AuthDetail.Call))
        }
    }

    LaunchedEffect(state.accessToken) {
        when (val r = friendsApi.listPendingRequests()) {
            is ApiResult.Success -> pendingCount = r.value.count { it.direction == "Incoming" }
            else -> {}
        }
    }

    // Detail screen has priority — no bottom bar shown.
    val detail = state.detail
    if (detail != null) {
        when (detail) {
            is AuthDetail.Chat -> {
                val ctx2 = androidx.compose.ui.platform.LocalContext.current
                val scope2 = rememberCoroutineScope()
                ChatScreen(
                    accessToken = state.accessToken,
                    currentUserId = state.userId,
                    peerUserId = detail.peerUserId,
                    peerUsername = detail.peerUsername,
                    onBack = { onUpdate(state.copy(detail = null)) },
                    onStartCall = {
                        scope2.launch {
                            CallManager.startOutgoing(ctx2, detail.peerUserId, detail.peerUsername, state.accessToken)
                        }
                        onUpdate(state.copy(detail = AuthDetail.Call))
                    },
                )
            }
            AuthDetail.AddFriend -> AddFriendScreen(
                accessToken = state.accessToken,
                onBack = { onUpdate(state.copy(detail = null)) },
                onRequestSent = { onUpdate(state.copy(detail = null)) },
            )
            AuthDetail.Admin -> AdminScreen(
                accessToken = state.accessToken,
                onBack = { onUpdate(state.copy(detail = null)) },
            )
            AuthDetail.ChangePassword -> ChangePasswordScreen(
                accessToken = state.accessToken,
                onBack = { onUpdate(state.copy(detail = null)) },
                onSuccessRequireRelogin = onForceLogout,
            )
            AuthDetail.Call -> CallScreen(
                accessToken = state.accessToken,
                onClose = { onUpdate(state.copy(detail = null)) },
            )
        }
        return
    }

    val tabs = listOf(
        MimirTab(AuthTab.Messages.name, "Mesajlar", Icons.AutoMirrored.Filled.Chat),
        MimirTab(AuthTab.Friends.name, "Arkadaşlar", Icons.Filled.People),
        MimirTab(AuthTab.Requests.name, "İstekler", Icons.Filled.Notifications, badge = pendingCount),
        MimirTab(AuthTab.Profile.name, "Profilim", Icons.Filled.Person),
    )

    Scaffold(
        bottomBar = {
            MimirBottomBar(
                tabs = tabs,
                selectedKey = state.tab.name,
                onSelect = { key ->
                    val newTab = AuthTab.valueOf(key)
                    if (newTab != state.tab) onUpdate(state.copy(tab = newTab))
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (state.tab) {
                AuthTab.Messages -> ChatListScreen(
                    accessToken = state.accessToken,
                    onOpenChat = { peerId, peerUsername ->
                        onUpdate(state.copy(detail = AuthDetail.Chat(peerId, peerUsername)))
                    },
                    onNewChat = { onUpdate(state.copy(tab = AuthTab.Friends)) },
                )
                AuthTab.Friends -> FriendsScreen(
                    accessToken = state.accessToken,
                    onAddFriend = { onUpdate(state.copy(detail = AuthDetail.AddFriend)) },
                    onOpenChat = { peerId, peerUsername ->
                        onUpdate(state.copy(detail = AuthDetail.Chat(peerId, peerUsername)))
                    },
                )
                AuthTab.Requests -> FriendRequestsScreen(accessToken = state.accessToken)
                AuthTab.Profile -> ProfileTab(
                    accessToken = state.accessToken,
                    isAdmin = state.isAdmin,
                    onChangePassword = { onUpdate(state.copy(detail = AuthDetail.ChangePassword)) },
                    onOpenAdmin = if (state.isAdmin) {
                        { onUpdate(state.copy(detail = AuthDetail.Admin)) }
                    } else null,
                    onLogout = onLogout,
                )
            }
        }
    }
}

private fun versionLessThan(a: String, b: String): Boolean {
    val ap = a.split(".").map { it.toIntOrNull() ?: 0 }
    val bp = b.split(".").map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(ap.size, bp.size)) {
        val av = ap.getOrElse(i) { 0 }
        val bv = bp.getOrElse(i) { 0 }
        if (av < bv) return true
        if (av > bv) return false
    }
    return false
}

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
