package com.aykutcincik.mimir.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * T-038 — JWT + auth state DataStore-Preferences kalıcı depolama.
 * App açılışta `loadAuth()` ile state restore + refresh, logout/şifre-değiş'te `clear()`.
 *
 * Sprint #5 ileride: encryption-at-rest (EncryptedSharedPreferences veya `androidx.security.crypto`).
 * Şu an plain DataStore — Android internal storage zaten app-private.
 */
data class SavedAuth(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresAt: String,
    val refreshExpiresAt: String,
    val username: String,
    val isAdmin: Boolean,
    val userId: String,
)

private val Context.tokenDataStore: DataStore<Preferences> by preferencesDataStore(name = "mimir_auth")

class DataStoreTokenStorage(private val context: Context) {
    private val keyAccess = stringPreferencesKey("access_token")
    private val keyRefresh = stringPreferencesKey("refresh_token")
    private val keyAccessExp = stringPreferencesKey("access_expires_at")
    private val keyRefreshExp = stringPreferencesKey("refresh_expires_at")
    private val keyUsername = stringPreferencesKey("username")
    private val keyIsAdmin = booleanPreferencesKey("is_admin")
    private val keyUserId = stringPreferencesKey("user_id")

    suspend fun load(): SavedAuth? =
        context.tokenDataStore.data.map { p ->
            val a = p[keyAccess]; val r = p[keyRefresh]
            val u = p[keyUsername]; val id = p[keyUserId]
            if (a != null && r != null && u != null && id != null) {
                SavedAuth(
                    accessToken = a,
                    refreshToken = r,
                    accessExpiresAt = p[keyAccessExp] ?: "",
                    refreshExpiresAt = p[keyRefreshExp] ?: "",
                    username = u,
                    isAdmin = p[keyIsAdmin] ?: false,
                    userId = id,
                )
            } else null
        }.first()

    suspend fun save(auth: AuthResponse, userId: String) {
        context.tokenDataStore.edit { p ->
            p[keyAccess] = auth.accessToken
            p[keyRefresh] = auth.refreshToken
            p[keyAccessExp] = auth.accessExpiresAt
            p[keyRefreshExp] = auth.refreshExpiresAt
            p[keyUsername] = auth.username
            p[keyIsAdmin] = auth.isAdmin
            p[keyUserId] = userId
        }
    }

    suspend fun clear() {
        context.tokenDataStore.edit { it.clear() }
    }
}
