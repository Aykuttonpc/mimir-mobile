package com.aykutcincik.mimir.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * DM + Active users client. JWT token-bound (constructor'da bağlanır).
 * Sprint #4 endpoint'leri: messages/conversations, messages/with, users/active.
 *
 * Sprint #4 mobile başlangıcında REST + polling pattern (basit + güvenli);
 * Sprint #5'te SignalR client'a yükseltilecek (real-time push).
 */
class MessagingApi(
    private val accessToken: String,
    baseUrl: String = MimirApi.DEFAULT_BASE_URL,
    private val appVersion: String = "0.0.0",
    private val appPlatform: String = "android",
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
    }

    private val client: HttpClient = HttpClient(OkHttp) {
        expectSuccess = false
        defaultRequest {
            url(baseUrl.trimEnd('/') + "/")
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            // ADR-015 version gate
            header("X-App-Version", appVersion)
            header("X-App-Platform", appPlatform)
        }
        install(ContentNegotiation) { json(this@MessagingApi.json) }
    }

    suspend fun listConversations(): ApiResult<List<ConversationDto>> = runCatching {
        val resp: HttpResponse = client.get("api/messages/conversations")
        parseResult<List<ConversationDto>>(resp)
    }.getOrElse { ApiResult.Failure(it) }

    suspend fun messagesWith(
        userId: String,
        before: String? = null,
        limit: Int = 50,
    ): ApiResult<List<MessageDto>> = runCatching {
        val resp: HttpResponse = client.get("api/messages/with/$userId") {
            if (before != null) parameter("before", before)
            parameter("limit", limit)
        }
        parseResult<List<MessageDto>>(resp)
    }.getOrElse { ApiResult.Failure(it) }

    suspend fun sendMessage(userId: String, content: String): ApiResult<MessageDto> = runCatching {
        val resp: HttpResponse = client.post("api/messages/with/$userId") {
            setBody(SendMessageRequest(content))
        }
        parseResult<MessageDto>(resp)
    }.getOrElse { ApiResult.Failure(it) }

    suspend fun markAsRead(messageId: String): ApiResult<Unit> = runCatching {
        val resp: HttpResponse = client.post("api/messages/$messageId/read")
        if (resp.status == HttpStatusCode.NoContent || resp.status.value in 200..299)
            ApiResult.Success(Unit)
        else
            ApiResult.Error(resp.status.value)
    }.getOrElse { ApiResult.Failure(it) }

    suspend fun editMessage(messageId: String, content: String): ApiResult<Unit> = runCatching {
        val resp: HttpResponse = client.patch("api/messages/$messageId") {
            setBody(EditMessageRequest(content))
        }
        if (resp.status == HttpStatusCode.NoContent || resp.status.value in 200..299)
            ApiResult.Success(Unit)
        else {
            val key = runCatching { resp.body<ApiErrorBody>().error }.getOrNull()
            ApiResult.Error(resp.status.value, key)
        }
    }.getOrElse { ApiResult.Failure(it) }

    suspend fun deleteMessage(messageId: String): ApiResult<Unit> = runCatching {
        val resp: HttpResponse = client.delete("api/messages/$messageId")
        if (resp.status == HttpStatusCode.NoContent || resp.status.value in 200..299)
            ApiResult.Success(Unit)
        else {
            val key = runCatching { resp.body<ApiErrorBody>().error }.getOrNull()
            ApiResult.Error(resp.status.value, key)
        }
    }.getOrElse { ApiResult.Failure(it) }

    suspend fun activeUsers(search: String? = null, limit: Int = 50): ApiResult<List<ActiveUserDto>> = runCatching {
        val resp: HttpResponse = client.get("api/users/active") {
            if (!search.isNullOrBlank()) parameter("search", search.trim())
            parameter("limit", limit)
        }
        parseResult<List<ActiveUserDto>>(resp)
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
