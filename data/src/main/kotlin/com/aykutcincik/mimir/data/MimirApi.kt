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
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Mimir backend client. Public domain üzerinden Let's Encrypt cert ile TLS.
 * baseUrl prod: https://aykutonpc.com/mimir
 * Kullanıcı admin onayı, davet yönetimi, register/verify/login/refresh işlemleri.
 *
 * Sprint #6'da iOS aktive olunca bu modül `:shared` KMP'ye taşınır;
 * OkHttp engine yerine multiplatform engine (Darwin iOS, OkHttp Android).
 */
class MimirApi(
    baseUrl: String = DEFAULT_BASE_URL,
    enableLogging: Boolean = true,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
    }

    private val client: HttpClient = HttpClient(OkHttp) {
        expectSuccess = false  // 4xx/5xx exception fırlatmasın — body parse edilebilsin
        defaultRequest {
            url(baseUrl.trimEnd('/') + "/")
            contentType(ContentType.Application.Json)
        }
        install(ContentNegotiation) { json(this@MimirApi.json) }
        if (enableLogging) install(Logging) { level = LogLevel.INFO }
    }

    // ─────────────────────── Public Endpoints ───────────────────────

    suspend fun register(req: RegisterRequest): ApiResult<RegisterResponse> =
        post("api/auth/register", req)

    suspend fun verifyEmail(token: String): ApiResult<VerifyEmailResponse> = runCatching {
        val resp: HttpResponse = client.get("api/auth/verify-email") {
            parameter("token", token)
        }
        parseResult<VerifyEmailResponse>(resp)
    }.getOrElse { ApiResult.Failure(it) }

    suspend fun login(req: LoginRequest): ApiResult<AuthResponse> =
        post("api/auth/login", req)

    suspend fun refresh(refreshToken: String): ApiResult<AuthResponse> =
        post("api/auth/refresh", RefreshRequest(refreshToken))

    suspend fun logout(refreshToken: String): ApiResult<Unit> = runCatching {
        val resp: HttpResponse = client.post("api/auth/logout") {
            setBody(RefreshRequest(refreshToken))
        }
        if (resp.status == HttpStatusCode.NoContent || resp.status.value in 200..299)
            ApiResult.Success(Unit)
        else
            ApiResult.Error(resp.status.value)
    }.getOrElse { ApiResult.Failure(it) }

    /** Public — auth gerekmez. T-039 force-update mekanizması. */
    suspend fun appVersion(platform: String = "android"): ApiResult<AppVersionInfoDto> = runCatching<ApiResult<AppVersionInfoDto>> {
        val resp: HttpResponse = client.get("api/app/version") {
            parameter("platform", platform)
        }
        if (resp.status.value in 200..299) ApiResult.Success(resp.body<AppVersionInfoDto>())
        else ApiResult.Error(resp.status.value)
    }.getOrElse { ApiResult.Failure(it) }

    /** Authenticated — accessToken Bearer header'ında gider. T-024 */
    suspend fun changePassword(accessToken: String, current: String, new: String): ApiResult<Unit> = runCatching {
        val resp: HttpResponse = client.post("api/auth/change-password") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            setBody(ChangePasswordRequest(current, new))
        }
        when (resp.status.value) {
            in 200..299 -> ApiResult.Success(Unit)
            else -> {
                val key = runCatching { resp.body<ApiErrorBody>().error }.getOrNull()
                ApiResult.Error(resp.status.value, key)
            }
        }
    }.getOrElse { ApiResult.Failure(it) }

    // ─────────────────────── Helpers ────────────────────────────────

    private suspend inline fun <reified Req, reified Resp> post(path: String, body: Req): ApiResult<Resp> =
        runCatching {
            val resp: HttpResponse = client.post(path) { setBody(body) }
            parseResult<Resp>(resp)
        }.getOrElse { ApiResult.Failure(it) }

    private suspend inline fun <reified Resp> parseResult(resp: HttpResponse): ApiResult<Resp> =
        when {
            resp.status.value in 200..299 -> ApiResult.Success(resp.body())
            else -> {
                val errorKey = runCatching { resp.body<ApiErrorBody>().error }.getOrNull()
                ApiResult.Error(resp.status.value, errorKey)
            }
        }

    fun close() { client.close() }

    companion object {
        const val DEFAULT_BASE_URL = "https://aykutonpc.com/mimir"
    }
}
