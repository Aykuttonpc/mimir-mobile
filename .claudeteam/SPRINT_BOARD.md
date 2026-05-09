# Sprint Board

> Aktif sprint. WIP'i bir kişide max 1-2 ile sınırla.
> Tarihler: ISO format (YYYY-MM-DD).

## Aktif Sprint

- **Sprint:** #4 — Messaging (DM altyapısı)
- **Başlangıç:** 2026-05-09 (Sprint #3 kapandı aynı gün — backend + mobile MVP eş zamanlı bitti)
- **Hedef bitiş:** [kullanıcı tempo verir]
- **Sprint hedefi:** 1-1 Direct Message altyapısı uçtan uca: backend `messages` tablosu + AES-256-GCM at-rest şifreleme + REST endpoints + SignalR Hub real-time push + Mobile ChatList/ChatScreen + Active kullanıcılar arası serbest DM. Push notification kararı bu sprint başında alınır (APNs+FCM-direct vs OneSignal).

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

## 📋 Todo (Sprint #4 — Messaging)

| ID | Başlık | Sahip | Tahmin | Notlar |
|---|---|---|---|---|
| T-027 | `Message` entity + AES-256-GCM crypto + 5. EF migration | Senior Dev #2 + AppSec | 2 saat | sender_id, recipient_id, iv, ciphertext, tag, created_at, read_at, edited_at, deleted_at. `IMessageCrypto` + `AesGcmMessageCrypto` impl. Key env'den (`Crypto:MessageKey`, 32 byte base64). |
| T-028 | `MessagesController` REST endpoints | Senior Dev #1 | 3 saat | `GET /api/messages/conversations` (özet liste + unread), `GET /api/messages/with/{userId}` (sayfalı tarih), `POST /api/messages/with/{userId}` (yeni mesaj fallback), `POST /api/messages/{id}/read` (read receipt) |
| T-029 | SignalR `DmHub` real-time push | Senior Dev #1 + Innovation Architect | 3 saat | OnConnected: `Groups.AddToGroup($"user-{userId}")`. `SendMessage(toUserId, plaintext)` → encrypt → DB save → `Clients.Group("user-{recipient}").MessageReceived(...)`. Read receipt invoke. |
| T-032 | `GET /api/users/active` (dm partner listesi) | Senior Dev #1 | 30 dk | Active kullanıcı listesi. Mimir kapalı network — herkes herkese DM (arkadaş ekleme modeli yok, ADR-013). Pagination + arama. |
| T-030 | Mobile ChatListScreen | Senior Dev #3 | 4 saat | Conversations API + LazyColumn + last-message preview + unread badge. Pull-to-refresh. |
| T-031 | Mobile ChatScreen + SignalR client | Senior Dev #3 + Innovation Architect | 6-8 saat | KMP-uyumlu SignalR client (Microsoft.SignalR.Client veya WebSocket fallback). Mesaj listesi + send + read receipt + typing indicator. JWT auth via query string `access_token`. |
| T-036 | Push notification kararı (brief) | ML/RAG Engineer + AppSec + Innovation Architect | 1 saat | APNs+FCM-direct (kendi backend'imizden push) vs OneSignal (3rd-party SaaS). Self-host network için APNs+FCM-direct uyar; iOS Sprint #6'da etkinleşir. Brief + ADR. |
| T-033 | Read receipts + typing indicator (Sprint sonu) | Senior Dev #1 + Senior Dev #3 | 2 saat | DM Hub'a `Typing(toUserId, isTyping)` ve `Read(messageIds)` invoke ekle. Mobile UI'da göster. |
| T-035 | Message edit / soft delete | Senior Dev #1 | 1.5 saat | `PUT /api/messages/{id}` (edit), `DELETE /api/messages/{id}` (soft — DeletedAt set). Sadece sender. Mobile UI: long-press menü. |

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

## Bloklayıcılar (Sprint #4)

- **T-029 → T-027 + T-028**: SignalR Hub mesaj DB schema'sı ve REST endpoint'leri olmadan kurulamaz.
- **T-031 → T-029**: Mobile real-time chat hub canlı olmadan çalışmaz.
- **T-036 (push notification brief) → T-031 sonrası**: client tarafı çalışırken brief paralel gidebilir.
- iOS toolchain (Sprint #6 öncesi): macOS erişimi hâlâ netleşmemiş.
