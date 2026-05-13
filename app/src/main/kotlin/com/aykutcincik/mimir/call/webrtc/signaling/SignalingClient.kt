package com.aykutcincik.mimir.call.webrtc.signaling

import kotlinx.coroutines.flow.SharedFlow

/**
 * GetStream'in `SignalingClient` interface'inin Mimir karşılığı.
 *
 * Mimir-spesifik fark: peerId tracking — 1-1 call (GetStream sample'da room var).
 * Her sinyal komutu hedef peerId'ye ihtiyaç duyar.
 *
 * Komutlar:
 *  - OFFER (caller → callee)
 *  - ANSWER (callee → caller)
 *  - ICE (her iki yön — ICE candidate exchange)
 *  - REJECT (callee → caller, gelen aramayı reddet)
 *  - END (her iki yön, aramayı bitir)
 */
enum class SignalingCommand {
    OFFER,
    ANSWER,
    ICE,
    REJECT,
    END,
}

/**
 * Signaling event — gelen mesajlar bu shape'te flow'a emit edilir.
 * `command` event tipi, `peerId` karşı tarafın ID'si, `payload` SDP/ICE/empty.
 */
data class SignalingEvent(
    val command: SignalingCommand,
    val peerId: String,
    val payload: String,
    val callerUsername: String = "",
)

interface SignalingClient {
    /** Gelen sinyal eventleri (SignalR + FCM injection birleştirilmiş). */
    val incoming: SharedFlow<SignalingEvent>

    /** Outgoing sinyal komutu gönder. peerId hedef kullanıcı. */
    suspend fun send(command: SignalingCommand, peerId: String, payload: String)

    /** Connection state — connection açık değilse send başarısız olabilir. */
    val isConnected: Boolean

    fun dispose()
}
