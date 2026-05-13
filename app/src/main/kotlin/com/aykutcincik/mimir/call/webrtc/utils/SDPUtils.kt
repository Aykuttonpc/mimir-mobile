/*
 * Port of GetStream/webrtc-in-jetpack-compose (Apache 2.0)
 * Original: io.getstream.webrtc.sample.compose.webrtc.utils.SDPUtils
 */
package com.aykutcincik.mimir.call.webrtc.utils

import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend inline fun createValue(
    crossinline call: (SdpObserver) -> Unit,
): Result<SessionDescription> = suspendCoroutine {
    val observer = object : SdpObserver {
        override fun onCreateSuccess(description: SessionDescription?) {
            if (description != null) it.resume(Result.success(description))
            else it.resume(Result.failure(RuntimeException("SessionDescription is null")))
        }
        override fun onCreateFailure(message: String?) =
            it.resume(Result.failure(RuntimeException(message ?: "createOffer/Answer failed")))
        override fun onSetSuccess() = Unit
        override fun onSetFailure(p0: String?) = Unit
    }
    call(observer)
}

suspend inline fun setValue(
    crossinline call: (SdpObserver) -> Unit,
): Result<Unit> = suspendCoroutine {
    val observer = object : SdpObserver {
        override fun onCreateFailure(p0: String?) = Unit
        override fun onCreateSuccess(p0: SessionDescription?) = Unit
        override fun onSetSuccess() = it.resume(Result.success(Unit))
        override fun onSetFailure(message: String?) =
            it.resume(Result.failure(RuntimeException(message ?: "setLocal/Remote failed")))
    }
    call(observer)
}

/** WebRTC codec name case normalize (sdp munge) */
fun String.mungeCodecs(): String =
    this.replace("vp9", "VP9").replace("vp8", "VP8").replace("h264", "H264")
