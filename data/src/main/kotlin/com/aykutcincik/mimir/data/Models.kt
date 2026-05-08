package com.aykutcincik.mimir.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─────────────────────── Auth ───────────────────────

@Serializable
data class LoginRequest(
    @SerialName("usernameOrEmail") val usernameOrEmail: String,
    @SerialName("password") val password: String,
)

@Serializable
data class AuthResponse(
    @SerialName("accessToken") val accessToken: String,
    @SerialName("accessExpiresAt") val accessExpiresAt: String,
    @SerialName("refreshToken") val refreshToken: String,
    @SerialName("refreshExpiresAt") val refreshExpiresAt: String,
    @SerialName("username") val username: String,
    @SerialName("isAdmin") val isAdmin: Boolean,
)

@Serializable
data class RegisterRequest(
    @SerialName("invitationToken") val invitationToken: String,
    @SerialName("email") val email: String,
    @SerialName("username") val username: String,
    @SerialName("password") val password: String,
    @SerialName("phone") val phone: String? = null,
)

@Serializable
data class RegisterResponse(
    @SerialName("userId") val userId: String,
    @SerialName("status") val status: String,
    @SerialName("message") val message: String,
)

@Serializable
data class RefreshRequest(
    @SerialName("refreshToken") val refreshToken: String,
)

@Serializable
data class VerifyEmailResponse(
    @SerialName("status") val status: String,
    @SerialName("message") val message: String,
)

// ─────────────────────── Admin ─────────────────────

@Serializable
data class InvitationCreateRequest(
    @SerialName("note") val note: String? = null,
    @SerialName("expiryDays") val expiryDays: Int? = null,
)

@Serializable
data class InvitationCreateResponse(
    @SerialName("id") val id: String,
    @SerialName("token") val token: String,
    @SerialName("expiresAt") val expiresAt: String,
)

@Serializable
data class PendingUserDto(
    @SerialName("id") val id: String,
    @SerialName("username") val username: String,
    @SerialName("email") val email: String,
    @SerialName("phone") val phone: String? = null,
    @SerialName("createdAt") val createdAt: String,
)

@Serializable
data class ApprovalDecisionRequest(
    @SerialName("decision") val decision: String,   // "approve" | "reject" | "suspend"
    @SerialName("reason") val reason: String? = null,
)

// ─────────────────────── Errors ─────────────────────

@Serializable
data class ApiErrorBody(
    @SerialName("error") val error: String,
)

sealed class ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>()
    data class Error(val code: Int, val errorKey: String? = null, val message: String? = null) : ApiResult<Nothing>()
    data class Failure(val cause: Throwable) : ApiResult<Nothing>()
}
