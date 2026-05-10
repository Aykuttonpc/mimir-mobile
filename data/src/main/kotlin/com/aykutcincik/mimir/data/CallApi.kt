package com.aykutcincik.mimir.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

// Sprint #12 — WebRTC call için TURN credentials. HMAC-time-limited (1 saat).
class CallApi(
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
        install(ContentNegotiation) { json(this@CallApi.json) }
        HttpResponseValidator {
            validateResponse { response ->
                if (response.status.value == 426) onVersionGate()
            }
        }
    }

    suspend fun getTurnCredentials(): ApiResult<TurnCredentialsDto> = runCatching {
        val r: HttpResponse = client.get("api/call/turn-credentials")
        if (r.status.value in 200..299) ApiResult.Success(r.body<TurnCredentialsDto>())
        else ApiResult.Error(r.status.value)
    }.getOrElse { ApiResult.Failure(it) }

    fun close() { client.close() }
}
