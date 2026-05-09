package com.aykutcincik.mimir.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

// ADR-016 — Me + Friends endpoint'leri tek client. Token-bound çağrılar.
class FriendsApi(
    private val accessToken: String,
    baseUrl: String = MimirApi.DEFAULT_BASE_URL,
    private val appVersion: String = "0.0.0",
    private val appPlatform: String = "android",
    private val onVersionGate: () -> Unit = {},
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = false }

    private val client: HttpClient = HttpClient(OkHttp) {
        expectSuccess = false
        defaultRequest {
            url(baseUrl.trimEnd('/') + "/")
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header("X-App-Version", appVersion)
            header("X-App-Platform", appPlatform)
        }
        install(ContentNegotiation) { json(this@FriendsApi.json) }
        HttpResponseValidator {
            validateResponse { response ->
                if (response.status.value == 426) onVersionGate()
            }
        }
    }

    // ─────────── /me ───────────

    suspend fun me(): ApiResult<MeDto> = runCatching {
        val r: HttpResponse = client.get("api/me")
        parseResult<MeDto>(r)
    }.getOrElse { ApiResult.Failure(it) }

    suspend fun regenerateFriendKey(): ApiResult<FriendKeyDto> = runCatching {
        val r: HttpResponse = client.post("api/me/regenerate-friend-key")
        parseResult<FriendKeyDto>(r)
    }.getOrElse { ApiResult.Failure(it) }

    // ─────────── /friends ───────────

    suspend fun listFriends(): ApiResult<List<FriendDto>> = runCatching {
        val r: HttpResponse = client.get("api/friends")
        parseResult<List<FriendDto>>(r)
    }.getOrElse { ApiResult.Failure(it) }

    suspend fun listPendingRequests(): ApiResult<List<FriendRequestDto>> = runCatching {
        val r: HttpResponse = client.get("api/friends/requests/pending")
        parseResult<List<FriendRequestDto>>(r)
    }.getOrElse { ApiResult.Failure(it) }

    suspend fun sendRequest(friendKey: String): ApiResult<FriendRequestDto> = runCatching {
        val r: HttpResponse = client.post("api/friends/requests") {
            setBody(SendFriendRequestRequest(friendKey))
        }
        parseResult<FriendRequestDto>(r)
    }.getOrElse { ApiResult.Failure(it) }

    suspend fun accept(requestId: String): ApiResult<Unit> = runCatching {
        val r: HttpResponse = client.post("api/friends/requests/$requestId/accept")
        if (r.status == HttpStatusCode.NoContent || r.status.value in 200..299)
            ApiResult.Success(Unit) else ApiResult.Error(r.status.value)
    }.getOrElse { ApiResult.Failure(it) }

    suspend fun reject(requestId: String): ApiResult<Unit> = runCatching {
        val r: HttpResponse = client.post("api/friends/requests/$requestId/reject")
        if (r.status == HttpStatusCode.NoContent || r.status.value in 200..299)
            ApiResult.Success(Unit) else ApiResult.Error(r.status.value)
    }.getOrElse { ApiResult.Failure(it) }

    suspend fun unfriend(userId: String): ApiResult<Unit> = runCatching {
        val r: HttpResponse = client.delete("api/friends/$userId")
        if (r.status == HttpStatusCode.NoContent || r.status.value in 200..299)
            ApiResult.Success(Unit) else ApiResult.Error(r.status.value)
    }.getOrElse { ApiResult.Failure(it) }

    private suspend inline fun <reified Resp> parseResult(resp: HttpResponse): ApiResult<Resp> =
        when {
            resp.status.value in 200..299 -> ApiResult.Success(resp.body())
            else -> {
                val key = runCatching { resp.body<ApiErrorBody>().error }.getOrNull()
                ApiResult.Error(resp.status.value, key)
            }
        }

    fun close() { client.close() }
}
