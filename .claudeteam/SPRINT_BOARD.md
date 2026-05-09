# Sprint Board

> Aktif sprint. WIP'i bir kişide max 1-2 ile sınırla.
> Tarihler: ISO format (YYYY-MM-DD).

## Aktif Sprint

- **Sprint:** #5 — Real-time + Push + Mesaj UX
- **Başlangıç:** 2026-05-09 (Sprint #4 kapandı aynı gün)
- **Hedef bitiş:** [kullanıcı tempo verir]
- **Sprint hedefi:** Polling'i SignalR'a çevir (real-time DM), push notification altyapısı kur (cihaz kapalıyken bildirim), mesaj edit/soft delete UX'i. Kullanıcı hissi: "WhatsApp-class" anlık + her cihazda tutarlı.

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

## 📋 Todo (Sprint #5 — Real-time + Push + UX)

| ID | Başlık | Sahip | Tahmin | Notlar |
|---|---|---|---|---|
| T-037 | SignalR mobile client (polling → real-time) | Senior Dev #3 + Innovation Architect | 4-6 saat | `com.microsoft.signalr:signalr:8.0.0` (Java client, Android-only) — KMP'de Sprint #6 öncesi placeholder, Android tarafına entegrasyon. JWT via query `access_token`. Hub reconnect logic. ChatScreen polling'i değiştir (event-driven). |
| T-036 | Push notification brief + ADR-014 | Tech Radar Engineer + AppSec + Innovation Architect | 1 saat | APNs+FCM-direct (self-host) vs OneSignal (SaaS). Decision matrix: setup karmaşıklığı, iOS uyumu, KVKK 3rd-party data flow, maliyet. ADR yaz. |
| T-034 | Push notification implementation | Senior Dev #1 + Senior Dev #3 | 6-8 saat | T-036 sonrası seçilen provider'a göre. Backend: yeni mesaj olunca push send. Mobile: device token kayıt endpoint'i + notification handler. iOS Sprint #6'da. |
| T-035 | Message edit + soft delete | Senior Dev #1 + Senior Dev #3 | 2-3 saat | `PATCH /api/messages/{id}` (edit, sadece sender, EditedAt set), `DELETE /api/messages/{id}` (soft, DeletedAt). Mobile: long-press menü → düzenle/sil. Edit'lenen mesajda "düzenlendi" badge. |
| T-033 | Typing indicator (DmHub `Typing` invoke + UI) | Senior Dev #3 | 1-2 saat | DmHub'a `Typing(toUserId, isTyping)` zaten var. Mobile ChatScreen'de "..." göster. T-037 sonrası kolay. |
| T-038 | Auto-login (DataStore'da JWT persist) | Senior Dev #3 | 1 saat | App açılışında DataStore'dan refresh token oku, `/auth/refresh` ile yeni access al, otomatik HomeScreen. Şu an her kapatış login'e dönüyor. |
| T-039 | Force-update min version check | Senior Dev #1 + Senior Dev #3 | 1 saat | Backend env `MIN_APP_VERSION`. Login response'a ekle. Mobile: response'ta low version → blocking dialog "yeni APK indir". APK güvenlik patch'leri için kritik. |
| T-017 | Cert pinning (P2 → opsiyonel) | Senior Dev #3 + AppSec | 1 saat | LE cert üzerinde — MITM ek katman. Cert rotation = APK rebuild. Yine P2. |

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
| T-033 partial | Read receipt UI | 2026-05-09 | Auto mark-as-read açıkken peer mesajlarını okur, "okundu" badge sender mesajlarında ReadAt set olunca görünür. Typing T-037 sonrası. |

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

## Bloklayıcılar (Sprint #5)

- **T-034 (push impl) → T-036 (provider brief)**: Hangi provider seçilmeden impl başlamaz.
- **T-033 (typing) → T-037 (signalr client)**: Real-time hub kullanılmadan typing pratik değil.
- iOS toolchain (Sprint #6 öncesi): macOS erişimi hâlâ netleşmemiş — push notification iOS tarafı bunu bekliyor.
