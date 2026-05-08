package com.aykutcincik.mimir.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
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

/**
 * Admin endpoint client. JWT bearer header ile gider, admin policy gerektirir.
 * Oturum başına yaratılır (token expire/refresh olunca yenisi yaratılır).
 */
class AdminApi(
    private val accessToken: String,
    baseUrl: String = MimirApi.DEFAULT_BASE_URL,
    enableLogging: Boolean = true,
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
        }
        install(ContentNegotiation) { json(this@AdminApi.json) }
        if (enableLogging) install(Logging) { level = LogLevel.INFO }
    }

    suspend fun createInvitation(req: InvitationCreateRequest): ApiResult<InvitationCreateResponse> =
        runCatching {
            val resp: HttpResponse = client.post("api/admin/invitations") { setBody(req) }
            parseResult<InvitationCreateResponse>(resp)
        }.getOrElse { ApiResult.Failure(it) }

    suspend fun listPending(): ApiResult<List<PendingUserDto>> = runCatching {
        val resp: HttpResponse = client.get("api/admin/users/pending")
        parseResult<List<PendingUserDto>>(resp)
    }.getOrElse { ApiResult.Failure(it) }

    suspend fun decide(userId: String, req: ApprovalDecisionRequest): ApiResult<Unit> = runCatching {
        val resp: HttpResponse = client.post("api/admin/users/$userId/approve") { setBody(req) }
        if (resp.status == HttpStatusCode.NoContent || resp.status.value in 200..299)
            ApiResult.Success(Unit)
        else
            ApiResult.Error(resp.status.value, resp.body<ApiErrorBody>().error)
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
