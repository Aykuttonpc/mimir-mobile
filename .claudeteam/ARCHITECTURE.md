# Architecture

## Stack Özeti

- **Frontend:** Kotlin Multiplatform (KMP) + Compose Multiplatform — tek codebase, Android + iOS native UI
- **Backend:** ASP.NET Core 9 (C#) — mevcut VPS stack'iyle hizalı
- **Database:** PostgreSQL 16 — ayrı container `mimir-db`, izolasyon için
- **Auth:** JWT (access + refresh token rotation) + email/SMS OTP + admin approval
- **Cache:** Redis 7 — rate limit, session, refresh token blacklist. **Ayrı container** (`mimir-redis`), AykutOnPC ile paylaşılmıyor (izolasyon).
- **Real-time:** SignalR — DM için WebSocket
- **Storage:** MinIO (S3-compat, self-host) — post medya, profil avatar
- **Hosting:** Hetzner Cloud CPX22 (Nuremberg, Ubuntu 24.04) — mevcut AykutOnPC VPS'inde paylaşılan
- **TLS:** Let's Encrypt (`insta.aykutonpc.com`) — Sprint #2'de
- **Distribution:** Signed APK direkt + IPA (TestFlight olmadan dev distribution) — force update via `min_supported_version` endpoint

## Major Components

```
[Mobile App — KMP + Compose Multiplatform]
   │   HTTPS (Let's Encrypt — aykutonpc.com) + JWT Bearer
   │   baseUrl = https://aykutonpc.com/mimir/api
   ▼
[Nginx (mevcut, paylaşılan — aykutonpc.com cert)]
   │
   ├─→ /mimir/           → mimir-web:8080 (aykutonpc_frontend network)   [path prefix, ADR-007]
   ├─→ /api/, /, /health → aykutonpc-web:8080 (mevcut, default)
   └─→ ssl: /etc/letsencrypt/live/aykutonpc.com/
        │
[ASP.NET Core 9 — mimir-web]
   │
   ├─→ mimir-db (PostgreSQL 16)        — users, invitations, posts, follows, messages
   ├─→ mimir-redis (Redis 7)            — rate limit, OTP store, JWT blacklist
   ├─→ minio (yeni, self-host)               — medya
   └─→ SignalR Hub /hubs/dm                  — real-time mesajlaşma

[External]
   ├─→ SMS provider (Netgsm / Twilio — Sprint #2'de seçilecek)
   └─→ SMTP (email verification — provider TBD)
```

## External Integrations

| Servis | Amaç | Kritiklik |
|---|---|---|
| Hetzner Cloud | Hosting | Kritik — tüm sistem |
| ~~SMS provider~~ | ~~Telefon OTP~~ | **İptal — ADR-010** |
| SMTP | Email verification | Kritik — kayıt block eder, tek doğrulama kanalı |
| Let's Encrypt | TLS sertifika | Yüksek — domain alındığında |
| MinIO (self-host) | Medya storage | Yüksek — post içerik |

## Veri Akışı (Kritik Path'ler)

### 1. Yeni Kullanıcı Onboarding (3-aşama gate, ADR-010)

```
Admin (Aykut) → /admin/invitations  → davet linki üretir (token, expiry 7 gün)
   ↓
Tanıdık → davet linki açar → kayıt formu (email, username, password, phone opsiyonel)
   ↓
POST /auth/register
   ├─ DB'ye user.status = "pending_email" yaz
   └─ Email verification token üret + SMTP'den mail at
   ↓
GET /auth/verify-email?token=...   → status: "pending_admin"
   ↓
Admin panel → /admin/approvals    → onayla
   ↓
status: "active" → access + refresh JWT issue → user login olur
```

### 2. DM Gönderme (Real-time)

```
Cihaz A → SignalR Hub /hubs/dm  (Auth: JWT)
   ↓
Hub.SendMessage(toUserId, ciphertext)
   ├─ DB'ye Message yaz (AES-256 at-rest, sender + recipient + timestamp)
   └─ Recipient'in active connection'larına push
        ├─ Connected → anlık deliver
        └─ Offline → unread count++ (push notification çözümü TBD — Sprint #4)
   ↓
Cihaz B → mesajı alır → read receipt PUT /messages/{id}/read
```

## Önemli Mimari Kararlar (Kısa Liste)

> Detay için [DECISIONS.md](DECISIONS.md).

- **Firebase exit** (ADR-002) — vendor lock-in + maliyet kontrolsüzlüğü + KVKK için veri lokasyon kontrolü
- **.NET 9 + Postgres + Redis** (ADR-003) — mevcut VPS stack'iyle hizala, container reuse
- **KMP + Compose Multiplatform** (ADR-004) — Kotlin yatırımı korunur, single codebase Android+iOS
- **Server-side encryption (TLS + AES-256 at-rest)** (ADR-005) — admin moderasyon mümkün, mimari basit
- ~~Subdomain `insta.aykutonpc.com` (ADR-006)~~ → **ADR-007 ile yerine path prefix** (mobile-only MVP, domain deferred)
- **Path prefix `/mimir/` + self-signed cert + mobile cert pinning** (ADR-007) — domain alma overhead'ini elemine eder
- **Mevcut VPS'e ek proje pattern'i** (ADR-006/-007) — yeni VPS maliyetinden kaçın, "Strateji A" rehberden
- **Backend + Mobile ayrı repo** (ADR-008) — bağımsız CI/CD, bağımsız versiyon hattı

## Bilinen Sınırlamalar / Tech Debt

| Item | Sebep | Sprint hedefi |
|---|---|---|
| Mevcut Java + Firebase Android kodu silinecek | Tamamen yeni stack (KMP) | Sprint #3 sonunda |
| ~~Domain `aykutonpc.com` alınmamış~~ | ~~TBD~~ → **Bekleme listesi** (ADR-007 mobile-only) | Web client gerekirse aç |
| ~~SMS provider seçilmedi~~ | **İptal — ADR-010** | shelf |
| Push notification çözümü yok | Firebase çıkartıldı, alternatif yok | Sprint #4 öncesi araştır |
| Backend repo henüz yok | Yeni proje | Sprint #2 (T-007) |
| KMP iskelet proje yok | Yeni proje | Sprint #2 (T-011) |
