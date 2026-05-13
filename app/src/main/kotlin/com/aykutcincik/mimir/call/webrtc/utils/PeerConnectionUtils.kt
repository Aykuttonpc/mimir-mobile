/*
 * Port of GetStream/webrtc-in-jetpack-compose (Apache 2.0)
 * Original: io.getstream.webrtc.sample.compose.webrtc.utils.PeerConnectionUtils
 */
package com.aykutcincik.mimir.call.webrtc.utils

import org.webrtc.AddIceObserver
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun PeerConnection.addRtcIceCandidate(iceCandidate: IceCandidate): Result<Unit> =
    suspendCoroutine { cont ->
        addIceCandidate(iceCandidate, object : AddIceObserver {
            override fun onAddSuccess() = cont.resume(Result.success(Unit))
            override fun onAddFailure(error: String?) =
                cont.resume(Result.failure(RuntimeException(error ?: "addIceCandidate failed")))
        })
    }
