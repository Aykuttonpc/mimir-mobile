# Operational Runbooks

> SRE sahibi. Bu dosya operasyonel runbook'lara **index** tutar.
> Her DevOps/incident işinde önce buraya bakılır, ilgili runbook açılır.

## Aktif Runbook'lar

| Runbook | Yer | Ne için | Son güncelleme |
|---|---|---|---|
| AykutOnPC VPS — Bağlantı + Operasyon | `D:\AYKUTONPC-VPS-REHBERI.md` | SSH, UFW lockout recovery, container yönetimi, Hetzner Web Console fallback. **Mimir aynı VPS'te `/opt/mimir` altında çalışacak — bu rehber Mimir için de geçerli.** | 2026-04-19 |
| Mimir deploy runbook | `docs/deployment/runbook.md` (Sprint #2'de yazılacak) | Mimir-spesifik deploy/rollback adımları | TBD |

> Format: dosya yolu absolute olabilir (örn. `D:\<PROJECT>-VPS-REHBERI.md`) veya proje-relative (örn. `docs/runbooks/deploy.md`).

---

## Her Runbook'ta Olması Gerekenler

Section 5.10 — "Standart Runbook Bölümleri":

1. Sunucu/sistem bilgileri (IP, panel, OS, paths)
2. Bağlantı talimatları (SSH, key, config)
3. **Acil kurtarma** (out-of-band fallback — her zaman ilk 3 bölümde)
4. Standart operasyonlar (container, deploy, DB)
5. **ASLA YAPMA listesi** (geri dönülemez hatalar)
6. Hızlı tanı akışı (semptom → çözüm dallanması)
7. İlgili dokümantasyon

---

## ASLA YAPMA — Cross-Project Liste

Bu liste tüm projeler için geçerli; project-specific runbook'larda ek olabilir.

| ❌ Yapma | Neden | ✅ Doğrusu |
|---|---|---|
| `ufw enable` SSH allow ETMEDEN | Anında SSH kopar | Önce `ufw allow 22/tcp`, sonra enable |
| `docker compose down -v` | DB volume silinir, **TÜM VERİ GİDER** | `-v` flag'ini ASLA kullanma |
| `image: <name>:latest` | Hangi versiyon deploy olduğu belirsiz | Semver veya commit SHA |
| Force push to main | Deploy hattı yıkılır, history kaybı | Branch + PR + merge |
| Secret commit | Repo'da kalır, sızar | `.env` (gitignore) veya vault |
| `iptables -F` | UFW state bozar | UFW kullan, raw iptables'a dokunma |
| Production'a manuel SSH müdahale | Kod-altyapı drift'i | Fix → kod → deploy hattı |
