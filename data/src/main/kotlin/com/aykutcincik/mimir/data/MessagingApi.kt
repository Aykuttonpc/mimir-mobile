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
 * Sprint #14: Conversation-based messaging API. DM ve Group aynı endpoint setini paylaşır.
 * `/api/conversations` Conversation CRUD, `/api/messages/{convId}` mesajlar.
 */
class MessagingApi(
    private val accessToken: String,
    baseUrl: String = MimirApi.DEFAULT_BASE_URL,
    private val appVersion: String = "0.0.0",
    private val appPlatform: String = "android",
    private val onVersionGate: () -> Unit = {},
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
        HttpResponseValidator {
            validateResponse { response ->
                if (response.status.value == 426) onVersionGate()
            }
        }
    }

    // ──────────────────── Conversations ────────────────────

    suspend fun listConversations(): ApiResult<List<ConversationDto>> = runCatching {
        val resp: HttpResponse = client.get("api/conversations")
        parseResult<List<ConversationDto>>(resp)
    }.getOrElse { ApiResult.Failure(it) }

    suspend fun getConversation(conversationId: String): ApiResult<ConversationDetailDto> = runCatching {
        val resp: HttpResponse = client.get("api/conversations/$conversationId")
        parseResult<ConversationDetailDto>(resp)
    }.getOrElse { ApiResult.Failure(it) }

    /**
     * DM oluştur veya zaten varsa onu döndür (idempotent). Tek üye gönderilmeli (kendi hariç).
     */
    suspend fun createDm(otherUserId: String): ApiResult<CreateConversationResponse> = runCatching {
        val resp: HttpResponse = client.post("api/conversations") {
            setBody(CreateConversationRequest(type = "Dm", name = null, memberIds = listOf(otherUserId)))
        }
        parseResult<CreateConversationResponse>(resp)
    }.getOrElse { ApiResult.Failure(it) }

    suspend fun createGroup(name: String, memberIds: List<String>): ApiResult<CreateConversationResponse> = runCatching {
        val resp: HttpResponse = client.post("api/conversations") {
            setBody(CreateConversationRequest(type = "Group", name = name, memberIds = memberIds))
        }
        parseResult<CreateConversationResponse>(resp)
    }.getOrElse { ApiResult.Failure(it) }

    suspend fun renameGroup(conversationId: String, name: String): ApiResult<Unit> = runCatching {
        val resp: HttpResponse = client.patch("api/conversations/$conversationId") {
            setBody(RenameConversationRequest(name))
        }
        emptyResult(resp)
    }.getOrElse { ApiResult.Failure(it) }

    suspend fun addMember(conversationId: String, userId: String): ApiResult<Unit> = runCatching {
        val resp: HttpResponse = client.post("api/conversations/$conversationId/members") {
            setBody(AddMemberRequest(userId))
        }
        emptyResult(resp)
    }.getOrElse { ApiResult.Failure(it) }

    suspend fun removeMember(conversationId: String, userId: String): ApiResult<Unit> = runCatching {
        val resp: HttpResponse = client.delete("api/conversations/$conversationId/members/$userId")
        emptyResult(resp)
    }.getOrElse { ApiResult.Failure(it) }

    suspend fun markConversationRead(conversationId: String): ApiResult<Unit> = runCatching {
        val resp: HttpResponse = client.post("api/conversations/$conversationId/read")
        emptyResult(resp)
    }.getOrElse { ApiResult.Failure(it) }

    // ──────────────────── Messages ────────────────────

    suspend fun listMessages(
        conversationId: String,
        before: String? = null,
        limit: Int = 50,
    ): ApiResult<List<MessageDto>> = runCatching {
        val resp: HttpResponse = client.get("api/messages/$conversationId") {
            if (before != null) parameter("before", before)
            parameter("limit", limit)
        }
        parseResult<List<MessageDto>>(resp)
    }.getOrElse { ApiResult.Failure(it) }

    suspend fun sendMessage(conversationId: String, content: String): ApiResult<MessageDto> = runCatching {
        val resp: HttpResponse = client.post("api/messages/$conversationId") {
            setBody(SendMessageRequest(content))
        }
        parseResult<MessageDto>(resp)
    }.getOrElse { ApiResult.Failure(it) }

    suspend fun editMessage(messageId: String, content: String): ApiResult<Unit> = runCatching {
        val resp: HttpResponse = client.patch("api/messages/$messageId") {
            setBody(EditMessageRequest(content))
        }
        emptyResult(resp)
    }.getOrElse { ApiResult.Failure(it) }

    suspend fun deleteMessage(messageId: String): ApiResult<Unit> = runCatching {
        val resp: HttpResponse = client.delete("api/messages/$messageId")
        emptyResult(resp)
    }.getOrElse { ApiResult.Failure(it) }

    suspend fun activeUsers(search: String? = null, limit: Int = 50): ApiResult<List<ActiveUserDto>> = runCatching {
        val resp: HttpResponse = client.get("api/users/active") {
            if (!search.isNullOrBlank()) parameter("search", search.trim())
            parameter("limit", limit)
        }
        parseResult<List<ActiveUserDto>>(resp)
    }.getOrElse { ApiResult.Failure(it) }

    // ──────────────────── Helpers ────────────────────

    private suspend inline fun <reified Resp> parseResult(resp: HttpResponse): ApiResult<Resp> =
        when {
            resp.status.value in 200..299 -> ApiResult.Success(resp.body())
            else -> {
                val key = runCatching { resp.body<ApiErrorBody>().error }.getOrNull()
                ApiResult.Error(resp.status.value, key)
            }
        }

    private suspend fun emptyResult(resp: HttpResponse): ApiResult<Unit> =
        if (resp.status == HttpStatusCode.NoContent || resp.status.value in 200..299) {
            ApiResult.Success(Unit)
        } else {
            val key = runCatching { resp.body<ApiErrorBody>().error }.getOrNull()
            ApiResult.Error(resp.status.value, key)
        }

    fun close() { client.close() }
}
