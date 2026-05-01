# Deployment: Solar Dashboard auf Synology DS218+

Pipeline: **GitHub `master` → GitHub Actions → GHCR → Synology (auto-pull)**

```
                                 ┌─────────────┐
                                 │   GHCR      │
git push master ─► GitHub Actions─► ghcr.io   │ ◄── solar-pull.sh (alle 5 min via cron)
                  (mvn + docker)  │  :latest   │      │
                                 └─────────────┘      ▼
                                                  Synology DS218+
                                                  └─ docker-compose
                                                     └─ solar-dashboard:8080
```

---

## Repo-Struktur (Monorepo)

Das GitHub-Repo `chutz68/general` ist ein Monorepo. Der Solar-Dashboard-Code liegt im
Unterverzeichnis `solar-dashboard-vaadin/`. Der GitHub Actions Workflow liegt deshalb
am **Repo-Root** unter `.github/workflows/docker-publish.yml` (nicht im Unterverzeichnis) —
GitHub Actions liest Workflows nur vom Root.

Der Workflow wird nur getriggert wenn Dateien unter `solar-dashboard-vaadin/**` geändert werden.

---

## Files

| Pfad (relativ zu `solar-dashboard-vaadin/`) | Zweck |
|---|---|
| `Dockerfile` | Multi-stage Build (Maven → JRE 21) |
| `.dockerignore` | Schliesst `target/`, `.git`, `homepage/` etc. aus |
| `deploy/docker-compose.yml` | Container-Definition für Synology |
| `deploy/synology-pull.sh` | Cron-Script: pullt neues Image und restartet Container |

| Pfad (relativ zu Repo-Root `general/`) | Zweck |
|---|---|
| `.github/workflows/docker-publish.yml` | Build & Push zu `ghcr.io` bei Push auf `master` |

---

## GCP Service Account

Für die Synology wurde ein separater **read-only** Service Account erstellt:
- **Name:** `solar-dashboard-reader@modern-cubist-412113.iam.gserviceaccount.com`
- **Rollen:** `roles/bigquery.dataViewer` + `roles/bigquery.jobUser`
- **Key-Datei auf Synology:** `/volume1/docker/solar-dashboard/secrets/gcp-key-dashboard.json`

Der bestehende `solar-runner` Service Account (Schreibzugriff, für Raspberry Pi Ingestion)
wird bewusst **nicht** für die Synology verwendet — Least-Privilege-Prinzip.

---

## GHCR Image

- **Registry:** `ghcr.io`
- **Image:** `ghcr.io/chutz68/solar-dashboard-vaadin:latest`
- **Sichtbarkeit:** Public (kein Login auf Synology nötig)
- **Tags:** `:latest`, `:sha-<short>`, `:<YYYYMMDD-HHmmss>`

Package-Seite: https://github.com/chutz68/general/pkgs/container/solar-dashboard-vaadin

---

## Synology Setup (einmalig)

### Wichtige Besonderheiten der DS218+

- Docker ist installiert, aber der Socket (`/var/run/docker.sock`) gehört `root:root`
- Kein `docker`-Gruppe vorhanden — nur `root` kann Docker-Befehle ausführen
- Alle Docker-Befehle müssen als **root** über den DSM Aufgabenplaner laufen
- Synology verwendet **`docker-compose`** (mit Bindestrich, alte Version) — nicht `docker compose`

### Dateien auf der Synology

```
/volume1/docker/solar-dashboard/
├── docker-compose.yml          ← aus deploy/docker-compose.yml
└── secrets/
    └── gcp-key-dashboard.json  ← GCP Service Account Key (read-only)

/volume1/scripts/
├── solar-pull.sh               ← aus deploy/synology-pull.sh
└── solar-pull.log              ← wird automatisch erstellt
```

### docker-compose.yml platzieren

Inhalt von `deploy/docker-compose.yml` nach `/volume1/docker/solar-dashboard/docker-compose.yml` kopieren.

### Pull-Script installieren

Inhalt von `deploy/synology-pull.sh` nach `/volume1/scripts/solar-pull.sh` kopieren:
```bash
chmod +x /volume1/scripts/solar-pull.sh
```

### DSM Aufgabenplaner

DSM → Systemsteuerung → Aufgabenplaner → Erstellen → Benutzerdefiniertes Script

| Einstellung | Wert |
|---|---|
| Name | Solar Dashboard Auto-Pull |
| Benutzer | `root` |
| Zeitplan | Täglich, alle 5 Min wiederholen |
| Befehl | `/volume1/scripts/solar-pull.sh >> /volume1/scripts/solar-pull.log 2>&1` |

### Reverse Proxy

DSM → Systemsteuerung → Anmeldeportal → Erweitert → Reverseproxy:

| Einstellung | Wert |
|---|---|
| Protokoll Quelle | HTTPS |
| Hostname Quelle | `solar.softhenge.ch` |
| Port Quelle | 443 |
| Protokoll Ziel | HTTP |
| Hostname Ziel | `localhost` |
| Port Ziel | 8080 |

### Erststart

Im Aufgabenplaner als root einmalig ausführen:
```bash
cd /volume1/docker/solar-dashboard && docker-compose pull && docker-compose up -d
```

---

## Tägliches Deployment

Einfach `git push origin master` mit Änderungen unter `solar-dashboard-vaadin/` — das war's:

1. GitHub Actions baut den Production-JAR (`mvn -Pproduction`) und das Docker-Image
2. Image wird mit Tags `:latest`, `:sha-<short>` und `:<datetime>` zu `ghcr.io` gepusht
3. Innerhalb von ≤ 5 Min zieht `solar-pull.sh` das neue Image und startet den Container neu
4. Downtime: ~30 Sek (Vaadin braucht beim Start ~20 Sek)

Workflow manuell triggern: GitHub → Actions → "Build & publish Solar Dashboard Docker image" → Run workflow

---

## Troubleshooting

### Container startet nicht — BigQuery-Fehler
```bash
# Im Aufgabenplaner als root:
docker logs solar-dashboard > /volume1/scripts/docker-status.log 2>&1
```
Häufige Ursachen:
- `gcp-key-dashboard.json` fehlt oder falscher Pfad
- Service Account hat keine BigQuery-Rolle

### Pull-Script läuft nicht
```bash
tail -50 /volume1/scripts/solar-pull.log
```
Häufige Ursachen:
- Cron-Job läuft nicht als `root`
- Image ist nicht mehr public → `docker-compose pull` schlägt fehl

### Reverse Proxy zeigt 502 / 504
- Vaadin braucht beim ersten Request bis zu 30 Sek (JIT warm-up)
- Reverse Proxy Timeout in DSM auf mind. 60 Sek setzen
- Direkt prüfen: Im Aufgabenplaner `curl -I http://localhost:8080/ > /volume1/scripts/curl.log 2>&1`

---

## Rollback

```bash
# Bisherige Tags ansehen (im Aufgabenplaner als root):
docker image ls ghcr.io/chutz68/solar-dashboard-vaadin

# In /volume1/docker/solar-dashboard/docker-compose.yml den Tag pinnen:
#   image: ghcr.io/chutz68/solar-dashboard-vaadin:sha-a1b2c3d
docker-compose up -d

# Auto-Pull pausieren während Rollback:
# DSM → Aufgabenplaner → Solar Dashboard Auto-Pull → deaktivieren
```

Nach Hotfix: Tag zurück auf `:latest`, Auto-Pull wieder aktivieren.
