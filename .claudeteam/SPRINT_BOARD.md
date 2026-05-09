# Sprint Board

> Aktif sprint. WIP'i bir kişide max 1-2 ile sınırla.
> Tarihler: ISO format (YYYY-MM-DD).

## Aktif Sprint

- **Sprint:** #6 — iOS + Push Notification + KMP Refactor
- **Başlangıç:** 2026-05-09 (Sprint #5 kapandı aynı gün)
- **Hedef bitiş:** [macOS erişimi sonrası başlar]
- **Sprint hedefi:** iOS target aktif et (KMP `:shared` modülüne refactor, RealtimeClient KMP-uyumlu hale gelir), APNs+FCM-direct push notification implementation (ADR-014), kalan Sprint #5 deferred.

## ✅ Sprint #5 KAPANDI (2026-05-09)

**Hedef:** Polling → SignalR real-time, mesaj UX (edit/delete/typing), push provider kararı, force-update + auto-login.
**Sonuç:** **WhatsApp-class DM canlı** — anlık iletim + typing indicator + edit/delete + okundu badge + auto-login + force-update altyapısı.

| Hedef | Durum |
|---|---|
| T-040 Davet listesi + revoke (ekstra fix) | ✅ |
| T-038 Auto-login (DataStore + refresh) | ✅ |
| T-039 Force-update min version + ForceUpdateScreen | ✅ |
| T-036 Push notif provider brief — ADR-014 (APNs+FCM-direct) | ✅ |
| T-037 SignalR mobile client (polling → events) | ✅ |
| T-035 Mesaj edit + soft delete (long-press + dialog) | ✅ |
| T-033 Typing indicator (debounced send + peer status) | ✅ |

### Sprint #5 deferred → Sprint #6
- T-034 Push notification impl (iOS APNs + Android FCM birlikte, KMP refactor sırasında)
- T-017 Cert pinning P2 (LE cert kullanılıyor, opsiyonel)

---

## ✅ Sprint #4 KAPANDI (2026-05-09)

**Hedef:** 1-1 DM uçtan uca + AES-256-GCM at-rest + Mobile ChatList/ChatScreen.
**Sonuç:** **Backend + Mobile DM canlı**. Polling-based real-time hissi (5sn aralık), AES-GCM round-trip OK, kullanıcı `aykut` mobile'dan alice'e mesaj gönderdi, sohbet listesi + balonlar çalışıyor.

| Hedef | Durum |
|---|---|
| `Message` entity + AES-256-GCM crypto + 5. migration | ✅ T-027 |
| `MessagesController` 4 REST endpoint | ✅ T-028 |
| `DmHub` (SignalR — backend hazır, mobile polling kullanıyor) | ✅ T-029 (backend) |
| `UsersController` active users list | ✅ T-032 |
| `ChatListScreen` + conversations + unread badge + FAB | ✅ T-030 |
| `ChatScreen` + polling 5sn + auto mark-as-read + okundu badge | ✅ T-031 |
| Read receipts UI (kısmen) | ✅ T-033 partial |
| `NewChatScreen` active user picker + debounced search | ✅ |

### Sprint #4 deferred → Sprint #5
- SignalR mobile client (polling'i değiştir, T-029 mobile yarısı)
- T-034 Push notifications (cihaz kapalıyken)
- T-036 Push provider brief (APNs+FCM-direct vs OneSignal)
- T-035 Message edit + soft delete

---

## ✅ Sprint #3 KAPANDI (2026-05-09)

**Hedef:** KMP-ready Android Compose iskelet + eski Java/Firebase temizlik + nginx kalıcılaştırma.
**Sonuç:** **Mobile MVP production-ready** — APK'da end-to-end auth flow + admin paneli + register + şifre değiştirme + gerçek email gönderimi (iCloud SMTP) çalışıyor.

| Hedef | Durum |
|---|---|
| nginx /mimir/ patch AykutOnPC repo'sunda commit | ✅ T-018 |
| KMP-ready Android Compose iskelet (`:app` + `:data`) | ✅ T-011 |
| Java/Firebase eski kod tam temizlik | ✅ T-019 |
| RegisterScreen + EmailSentScreen + Login linki | ✅ T-022 |
| Admin Paneli (davet üret + onay/red + share) | ✅ T-026 |
| Real SMTP (iCloud, MailKit 4.16) — gerçek email | ✅ T-023 |
| Admin/user şifre değiştirme + refresh token revoke | ✅ T-024 |

### Sprint #3 deferred (Sprint #4+'a)
- T-017 Cert pinning (P2 — LE cert var, opsiyonel ek katman)
- T-025 Redis-distributed rate limit (multi-replica öncesi yetiyor in-memory)

---

## ✅ Sprint #2 KAPANDI (2026-05-09)

**Hedef:** Self-host backend MVP + 3-aşama onboarding flow (SMS iptal sonrası).
**Sonuç:** **Backend production-ready**. `https://aykutonpc.com/mimir/` üzerinden tam auth flow + RBAC + rate limit + email fallback çalışıyor.

| Hedef | Durum |
|---|---|
| VPS iskelet (db + redis) | ✅ T-006 |
| ASP.NET Core 9 backend + Initial migration | ✅ T-007 |
| Domain entity'ler + 3 migration (User, Invitation, OtpCode, AdminApproval) | ✅ T-008 |
| 3-aşama onboarding endpoint'leri + admin RBAC | ✅ T-009 (ADR-010) |
| nginx `/mimir/` path prefix | ✅ T-015 (geçici, kalıcılaştırma Sprint #3) |
| GitHub repo + push | ✅ T-016 |
| Mimir naming + ADR-009 | ✅ T-RENAME |
| SMS verify iptal + ADR-010 | ✅ T-010 cancelled |
| Rate limiter (Redis-yerine in-memory) | ✅ T-014 (ADR-011) |
| SMTP gerçek impl (MailKit) + mock fallback | ✅ ADR-011 |
| DataProtection-Keys persistent volume | ✅ |
| Compose service `web` → `api` (alias çakışması fix) | ✅ ADR-011 |
| Bootstrap admin seed | ✅ |

---

---

## 📋 Todo (Sprint #6 — iOS + Push + KMP Refactor)

| ID | Başlık | Sahip | Tahmin | Notlar |
|---|---|---|---|---|
| T-041 | iOS toolchain teyit (macOS + Xcode erişimi) | PO + Senior Dev #3 | - | Kullanıcı: Mac mevcut mu, Xcode hazır mı, Apple developer hesabı var mı ($99/yıl). Bu olmadan Sprint #6 başlamaz. |
| T-042 | KMP refactor: `:data` → `:shared` + iOS target | Senior Dev #3 + Innovation Architect | 1-2 gün | Kotlin Multiplatform plugin + Compose Multiplatform plugin. Ktor engine: OkHttp → multi (OkHttp Android, Darwin iOS). RealtimeClient KMP'ye taşı (Microsoft SignalR Java client iOS'ta yok → SignalR.Client.Kotlin veya manual WebSocket). |
| T-034a | Backend: device token + FCM push send | Senior Dev #1 + AppSec | 4 saat | Yeni tablo `device_tokens`. POST /api/users/me/device-token. `IPushSender` + `FcmPushSender` (Google service account JWT). Yeni mesaj olunca recipient device tokens → FCM HTTP v1. |
| T-034b | Backend: APNs push send (iOS) | Senior Dev #1 + AppSec | 4 saat | `ApnsPushSender` (Apple p8 key + JWT). Service factory: platform'a göre seçim. |
| T-034c | Mobile: FCM SDK + token register + handler | Senior Dev #3 | 3 saat | `com.google.firebase:firebase-messaging` (sadece messaging, BOM değil). google-services.json gerekli — Apple developer + FCM project setup. Token alma + backend kayıt + notification handler. |
| T-034d | Mobile iOS: APNs entegrasyon (KMP) | Senior Dev #3 | 3 saat | iOS UserNotifications framework. Token register backend'e. Sprint #6 öncesi T-041 zorunlu. |
| T-043 | Mobile: APK distribution channel | DevOps + Tech Lead | 2 saat | Şu an local APK; T-039'da `APP_DOWNLOAD_URL_ANDROID` boş. Cloud storage (Hetzner Object Storage / S3) public URL veya GitHub Releases. Build → upload → URL set. |
| T-017 | Cert pinning (P2 → opsiyonel) | Senior Dev #3 + AppSec | 1 saat | LE cert üzerinde MITM ek katman. Cert rotation = APK rebuild. P2. |
| T-044 | iOS: KMP build + IPA + sideload | Senior Dev #3 + DevOps | 4 saat | Kotlin/Native iOS framework + Xcode SwiftUI/UIKit + Apple developer signed IPA. TestFlight olmadan dev provisioning ile sideload. |

## 🚧 In Progress

| ID | Başlık | Sahip | Başlangıç | Notlar |
|---|---|---|---|---|

## 👀 Review

| ID | Başlık | Sahip | PR/Branch | Bekleyen |
|---|---|---|---|---|

## ✅ Done

| ID | Başlık | Tamamlanan | Notlar |
|---|---|---|---|
| T-000 | `.claudeteam/` bootstrap (template'ten kopyalandı) | 2026-05-08 | `~/.claude/team-template/` üzerinden |
| T-001 | İlk takım toplantısı: scope ve kritik kararlar | 2026-05-08 | 6 soru cevaplandı, kullanıcı onayladı |
| T-002 | `PROJECT_CONTEXT.md` + `ARCHITECTURE.md` doldur | 2026-05-08 | Tam içerik (proje + stack + akışlar + tech debt) |
| T-003 | Mevcut stack'i `TECH_RADAR.md`'ye haritala | 2026-05-08 | Firebase→Hold, .NET 9/Postgres/Redis→Adopt, KMP+CMP→Trial |
| T-004 | ADR-002 Firebase exit | 2026-05-08 | DECISIONS.md'de yazıldı |
| T-005 | Hedef backend stack araştırma | 2026-05-08 | Brief gereksiz — ASP.NET 9 doğrudan kabul (mevcut VPS uyumu) |
| T-006 | VPS `/opt/mimir` iskeleti + DB + Redis ayağa | 2026-05-08 | mimir-db (Postgres 16) + mimir-redis (Redis 7) **healthy**. .env.prod 600 perm, secret'lar VPS-side generated. AykutOnPC stack'ine zarar yok. |
| T-016a | Local backend repo (D:\Projeler\mimir-api) git init + ilk commitler | 2026-05-08 | 3 commit: init, T-006 deployment, gitignore fix |
| T-016 | GitHub backend repo `mimir-api` aç + ilk push | 2026-05-09 | https://github.com/Aykuttonpc/mimir-api — kullanıcı manuel açtı, 4 commit pushlandı |
| T-RENAME | Ürün adı InstaClone → Mimir rename (ADR-009) | 2026-05-09 | Local + VPS + dökümanlar tam senkron. Mobile repo Sprint #3'te yeniden adlandırılacak. |
| T-007 | ASP.NET Core 9 backend iskelet + Initial migration + Dockerfile + VPS deploy | 2026-05-09 | mimir-web container **healthy**, EF Core migration uygulandı (`users` tablosu canlı), `/health` + `/health/ready` 200. Image `mimir-api:b23d333`. Startup migration MVP pattern'iyle. |
| T-015 | nginx `/mimir/` path prefix patch + reload | 2026-05-09 | aykutonpc nginx config'ine location bloğu eklendi (yedek alındı). `https://aykutonpc.com/mimir/health` → 200 (Let's Encrypt cert üzerinden). AykutOnPC site etkilenmedi. |
| T-008 | PostgreSQL şema v1 (Invitation + OtpCode + AdminApproval + RefreshToken) | 2026-05-09 | 5 entity, 6 tablo VPS'te canlı. 4 migration uygulandı. |
| T-010 | SMS provider seçim brief'i | 2026-05-09 | **İPTAL** — ADR-010 (SMS verification kaldırıldı, maliyet/karmaşıklık) |
| T-009 | 3-aşama onboarding endpoint'leri | 2026-05-09 | 8 endpoint çalışıyor: register/verify-email/login/refresh/logout + admin invitations/users-pending/approve. End-to-end smoke test başarılı. RBAC (admin policy) çalışıyor. JWT access + refresh rotation + reuse detection. |
| T-014 | Rate limit (in-memory fixed window) | 2026-05-09 | 4 policy: auth-register (5/dk), auth-login (10/dk), auth-verify (30/dk), admin-invite (20/dk). Smoke test'te 10. login fail'de 429 döndü ✅ (ADR-011). |
| T-Hardening | Sprint #2 hardening: SmtpEmailSender + DataProtection-Keys volume + compose service rename + ForwardedHeaders | 2026-05-09 | Ek paket MailKit 4.16.0 (vulnerability fix), DP-Keys volume + Dockerfile chown, service `web`→`api` (alias fix), Docker subnetleri ForwardedHeaders trust |
| T-018 | nginx `/mimir/` patch'in AykutOnPC repo'sunda kalıcılaştırılması | 2026-05-09 | AykutOnPC commit `7c58362`, GitHub Actions auto-deploy ~2 dk içinde uyguladı. Artık her deploy'da git tracked, silinmez. |
| T-011 | KMP-ready Android Compose iskelet | 2026-05-09 | `:app` (Compose UI) + `:data` (Ktor + models) modüller. Branch `kmp-rewrite`. APK build. Login + Pending + Home + Register + EmailSent screens. |
| T-019 | Eski Java/Firebase Android kodu temizliği | 2026-05-09 | Tüm `app/src/main/java/com/aykutcincik/javainstagramclone/*` + Firebase BOM + Picasso + google-services.json + eski layout XML'leri silindi. Branch `kmp-rewrite`'a Kotlin-only. |
| T-022 | Register + EmailSent UI + LoginScreen entry link | 2026-05-09 | RegisterScreen (davet token + email + username + password + opsiyonel telefon + validation). EmailSentScreen (verify-email talimat). |
| T-026 | Admin Paneli mobile UI | 2026-05-09 | AdminScreen: davet üret kartı (note + expiryDays + copy/share Android intent) + bekleyen kullanıcı listesi (onayla/reddet + Toast). AdminApi sınıfı (token-bound). |
| T-023 | Real SMTP (iCloud) | 2026-05-09 | MailKit 4.16.0 + SmtpEmailSender. `Smtp:Host=smtp.mail.me.com`, `:Port=587`. Smoke test: `aykutcincik+mimirtest@icloud.com` adresine register sonrası email gönderildi (log'da `Email gönderildi`). |
| T-024 | `POST /api/auth/change-password` + ChangePasswordScreen | 2026-05-09 | Backend: BCrypt verify + new ≠ old check + tüm refresh token revoke. Mobile: HomeScreen → ChangePasswordScreen → success → logout. Test: 4 validation path doğrulandı (yanlış current → 400, same → 400, kısa → 400, no-auth → 401). |
| T-027 | Message entity + AES-256-GCM crypto + 5. migration | 2026-05-09 | `messages` tablosu canlı, key env'den (32 byte). |
| T-028 | MessagesController REST (4 endpoint) | 2026-05-09 | conversations, with/{id}, send, mark-read. AES-GCM round-trip smoke test başarılı (`Selam alice...` mesajı encrypt → DB → decrypt). |
| T-029 (backend) | DmHub real-time + typing | 2026-05-09 | Group-per-user pattern, OnConnected/OnDisconnected, SendMessage/MarkAsRead/Typing. Mobile client Sprint #5'te (polling-replace). |
| T-032 | UsersController active list + arama | 2026-05-09 | EF Functions Like substring search. Limit 1-200. Test: alice listede. |
| T-030 | ChatListScreen | 2026-05-09 | Conversations LazyColumn + avatar initials + unread badge + FAB + Refresh. Empty state. |
| T-031 | ChatScreen + polling 5sn | 2026-05-09 | LazyColumn + balon + auto-scroll-to-bottom + auto mark-as-read + send + okundu badge. SignalR client Sprint #5'te. |
| T-033 partial | Read receipt UI | 2026-05-09 | Auto mark-as-read açıkken peer mesajlarını okur, "okundu" badge sender mesajlarında ReadAt set olunca görünür. |
| T-040 | AdminScreen davet listesi + revoke | 2026-05-09 | GET/DELETE /api/admin/invitations + UI 3. card. Token plain text gösterilmez (hash). Smoke: 3 davet listede görünüyor. |
| T-038 | Auto-login (DataStore JWT persist + refresh) | 2026-05-09 | App startup Bootstrap → DataStore.load → /auth/refresh → success → HomeScreen direkt. Logout/pwd-change → clear. |
| T-039 | Force-update min version | 2026-05-09 | Backend GET /api/app/version (config-bazlı). Mobile Bootstrap'ta version check → ForceUpdateScreen blocking. BuildConfig.VERSION_NAME karşılaştırma. |
| T-036 | Push notif provider brief — ADR-014 | 2026-05-09 | APNs+FCM-direct seçildi (self-host disiplini, ADR-002 uyumu). T-034'te impl Sprint #6'da. |
| T-037 | SignalR mobile client | 2026-05-09 | com.microsoft.signalr:signalr:8.0.0 + RxJava3 köprü. RealtimeClient (events SharedFlow). ChatScreen polling kaldırıldı, event-driven. Connect status hint top bar. |
| T-035 | Mesaj edit + soft delete | 2026-05-09 | Backend PATCH/DELETE /api/messages/{id} + DmHub MessageEdited/MessageDeleted broadcast. Mobile long-press → DropdownMenu (Düzenle/Sil) + AlertDialog. "düzenlendi" badge. |
| T-033 final | Typing indicator | 2026-05-09 | TypingEvent payload type (anonymous → record). RealtimeClient.sendTyping. ChatScreen input change → debounced send (2.5sn pause = false). Peer typing top bar'da "yazıyor…". |

---

## Sıradaki Sprint'ler (planlama)

- **Sprint #3 — Migration + Mobile MVP:** Mevcut Java/Firebase Android kodu kaldır. KMP+CMP'de full login + feed + profile UI. Android APK distribution akışı. Mevcut repo `JavaInstagramClone` → `mimir-mobile` rename.
- **Sprint #4 — Messaging:** DM altyapısı — SignalR Hub, mesaj DB, AES-256 at-rest, push notification çözümü (APNs+FCM-direct veya OneSignal)
- **Sprint #5 — UI Redesign:** Profesyonel UI overhaul (design system, theming, accessibility)
- **Sprint #6 — iOS Build + Test:** iOS target aktive et (KMP shared module zaten hazır olacak), IPA build, TestFlight olmadan dev distribution, iOS-spesifik UI tweaks. **Önkoşul:** macOS + Xcode erişimi netleştirilecek.
- **Sprint #7 — Hardening:** Penetration test, KVKK aydınlatma metni, backup automation, uptime monitor

## Bekleme Listesi (Shelved)

> Şu an gerek yok, ihtiyaç doğunca yeniden Todo'ya çekilir.

- **Domain alımı + Let's Encrypt cert** (eski T-012/T-013) — `aykutonpc.com` zaten canlı, mevcut cert kullanılıyor. Sub-domain ihtiyacı doğarsa açılır.
- **SMS verification + provider** (eski T-010) — ADR-010 ile iptal. Opt-in 2FA (TOTP/WebAuthn) ileride değerlendirilebilir.

## Bloklayıcılar (Sprint #6)

- **T-041 (iOS toolchain teyit) → T-042 + T-044**: macOS + Xcode + Apple developer netleşmeden iOS işi başlamaz.
- **T-034d (iOS APNs) → T-041 + T-042**: iOS toolchain + KMP refactor önkoşul.
- **T-043 (APK distribution) → T-039**: Force-update download URL'si gerek; cloud storage + build pipeline kurulumu.
