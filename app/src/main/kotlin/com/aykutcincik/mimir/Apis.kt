package com.aykutcincik.mimir

import com.aykutcincik.mimir.data.AdminApi
import com.aykutcincik.mimir.data.FriendsApi
import com.aykutcincik.mimir.data.MessagingApi
import com.aykutcincik.mimir.data.MimirApi

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

    fun mimir() = MimirApi(appVersion = version, appPlatform = PLATFORM)
    fun admin(token: String) = AdminApi(accessToken = token, appVersion = version, appPlatform = PLATFORM)
    fun messaging(token: String) = MessagingApi(accessToken = token, appVersion = version, appPlatform = PLATFORM)
    fun friends(token: String) = FriendsApi(accessToken = token, appVersion = version, appPlatform = PLATFORM)
}
