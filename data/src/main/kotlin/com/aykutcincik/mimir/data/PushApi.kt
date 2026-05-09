package com.aykutcincik.mimir.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ADR-017: FCM signal-only push. Mobile token kayıt/silme.
class PushApi(
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
        install(ContentNegotiation) { json(this@PushApi.json) }
        HttpResponseValidator {
            validateResponse { response ->
                if (response.status.value == 426) onVersionGate()
            }
        }
    }

    suspend fun registerDevice(fcmToken: String): ApiResult<Unit> = runCatching {
        val r: HttpResponse = client.post("api/me/devices") {
            setBody(RegisterDeviceRequest(fcmToken = fcmToken, platform = "Android"))
        }
        if (r.status == HttpStatusCode.NoContent || r.status.value in 200..299)
            ApiResult.Success(Unit) else ApiResult.Error(r.status.value)
    }.getOrElse { ApiResult.Failure(it) }

    suspend fun unregisterDevice(fcmToken: String): ApiResult<Unit> = runCatching {
        val r: HttpResponse = client.delete("api/me/devices/$fcmToken")
        if (r.status == HttpStatusCode.NoContent || r.status.value in 200..299)
            ApiResult.Success(Unit) else ApiResult.Error(r.status.value)
    }.getOrElse { ApiResult.Failure(it) }

    fun close() { client.close() }
}

@Serializable
internal data class RegisterDeviceRequest(
    val fcmToken: String,
    val platform: String,
)
