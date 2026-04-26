# Deployment: Solar Dashboard auf Synology DS218+

Pipeline: **GitHub `main` → GitHub Actions → GHCR → Synology Container Manager**

```
                                 ┌─────────────┐
                                 │   GHCR      │
git push main ──► GitHub Actions ─► ghcr.io   │ ◄── docker pull (alle 5 min via cron)
                  (mvn + docker)  │  :latest   │      │
                                 └─────────────┘      ▼
                                                  Synology DS218+
                                                  └─ Container Manager
                                                     └─ docker compose up -d
```

Kein offener SSH-Port nach aussen, keine Service-Account-Keys in GitHub Actions
(BigQuery braucht den Key nur zur Laufzeit auf der Synology).

---

## Files in diesem Repo

| Pfad | Zweck |
|---|---|
| `Dockerfile` | Multi-stage Build (Maven → JRE 21) |
| `.dockerignore` | Schliesst `target/`, `.git`, `homepage/` etc. aus |
| `.github/workflows/docker-publish.yml` | Build & Push to `ghcr.io` bei jedem Push auf `main` |
| `deploy/docker-compose.yml` | Container-Definition für Synology |
| `deploy/synology-pull.sh` | Cron-Script: pullt neues Image und restartet Container |
| `homepage/index.html` | Statische Homepage für `softhenge.ch` |

---

## A) Einmalige Setup-Schritte

### 1. GCP Service Account für BigQuery erstellen

Auf einer Maschine mit `gcloud` (z.B. dein Laptop):

```bash
PROJECT=modern-cubist-412113
SA_NAME=solar-dashboard-reader

gcloud iam service-accounts create $SA_NAME \
    --project=$PROJECT \
    --display-name="Solar Dashboard read-only"

gcloud projects add-iam-policy-binding $PROJECT \
    --member="serviceAccount:$SA_NAME@$PROJECT.iam.gserviceaccount.com" \
    --role="roles/bigquery.dataViewer"

gcloud projects add-iam-policy-binding $PROJECT \
    --member="serviceAccount:$SA_NAME@$PROJECT.iam.gserviceaccount.com" \
    --role="roles/bigquery.jobUser"

gcloud iam service-accounts keys create gcp-key.json \
    --iam-account=$SA_NAME@$PROJECT.iam.gserviceaccount.com
```

Die Datei `gcp-key.json` per SCP/Drive auf die Synology kopieren:

```bash
mkdir -p /volume1/docker/solar-dashboard/secrets
mv ~/gcp-key.json /volume1/docker/solar-dashboard/secrets/
chmod 600 /volume1/docker/solar-dashboard/secrets/gcp-key.json
```

### 2. GitHub: GHCR Sichtbarkeit

Standardmässig erstellt GitHub Actions das Package privat. Damit die Synology
ohne PAT-Login pullen kann (optional, sonst PAT verwenden):

- Erste GitHub Action laufen lassen (Push auf `main` oder manuell triggern)
- Auf GitHub: Profil → Packages → `solar-dashboard-vaadin` → Package settings
- → "Change visibility" auf **public** wenn du keine PAT-Authentifizierung willst

Empfohlen jedoch: **privat lassen** und auf der Synology einmalig einloggen.

### 3. Synology: Container Manager + GHCR-Login

GitHub Personal Access Token erstellen (https://github.com/settings/tokens):
- Classic token mit Scope `read:packages`
- Token sicher notieren

Auf der Synology per SSH:

```bash
docker login ghcr.io -u chutz68
# Password: <PAT>
```

Das speichert den Token in `~/.docker/config.json` und überlebt Reboots.

### 4. docker-compose.yml platzieren

```bash
mkdir -p /volume1/docker/solar-dashboard
# docker-compose.yml aus diesem Repo (deploy/docker-compose.yml) kopieren nach:
#   /volume1/docker/solar-dashboard/docker-compose.yml
```

### 5. Pull-Script installieren

```bash
mkdir -p /volume1/scripts
# deploy/synology-pull.sh aus diesem Repo kopieren nach:
#   /volume1/scripts/solar-pull.sh
chmod +x /volume1/scripts/solar-pull.sh
```

### 6. Synology Task Scheduler

DSM → Systemsteuerung → Aufgabenplaner → Erstellen → Geplante Aufgabe → Benutzerdefiniertes Script

- **Name:** Solar Dashboard Auto-Pull
- **Benutzer:** `root`
- **Zeitplan:** Täglich, jede 5 Min wiederholen
- **Befehl:**
  ```
  /volume1/scripts/solar-pull.sh >> /volume1/scripts/solar-pull.log 2>&1
  ```

### 7. Erststart

```bash
cd /volume1/docker/solar-dashboard
docker compose pull
docker compose up -d
docker compose logs -f solar-dashboard   # ein paar Sekunden zuschauen
```

Danach im Browser: `https://solar.softhenge.ch/` (vorausgesetzt Reverse Proxy
und Cloudflare-DNS sind wie in CLAUDE.md beschrieben konfiguriert).

---

## B) Tägliches Deployment

Einfach `git push origin main` &mdash; das war's:

1. GitHub Actions baut den Production-JAR (`mvn -Pproduction`) und das Docker-Image
2. Image wird mit Tags `:latest`, `:sha-<short>` und `:<datetime>` zu `ghcr.io` gepusht
3. Innerhalb von ≤ 5 Min zieht die Synology das neue Image und startet den Container neu
4. Downtime: ~30 Sek (Vaadin braucht beim Start ~20 Sek)

Push aktiviert die Pipeline **nicht**, wenn nur `homepage/`, `docs/` oder `*.md`
geändert wurden (siehe `paths-ignore` im Workflow).

Manuell triggern: GitHub → Actions → "Build & publish Docker image" → Run workflow

---

## C) Homepage softhenge.ch via Web Station

Ziel: `https://softhenge.ch` und `https://www.softhenge.ch` zeigen die statische
Homepage aus `homepage/index.html`.

### 1. Web Station installieren

DSM → Paket-Zentrum → Web Station installieren (inkl. Apache HTTP Server falls
noch nicht vorhanden — die statische Seite braucht nicht zwingend PHP).

### 2. Homepage-Dateien deployen

```bash
mkdir -p /volume1/web/softhenge
# homepage/index.html aus diesem Repo nach /volume1/web/softhenge/index.html kopieren
chmod 644 /volume1/web/softhenge/index.html
```

Tipp für künftige Updates: kleines Script auf der Synology, das aus dem
GitHub-Repo zieht — oder du editierst `index.html` direkt via Synology File
Station, da die Seite single-file ist.

### 3. Web Station: Web Service hinzufügen

Web Station → "Web Service" → Erstellen
- **Name:** softhenge-homepage
- **Service:** Apache HTTP Server (oder Nginx, beides ok)
- **PHP:** keine
- **Document Root:** `web/softhenge`

### 4. Web Station: Web Portal

Web Station → "Web Portal" → Erstellen
- **Service:** softhenge-homepage (von oben)
- **Portal-Typ:** Name-based
- **Hostname:** `softhenge.ch` und `www.softhenge.ch` (je ein Portal-Eintrag)
- **Port:** HTTPS 443 (Web Station legt Reverse-Proxy-Eintrag im Apache an)

### 5. Cloudflare DNS

Cloudflare Dashboard → softhenge.ch → DNS → Records:

| Type | Name | Content | Proxy |
|---|---|---|---|
| A | `softhenge.ch` (`@`) | <Heim-IP, wird per DynDNS aktuell gehalten> | DNS only |
| A | `www` | <gleiche IP> | DNS only |
| A | `solar` | <gleiche IP> | DNS only (existiert schon) |

Das DynDNS-Script aus CLAUDE.md aktualisiert nur den `solar`-Record. Erweitere
`/volume1/scripts/cloudflare-dyndns.sh` um zusätzlich `@` und `www` zu
aktualisieren — oder setze in Cloudflare einen CNAME `www → softhenge.ch` und
`@` als A-Record, dann reicht ein Update auf den A-Record `@`.

### 6. Let's Encrypt Zertifikat (SAN für softhenge.ch + www)

Erweitere das bestehende acme.sh-Setup um die neuen Hostnames:

```bash
export CF_Token="<dein cloudflare token>"
~/.acme.sh/acme.sh --issue --dns dns_cf \
    -d softhenge.ch \
    -d www.softhenge.ch
```

Im DSM (Systemsteuerung → Sicherheit → Zertifikat) das neue Zertifikat
importieren und dem Web Portal `softhenge.ch` zuweisen.

### 7. FritzBox Portforwarding

Bereits vorhanden: TCP 443 → Synology. Keine Änderung nötig — Web Station
hängt sich auf den gleichen Port und unterscheidet anhand des SNI-Hostnames.

---

## D) Troubleshooting

### Container startet nicht — BigQuery-Fehler

```bash
docker compose logs solar-dashboard | grep -i "credentials\|bigquery\|gcp"
```

Häufige Ursachen:
- `gcp-key.json` fehlt oder Pfad im compose-File falsch (muss `/etc/gcp/key.json` im Container sein)
- Service Account hat keine BigQuery-Rolle (siehe Schritt A.1)
- Service Account ist im falschen Projekt (`modern-cubist-412113`)

### Pull-Script läuft nicht

```bash
tail -50 /volume1/scripts/solar-pull.log
```

Häufige Ursachen:
- Cron-Job läuft als falscher User (muss `root` sein, sonst kein `docker`-Zugriff)
- GHCR-Login ist abgelaufen — `docker login ghcr.io -u chutz68` neu ausführen
- Compose-Datei nicht wo erwartet (`/volume1/docker/solar-dashboard/docker-compose.yml`)

### Reverse Proxy zeigt 502 / 504

- Container läuft, aber Vaadin braucht beim ersten Request bis zu 30 Sek bis JIT warm ist
- Prüfen: `curl -I http://localhost:8080/` direkt auf der Synology
- Reverse Proxy Timeout in DSM auf mind. 60 Sek setzen wegen Vaadin Long-Polling/WebSocket

### Browser zeigt Mixed Content / WebSocket-Fehler

- Cloudflare Proxy-Status für `solar.softhenge.ch` muss **DNS only** sein (graue Wolke)
- Vaadin verwendet WebSocket — Cloudflare-Proxy würde diese blockieren

---

## E) Rollback

Falls ein Deploy etwas kaputt macht: vorheriges Image-Tag pinnen.

```bash
# Auf der Synology — bisherige Tags ansehen
docker image ls ghcr.io/chutz68/solar-dashboard-vaadin

# In docker-compose.yml den Tag von :latest auf den letzten guten ändern, z.B.:
#   image: ghcr.io/chutz68/solar-dashboard-vaadin:sha-a1b2c3d
docker compose up -d

# Pull-Script währenddessen pausieren (sonst überschreibt es das Pinning)
# DSM → Aufgabenplaner → Solar Dashboard Auto-Pull → deaktivieren
```

Nach Hotfix in Git: Tag in compose-File zurück auf `:latest`, Pull-Script
wieder aktivieren.
