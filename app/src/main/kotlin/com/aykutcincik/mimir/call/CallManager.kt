package com.aykutcincik.mimir.call

import android.content.Context
import com.aykutcincik.mimir.call.webrtc.peer.StreamPeerConnectionFactory
import com.aykutcincik.mimir.call.webrtc.session.CallSession
import com.aykutcincik.mimir.call.webrtc.signaling.MimirSignalingAdapter
import com.aykutcincik.mimir.realtime.RealtimeClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Mimir call katmanının uygulama-seviyesi tek girişi.
 *
 * Hayat çevrimi:
 *  - `bind(ctx, realtime, userId, accessToken)` — AuthedScaffold mount'ta çağrılır
 *  - `startOutgoing(peerId, peerUsername)` — Chat ekranı "Ara" butonu
 *  - `acceptIncoming() / rejectIncoming() / hangup()` — CallScreen butonları
 *  - `injectIncomingOffer(callerId, username, sdp)` — FCM payload üzerinden
 *  - `unbind()` — logout / force-update
 *
 * State flow `CallSession.State` (Idle/Outgoing/Incoming/Connecting/Connected/Ended).
 *
 * Tüm WebRTC complexity GetStream-derived `CallSession`'da. Bu sadece facade.
 */
object CallManager {

    private val _state = MutableStateFlow<CallSession.State>(CallSession.State.Idle)
    val state: StateFlow<CallSession.State> = _state.asStateFlow()

    private var factory: StreamPeerConnectionFactory? = null
    private var signaling: MimirSignalingAdapter? = null
    private var session: CallSession? = null
    private var observerJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun bind(ctx: Context, realtime: RealtimeClient, myUserId: String, accessToken: String) {
        if (factory == null) {
            factory = StreamPeerConnectionFactory(ctx.applicationContext)
        }
        signaling?.dispose()
        signaling = MimirSignalingAdapter(realtime)

        val s = CallSession(
            context = ctx.applicationContext,
            signalingClient = signaling!!,
            peerConnectionFactory = factory!!,
            myUserId = myUserId,
            accessToken = accessToken,
        )
        session = s

        observerJob?.cancel()
        observerJob = scope.launch {
            s.state.collect { _state.value = it }
        }
    }

    fun unbind() {
        observerJob?.cancel()
        observerJob = null
        session?.dispose()
        session = null
        signaling?.dispose()
        signaling = null
        _state.value = CallSession.State.Idle
    }

    fun startOutgoing(peerId: String, peerUsername: String) =
        session?.startOutgoing(peerId, peerUsername)

    fun acceptIncoming() = session?.acceptIncoming()
    fun rejectIncoming() = session?.rejectIncoming()
    fun hangup() = session?.hangup()
    fun setMuted(muted: Boolean) = session?.setMuted(muted)
    fun setSpeakerphone(on: Boolean) = session?.setSpeakerphone(on)
    val isSpeakerphoneOn: Boolean get() = session?.isSpeakerphoneOn ?: false

    /** FCM `callOffer` payload'ı handler — app dead'ken state machine'i tetikler. */
    fun injectIncomingOffer(callerId: String, callerUsername: String, sdpOffer: String) {
        // Session olmasa bile ham state'i set et ki app açılıp bind() çağrıldığında pickup edilir.
        // Pratikte FCM geldiğinde app uyanır → MainActivity.onCreate → AuthedScaffold mount →
        // bind() → bizim collect flow state'i replay'lemez (StateFlow latest value tutar) —
        // o yüzden state'i internal'da tut, bind sonrası inject et.
        pendingFcmOffer = Triple(callerId, callerUsername, sdpOffer)
        session?.injectIncomingOffer(callerId, callerUsername, sdpOffer)
    }

    private var pendingFcmOffer: Triple<String, String, String>? = null

    /** Bind sonrası MimirApp çağırır — pending FCM offer'ı session'a aktar. */
    fun consumePendingFcmOffer() {
        val pending = pendingFcmOffer ?: return
        pendingFcmOffer = null
        session?.injectIncomingOffer(pending.first, pending.second, pending.third)
    }
}
