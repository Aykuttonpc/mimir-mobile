# Team Notes

> Serbest format takım notları. "Bunu unutma", "şuna dikkat", "şu yüzden böyle yaptık" tarzı.
> Code'dan derive edilemeyen, ama kararları etkileyen bilgiler buraya.

---

## Çalışma Tarzı (bu proje özelinde)

- **Karar disiplini:** Her mimari kararı ADR olarak [DECISIONS.md](DECISIONS.md)'a yaz. 2 hafta sonra "neden böyle yaptık" sorusunu doğurabilecek her şey kayıtlı.
- **Sprint sıralaması:** Backend MVP → Mobile rewrite → Messaging → UI redesign → iOS → Hardening. UI overhaul bilinçli olarak sona bırakıldı (önce çalışsın, sonra güzelleşsin).
- **Branch disiplini:** Mevcut `kotlin-rewrite` branch'i Sprint #3 sonunda silinecek (Java/Firebase kodu temizlenince). Yeni branch `kmp-rewrite` Sprint #2'de açılacak.
- **APK distribution:** Play Store kullanılmayacak. Force update mekanizması (backend `min_supported_version`) v1'den itibaren olmalı yoksa eski APK'lar güvenlik patch alamaz.

## Gotchas / "Buraya Dikkat"

- **VPS paylaşımı:** AykutOnPC stack'i aynı VPS'te. Container `mem_limit` ZORUNLU (rehber 333. satır). Port `0.0.0.0`'a bind YASAK — `127.0.0.1` üzerinden nginx çıkar.
- **Domain henüz yok:** `aykutonpc.com` daha alınmamış (rehber 209-221. satır geçici fix anlatıyor). Bizim `insta.aykutonpc.com` ana domain alındıktan sonra subdomain olarak gelir.
- **fail2ban tuzağı:** VPS'e başarısız SSH denemeleri ban'a yol açar (rehber 119-159). Yeni Claude session'larında SSH key path'ini doğru ver.
- **`docker compose down -v` kesinlikle yasak** (rehber 257. satır + Section 6 madde 36): DB volume silinir, TÜM VERİ GİDER.
- **Mevcut nginx config'inde `git reset --hard` tehlikesi** (rehber 218): `bash scripts/deploy.sh` geçici cert fix'ini siler. Mimir deploy hattında benzer kapan oluşmamalı.
- **`.env.prod` symlink → `.env`** pattern: Compose `--env-file` belirtilmediğinde default `.env` arar. `/opt/mimir/.env -> .env.prod` symlink ile `docker compose ps` warning'siz çalışır. Aynı pattern AykutOnPC için de uygulanabilir.
- **Secret üretimi server-side:** `.env.prod` random secret'ları VPS'te `openssl rand` ile üretildi, lokal makineye ya da Bash history'ye düşmedi. JWT_KEY 64 char, DB_PASSWORD/REDIS_PASSWORD 32 char. Disk perm 600 (sadece deploy okur).
- **`aykutonpc.com` domain canlı + Let's Encrypt cert var** — VPS rehberi 209-221 (2026-04-19) "domain henüz yok" diyordu, ama nginx config'e bakılırsa domain alındı, certbot --standalone ile cert oluşturuldu (expiry 2026-07-18, renewal hook tanımlı). Mimir baseUrl public domain üzerinden çalışıyor — cert pinning **şart değil** (LE root CA mobil OS trust store'da var), sadece ek MITM koruması olarak değerlendirilir.
- **mimir-web çift network'lü:** `mimir_mimir-net` (db+redis ile internal) + `aykutonpc_frontend` (nginx ile direct). AykutOnPC compose'a sıfır dokunma; external network olarak join'lendi.
- **nginx config edit safety pattern:** Edit öncesi `aykutonpc.conf.bak.YYYYMMDD-HHMMSS` yedek alındı. SCP sonrası `nginx -t` test, OK ise `nginx -s reload`. AykutOnPC site downtime: 0.
- **🚨 NGINX /mimir/ PATCH GEÇİCİ — AykutOnPC auto-deploy ile silinebilir.** AykutOnPC repo'sunda `aykutonpc.conf` git tracked. CI/CD ya da manuel deploy `git reset --hard` ile patch'i siler → mobile auth flow'u kırar. Sprint #3 T-018: AykutOnPC repo'da kalıcı commit. Tetiklenince geçici patch tekrar uygulanır (snapshot `D:\Projeler\mimir-api\deployment\nginx\aykutonpc.conf.snapshot`'ta). Sprint #2 boyunca **3 kez tetiklendi**.
- **Compose service alias çakışması (ADR-011):** `aykutonpc_frontend` external network'üne join olan iki container'ın da `web` service alias'ı olunca DNS round-robin AykutOnPC trafiğini mimir-web'e sızdırdı. Compose `aliases:` directive default service-name alias'ı kaldırmıyor — service ismini `web` → `api` yapmak tek yol. Container ismi (`mimir-web`) DNS'te ayrıca alias olarak görünür.
- **DataProtection-Keys volume permission tuzağı:** Anonymous volume mount mimir user (UID:1000) yazma izni vermez (root-owned). Dockerfile'da `mkdir + chown` ile dizini önceden hazırlamak şart. Yoksa "Permission denied" loop'a girer.
- **Bootstrap admin pattern:** İlk admin user `Admin:Bootstrap{Email,Password,Username}` env'inden seed edilir. Admin user mevcutsa atlanır (idempotent). VPS .env.prod'da hâlâ `ADMIN_BOOTSTRAP_PASSWORD=4AVIgwIdoZdjkcP6Qmw` — kullanıcı kaydetti, ileride şifre değiştirme endpoint'i (T-024) sonrası temizlenebilir.

## Stakeholder İletişim Notları

### 2026-05-08 — Sprint #1 İlk Toplantı (kullanıcı cevapları)

**Onboarding kararı (Soru 6):**
- Davet mekanizması: Kullanıcı (admin) hem davet linki gönderir, hem başvuruları onaylar.
- Email verification: Zorunlu
- Telefon verification: Zorunlu (SMS OTP)
- Tahmini network: ~100 kullanıcı (büyüme ihtimali için tasarımda esneklik)
- Distribution: **Play Store yok** — APK doğrudan tanıdıklara gönderilecek

**Onboarding flow (4-aşama gate):**
1. Kullanıcı kayıt formu doldurur (davet linki üzerinden)
2. Email verify
3. SMS OTP verify
4. Admin (kullanıcı) onayı → hesap aktif

**Türevler / takım notları:**
- ~~SMS provider gerekli~~ → **iptal (ADR-010, 2026-05-09)** — onboarding 3-aşama'ya indirildi (email verify + admin onay)
- APK distribution = force-update mekanizması zorunlu (backend'de `min_supported_version`)
- Signed APK + certificate pinning (Red Team önerisi)
- ~~SMS abuse koruması~~ → email bombing rate limit'e dönüştü (T-014)

### 2026-05-08 — Domain Shelf, Mobile-Only Routing

Kullanıcı netleştirdi: v1 sadece mobil, web yok. Domain + Let's Encrypt cert mobile-only context'te gereksiz overhead → bekleme listesine.

**Sonuç (ADR-007):** `https://178.104.198.249/mimir/` path prefix + self-signed cert + mobile cert pinning.
**Etki:** ADR-006 (subdomain) supersede edildi, T-012/T-013 shelf, yeni T-015/T-017 eklendi.

### 2026-05-08 — Repo Yapısı: Backend + Mobile Ayrı (ADR-008)

Backend `mimir-api` (yeni) — Mobile [JavaInstagramClone](https://github.com/Aykuttonpc/JavaInstagramClone.git) (Sprint #3'te `mimir-mobile` rename).

### 2026-05-08 — Platform Sıralaması: Android Önce, iOS Sonra

Kullanıcı kararı: KMP+CMP shared codebase devam, ama Sprint #2'de **Android target aktif, iOS shelf**. iOS Sprint #6'ya kalır. macOS/Xcode toolchain konusu o zaman tekrar açılacak.

**Sebep:** Hız + tek platform üzerinde feature complete olup sonra port etmek, paralel iki platform yönetmekten basit.

**Etkiler:**
- T-011 → "Android-only iskelet" olarak revize
- Sprint #6 önkoşulu: macOS erişimi netleştirilecek
- KMP shared module yine de iOS-uyumlu yazılacak (ileride port hızlansın diye — yapı bozulmamalı)

## Sprint #2 Retrospective (2026-05-09)

**Yaptıklarımız:**
- 11 commit'lik backend MVP (init → deployment → 3 migration → auth flow → rate limit → harden)
- Hetzner CPX22 üzerinde 3 container (mimir-db + mimir-redis + mimir-web "api" service)
- 8 endpoint canlı, JWT auth + RBAC + rate limit
- Production-ready public URL: `https://aykutonpc.com/mimir/`

**İyi gitti:**
- VPS rehberi disiplini sayesinde AykutOnPC stack'ine zarar gelmedi
- ADR'ler (009 Mimir, 010 SMS iptal, 011 rate limit) kararları net belgeledi
- Migration-driven schema değişiklikleri downtime'sız uygulandı
- End-to-end smoke test (admin login → invite → register → verify → approve → user login → RBAC + 429 rate limit) tek seferde geçti

**Riskli/öğrendiklerimiz:**
- Compose service-name alias tuzağı (`web` çakışması) — ADR-011'le kalıcı çözüm
- nginx /mimir/ patch AykutOnPC repo'sunda kalıcı değil — Sprint #3 T-018 öncelikli
- Tek tetikleyiciyle birden fazla rebuild (rate limiter middleware sıralaması, DP-Keys permission, alias) → routing pipeline değişikliklerinde **smoke test her aşamada** disiplini güçlendirdi

**Sprint #3 odak:** Mobile (KMP+CMP) UI iskelet + nginx patch kalıcılaştırma + eski kod temizliği.

---

## "Neden Böyle Yapıldı" Defteri

> Code'a bakınca anlamayan birinin sorabileceği "neden?" sorularının cevapları.

- **Neden compose'da service ismi `api`, container ismi `mimir-web`?** → AykutOnPC stack'i de aynı `aykutonpc_frontend` network'üne `web` service alias'ı ile bağlı. Aynı alias iki container = DNS round-robin = AykutOnPC trafiği bizim container'a sızar. Container ismi nginx routing için stabil (`proxy_pass http://mimir-web:8080/`), service ismi `api` default alias çakışmasını önler.
- **Neden bootstrap admin env-bazlı seed (random password)?** → Davet+admin onaylı sistemde davet üretmek için zaten admin lazım (chicken-egg). Seed kapısı startup'ta kontrollü, idempotent (admin var ise skip). Random password kullanıcının elinde tek seferlik başlangıç noktası.
- **Neden refresh token DB'de hash'le?** → Plain token client'ta. DB compromise olursa hash'ten plain çıkarılamaz. Rotation history (`ReplacedByTokenId`) ile **token reuse detection** mümkün (revoked token tekrar kullanılırsa = saldırgan kopyaladı → tüm session'ları revoke).
- **Neden ASP.NET Core 9, 8 değil?** → Mevcut VPS'te .NET 9 zaten kurulu (AykutOnPC stack'i). Container base image, deploy script, monitoring tek versiyon altında topla, drift'i önle.
- **Neden Postgres ayrı container (`mimir-db`), AykutOnPC'nin DB'siyle paylaşılmıyor?** → İzolasyon — ileride Mimir'u ayrı VPS'e taşımak gerekirse DB'yi tek container olarak göç ettirmek temiz.
- **Neden subdomain (`insta.aykutonpc.com`), path prefix değil?** → JWT cookie scope + CORS + nginx routing path prefix'te karışıyor. Subdomain temiz boundary verir.
- **Neden KMP+CMP, Flutter değil?** → Branch zaten `kotlin-rewrite`, Kotlin yatırımı korunur. CMP iOS desteği 2026'da production-ready. Tek dil = single team yetkinliği.
- **Neden server-side encryption, E2E değil?** → Kullanıcı admin + çok-cihaz history senkronu kritik. E2E'de key kaybı = mesaj kaybı, mimari 5x karmaşık. v1 için yeterli, ileride hybrid eklenebilir.
- **Neden APK direkt, Play Store yok?** → Kapalı network — public discovery istemiyoruz. Play Store review + politika baskısı küçük scope için overhead. Tradeoff: force update mekanizması bizim sorumluluğumuz.
