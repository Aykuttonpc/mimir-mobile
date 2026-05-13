# Routine: Mimir Nightly Security Audit

> **Schedule:** Daily 03:00 GMT+3 (VPS off-peak)
> **Trigger:** Cron — Claude Code Routines
> **Output:** `.claudeteam/SECURITY_REPORTS/YYYY-MM-DD.md`
> **Critical action:** SPRINT_BOARD'a flag + git commit "chore(security): nightly audit"

---

## Routine Prompt

Mimir projesinde **gece-yarısı güvenlik denetimi** yap. Her bulgu severity (🔴 critical / 🟡 medium / 🟢 info) ile sınıflandır.

### 0. Hazırlık
- Bugünün tarihi: `date +%Y-%m-%d`
- Bir önceki güvenlik raporu: `.claudeteam/SECURITY_REPORTS/` altında en son `.md` dosyası — diff için baz al

### 1. Dependency Vulnerabilities

**Backend (mimir-api):**
```bash
cd D:/Projeler/mimir-api
dotnet list package --vulnerable --include-transitive 2>&1
```

**Mobile (mimir-mobile):**
```bash
cd D:/Projeler/InstaClone
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" \
PATH="/c/Program Files/Android/Android Studio/jbr/bin:$PATH" \
./gradlew :app:dependencies --configuration releaseRuntimeClasspath 2>&1 | head -100
```

Yeni vuln paket varsa **🔴**, version bump önerilen varsa **🟡** olarak listele.

### 2. Secret Leak Detection (son 24 saat commit'leri)

Her iki repo için:
```bash
git log --since="24 hours ago" -p | head -2000
```

Regex pattern ara (Grep tool kullan):
- `(api[_-]?key|secret|password|token)\s*[:=]\s*["'][^"']{8,}["']` — hardcoded literal
- `Bearer\s+ey[A-Za-z0-9._-]+` — JWT token
- `AKIA[0-9A-Z]{16}` — AWS access key
- `AIza[A-Za-z0-9_-]{35}` — Google API key
- `xox[bao]-` — Slack token
- `-----BEGIN.*PRIVATE KEY-----` — herhangi private key

Bulguları **🔴** olarak işaretle. `.example` dosyaları muaf (placeholder OK).

### 3. Code-Level Risks

**Authentication coverage** (backend yeni endpoint):
```bash
# Son 24 saat değişen Controller dosyaları
cd D:/Projeler/mimir-api
git log --since="24 hours ago" --name-only --pretty=format: -- 'src/**/Controllers/*.cs' | sort -u
```
Bu dosyalardaki yeni HTTP method'larda `[Authorize]` veya `[AllowAnonymous]` attribute var mı? Yoksa **🟡** flag.

**SQL raw queries:**
```bash
cd D:/Projeler/mimir-api
grep -rn "FromSqlRaw\|ExecuteSqlRaw" src/Mimir.Api/ 2>&1
```
Bulunursa parameterize edilmiş mi kontrol et. `$"..."` interpolation varsa **🔴** SQL injection.

**HTTP cleartext URL** (mobile):
```bash
cd D:/Projeler/InstaClone
grep -rn '"http://' app/src/main/ data/src/main/ 2>&1 | grep -v "localhost\|127.0.0.1\|10.0.2.2"
```
LAN/test dışı `http://` varsa **🟡**.

**Logging hassas data:**
```bash
cd D:/Projeler/mimir-api
grep -rn "_logger.Log.*password\|_logger.Log.*token\|_logger.Log.*PasswordHash" src/Mimir.Api/ 2>&1
```
Bulunursa **🔴** PII/secret log leak.

### 4. VPS Operational (SSH ile)

```bash
ssh -i ~/.ssh/hetzner_aopc deploy@178.104.198.249 "
df -h / | tail -1
free -h | grep Mem
docker ps --format '{{.Names}}\t{{.Status}}'
docker logs mimir-web --since 24h 2>&1 | grep -cE 'Exception|ERROR'
sudo ufw status numbered | tail -15
"
```

- Disk %85+ → **🔴**
- RAM %90+ → **🟡**
- Container "(healthy)" değilse → **🔴**
- Error count > 50/24h → **🟡** (anormal log noise)
- UFW beklenmedik port (22/80/443/3478/5349/49152-49200 dışı) → **🟡**

### 5. Configuration Drift

```bash
ssh -i ~/.ssh/hetzner_aopc deploy@178.104.198.249 "
grep -E 'MIN_APP_VERSION|LATEST_APP_VERSION|TURN_SECRET|JWT_KEY' /opt/mimir/.env.prod | grep -v '^#'
grep -E 'image:.*:latest' /opt/mimir/docker-compose.prod.yml
"
```

- `MIN_APP_VERSION == LATEST_APP_VERSION` ise → **🟢 info** (force update aktif — kasıtlıysa OK, değilse Sprint geride)
- `MIN_APP_VERSION` 2 versiyon altında → **🟡** (kullanıcı eski sürümde mahsur)
- `:latest` tag bulunursa → **🔴** anti-pattern 33

### 6. Certificate Expiry

```bash
echo | openssl s_client -connect aykutonpc.com:443 -servername aykutonpc.com 2>/dev/null | openssl x509 -noout -enddate
```

Cert < 30 gün → **🟡**, < 7 gün → **🔴**.

### 7. Rapor Yaz

`.claudeteam/SECURITY_REPORTS/YYYY-MM-DD.md` dosyasına şu format:

```markdown
# Security Audit YYYY-MM-DD HH:MM (UTC+3)

**Özet:** ✅ Temiz / ⚠️ N uyarı / 🔴 N kritik bulgu

## 🔴 Kritik (varsa)
- [Bulgu başlığı] — [açıklama] — [öneri]

## 🟡 Orta (varsa)
- [Bulgu başlığı] — [açıklama]

## 🟢 Info
- Backend dep status: temiz/N vuln
- Mobile dep status: temiz/N
- VPS disk: %X
- Cert kalan gün: N
- ...

## 📊 Önceki rapor ile karşılaştırma
- **Yeni:** [bulgu varsa]
- **Kapanan:** [bulgu varsa]
- **Süregelen:** [N gündür var olan]
```

### 8. Kritik bulgu varsa

`.claudeteam/SPRINT_BOARD.md` dosyasının Aktif Sprint bölümüne aşağıdaki satırı **prepend**'le:

```
> 🔴 **Acil güvenlik uyarısı (YYYY-MM-DD):** [bulgu özeti] — [SECURITY_REPORTS link]
```

### 9. Commit

```bash
cd D:/Projeler/InstaClone
git add .claudeteam/SECURITY_REPORTS/ .claudeteam/SPRINT_BOARD.md
git commit -m "chore(security): nightly audit YYYY-MM-DD — N findings"
git push origin master
```

---

## Notlar

- **Sessiz başarı:** Hiç bulgu yoksa rapor yine yazılır (✅ Temiz) — geçmiş için track edilir.
- **Idempotent:** Aynı bulgu birden fazla gün açıksa "Süregelen N gün" olarak listelenir, spam değil.
- **Sessizlik = başarısızlık değil:** Routine çalışmadıysa kullanıcı dosyanın gelmemesinden anlar. Manuel re-run.
- **VPS SSH erişimi şart:** Bu routine `~/.ssh/hetzner_aopc` key'i kullanıyor. Aykut'un local makinesinden çalışırsa OK; cloud agent'ında SSH key olmaz → o senaryo için sadece backend repo + mobile repo + GitHub Actions API yeterli (VPS bölümü skip edilir).
