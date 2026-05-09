# Project Context

## Temel Bilgi

- **Proje adı:** **Mimir** (Norse mit. — bilgelik kuyusunun başı; ADR-009)
- **Sahibi / paydaş:** Aykut (tek paydaş, admin)
- **Durum:** **WhatsApp-class DM production-ready** (Sprint #2-#3-#4-#5 close 2026-05-09 — backend + mobile + auth + admin + real-time messaging + edit/delete/typing + auto-login + force-update). Sprint #6 iOS + push notification.
- **Aktif branch:** `kmp-rewrite` (mobile, Sprint #3 itibariyle Kotlin+Compose). Backend `mimir-api` master branch.
- **URL:** `https://aykutonpc.com/mimir/` — Let's Encrypt cert üzerinden public erişim. Mobile-only, path prefix routing (ADR-007). `/mimir/health` canlı.
- **Repolar:**
  - Backend: [mimir-api](https://github.com/Aykuttonpc/mimir-api) — `D:\Projeler\mimir-api`
  - Mobile: [JavaInstagramClone](https://github.com/Aykuttonpc/JavaInstagramClone) — `D:\Projeler\InstaClone` (Sprint #3'te `mimir-mobile` rename)

## Amaç

Aykut'un kapalı, kişisel iletişim ağı. Firebase bağımlılığı kaldırılıp kendi Hetzner VPS'inde self-host edilecek. Davet+admin-onay gating ile sadece tanıdıkların erişebildiği güvenli bir Instagram-benzeri (post + takip + DM) deneyim. Birincil kullanım: Aykut'un kişisel mesajlaşması + paylaşımı.

## Hedef Kullanıcı

- **Aykut (admin)** — Tek admin. Davet linki üretir, başvuruları onaylar, full sistem kontrolüne sahip.
- **Davet edilen tanıdıklar** (~100 hedef, büyüme ihtimali) — Kapalı network üyesi. Post atar, takip eder, DM kullanır. Kayıt sadece davet üzerinden.

## Başarı Kriterleri

- [ ] Yeni kullanıcı 3-aşama gate'ten geçer (email verify + admin onay) — ADR-010
- [x] DM gerçek-zamanlı (Sprint #5 SignalR ile event-driven, gecikme <1sn)
- [ ] Backend uptime > %99 (mevcut VPS uptime'ı baseline)
- [ ] Android + iOS native uygulamada ortak codebase (KMP + Compose Multiplatform)
- [ ] APK direkt distribution + force update mekanizması
- [x] Firebase SDK'larından %100 ayrılma (Sprint #3 T-019 — tüm Firebase BOM + Picasso silindi)

## Kısıtlar

- **Bütçe:** Hetzner VPS mevcut (CPX22, ek maliyet yok). SMS provider yıllık ~50-200 TL. Domain ~50-100 TL/yıl.
- **Zaman:** Hard deadline yok. Sprint disiplinine bağlı çalış.
- **Teknik:** Mevcut VPS stack'iyle uyumlu olmalı (.NET 9, Postgres 16, mevcut nginx paylaşılır).
- **Compliance:** KVKK kapsamında — kişisel veri (mesaj, foto, telefon, e-mail) işleniyor. Veri sahibi bilgisi + saklama süresi politikası gerekecek.

## Out of Scope

- Public registration / open signup — her zaman davet+admin onay
- Google Play / App Store distribution — APK direkt
- Web client — v2 düşünülebilir, v1'de yok
- Grup mesajlaşma — sadece 1-1 DM (Sprint #4 sonrası gözden geçir)
- E2E encryption — server-side seçildi (ADR-005); ileride hybrid değerlendirilebilir
- AI/LLM özellikleri — şimdilik yok
