# Tech Radar

> Canlı doküman. Tech Radar Engineer sahibi.
> Kategoriler: 🟢 Adopt | 🟡 Trial | 🔵 Assess | 🔴 Hold

---

## 🟢 Adopt (production'da güvenle kullanılır)

| Tech | Kategori | Eklendi | Not |
|---|---|---|---|
| .NET 9 / ASP.NET Core | Backend runtime | 2026-05-08 | Mevcut VPS'te kullanımda, container reuse |
| PostgreSQL 16 | DB | 2026-05-08 | Mevcut VPS'te aynı versiyon |
| Redis 7 | Cache / rate limit | 2026-05-08 | Mevcut VPS'te paylaşılabilir |
| Nginx | Reverse proxy | 2026-05-08 | Mevcut VPS'te paylaşılan |
| Docker Compose | Container orchestration | 2026-05-08 | "Strateji A" pattern (VPS rehberi) |
| Ubuntu 24.04 LTS | OS | 2026-05-08 | Mevcut VPS |
| Hetzner Cloud (CPX22) | Hosting | 2026-05-08 | Mevcut, ek proje gir |
| EF Core | ORM | 2026-05-08 | .NET ekosistem standardı |
| SignalR | Real-time | 2026-05-08 | DM için WebSocket katmanı |
| MinIO | S3-compat storage | 2026-05-08 | Self-host, AWS bağımlılığı yok |
| JWT (access + refresh) | Auth | 2026-05-08 | Standard, refresh rotation şart |
| Let's Encrypt | TLS | 2026-05-08 | Mevcut bootstrap pattern |

## 🟡 Trial (düşük riskli pilot için hazır)

| Tech | Kategori | Eklendi | Brief | Pilot kapsamı |
|---|---|---|---|---|
| Kotlin Multiplatform 2.x | Cross-platform mobile | 2026-05-08 | TBD (Sprint #2) | Mimir Android + iOS — zaten kotlin-rewrite branch hazır |
| Compose Multiplatform 1.7+ | UI framework | 2026-05-08 | TBD (Sprint #2) | Mimir tüm UI |

## 🔵 Assess (takip ediyoruz, brief var, henüz sıra değil)

| Tech | Kategori | Eklendi | Brief | Bekleme sebebi |
|---|---|---|---|---|
| Matrix protokolü | DM altyapısı | 2026-05-08 | TBD | Custom DM önce çalışsın, ihtiyaç olursa migrate |
| APNs + FCM-direct push | Push notification | 2026-05-08 | TBD | Sprint #4 öncesi karar |
| OneSignal (push aggregator) | Push alternatifi | 2026-05-08 | TBD | APNs/FCM-direct alternatifi |
| ~~Netgsm~~ | ~~SMS provider (TR)~~ | 2026-05-09 | **Drop — ADR-010** | SMS verify kaldırıldı |
| ~~Twilio~~ | ~~SMS provider (global)~~ | 2026-05-09 | **Drop — ADR-010** | SMS verify kaldırıldı |

## 🔴 Hold (kullanma — eski/sorunlu/risk)

| Tech | Kategori | Eklendi | Sebep | Migration planı |
|---|---|---|---|---|
| Firebase Auth | Auth | 2026-05-08 | Vendor lock-in, KVKK veri lokasyonu kontrolü yok | Self-host JWT — Sprint #2 |
| Firebase Firestore | DB | 2026-05-08 | Vendor lock-in, document model relational ihtiyaca uymuyor | PostgreSQL 16 — Sprint #2 |
| Firebase Storage | Medya | 2026-05-08 | Vendor lock-in, maliyet ölçeklenmesi tehlikeli | MinIO self-host — Sprint #3 |
| Firebase Analytics | Analytics | 2026-05-08 | Üçüncü taraf veri paylaşımı KVKK riski | Self-host minimal logging — Sprint #4 |
| Picasso (Android) | Image loading | 2026-05-08 | Native Android'e bağlı, KMP'de çalışmaz | Coil-multiplatform — Sprint #2 |
| Java (Android source) | Dil | 2026-05-08 | KMP+CMP Kotlin gerektirir | Kotlin'e tam geçiş — Sprint #3 |

---

## Geçiş Tarihçesi

| Tarih | Tech | Eski → Yeni | Sebep | ADR |
|---|---|---|---|---|
| 2026-05-08 | Firebase (Auth/Firestore/Storage/Analytics) | (yok) → Hold | Self-host migration | ADR-002 |
| 2026-05-08 | Picasso | (yok) → Hold | KMP uyumsuz | ADR-004 |
| 2026-05-08 | Java | (yok) → Hold | KMP Kotlin gerektirir | ADR-004 |
