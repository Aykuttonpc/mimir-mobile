/*
 * Port of GetStream/webrtc-in-jetpack-compose (Apache 2.0)
 */
package com.aykutcincik.mimir.call.webrtc.utils

import com.aykutcincik.mimir.call.webrtc.peer.StreamPeerType
import org.webrtc.IceCandidateErrorEvent
import org.webrtc.MediaStreamTrack
import org.webrtc.SessionDescription

fun SessionDescription.stringify(): String = "SessionDescription(type=$type, sdpLen=${description.length})"

fun MediaStreamTrack.stringify(): String =
    "MediaStreamTrack(id=${id()}, kind=${kind()}, enabled=${enabled()}, state=${state()})"

fun IceCandidateErrorEvent.stringify(): String =
    "IceCandidateError(code=$errorCode, $errorText, $address:$port, url=$url)"

fun StreamPeerType.stringify() = when (this) {
    StreamPeerType.PUBLISHER -> "publisher"
    StreamPeerType.SUBSCRIBER -> "subscriber"
}
