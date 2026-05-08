package com.aykutcincik.mimir.data

/**
 * JWT access + refresh token kalıcı depolama soyutlaması.
 * App katmanında DataStore-Preferences ile implement edilecek (Android-spesifik).
 */
interface TokenStorage {
    suspend fun get(): TokenPair?
    suspend fun save(pair: TokenPair)
    suspend fun clear()
}

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    /** ISO-8601 string (server formatı). Client tarafında parse opsiyonel — refresh genelde 401 sonrası çağrılır. */
    val accessExpiresAt: String,
    val refreshExpiresAt: String,
)
