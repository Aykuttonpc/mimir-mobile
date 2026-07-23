# Decisions Log (ADR-Style)

> Her önemli teknik/ürün kararı buraya yazılır.
> Format: tarih ↑ olacak şekilde, en yeni en üstte.
> "Önemli" tanımı: 2 hafta sonra "neden böyle yaptık?" sorusunu doğurabilecek her şey.

---

## Şablon

```
### ADR-NNN — [Karar başlığı]

- **Tarih:** YYYY-MM-DD
- **Durum:** Önerildi / Kabul edildi / Geri çekildi / Yerine geçen ADR-XXX
- **Karar verenler:** [Roller / kişiler]

**Bağlam:**
[Neden karar gerekiyordu? Hangi problem?]

**Değerlendirilen Seçenekler:**
1. [Seçenek A] — [artısı/eksisi]
2. [Seçenek B] — [artısı/eksisi]

**Karar:**
[Hangisi seçildi]

**Rationale:**
[Neden bu seçildi]

**Sonuçlar / Trade-off'lar:**
[Bu kararın yan etkileri, kabul ettiğimiz dezavantajlar]
```

---

## ADR-023 — Release İmzalama Ayrıştırıldı (public debug key → repo-dışı release keystore)

- **Tarih:** 2026-07-23
- **Durum:** Kabul edildi
- **Karar verenler:** Kullanıcı, AppSec, SecOps, Tech Lead

**Bağlam:**
Release buildType `mimirDebug` signingConfig'ini kullanıyordu (`app/build.gradle.kts`, "geçici, release-signed Sprint #15" notuyla). O keystore `app/mimir-debug.keystore` olarak repo'da ve üç parolası da (`mimirdebug`) build script'inde açık yazılı. Repo public (`Aykuttonpc/mimir-mobile`).

Sonuç: keystore + parola herkese açık olduğu için üçüncü bir taraf **birebir aynı imzayla** APK üretebilir. Android imza kontrolünden geçtiği için bu APK, kullanıcının yüklü Mimir'inin üzerine sorunsuz güncelleme olarak kurulur. "Seamless update" için alınan tasarım kararı, saldırgan için de aynı şekilde çalışıyordu.

Aynı denetimde ikinci bulgu: `javaInstagramClone.jks` (eski repo adından kalma yetim release keystore) Ağustos 2024'ten beri public history'de. Hiçbir build referans etmiyor.

Üçüncü bulgu: `.gitignore`'da satır-sonu yorumu kullanılmış (`!app/mimir-debug.keystore   # ...`). gitignore bu formatı desteklemez, pattern yorumu da kapsar → hem bu negation hem `google-services.local.json` kuralı hiç çalışmıyormuş.

**Değerlendirilen Seçenekler:**
1. Debug key'i release'de kullanmayı sürdür, repo'yu private yap — sızıntı durur ama dağıtılmış APK'lar zaten o key'le imzalı, geriye dönük çözmez
2. Release için ayrı keystore, parametreler repo dışında — imza yüzeyi tamamen ayrışır
3. Play App Signing'e geç — Google upload/signing key ayrımını yönetir, ama şu an Play Store'da değiliz

**Karar:**
Seçenek 2. Release imzalama parametreleri `signing.properties` (gitignored, repo dışı keystore yolu) üzerinden okunuyor. Dosya yoksa build kırılmıyor; uyarı basıp debug key'e düşüyor — CI ve yeni klonlar bozulmasın diye.

**Rationale:**
Seçenek 1 dağıtılmış APK'ları kurtarmıyor; anahtar zaten yanmış. Seçenek 3 doğru uzun vadeli hedef ama Play Store'a girmeden uygulanamaz ve bugünkü riski çözmez. Seçenek 2 hem acil riski kapatıyor hem Play App Signing'e geçişte upload key olarak yeniden kullanılabiliyor.

Debug keystore bilerek repo'da kaldı: artık **sadece** debug variant'ta kullanılıyor, güvenlik sınırında değil ve deterministik debug imzası geliştirme akışı için gerçek fayda sağlıyor.

**Sonuçlar / Trade-off'lar:**
- (+) Release imza anahtarı public yüzeyden tamamen çıktı (4096-bit RSA, `CN=Aykut Cincik`, SHA-256 `ED:9C:F4:A4:…`)
- (+) `.gitignore` kuralları fiilen çalışır hale geldi (`git check-ignore` ile doğrulandı)
- (+) Debug geliştirme akışı değişmedi — debug variant hâlâ `mimirDebug`
- (−) **İmza değişti → mevcut kurulumlar güncelleme kabul etmez.** Test kullanıcıları uygulamayı silip yeniden kurmak zorunda; DataStore'daki JWT de silineceği için yeniden login gerekiyor
- (−) Yeni klonda `signing.properties` yoksa release build sessizce debug key'e düşer — uyarı basılıyor ama dikkat edilmezse gözden kaçabilir
- ⚠️ `mimirdebug` anahtarıyla imzalanmış tüm eski APK'lar **güvenilmez** kabul edilmeli
- ⚠️ Release key değişti → SHA-1 fingerprint de değişti; Firebase Console'da SHA kaydı varsa güncellenmeli

---

## ADR-022 — Unified Conversation Model (Group Chat MVP)

- **Tarih:** 2026-05-13
- **Durum:** Kabul edildi
- **Karar verenler:** Tech Lead, Innovation Architect, Tech Radar Eng, Dev #2 (data), AppSec, PO

**Bağlam:**
Sprint #4'ten beri DM modeli `Message.SenderId + RecipientId` çifti üzerinde duruyordu — `ConversationDto` runtime'da bu çiftlerden türetiliyordu. Sprint #14 grup sohbeti istedi: aynı kanalda N üye (DM = 2-üyeli özel hal). Mevcut çift bazlı şema grup'a uymuyor.

**Değerlendirilen Seçenekler:**
1. **Ayrı tablo: DmMessage + GroupMessage** — domain ayrımı net, ama duplicate logic (encrypt, edit, delete, broadcast, push)
2. **Unified `Conversation` + `ConversationMember` + `Message.ConversationId`** — tek tip mesaj, üye lookup ile authorize, broadcast SignalR group "conv-{id}"
3. **Matrix room model (Element X)** — Rust SDK overhead, federation kompleksitesi gereksiz

**Karar:** Seçenek 2 — GetStream/stream-chat-android baseline.

**Rationale:**
- GetStream production-verified (milyonlarca user, Apache 2.0). Sprint #12 WebRTC GetStream baseline başarısının tekrarı.
- DM = `Conversation(Type=Dm)` + 2 member; Group = `Conversation(Type=Group)` + N member. Tek mesaj tablosu, tek API yüzeyi.
- Friend gating sadece `Create` üzerinde — üye olduğun konuşmada her şey serbest.
- Migration: mevcut DM çiftleri (`SenderId,RecipientId`) DO block ile `Conversation` + `ConversationMember`'a backfill. Mesaj kaybı yok.
- SignalR group `conv-{id}` — connection sırasında üye olduğun tüm conv'lara otomatik join. Broadcast tek noktadan.

**Sonuçlar / Trade-off'lar:**
- (+) Tek mesaj endpoint set (`/api/messages/{convId}` + `/api/conversations`).
- (+) Read state per-member (`ConversationMember.LastReadAt`) — group için doğal, DM için de yeterli.
- (+) Mobile `ChatScreen` tek render path; `isGroup` flag header + sender prefix farkını sağlar.
- (−) Eski `MessageDto` shape değişti (`RecipientId` → `ConversationId`, `ReadAt` kaldırıldı). Eski APK'lar 426 alır (force-update).
- (−) Voice call DM-only kalır — group call başka bir sprint'in işi.
- (Risk) Migration backfill prod'da çalışırken backup şart. Down() rollback DM-only çalışır; group oluştuktan sonra rollback edilmemeli.

---

## ADR-021 — iOS Hedefi Terk: Android-Only (KMP Refactor Iptal)

- **Tarih:** 2026-05-13
- **Durum:** Kabul edildi
- **Karar verenler:** PO (Aykut), Tech Lead, Innovation Architect

**Bağlam:**
Sprint #3'te (ADR-004) KMP + Compose Multiplatform yolu seçilmişti — iki platform tek codebase. `:data` modülü iOS için "shared"a refactor edilecekti. Production'da Android sağlam, iOS başlangıç tetiklenmemişti.

PO Aykut iOS hedefini geri çekti — somut sebepler:
1. **Apple Developer Program $99/yıl** — kapalı/hobi kullanım için pahalı
2. **Dağıtım imkansız** — App Store inceleme + TestFlight 25-cihaz/yıl sınırı + ad-hoc cert overhead. Android'deki "APK linkini WhatsApp'tan gönder" pattern'i iOS'ta yok
3. **Kullanıcı dağılımı** — Aykut'un çevresinde iOS kullanıcı sayısı düşük (~100 hedef ağ Android-dominant)
4. **Geliştirme maliyeti** — macOS + Xcode + APNs `.p8` + Apple cert + KMP `:shared` refactor → minimum 1-2 hafta iş, ROI yok

**Karar:**
**Android-only odak.** iOS terk edildi (silindi değil, tamamen "out of scope"). KMP refactor iptal. Mevcut Android-native (Kotlin + Jetpack Compose) yapı korunur.

**Sonuçlar / Trade-off'lar:**
- ✅ Kod karmaşıklığı azalır — `:data` JVM module kalır, KMP `:shared` build complexity yok
- ✅ Sprint hızı artar — iOS-side hazırlığa zaman gitmez
- ✅ Mevcut dependency'ler Android-native pattern kullanabilir (Compose, DataStore, SignalR Java client) — KMP-uyumluluk derdi yok
- ❌ Apple ecosystem kullanıcılar dışlanır — bilinçli kabul edildi
- 🔄 Geri çevrilebilir: gelecekte istenirse `:data → :shared` refactor + iOS skeleton yapılır. Mevcut kod KMP-friendly pattern'ler (Ktor multiplatform-ready, kotlinx-serialization) kullanıyor.

**ADR-004'ü kısmen geçersiz kılar:** KMP karar gerekçesi (iki platform) artık geçerli değil. ADR-004 status "Kısmen yerine geçen ADR-021 (iOS hedefi düştü, Android Kotlin+Compose tarafı korunur)".

**İlgili dosyalar:**
- `PROJECT_CONTEXT.md` — iOS başarı kriteri kaldırıldı
- `SPRINT_BOARD.md` — "iOS başlangıç" sırlanmadan silindi

---

## ADR-020 — Sprint #13 Stabilization (security audit + cleanup)

- **Tarih:** 2026-05-13
- **Durum:** Uygulandı
- **Karar verenler:** AppSec, SecOps, Red Team, Tech Lead, Knowledge Curator

**Bağlam:**
Sprint #12 (sesli arama) GetStream baseline ile prod-hazır hale geldi. Ara sprint olarak güvenlik denetimi + codebase temizliği + dokümantasyon yenileme; portföye uygun README + eksik ADR'ler yazıldı.

**Bulgular ve düzeltmeler:**
- FriendsController.ResubmitRequestAsync logical bug (reject sonrası yeniden istek → self-friend potansiyeli) → fix
- AdminController.Decide → invalid_decision artık 400 (eskiden 500)
- mimir-mobile .gitignore'a `secrets/`, `*.keystore`, `.env*` eklendi (debug keystore explicit istisna)
- AndroidManifest `usesCleartextTraffic="false"` explicit (Android 9+ default false, disiplin için)
- Ölü kod: HomeScreen.kt + MeScreen.kt silindi (bottom nav refactor sonrası unused)
- Program.cs eski TODO açıklayıcı yoruma çevrildi

**Sağlam alanlar (rapor):**
AES-256-GCM crypto, BCrypt 12, JWT HS256 + key validation, refresh token reuse-detection, rate limit, friendship gating, IDOR koruması, user/email enumeration prevention, WebRTC DTLS-SRTP, coturn HMAC short-lived, docker compose latest yasak + localhost port + RO secret mount.

**Sonuçlar:**
- OWASP Top 10 critical bulgu yok
- Sprint #14'e güvenli + temiz baseline

---

## ADR-019 — WebRTC Voice Call: GetStream Baseline + Adapter Pattern (ADR-014'ten Pivot)

- **Tarih:** 2026-05-13
- **Durum:** Uygulandı (Sprint #12)
- **Karar verenler:** PO, Tech Lead, Innovation Architect, Senior Dev #1 (Android), SecOps

**Bağlam:**
Sprint #12'de kendi yazdığımız CallManager 3 sprint debug edildi: scope cancellation race, mutex+I/O deadlock, hangup sync miss. Aykut "patch-and-pray" döngüsünü stop edip açık kaynak referans baz almayı önerdi (NIH terk — anti-pattern 26).

**Değerlendirilen Seçenekler:**
| Repo | Audio | Self-host signaling | Maintained |
|---|---|---|---|
| **GetStream/webrtc-in-jetpack-compose** ⭐ | ✅ | ✅ Ktor WebSocket (adapt edilebilir) | Ocak 2025 |
| lyh990517/WebRTC-with-Jetpack-Compose | ❌ video only | ❌ Firebase tight | ⚠️ |
| Telnyx sample | ✅ | ❌ commercial SDK | ✅ |

**Karar:**
**GetStream/webrtc-in-jetpack-compose** Apache 2.0 baseline. Bizim kullandığımız aynı WebRTC fork (`io.github.webrtc-sdk:android`). Signaling adapter ile bizim SignalR `DmHub.OfferCall/AnswerCall/...`'a bağlandı.

**Mimari:**
- `StreamPeerConnection` + `StreamPeerConnectionFactory` direkt kopyalandı (Apache 2.0 attribution)
- `SignalingClient` interface — `MimirSignalingAdapter` implementasyonu RealtimeClient'ı kullanır
- `CallSession` state machine (Idle/Outgoing/Incoming/Connecting/Connected/Ended)
- FCM payload'da SDP offer var → app dead'ken bile IncomingCall yakalanır
- UI: Mimir tema ile yeniden çizilmiş CallScreen (gradient + pulse + 3 buton)

**Sonuçlar / Trade-off'lar:**
- ✅ Test edilmiş baseline, race condition'lar geçmişte kaldı
- ✅ Video tracks Sprint #14'te 1-2 günde aktif edilebilir (foundation hazır)
- ✅ Apache 2.0 attribution README + dosya başlığında
- ⚠️ Glare resolution (concurrent simultaneous call) basit busy-reject (tie-breaker yok)
- ⚠️ Call kaydı yok (Aykut'un kararı — ephemeral, ADR-019 prensibi)

---

## ADR-018 — Presence: In-Memory PresenceTracker + SignalR Broadcast

- **Tarih:** 2026-05-09
- **Durum:** Uygulandı (Sprint #11)
- **Karar verenler:** Senior Dev #1 (.NET), SecOps, PO

**Bağlam:**
Sprint #11'de online/offline + last-seen istendi. Mimari karar: persistent storage mı (DB), in-memory mı (single-instance), distributed mı (Redis).

**Değerlendirilen Seçenekler:**
1. **In-memory `ConcurrentDictionary<Guid, int>`** ⭐ — single-instance MVP'de ideal, sıfır overhead
2. Redis-distributed — multi-replica'ya geçince mecbur, şu an gereksiz karmaşa
3. DB persist (User.IsOnline) — write amplification, scaling kötü

**Karar:**
- `PresenceTracker` singleton — connection count (`AddOrUpdate` ile multi-device safe)
- `User.LastSeenAt` DB persist (offline kullanıcıların son görülme)
- DmHub `OnConnectedAsync` → TrackConnect, `transitioned` ise arkadaşlara `PresenceChanged` broadcast
- `OnDisconnectedAsync` → TrackDisconnect + LastSeenAt update + broadcast
- `BroadcastPresenceToFriendsAsync` — ADR-016 gating, sadece kabul edilmiş arkadaşlara

**Sonuçlar:**
- ✅ Multi-device: aynı user 2 cihaz → 2 connection → biri kapansa hala online
- ✅ Privacy: sadece arkadaşlara presence (kapalı ağ)
- ⚠️ Multi-replica olunca Redis-distributed'a taşımalı (instance-local tracker farklı verir)

---

## ADR-017 — FCM Signal-Only Push (ADR-014'ü kısmen geçersiz kılar — Android tarafı)

- **Tarih:** 2026-05-09
- **Durum:** Kabul edildi (Sprint #7'de implement)
- **Karar verenler:** PO, AppSec, SecOps, Tech Lead, Senior Dev #1 (.NET), Senior Dev #1 (Android)

**Bağlam:**
T-034 push notification için ADR-014 "FCM direct" demişti. PO Firebase ekosistemine karşı çıktı ("firebase istemiyom"). 4 alternatif değerlendirildi:

| Seçenek | Sticky? | Ek app? | Real-time? | Self-host? |
|---|---|---|---|---|
| WorkManager polling | ❌ | ❌ | ❌ (~15 dk gecikme) | ✅ |
| ForegroundService persistent WS | ⚠️ (sticky) | ❌ | ✅ | ✅ |
| Email bildirimi | ❌ | ❌ | ✅ (SMTP gecikme) | ✅ |
| ntfy self-host + ntfy app | ❌ | ⚠️ (kullanıcı F-Droid'den) | ✅ | ✅ |
| FCM full ekosistem | ❌ | ❌ (Play Services zaten yüklü) | ✅ | ❌ |

PO ile mimari tartışıldı — anlaşıldı ki "Firebase = Auth + Firestore + Storage + ... + FCM" tüm paket değil, **sadece Cloud Messaging** kullanmak Signal/WhatsApp pattern'i. Mesaj **içeriği** Mimir backend'inde kalır, FCM'e sadece "uyan" sinyali gider.

**Karar:**
**Android:** FCM **signal-only** — payload `{type:"newMessage", senderUserId}`, içerik yok. Mobile uyandığında Mimir API'sinden mesajı çeker.
**iOS:** APNs direct (Sprint #8+, Apple developer hesabı + .p8 + JWT — Firebase iOS SDK kullanılmaz).

**Rationale:**
- Sticky notification yok (PO'nun en kuvvetli kısıtı)
- Kullanıcı tarafında ek app yok (Play Services stock Android'de zaten var)
- Real-time (FCM Google Play Services aracılığıyla OS-level uyandırır)
- Mesaj içeriği Google'a gitmiyor → KVKK kompozisyonu temiz, sızıntı yüzeyi minimal
- Mimir Auth/DB/Storage hala self-host (sadece push transport Google'da)
- iOS tarafında APNs direct ile **iki provider** disiplini korunur

**Sonuçlar / Trade-off'lar:**
- ✅ Mobile: `firebase-messaging` artifact + `google-services.json` — diğer Firebase paketleri yok
- ✅ Backend: `FirebaseAdmin` SDK 3.0.0 + service account JSON (`/opt/mimir/secrets/`, repo dışı)
- ✅ Push payload data-only (notification field yok) — Android sistem default bildirim üretmez, biz Notifications.kt ile manuel oluştururuz
- ⚠️ Google Play Services olmayan cihazlarda (Huawei post-2019, GrapheneOS, vs.) push çalışmaz — Türkiye'de marjinal vaka
- ⚠️ Google "Mimir için push var" görür (protokol gereği) — içerik göremez ama metadata'sı vardır
- ⚠️ Service account JSON private key — repo'ya commit edilmemeli (`.gitignore` `secrets/`)
- 🔄 iOS implementasyonu Sprint #8+ (APNs direct, dotAPNS veya manuel HTTP/2)

**İlgili tasklar:** T-051..T-066

---

## ADR-016 — Arkadaşlık Modeli: Gizli Key + Karşılıklı Onay (ADR-013'ü geçersiz kılar)

- **Tarih:** 2026-05-09
- **Durum:** Önerildi (Sprint #6 Tur 2'de implement)
- **Karar verenler:** PO, AppSec, Tech Lead, Senior Dev #2

**Bağlam:**
ADR-013'te "tüm Active kullanıcılar birbirine DM atabilir" denmişti — kapalı network argümanıyla. Kullanıcı 2026-05-09'da privacy ihtiyacı netleştirdi: "her kullanıcının kendi gizli key'i, vererek arkadaş ekler". Listede tüm kullanıcılar görünmemeli.

**Karar:**
ADR-013 supersede. Yeni model:
- `User.FriendKey` — 12 char URL-safe random, register'da üretilir, regen edilebilir
- `friendships` tablosu (RequesterId, AddresseeId, Status: Pending/Accepted/Rejected/Blocked)
- A → B'nin key'ini biliyorsa `POST /friends/requests` (key body'de) → B'ye Pending istek
- B kabul ederse → Accepted, DM açılır
- DM gating: `IFriendshipChecker.AreAccepted(a, b)` — MessagesController + DmHub her endpoint'te kontrol
- `GET /api/users/active` **kaldırılır** (privacy)
- Migration: mevcut DM çiftlerini Auto-Accepted (alice ↔ aykut korunur)

**Rationale:**
- Kullanıcı kontrolü: kim DM atabileceğini key paylaşımı ile belirler
- Spam/abuse riski azalır (sadece tanıdığı key'lerden istek gelir)
- Privacy: rastgele başkası kullanıcı adımı tahmin edip mesaj atamaz

**Sonuçlar / Trade-off'lar:**
- (+) Kullanıcı agency: kim ekleyeceğini admin değil kendisi belirler
- (+) Discovery yüzeyi yok — kim üye olduğu görünmez (sadece arkadaşları)
- (−) Onboarding sürtünme: yeni kullanıcı önce key sahibinden almalı
- (−) Mevcut UI gözden geçirme — NewChatScreen kalkıyor, FriendsScreen geliyor
- ⚠️ Migration'da var olan DM çiftleri otomatik Accepted (veri kaybı yok)

---

## ADR-015 — App Version Gate: Backend Authoritative Force Update

- **Tarih:** 2026-05-09
- **Durum:** Kabul edildi
- **Karar verenler:** AppSec, Tech Lead, Senior Dev #1

**Bağlam:**
T-039 client-side version check (Bootstrap state'inde `/api/app/version`) yumuşak — network fail durumunda bypass, kullanıcı APK'yı eski sürümle çalıştırırsa devam edebilir. Kullanıcı 2026-05-09'da netleştirdi: **eski APK'lar kesinlikle çalışmamalı** (güvenlik patch'leri kritik).

**Değerlendirilen Seçenekler:**
1. Client-side check güçlendir (offline'da app açılmasın) — UX kötü
2. Backend middleware: her authenticated request'te `X-App-Version` header check, eski → 426 Upgrade Required
3. JWT'ye versiyon claim'i göm — token üretirken ekle, every-request kontrol

**Karar:**
Seçenek 2 — Backend middleware authoritative.

**Implementation:**
- `AppVersionGateMiddleware`: Path muaf değilse `X-App-Version` header oku
- `MinAppVersion:{Android|Ios}` config'inden alınan değerle karşılaştır
- Eksik header veya `< minVersion` → 426 + `{error:"app_version_too_old",minVersion,downloadUrl,platform}`
- Muaf prefix'ler: `/api/auth/*` (login flow eski APK'ya da açık), `/api/app/version` (gate kontrol kendisi), `/health`, `/hubs/*` (SignalR — REST katmanı korur zaten)
- Mobile: Ktor client'a default header ekle (`BuildConfig.VERSION_NAME`)
- Mobile: Bootstrap'ta `/auth/refresh` 426 → ForceUpdateScreen + DataStore.clear()

**Sonuçlar / Trade-off'lar:**
- (+) Backend authoritative — client tampering veya offline bypass yok
- (+) Operasyonel kontrol: env değiştir, restart, eski APK'lar anında 426
- (−) Login endpoint'i muaf (chicken-egg: eski APK login dener, refresh'te 426 alır → ForceUpdate)
- (−) Eski APK çevrimdışı bile içerik görüntülemez (aslında zaten cache yok, online tasarım)
- ⚠️ Header yoksa missing-version değerlendirmesi 426 — tüm legacy API client'ları bloklar (Postman/curl manuel test'te `X-App-Version: 99.0.0` ile geçilebilir)

---

## ADR-014 — Push Notification Provider: APNs + FCM Direct (self-host disiplini)

- **Tarih:** 2026-05-09
- **Durum:** Yerine geçen ADR-017 (Android: FCM signal-only; iOS: APNs hala bekliyor)
- **Karar verenler:** AppSec, Innovation Architect, ML/RAG Engineer, Tech Lead, PO

**Bağlam:**
T-034 push notification implementasyonu için provider seçimi. ADR-002'de Firebase exit yapıldı; şimdi push için ne kullanacak?

**Değerlendirilen Seçenekler:**

| | APNs + FCM direct | OneSignal | AWS SNS |
|---|---|---|---|
| Setup karmaşıklığı | Orta-Yüksek (iki provider) | Düşük (tek SDK) | Orta (iki provider altında) |
| iOS uyumu | ✅ APNs direct | ✅ APNs aracılı | ✅ |
| Android | FCM direct | OneSignal aracılı | ✅ |
| Self-host | Backend kendisi | 3rd party | 3rd party (AWS) |
| KVKK / 3rd-party flow | Sadece Apple/Google | + OneSignal | + AWS |
| Maliyet | Apple developer $99/yıl, FCM free | Free tier 100k device | Free tier sonra metered |
| ADR-002 (vendor min) uyum | ✅ | ❌ | ❌ |

**Karar:**
**APNs + FCM direct** — kendi backend'imizden push.

**Rationale:**
- ADR-002 (Firebase exit) felsefesi: vendor lock-in min, KVKK self-host disiplini
- 3rd party (OneSignal/AWS) ekleme = veri akışı ek hop, KVKK kapsam genişler
- 100 kullanıcı boyutu için OneSignal SDK + dashboard overhead'i orantısız
- FCM HTTP v1 API ücretsiz (Google Android için open free service)
- APNs Apple developer hesabı zaten iOS için zorunlu (Sprint #6)

**Implementation (Sprint #5 sonu / #6):**
- Backend: `IPushSender` interface, `FcmPushSender` (Android), `ApnsPushSender` (iOS, Sprint #6)
- Yeni mesaj olunca `MessagesController` + `DmHub` recipient device tokens'ını çek + send
- Yeni tablo: `device_tokens` (UserId, Token, Platform: android|ios, RegisteredAt, RevokedAt)
- Yeni endpoint: `POST /api/users/me/device-token` (auth, device token register)
- Android: `firebase-messaging` SDK (sadece messaging — Firebase BOM full değil) ya da pure FCM HTTP + Google service-account JWT
- Quiet hours / batching: ileride ek katman

**Sonuçlar / Trade-off'lar:**
- (+) Self-host disiplini korundu (ADR-002 ile uyumlu)
- (+) Maliyet kontrol altında (Android ücretsiz, iOS Apple developer ekonomisi)
- (+) Veri akışı sadece Apple+Google (zaten platform sahipleri)
- (−) İki provider impl gerekli (iOS Sprint #6'da)
- (−) Apple/Google geçici outage = push gitmez (kabul, business critical değil — DM polling fallback'i ile mesaj kaybı yok)
- ⚠️ Service account JSON sensitive — VPS .env.prod'da saklanır, git'e gitmez
- ⚠️ Apple developer hesabı $99/yıl — iOS Sprint #6'da kullanıcıya teyit ettirilir

---

## ADR-013 — DM Yetkisi: Tüm Active Kullanıcılar Birbirine Mesaj Atabilir (arkadaş ekleme modeli yok)

- **Tarih:** 2026-05-09
- **Durum:** Kabul edildi
- **Karar verenler:** PO, Analist 1, AppSec, Tech Lead

**Bağlam:**
Mimir kapalı network. Her kullanıcı admin (Aykut) onayından geçmiş = "tanıdık". DM yetkilendirme modeli iki yol: (a) arkadaş ekleme + onay → DM, (b) Active herkes herkese DM.

**Karar:**
(b) — Tüm `UserStatus.Active` kullanıcılar birbirine DM atabilir. Arkadaş ekleme akışı yok.

**Rationale:**
- Kapalı network'te admin filtresi zaten "kim üye olabilir"i belirler — ek arkadaş onayı redundant
- 100 hedef kullanıcı boyutunda complex social graph overhead'i mantıksız
- v1 basit kalır; spam abuse riski düşük (admin tanıdıklarını filtreler)

**Sonuçlar / Trade-off'lar:**
- (+) UI basit (arkadaş listesi yerine "tüm üyeler")
- (+) Network-effect: yeni üye anında herkese erişebilir
- (−) İleride istenirse engelleme/sessize alma (block/mute) eklenebilir — ADR-013 supersede edilmez
- ⚠️ Spam yaşanırsa: rate limit DM endpoint'inde (T-031'de değerlendirilir)

---

## ADR-012 — DM Mesaj Şifreleme: AES-256-GCM At-Rest, Server-Side Key

- **Tarih:** 2026-05-09
- **Durum:** Kabul edildi
- **Karar verenler:** AppSec, SecOps, Senior Dev #1, Senior Dev #2, Tech Lead

**Bağlam:**
ADR-005'te server-side encryption seçildi (E2E değil). Sprint #4'te DM tablosu kuruluyor — `messages.body` plain text mi, encrypted mi, hangi algoritma?

**Değerlendirilen Seçenekler:**
1. Plain text (TLS in-transit yeter) — mahremiyet katmansız
2. AES-256-CBC + HMAC — manuel auth, hata-yatkın
3. AES-256-GCM (auth-encrypt) — modern AEAD, .NET built-in
4. ChaCha20-Poly1305 — alternatif AEAD, .NET 8+ destekli

**Karar:**
Seçenek 3 — AES-256-GCM at-rest.

**Rationale:**
- DB compromise senaryosunda mesaj plain text okunamaz (key ayrı env'de)
- AES-GCM authenticate-encrypt: tampering otomatik tespit
- .NET `System.Security.Cryptography.AesGcm` built-in (no extra package)
- Per-message random IV (12 byte), key sabit (32 byte env'den)
- ChaCha20 da seçenek ama AES-GCM daha yaygın test edilmiş

**Implementation:**
- `Crypto:MessageKey` env: 32 byte base64 (server-side `openssl rand -base64 32`)
- Schema: `messages.iv` (bytea 12), `messages.ciphertext` (bytea), `messages.tag` (bytea 16)
- `IMessageCrypto.Encrypt(plaintext) → (iv, ciphertext, tag)`, `Decrypt(...) → plaintext`
- Encrypt = sender side server'da (controller içinde, request body plaintext)
- Decrypt = server'da read sırasında (controller response body plaintext)

**Sonuçlar / Trade-off'lar:**
- (+) DB-only sızıntıda mesajlar güvenli (key ayrı)
- (+) AEAD ile tamper detect (saldırgan ciphertext değiştirirse decrypt fails)
- (+) E2E'ye göre çok-cihaz senkron sorunsuz (server elinde key)
- (−) Server compromise (env+DB) → mesajlar açılır (kabul edilen risk, ADR-005)
- (−) Key rotation: mevcut mesajlar eski key'le saklanır → key versioning patterni Sprint #5+'te düşünülür
- ⚠️ Anahtar `.env.prod`'dan kaybolursa eski mesajlar kalıcı kayıp — backup şart (env file Hetzner snapshot'ında var)

---

## ADR-011 — Rate Limit Stratejisi (in-memory fixed-window) + Compose Service Naming + Email Fallback

- **Tarih:** 2026-05-09
- **Durum:** Kabul edildi
- **Karar verenler:** AppSec, DevOps, Tech Lead, Senior Dev #1

**Bağlam (3 ayrı micro-karar tek ADR):**

### 1) Rate Limit
T-014: Brute-force / abuse / SMS bombing yerine artık email bombing korumasına ihtiyaç. Single-instance MVP'de Redis-distributed rate limiter overkill.

**Karar:** ASP.NET Core 9 built-in `AddRateLimiter` (System.Threading.RateLimiting). IP-bazlı fixed window:
- `auth-register`: 5/dk
- `auth-login`: 10/dk
- `auth-verify`: 30/dk
- `admin-invite`: 20/dk

**Forwarded headers:** Docker subnet'leri (172.16/12, 10/8) `KnownNetworks`'e eklendi → nginx X-Forwarded-For trust edilir, gerçek client IP rate-limit bucket'ında kullanılır.

**Trade-off:** Multi-replica deployment olunca Redis-distributed'a taşınması gerekecek (token bucket Redis'te). Sprint backlog.

### 2) Compose Service Naming
`mimir compose`'da service ismi `web` → `api`. Sebep: AykutOnPC stack'i de `web` servisini kullanıyor; aynı `aykutonpc_frontend` external network'üne join olunca DNS alias çakışması (`web` ↔ random container) AykutOnPC'nin ana site routing'ini kırıyordu. `api` alias'ı çakışma yok. Container ismi `mimir-web` olarak kalır (DNS'te bu da alias).

### 3) Email Sender Fallback
SMTP gerçek implementasyonu (MailKit 4.16.0) yazıldı (`SmtpEmailSender`). DI'da koşullu register: `Smtp:Host` config'i set ise SMTP, yoksa `ConsoleEmailSender` (mock — log'a yazar).

Sprint #3'te kullanıcı gerçek SMTP host set edince otomatik geçer.

**Sonuçlar:**
- (+) Brute-force koruması canlı (smoke test'te 10. denemede 429 döndü)
- (+) AykutOnPC site etkilenmedi alias fix sonrası
- (+) SMTP gerçek/mock fallback temiz pattern
- (−) Rate limit single-instance scope — multi-replica gerek olunca Redis migration
- ⚠️ **Kritik nginx /mimir/ patch SÜRDÜRÜLEMEZ** — AykutOnPC auto-deploy `git reset --hard` ile silebiliyor. Sprint #3'e "AykutOnPC repo'sunda kalıcılaştır" todo'su eklendi.

---

## ADR-010 — SMS Verification İptal — Onboarding 4-Aşamadan 3-Aşamaya İndirildi

- **Tarih:** 2026-05-09
- **Durum:** Kabul edildi
- **Karar verenler:** Kullanıcı, PO, AppSec, SecOps, Senior Dev #2

**Bağlam:**
ADR yapımı sırasında 4-aşama gate (email verify + SMS OTP + admin onay) tasarlandı. SMS provider (Netgsm/Twilio/Vonage) seçimi T-010 brief'i olarak Sprint #2'ye eklendi.

Kullanıcı uygulama maliyet analizi sonrası SMS gönderimi istemiyor: provider başına yıllık 50-200 TL + abuse koruması karmaşıklığı + KVKK telefon işleme yüzeyi.

**Değerlendirilen Seçenekler:**
1. SMS verify devam — provider seç, abuse koruması yaz, maliyet kabul et
2. SMS verify iptal — Phone alanı tamamen kaldır
3. SMS verify iptal — Phone alanı **opsiyonel** olarak kalsın (verify yok, bilgi-only)

**Karar:**
Seçenek 3 — SMS verify iptal, Phone alanı opsiyonel.

**Rationale:**
- Davet+admin onaylı kapalı network: SMS verification'ın sağladığı "telefon = kişi" güvencesi zaten admin'in tanıdığı süzgeciyle gerçekleşiyor. Çift kontrol gereksiz.
- Maliyet ↓ (sıfır) + karmaşıklık ↓ (abuse koruması yok)
- Phone field nullable kalır → admin tanıdığını phone'la eşleştirmek isterse veri toplanır. Data minimization açısından "opsiyonel + verify yok" KVKK uyumlu.
- Email verify + admin manuel onayı, kapalı network için yeterli güvence.

**Etkiler:**
- `UserStatus` enum: `PendingSms` value kaldırıldı → 3 aşama (PendingEmail → PendingAdmin → Active)
- `OtpCode` entity: `Type` kolonu kaldırıldı (sadece email kullanılıyor); `OtpType` enum silindi
- `User.Phone`: nullable string (`Phone?`)
- `appsettings.json`: `Sms` section silindi
- `.env.prod` ve `docker-compose.prod.yml`: `SMS_PROVIDER`, `SMS_API_KEY` env'leri silindi
- 3. EF migration: `DropSmsVerification` (Phone nullable + otp_codes.Type drop)
- T-010 (SMS provider brief) → **iptal**, shelf
- T-014: SMS bombing korumasıydı → email bombing korumasına dönüşür (rate limit hâlâ değerli)
- TECH_RADAR: Netgsm, Twilio, Vonage (Assess) → kaldırıldı

**Sonuçlar / Trade-off'lar:**
- (+) Sıfır SMS maliyeti
- (+) Sıfır abuse riski (SMS bombing yüzeyi yok)
- (+) Daha basit onboarding flow, az kod
- (−) Telefon-bazlı identity yok — admin'e bağlı güven
- (−) Self-service şifre sıfırlama "SMS link" pattern'i mümkün değil → email-based reset Sprint #2 sonu öncesi yapılır
- ⚠️ Future-proof: ileride opt-in 2FA (TOTP, WebAuthn) eklenebilir, ADR-010 supersede edilmez

---

## ADR-009 — Ürün Adı: InstaClone → Mimir

- **Tarih:** 2026-05-09
- **Durum:** Kabul edildi
- **Karar verenler:** Kullanıcı, PO, Innovation Architect, Knowledge Curator

**Bağlam:**
"InstaClone" geçici kod adıydı (mobile repo `JavaInstagramClone`'dan miras). Üretim öncesi marka kararı: kullanıcı **mitolojik + anlamı kapalı** (sadece soranlar öğrenebilsin) bir ürün adı istedi.

**Değerlendirilen Seçenekler:**
1. Anadolu/Türk: Yada, Umay, Inara
2. Dış: Mimir (Norse), Khepri (Mısır)

**Karar:**
**Mimir** (Norse mitolojisi).

**Rationale:**
- Bilgelik kuyusunun başı — sırların ve hafızanın koruyucusu, kendisine danışılır
- Mesajlaşma + hafıza + güvenli iletişim metaforu doğal rezonans
- Az bilinen (Marvel/Thor mitolojisinde geçmedi, mainstream değil) → "anlamı sadece soranlar öğrensin" kriterine uyar
- Kısa (5 harf), URL-safe, telaffuzu net

**Uygulanan Rename Etkileri:**
- Repo: `instaclone-api` → `mimir-api` (https://github.com/Aykuttonpc/mimir-api, ilk push tamam)
- VPS: `/opt/instaclone` → `/opt/mimir`
- Container'lar: `instaclone-db/redis` → `mimir-db/redis` (yeniden ayağa, healthy)
- Compose project name: `instaclone` → `mimir`
- Volume'lar: `instaclone_*` → `mimir_*` (eski boş volume'lar silindi, sıfır veri kaybı)
- Path prefix: `/insta/` → `/mimir/`
- DB user + name: `instaclone` → `mimir`
- JWT: `instaclone-api` / `instaclone-mobile` → `mimir-api` / `mimir-mobile`
- Mobile repo (Sprint #3): `instaclone-mobile` → `mimir-mobile`
- 7 `.claudeteam/` dökümanında string referansları güncellendi

**Maliyet:**
~30 dk. Yatırım küçükken (sadece deployment iskelet + boş DB) yapıldığı için ucuz. Erteleme ileride 10x pahalanırdı.

---

## ADR-008 — Repo Yapısı: Backend + Mobile Ayrı Repolar

- **Tarih:** 2026-05-08
- **Durum:** Kabul edildi
- **Karar verenler:** Tech Lead, DevOps, PO

**Bağlam:**
Mevcut [JavaInstagramClone](https://github.com/Aykuttonpc/JavaInstagramClone.git) repo'su Android tarafı. Backend yeni — nereye gitsin?

**Değerlendirilen Seçenekler:**
1. Monorepo — backend/ klasörü mevcut repo'ya ekle
2. Ayrı repo — `mimir-api` (backend) + mevcut repo (mobile, ileride `mimir-mobile`'a rename)

**Karar:**
Seçenek 2 — Ayrı repo.

**Rationale:**
Backend ve mobile farklı CI/CD hattı, farklı dil ekosistemi, farklı versiyon hızı. Backend daily deploy, mobile haftalık APK release. Monorepo CI workflow karmaşıklığı (path filter, conditional jobs) küçük takım için overhead.

**Sonuçlar / Trade-off'lar:**
- (+) CI/CD basit ve ayrı
- (+) Versiyon hatları bağımsız
- (+) `gh repo` görünümü temiz
- (−) API contract değişiminde iki repo'da koordinasyon gerek (kabul — küçük scope)
- ⚠️ Mevcut `JavaInstagramClone` repo'su ileride `mimir-mobile`'a rename edilecek (Sprint #3'te Java kodu temizlenince)

---

## ADR-007 — Mobile-Only MVP: Path Prefix Routing + Domain Deferred

- **Tarih:** 2026-05-08
- **Durum:** Kabul edildi (ADR-006'nın yerine geçer)
- **Karar verenler:** DevOps, Tech Lead, AppSec, PO

**Bağlam:**
ADR-006'da subdomain `insta.aykutonpc.com` kararı verildi — ama bu **web client** varsayımıyla. Kullanıcı netleştirdi: v1 sadece mobil. Domain + Let's Encrypt + DNS işi mobile-only senaryoda gereksiz overhead.

**Değerlendirilen Seçenekler:**
1. Subdomain (ADR-006) — domain alımı + DNS + ayrı cert + nginx server block
2. Path prefix — `https://178.104.198.249/mimir/` mevcut nginx + self-signed + mobile cert pinning
3. Ayrı port — `https://178.104.198.249:9443` ayrı cert ayrı listen

**Karar:**
Seçenek 2 — Path prefix routing. Mevcut nginx'e `location /mimir/` bloğu eklenir, mimir-web:9001'e proxy.

**Rationale:**
- Mobil app TLS doğrulamasını **certificate pinning** ile yapar — browser uyarısı umurda değil
- Mevcut bootstrap self-signed cert reuse → cert/domain işi sıfır
- JWT Bearer token (cookie değil) → path prefix scope sorun yok
- Domain ileride lazım olursa subdomain'e taşımak nginx config tek değişiklik

**Sonuçlar / Trade-off'lar:**
- (+) Domain alma + DNS + cert işi shelf — sprint hızı ↑
- (+) Mevcut altyapı reuse, ek maliyet sıfır
- (−) Cert rotation = APK rebuild + force update zorunlu (cert pinning gereği)
- (−) Web client (v2 düşüncesi) için domain gerekecek — o zaman ADR yazılır
- ⚠️ Mobil app'e cert public key fingerprint bundle'lanmalı (T-007 backlog)

**ADR-006'nın Durumu:**
ADR-006 "Yerine geçen ADR-007". Subdomain kararı domain alımı yapıldığında tekrar gündeme alınacak — ileri tarihte yeni ADR yazılır.

---

## ADR-006 — Subdomain `insta.aykutonpc.com` + Mevcut VPS'e Ek Proje Pattern'i

- **Tarih:** 2026-05-08
- **Durum:** ⚠️ Yerine geçen **ADR-007** (mobile-only context'te path prefix yeterli)
- **Karar verenler:** SecOps, DevOps, Tech Lead, PO

**Bağlam:**
Hetzner CPX22 VPS hazır + üzerinde AykutOnPC adlı .NET 9 stack çalışıyor (Postgres, Redis, Nginx, %0.01 load, 3 GB free RAM). Mimir'u nereye/nasıl deploy edeceğiz?

**Değerlendirilen Seçenekler:**
1. Yeni VPS al — €4/ay ek maliyet, izolasyon ↑
2. Mevcut VPS'e ek proje (path prefix `/mimir/`) — JWT auth path karışıklığı, nginx routing kompleks
3. Mevcut VPS'e ek proje (subdomain `insta.aykutonpc.com`) — temiz routing, ayrı cert

**Karar:**
Seçenek 3 — VPS rehberindeki "Strateji A" pattern'i. `/opt/mimir` klasörü, kendi `docker-compose.prod.yml`, container `127.0.0.1:9001` (8080 mevcut), mem_limit 1GB, mevcut nginx'e yeni `server` bloğu, ayrı Let's Encrypt cert.

**Rationale:**
Mevcut VPS bol kaynaklı. İzolasyon container seviyesinde yeterli. Subdomain JWT cookie/CORS açısından temiz. Maliyet sıfır.

**Sonuçlar / Trade-off'lar:**
- (+) Ek maliyet yok
- (+) Mevcut nginx + cert mekanizmasını reuse
- (−) AykutOnPC ile kaynak çakışması riski (mem_limit zorunlu)
- (−) Bir VPS down olursa iki proje birden etkilenir (kabul, kritiklik düşük)

---

## ADR-005 — Mesaj Şifreleme: Server-Side (TLS + AES-256 at-rest)

- **Tarih:** 2026-05-08
- **Durum:** Kabul edildi
- **Karar verenler:** AppSec, Tech Lead, PO

**Bağlam:**
Mesaj şifreleme modeli: server-side (admin okuyabilir) mi, E2E (kimse okuyamaz) mı?

**Değerlendirilen Seçenekler:**
1. Server-side — TLS in-transit + AES-256 at-rest, admin DB'den okuyabilir
2. E2E (Signal protokolü) — sadece gönderen + alıcı okuyabilir, çok-cihaz senkronu zor
3. Hybrid — normal mesaj server-side, "secret chat" modu E2E

**Karar:**
Seçenek 1 — Server-side encryption.

**Rationale:**
Kullanıcı kişisel mesajlaşmasını yürütüyor + admin kendisi → çok-cihazda history senkronu kritik. E2E key kaybı = mesaj kaybı. Mimari karmaşıklığı 5x. v1 için server-side yeterli.

**Sonuçlar / Trade-off'lar:**
- (+) Çok-cihaz senkron kolay
- (+) Admin moderasyon mümkün (gerekirse)
- (+) Mimari basit, deliver hızlı
- (−) DB compromise = mesaj kaybı (AES-256 at-rest hafifletir ama key access olursa güvenli değil)
- (−) "True privacy" beklentisi olan kullanıcı için yeterli değil — proje kapsamı bunu istemiyor
- ⚠️ İleride hybrid modu eklenebilir (Sprint backlog)

---

## ADR-004 — Mobile Cross-Platform: Kotlin Multiplatform + Compose Multiplatform

- **Tarih:** 2026-05-08
- **Durum:** Kısmen yerine geçen ADR-021 (iOS hedefi düştü; Kotlin + Compose Android tarafı korunur, KMP `:shared` refactor iptal)
- **Karar verenler:** Senior Dev #3, Tech Lead, Innovation Architect, PO

**Bağlam:**
Kullanıcının iOS cihazı da var → iOS desteği gerekli. Native iki ayrı kod tabanı (Swift + Kotlin) maliyetli. Cross-platform framework seçimi.

**Değerlendirilen Seçenekler:**
1. Kotlin Multiplatform + Compose Multiplatform — Kotlin tek dil, native UI, JetBrains backed
2. Flutter — Dart, en olgun ekosistem, hot reload, geniş paket havuzu
3. React Native — JS, web ekibi varsa cazip — yok

**Karar:**
Seçenek 1 — KMP + CMP.

**Rationale:**
Branch zaten `kotlin-rewrite`. Kotlin yatırımı korunur. CMP iOS desteği 1.7+ ile production-ready (2026 itibariyle stable). Tek dil = single team yetkinliği. Native widget performansı.

**Sonuçlar / Trade-off'lar:**
- (+) Mevcut Kotlin migration'u boşa gitmez
- (+) Native performance, native widget
- (+) Tek dil
- (−) iOS HIG için bazen SwiftUI bridging gerekebilir
- (−) Flutter kadar olgun ekosistem yok (kütüphane sayısı az)
- ⚠️ Sprint #2'de iskelet kurulurken iOS toolchain (Xcode) hazırlığı şart

---

## ADR-003 — Backend Stack: ASP.NET Core 9 + Postgres + EF + SignalR + Redis + MinIO

- **Tarih:** 2026-05-08
- **Durum:** Kabul edildi
- **Karar verenler:** Senior Dev #1, Senior Dev #2, Tech Lead, PO

**Bağlam:**
Self-host backend stack'i seçimi. Performans, ekip yetkinliği, mevcut altyapıyla uyum.

**Değerlendirilen Seçenekler:**
1. ASP.NET Core 9 + Postgres — mevcut VPS'te aynı stack
2. Node.js (NestJS) + Postgres — JS ekosistem
3. Go (Echo/Fiber) + Postgres — performans odaklı

**Karar:**
Seçenek 1 — ASP.NET Core 9 + PostgreSQL 16 + EF Core + SignalR + Redis 7 + MinIO.

**Rationale:**
Mevcut VPS'te aynı .NET 9 runtime çalışıyor. Container base image, deploy script, monitoring pattern reuse edilebilir. SignalR DM real-time için doğal çözüm. EF Core migration disiplini iyi.

**Sonuçlar / Trade-off'lar:**
- (+) Container reuse, deploy hızı
- (+) Mevcut deploy.sh / runbook pattern'leri
- (+) SignalR — WebSocket için en olgun .NET çözümü
- (−) Mobil ekosistem perspektifinden Node.js daha "yaygın", ama ekip yetkinliği belirleyici
- ⚠️ Versiyon hizalaması: ASP.NET Core 8 değil **9** — VPS'te 9 var

---

## ADR-002 — Firebase'den Self-Host'a Geçiş

- **Tarih:** 2026-05-08
- **Durum:** Kabul edildi
- **Karar verenler:** Kullanıcı, PO, AppSec, Tech Lead

**Bağlam:**
Mevcut Mimir Firebase BOM 33.1.2 kullanıyor: Auth, Firestore, Storage, Analytics. Kullanıcı bu bağımlılığı kaldırıp kendi Hetzner VPS'inde self-host etmek istiyor. Sebepler: vendor lock-in, maliyet kontrolsüzlüğü (kullanıcı arttıkça okuma/yazma faturası), KVKK veri lokasyon kontrolü, Google bağımlılığını minimize etme.

**Değerlendirilen Seçenekler:**
1. Tam self-host — tüm Firebase SDK'larını çıkar, kendi backend kur
2. Hibrit — Auth'ı kendi backend'inde, Firestore'da kal
3. Status quo — Firebase'de kal

**Karar:**
Seçenek 1 — Tam self-host. Auth + DB + Storage + Analytics → kendi backend.

**Rationale:**
Kullanıcının vizyonu net: kapalı, kişisel network + admin onay + kişisel mesajlaşma + yüksek güvenlik. Bu vizyon Firebase'in pattern'iyle (open client SDK, kullanıcı self-service Auth) çelişiyor. Hibrit kompleks (iki auth context). Tek atışta self-host'a geç.

**Sonuçlar / Trade-off'lar:**
- (+) Tam veri kontrolü (KVKK, log, backup)
- (+) Maliyet öngörülebilir (VPS sabit, Firebase metered)
- (+) Custom logic (admin onay flow Firebase'de zor)
- (−) Yazılacak kod miktarı ↑↑ (auth, OTP, storage, analytics — hepsi)
- (−) Operasyonel sorumluluk (uptime, backup, security patch — DevOps/SRE'ye düşer)
- (−) Bug surface ↑ (Firebase battle-tested → kendi kod yeni)
- ⚠️ Sprint #2-3 boyunca Firebase'le **paralel çalışma yok** — branch ayrı, eski kod silinene kadar dokunma

---

## ADR-001 — Enterprise Takım Context'i Bu Projede Aktive Edildi

- **Tarih:** 2026-05-08
- **Durum:** Kabul edildi
- **Karar verenler:** Kullanıcı, PO

**Bağlam:**
Global enterprise takım promptu (`~/.claude_enterprise_team.md`) zaten yüklüydü. Bu projede ek olarak `.claudeteam/` context dizini açıldı → karar geçmişi, mimari, sprint disiplini bu repo'da yaşar.

Tetikleyen vizyon: Firebase'den self-host VPS'e geçiş + admin-onaylı kapalı sosyal ağ + güvenli kişisel mesajlaşma. Çok-epic'li bir iş, scope ve kararlar kayıt altında olmalı.

**Karar:**
`/enterpriseteam` slash command ile `~/.claude/team-template/` üzerinden bootstrap yapıldı.

**Sonraki adımlar:**
- T-001: İlk takım toplantısı (scope + kritik kararlar)
- T-002: PROJECT_CONTEXT.md ve ARCHITECTURE.md gerçek içerikle doldurulacak
- T-003: Mevcut stack TECH_RADAR.md'ye haritalanacak
- T-004: ADR-002 (Firebase exit kararı)
