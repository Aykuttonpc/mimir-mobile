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

@Serializable
data class ChangePasswordRequest(
    @SerialName("currentPassword") val currentPassword: String,
    @SerialName("newPassword") val newPassword: String,
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

/**
 * Admin davet liste satırı (T-040). Token plain text DÖNMEZ — sadece hash backend'de.
 * Status: "Active" | "Used" | "Expired".
 */
@Serializable
data class InvitationSummaryDto(
    @SerialName("id") val id: String,
    @SerialName("note") val note: String? = null,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("expiresAt") val expiresAt: String,
    @SerialName("redeemedAt") val redeemedAt: String? = null,
    @SerialName("redeemedByUsername") val redeemedByUsername: String? = null,
    @SerialName("status") val status: String,
)

// ─────────────────────── Messaging (Sprint #4) ───────

@Serializable
data class ConversationDto(
    @SerialName("otherUserId") val otherUserId: String,
    @SerialName("otherUsername") val otherUsername: String,
    @SerialName("lastMessageContent") val lastMessageContent: String? = null,
    @SerialName("lastMessageAt") val lastMessageAt: String? = null,
    @SerialName("lastMessageFromMe") val lastMessageFromMe: Boolean,
    @SerialName("unreadCount") val unreadCount: Int,
)

@Serializable
data class MessageDto(
    @SerialName("id") val id: String,
    @SerialName("senderId") val senderId: String,
    @SerialName("recipientId") val recipientId: String,
    @SerialName("content") val content: String,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("readAt") val readAt: String? = null,
    @SerialName("editedAt") val editedAt: String? = null,
    @SerialName("deletedAt") val deletedAt: String? = null,
)

@Serializable
data class ActiveUserDto(
    @SerialName("id") val id: String,
    @SerialName("username") val username: String,
)

// ─────────────────────── ADR-016: Me + Friends ───────

@Serializable
data class MeDto(
    @SerialName("id") val id: String,
    @SerialName("username") val username: String,
    @SerialName("email") val email: String,
    @SerialName("phone") val phone: String? = null,
    @SerialName("status") val status: String,
    @SerialName("isAdmin") val isAdmin: Boolean,
    @SerialName("friendKey") val friendKey: String? = null,
    @SerialName("createdAt") val createdAt: String,
)

@Serializable
data class FriendKeyDto(
    @SerialName("friendKey") val friendKey: String,
)

@Serializable
data class SendFriendRequestRequest(
    @SerialName("friendKey") val friendKey: String,
)

@Serializable
data class FriendRequestDto(
    @SerialName("id") val id: String,
    @SerialName("otherUserId") val otherUserId: String,
    @SerialName("otherUsername") val otherUsername: String,
    @SerialName("direction") val direction: String,   // "Incoming" | "Outgoing"
    @SerialName("status") val status: String,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("respondedAt") val respondedAt: String? = null,
)

@Serializable
data class FriendDto(
    @SerialName("userId") val userId: String,
    @SerialName("username") val username: String,
    @SerialName("friendsSince") val friendsSince: String,
    @SerialName("isOnline") val isOnline: Boolean = false,
    @SerialName("lastSeenAt") val lastSeenAt: String? = null,
)

// Sprint #11 — presence
@Serializable
data class PresenceChangedEvent(
    @SerialName("userId") val userId: String,
    @SerialName("online") val online: Boolean,
    @SerialName("lastSeenAt") val lastSeenAt: String? = null,
)

@Serializable
data class SendMessageRequest(
    @SerialName("content") val content: String,
)

@Serializable
data class MessageReadEvent(
    @SerialName("messageId") val messageId: String,
    @SerialName("readAt") val readAt: String,
)

// T-035 — edit / soft delete
@Serializable
data class EditMessageRequest(
    @SerialName("content") val content: String,
)

@Serializable
data class MessageEditedEvent(
    @SerialName("messageId") val messageId: String,
    @SerialName("content") val content: String,
    @SerialName("editedAt") val editedAt: String,
)

@Serializable
data class MessageDeletedEvent(
    @SerialName("messageId") val messageId: String,
    @SerialName("deletedAt") val deletedAt: String,
)

// T-033 — typing
@Serializable
data class TypingEvent(
    @SerialName("fromUserId") val fromUserId: String,
    @SerialName("isTyping") val isTyping: Boolean,
)

// ─────────────────────── App version (T-039) ────────

@Serializable
data class AppVersionInfoDto(
    @SerialName("minSupportedVersion") val minSupportedVersion: String,
    @SerialName("latestVersion") val latestVersion: String,
    @SerialName("downloadUrl") val downloadUrl: String,
    @SerialName("platform") val platform: String,
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
