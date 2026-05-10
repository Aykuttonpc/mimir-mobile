package com.aykutcincik.mimir

import com.aykutcincik.mimir.data.AdminApi
import com.aykutcincik.mimir.data.CallApi
import com.aykutcincik.mimir.data.FriendsApi
import com.aykutcincik.mimir.data.MessagingApi
import com.aykutcincik.mimir.data.MimirApi
import com.aykutcincik.mimir.data.PushApi
import com.aykutcincik.mimir.util.VersionGate

/**
 * Tek noktadan API client factory — `BuildConfig.VERSION_NAME`'i her API'ye taşır.
 * ADR-015 (force update gate): client `X-App-Version` header'ını her isteğe ekler.
 *
 * Screen'lar `Apis.mimir()`, `Apis.admin(token)`, `Apis.messaging(token)` çağırarak
 * sürüm parametre belirtmek zorunda kalmaz.
 */
object Apis {
    private val version: String get() = BuildConfig.VERSION_NAME
    private const val PLATFORM = "android"

    private val onGate: () -> Unit = { VersionGate.trigger() }

    fun mimir() = MimirApi(appVersion = version, appPlatform = PLATFORM, onVersionGate = onGate)
    fun admin(token: String) = AdminApi(accessToken = token, appVersion = version, appPlatform = PLATFORM, onVersionGate = onGate)
    fun messaging(token: String) = MessagingApi(accessToken = token, appVersion = version, appPlatform = PLATFORM, onVersionGate = onGate)
    fun friends(token: String) = FriendsApi(accessToken = token, appVersion = version, appPlatform = PLATFORM, onVersionGate = onGate)
    fun push(token: String) = PushApi(accessToken = token, appVersion = version, appPlatform = PLATFORM, onVersionGate = onGate)
    fun call(token: String) = CallApi(accessToken = token, appVersion = version, appPlatform = PLATFORM, onVersionGate = onGate)
}
