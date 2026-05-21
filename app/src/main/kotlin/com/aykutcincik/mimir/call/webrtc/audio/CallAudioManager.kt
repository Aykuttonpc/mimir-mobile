/*
 * Adapted from GetStream/webrtc-in-jetpack-compose (Apache 2.0)
 * Original: io.getstream.webrtc.sample.compose.webrtc.audio.{AudioSwitch, AudioManagerAdapterImpl,
 *           AudioFocusRequestWrapper}
 *
 * Mimir farkları:
 *  - 1-1 voice call için trim — bluetooth/wired headset auto-enumeration yok (UI'de toggle yok)
 *  - Speaker/earpiece switch modern API ile (setCommunicationDevice, API 31+)
 */
package com.aykutcincik.mimir.call.webrtc.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.core.content.getSystemService
import com.aykutcincik.mimir.call.webrtc.utils.taggedLogger

/**
 * Sesli arama audio focus + routing yöneticisi.
 *
 * Kritik: WebRTC playout/capture'ın düzgün çalışması için iki şey ŞART —
 *  1. Audio focus (AudioFocusRequest, USAGE_VOICE_COMMUNICATION) — yoksa OS playout
 *     stream'ini route etmez, ses gelmez.
 *  2. MODE_IN_COMMUNICATION — VoIP playout/recording bu modda başlamalı.
 *
 * Android 12+ (API 31): `isSpeakerphoneOn` deprecated + no-op. Speaker/earpiece
 * yönlendirmesi `setCommunicationDevice()` ile yapılmak zorunda.
 */
class CallAudioManager(context: Context) {

    private val logger by taggedLogger("Mimir.CallAudio")
    private val audioManager: AudioManager? = context.getSystemService()

    private var focusRequest: AudioFocusRequest? = null
    private var savedAudioMode = AudioManager.MODE_NORMAL
    private var savedIsMicrophoneMute = false
    private var savedIsSpeakerphoneOn = false
    private var active = false

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        logger.i { "audio focus change: $change" }
    }

    /** Arama başlangıcı — audio state cache + audio focus + MODE_IN_COMMUNICATION. */
    fun start() {
        val am = audioManager ?: return
        if (active) return

        savedAudioMode = am.mode
        savedIsMicrophoneMute = am.isMicrophoneMute
        @Suppress("DEPRECATION")
        savedIsSpeakerphoneOn = am.isSpeakerphoneOn

        am.isMicrophoneMute = false
        requestFocus(am)
        // VoIP playout/recording MODE_IN_COMMUNICATION'da başlamalı (bazı cihazlarda
        // bu set edilmezse speaker mode hiç çalışmaz).
        am.mode = AudioManager.MODE_IN_COMMUNICATION
        active = true
        logger.i { "started — mode=IN_COMMUNICATION + audio focus" }
    }

    /** Arama bitişi — audio focus bırak + audio state restore. */
    fun stop() {
        val am = audioManager ?: return
        if (!active) return

        abandonFocus(am)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { am.clearCommunicationDevice() }
        }
        am.mode = savedAudioMode
        am.isMicrophoneMute = savedIsMicrophoneMute
        @Suppress("DEPRECATION")
        am.isSpeakerphoneOn = savedIsSpeakerphoneOn
        active = false
        logger.i { "stopped — audio state restored" }
    }

    /** Speaker (true) / earpiece (false) yönlendirme. */
    fun setSpeakerphone(on: Boolean) {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val targetType = if (on) AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            else AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
            val device = am.availableCommunicationDevices.firstOrNull { it.type == targetType }
            if (device != null) {
                val ok = am.setCommunicationDevice(device)
                logger.i { "setCommunicationDevice(speaker=$on) → $ok" }
            } else {
                logger.w { "communication device bulunamadı (type=$targetType)" }
            }
        } else {
            @Suppress("DEPRECATION")
            am.isSpeakerphoneOn = on
        }
    }

    val isSpeakerphoneOn: Boolean
        get() {
            val am = audioManager ?: return false
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                am.communicationDevice?.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            } else {
                @Suppress("DEPRECATION")
                am.isSpeakerphoneOn
            }
        }

    private fun requestFocus(am: AudioManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attrs)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(focusListener)
                .build()
            focusRequest = req
            val granted = am.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            logger.i { "requestAudioFocus(new) granted=$granted" }
        } else {
            @Suppress("DEPRECATION")
            val granted = am.requestAudioFocus(
                focusListener,
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            logger.i { "requestAudioFocus(legacy) granted=$granted" }
        }
    }

    private fun abandonFocus(am: AudioManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { am.abandonAudioFocusRequest(it) }
            focusRequest = null
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(focusListener)
        }
    }
}
