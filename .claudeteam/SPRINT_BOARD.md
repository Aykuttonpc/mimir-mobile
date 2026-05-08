# Sprint Board

> Aktif sprint. WIP'i bir kişide max 1-2 ile sınırla.
> Tarihler: ISO format (YYYY-MM-DD).

## Aktif Sprint

- **Sprint:** #3 — Mobile MVP + Hardening
- **Başlangıç:** 2026-05-09 (Sprint #2 kapandı)
- **Hedef bitiş:** [kullanıcı tempo verir]
- **Sprint hedefi:** KMP + Compose Multiplatform Android iskelet, eski Java/Firebase mobile kodu temizliği, nginx /mimir/ patch'in AykutOnPC repo'sunda kalıcılaştırılması. Sprint sonunda: Android APK'da login + admin onay flow + JWT işleyen iskelet ekran.

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

## 📋 Todo (Sprint #3)

| ID | Başlık | Sahip | Tahmin | Notlar |
|---|---|---|---|---|
| T-018 | nginx `/mimir/` patch'in AykutOnPC repo'sunda kalıcılaştırılması | DevOps + Tech Lead | 30 dk | **KRİTİK**: `D:\Projeler\Voxi\AykutOnPC\nginx\conf.d\aykutonpc.conf`'a /mimir/ location bloğu commit + push. Aksi halde her AykutOnPC deploy'unda silinir. |
| T-011 | KMP + Compose Multiplatform iskelet proje (**Android-only**) | Senior Dev #3 + Innovation Architect | 1-2 gün | Yeni branch `kmp-rewrite`. Login screen iskeleti, JWT storage, OkHttp + Retrofit (KMP) ile mimir-api bağlantısı |
| T-019 | Eski Java/Firebase Android kodu temizliği | Senior Dev #3 | 0.5 gün | Mevcut JavaInstagramClone repo'sundaki Firebase/Picasso/Java kod silinir. Repo `instaclone-mobile` (sonra `mimir-mobile`)'a rename |
| T-022 | Login + Pending screen + Approve flow UI | Senior Dev #3 | 1-2 gün | Davet token entry → register → email verify (manual link tıklama) → "admin onayı bekleniyor" → login → home |
| T-023 | Real SMTP konfigürasyonu (production email) | DevOps + Senior Dev #1 | 30 dk | `Smtp__Host` env set; provider seç (Gmail SMTP / SendGrid / Mailgun). ConsoleEmailSender → SmtpEmailSender otomatik geçecek. |
| T-017 | Mobile app cert pinning (P2 → opsiyonel) | Senior Dev #3 + AppSec | 1 saat | LE cert üzerinde — MITM koruması için ek katman. Cert rotation = APK rebuild. P2 düşük öncelik. |
| T-024 | Bootstrap admin password değiştirme endpoint'i (ya da SQL) | AppSec + Senior Dev #1 | 30 dk | İlk login sonrası admin şifresini değiştirebilmek lazım — şu an manuel SQL UPDATE gerek. POST /api/auth/change-password endpoint. |
| T-025 | Multi-replica hazırlık: Redis-distributed rate limit | AppSec + Senior Dev #1 | 2 saat | Şu an in-memory single-instance. ASP.NET Core'un Redis-based partition store impl edilmeli (multi-replica deploy gerekirse) |

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

## Bloklayıcılar (Sprint #3)

- **T-018 BLOCKING T-022**: nginx /mimir/ patch kalıcı olmazsa, AykutOnPC her deploy'da mobile auth'u kırar. Mobile UI iş başlamadan önce halledilmeli.
- **T-022 → T-011 + T-019**: UI öncesi KMP iskelet ve eski kod temizliği.
- iOS toolchain (Sprint #6 öncesi): macOS erişimi netleşmemiş — kullanıcıdan teyit alınacak.
