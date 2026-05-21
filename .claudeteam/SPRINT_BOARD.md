# Sprint Board

> Aktif sprint'i `## Aktif Sprint` altında, kapanan sprint'leri `## ✅ Sprint #N KAPANDI` ile alta ekle.
> WIP max 1-2 task / sprint. Tarihler ISO format (YYYY-MM-DD).

## Aktif Sprint

- **Sprint:** #14 — **Group Chat MVP (v1.0.0)**
- **Başlangıç:** 2026-05-13
- **Sprint hedefi:** Unified Conversation modeli — DM ve grup tek mesaj/endpoint setinde (ADR-022). GetStream/stream-chat-android baseline (Apache 2.0).

| İş | Durum |
|---|---|
| Spike — GetStream `stream-chat-android-compose-sample` pattern analizi (2-step wizard, `messaging` channel) | ✅ |
| Plan + paydaş onayı (max 50 üye, group voice call yok, avatar yok) | ✅ |
| Backend: `Conversation` + `ConversationMember` entities + EF config | ✅ |
| Backend: `AddConversations` migration + DM çift → DM Conversation backfill (DO block) | ✅ |
| Backend: `ConversationsController` (list, detail, create, rename, addMember, removeMember, read) | ✅ |
| Backend: `MessagesController` refit — `{convId}` path + tek-yol broadcast | ✅ |
| Backend: `DmHub` `SendMessage(convId)` + auto-join `conv-{id}` + MemberAdded/Renamed events | ✅ |
| Backend: `IPushDispatcher` payload'a `conversationId` (FCM → ChatScreen deep-link hazırlığı) | ✅ |
| Mobile: `ConversationDto` + `MessageDto` shape update + yeni event modelleri | ✅ |
| Mobile: `MessagingApi` conversation CRUD + message refit | ✅ |
| Mobile: `RealtimeClient` conversation-scoped event + `SendMessage(convId)` invoke | ✅ |
| Mobile: `ChatListScreen` unified (DM + Group, group icon + üye sayısı) | ✅ |
| Mobile: `ChatScreen` `conversationId`-based + group'ta sender username prefix | ✅ |
| Mobile: `CreateGroupScreen` 2-step wizard (SELECT_FRIENDS → ENTER_NAME) | ✅ |
| Mobile: `GroupDetailScreen` (üyeler, rename, ekle/çıkar, ayrıl) | ✅ |
| Mobile: FCM `Notifications.showNewMessage(conversationId, ...)` | ✅ |
| ADR-022 + SPRINT_BOARD update | ✅ |
| Backend deploy v1.0.0 — migration çalıştı, healthcheck 200 | ✅ |
| Mobile deploy v1.0.0 — APK build + VPS upload | ✅ |
| **Fix: voice call "ses yok"** — audio focus + modern device routing (CallAudioManager) | ✅ |
| Mobile deploy v1.0.1 (voice fix) — versionCode 20 | 🟡 build |
| WIP — E2E test: DM regression + group + voice call ses | 🟡 |
| force-update dispatch (MIN_APP_VERSION_ANDROID=1.0.1) | 🟡 |

**Voice "ses yok" root cause (2026-05-13):**
GetStream/webrtc-in-jetpack-compose portunda audio subsystem (`AudioSwitch`/`AudioManagerAdapter`) tamamen atlanmıştı. Audio focus hiç alınmıyordu → OS WebRTC playout stream'ini route etmiyor. `isSpeakerphoneOn` Android 12+ no-op. Fix: `CallAudioManager` — `AudioFocusRequest` + `setCommunicationDevice()`.

**Açık riskler:**
- Eski APK'lar (`0.9.2`/`1.0.0`) → force-update gerekli. `MIN_APP_VERSION_ANDROID=1.0.1` dispatch bekliyor.

---

## ✅ Sprint #13 KAPANDI (2026-05-13) — **Stabilization**

**Hedef:** Güvenlik denetimi + codebase cleanup + dokümantasyon + portfolio README.
**Sonuç:** OWASP Top 10 critical bulgu yok. 4 orta öncelikli fix uygulandı. Ölü kod silindi. ADR-018, ADR-019, ADR-020 yazıldı. README portföyleştirildi.

| İş | Durum |
|---|---|
| Backend security audit (Controllers + Hub + Crypto + Auth) | ✅ |
| Mobile security audit (DataStore + Manifest + Network) | ✅ |
| Infra audit (compose + secrets + nginx + coturn) | ✅ |
| Fix: FriendsController.ResubmitRequest logical bug | ✅ |
| Fix: AdminController.Decide invalid → 400 | ✅ |
| Fix: mimir-mobile .gitignore secret patterns | ✅ |
| Fix: AndroidManifest usesCleartextTraffic=false explicit | ✅ |
| Cleanup: ölü HomeScreen.kt + MeScreen.kt | ✅ |
| ADR-018 Presence + ADR-019 Voice Call + ADR-020 Stabilization | ✅ |
| SPRINT_BOARD güncel state'e taşı | ✅ |
| README portfolio (mimir-api + mimir-mobile) | ✅ |

---

## ✅ Sprint #12 KAPANDI (2026-05-13) — **Sesli Arama (WebRTC + GetStream baseline)**

**Hedef:** Ephemeral WebRTC P2P sesli arama — kayıt yok.
**Sonuç:** GetStream/webrtc-in-jetpack-compose baseline'ı port edildi. Sesli arama çalışıyor (Aykut + Yusufcincik74 başarılı test). Speaker toggle + hangup sync.

| İş | Durum |
|---|---|
| **Spike** — kendi CallManager'ı 3 sprint debug edildi, scope race + mutex+I/O deadlock + hangup sync miss | — |
| Pivot: NIH terk + GetStream baseline (Aykut'un kararı, ADR-019) | ✅ |
| Backend: SignalR DmHub OfferCall/AnswerCall/IceCandidate/Reject/End | ✅ |
| Backend: coturn container (UDP 3478/5349 + media relay 49152-49200) | ✅ |
| Backend: TURN credentials endpoint (HMAC time-limited) | ✅ |
| Backend: FCM payload'a SDP offer (app dead'ken yakalama) | ✅ |
| Mobile: GetStream baseline port (StreamPeerConnection + factory) | ✅ |
| Mobile: MimirSignalingAdapter (SignalR ↔ GetStream interface) | ✅ |
| Mobile: CallSession state machine + endSync pattern | ✅ |
| Mobile: CallScreen (Mimir tema, gradient, pulse, 3 buton: mic/hangup/speaker) | ✅ |
| Mobile: FCM `callOffer` → CallManager.injectIncomingOffer | ✅ |
| VPS: UFW UDP portları, .env.prod TURN_*, coturn start | ✅ |
| 0.7.x → 0.9.2 (signing fix, send→invoke, scope fix, pivot, hangup sync, speaker) | ✅ |

---

## ✅ Sprint #11 KAPANDI (2026-05-09) — **Presence + Search**

**Hedef:** Online/offline/last seen + ChatList/Friends local search.
**Sonuç:** Real-time presence (ADR-018). Search bar'lar local filter.

| İş | Durum |
|---|---|
| User.LastSeenAt + migration | ✅ |
| PresenceTracker singleton (ConcurrentDictionary) | ✅ |
| DmHub.OnConnected/Disconnected → broadcast arkadaşlara (ADR-016 gate) | ✅ |
| FriendDto presence fields + GET /api/friends/{userId}/presence | ✅ |
| Mobile RealtimeClient PresenceChanged event | ✅ |
| MimirAvatar online dot + ring | ✅ |
| ChatList + Friends search bars (local filter) | ✅ |

---

## ✅ Sprint #10 KAPANDI (2026-05-09) — **Bottom Nav Refactor**

**Hedef:** Tile-based Home → modern bottom navigation (4 tab).
**Sonuç:** `Screen.Authed(tab, detail?)` state machine.

| İş | Durum |
|---|---|
| Screen sealed interface refactor | ✅ |
| AuthTab enum + AuthDetail (Chat, AddFriend, Admin, ChangePassword, Call) | ✅ |
| MimirBottomBar component (pill background + scale animation + badge) | ✅ |
| ProfileTab.kt (MeScreen yerine) — gradient header + theme + actions | ✅ |
| Tab screens'ten onBack kaldırıldı | ✅ |

---

## ✅ Sprint #9 KAPANDI (2026-05-09) — **UI Pro (App Icon + Splash + Gradient + Animations)**

| İş | Durum |
|---|---|
| App icon: Mannaz rune (M) + nordic blue gradient + amber accent | ✅ |
| Splash screen (Android 12+ native + core-splashscreen backport) | ✅ |
| MimirGradientPanel + MimirHeroGradient | ✅ |
| Avatar gradient + ring + pulsing online dot | ✅ |
| ShimmerConversationCard loading state | ✅ |

---

## ✅ Sprint #8 KAPANDI (2026-05-09) — **UI Overhaul**

| İş | Durum |
|---|---|
| Theme system (Color/Type/Shape/Theme) — Norse brand palette | ✅ |
| DataStore theme preference (System/Light/Dark) | ✅ |
| 7 reusable component (Button/Card/Avatar/TextField/TopBar/Animations) | ✅ |
| 14 ekran modernize | ✅ |
| CI/CD: GitHub Actions auto-deploy (mimir-api + mimir-mobile) | ✅ |
| Repo rename: JavaInstagramClone → mimir-mobile | ✅ |
| Branch consolidation: kmp-rewrite → master | ✅ |

---

## ✅ Sprint #7 KAPANDI (2026-05-09) — **FCM Push (Signal-only)**

**Hedef:** Mesaj geldiğinde push notification (Firebase ekosisteminden sadece Cloud Messaging).
**Sonuç:** ADR-017 — signal-only pattern (içerik FCM'e gitmez, mobile uyandığında backend'den çeker).

| İş | Durum |
|---|---|
| Backend: FirebaseAdmin SDK + FcmDispatcher + UserDeviceToken entity | ✅ |
| Backend: DevicesController (POST/DELETE /api/me/devices) | ✅ |
| Mobile: firebase-messaging + MimirFcmService + Notifications channel | ✅ |
| Mobile: PushRegistrar (login/logout token sync) | ✅ |
| In-app sideload update (DownloadManager + FileProvider + REQUEST_INSTALL_PACKAGES) | ✅ |
| Force update gate (ADR-015) — eski APK 426 + ForceUpdateScreen | ✅ |
| Friend key + onay-bazlı arkadaşlık (ADR-016) | ✅ |

---

## ✅ Sprint #5 KAPANDI (2026-05-09)

**Hedef:** Polling → SignalR real-time, mesaj UX (edit/delete/typing), push provider kararı, force-update + auto-login.
**Sonuç:** WhatsApp-class DM canlı.

---

## ✅ Sprint #4 KAPANDI (2026-05-09) — **1-1 DM + AES-GCM**

---

## ✅ Sprint #3 KAPANDI (2026-05-09) — **KMP-ready Android Compose iskelet + nginx kalıcılaştırma**

---

## ✅ Sprint #2 KAPANDI (2026-05-09) — **Backend foundation: auth + admin + invitations**

---

## ✅ Sprint #1 KAPANDI (2026-05-08) — **Bootstrap**

`.claudeteam/` init, ADR-001..ADR-009, mimir-api repo + Hetzner VPS deploy.
