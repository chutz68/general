# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

**Build:**
```bash
mvn clean package
```

**Run (development):**
```bash
mvn spring-boot:run
```

**Production build** (bundles frontend assets):
```bash
mvn clean package -Pproduction
```

**Run tests:**
```bash
mvn test
```

There is no linting plugin configured. Kotlin compilation errors surface via `mvn compile`.

## Architecture

This is a **Kotlin + Spring Boot + Vaadin Flow** application that visualizes solar energy data. It is a pure server-side UI app (no Hilla/React); all views are Vaadin Flow components rendered server-side.

### Data flow

1. Solar hardware data is ingested externally into **Google BigQuery** (project `modern-cubist-412113`, dataset `SolarManager`).
2. Two BigQuery tables are queried:
   - `SolarManager_1d` — daily aggregates (production, consumption, import, export, self-consumption %, autarky %, battery, temperature, rain)
   - `SolarManager_5m` — 5-minute interval readings (power watts + energy Wh per slot)
   - `SolarManager_5m_v` — view with calculated KPIs
3`SolarService` is the sole service layer; it builds and executes BigQuery SQL queries directly via the GCP Java client, then maps rows to Kotlin data classes (`DailyData`, `FiveMinData`, `CurrentData`, `TodaySums`).

### Views (routes)

| Class | Route | Purpose |
|---|---|---|
| `DashboardView` | `/` | Today's KPI cards + 30-day data grid |
| `LiveView` | `/live` | ECharts line/bar chart of 5-min data; day/week/month periods |
| `EnergyFlowView` | `/flow` | ECharts graph showing real-time energy flow; auto-refreshes every 5 min via Vaadin poll |

All views extend `VerticalLayout` and are Spring-managed beans (Vaadin handles instantiation per navigation).

### Key utilities

- `TimeUtils` — all date/time handling uses `Europe/Zurich` timezone. Always use `TimeUtils.today()` instead of `LocalDate.now()`.
- `TranslationProvider` — Vaadin `I18NProvider` backed by `messages.properties` (English only). Use `getTranslation(key)` in views.

### GCP authentication

`SolarService` uses `BigQueryOptions.newBuilder()` without explicit credentials, so it relies on **Application Default Credentials** (ADC). The environment must have ADC configured (e.g., `gcloud auth application-default login` or a service account key via `GOOGLE_APPLICATION_CREDENTIALS`).

### Frontend

Vaadin Flow generates frontend resources under `frontend/generated/` — do not edit these files manually. Custom CSS lives in `frontend/themes/solar-dashboard/styles.css`. Charts use **Apache ECharts 5** loaded from CDN via `@JavaScript` annotations on the view classes; chart rendering is done by injecting JavaScript strings via `element.executeJs()`.

## Project: Solar Dashboard (ch.softhenge.solar)
Vaadin Flow + Kotlin dashboard for home solar energy monitoring.

## Stack
- Language: Kotlin (preferred over Java)
- Framework: Vaadin Flow
- Build: Maven 3.9.14, Azul Zulu Java 21
- Charts: Apache ECharts
- DB: BigQuery (GCP project: modern-cubist-412113, region: europe-west6)
- GCS bucket: solar-isg-data
- CI/CD: GitHub Actions + Workload Identity Federation (no JSON keys)
- GCP services: Cloud Run, Cloud Scheduler, Secret Manager, Artifact Registry

## Data Quality Flag `v`
- 1111: all sources present
- 1101: ISG missing
- 1100: ISG + battery missing
- 1: legacy

## Ingestion
- Python `ingest_5m.py` on Raspberry Pi (hostname: RuRosRaspy5, user: wern)
- Cron: 1,6,11,16,... min past hour
- Sources: Solarmanager API, OpenWeatherMap, Stiebel Eltron ISG (HTML scraping)
- Synology DS218+ standby: checks `raspy_status.json` in GCS, takes over if >6min gap

## Conventions
- Prefer Kotlin over Java everywhere
- Prefer direct SQL/BigQuery over Python scripts where possible
- Avoid redundant tables — use views when sufficient
- Keep concerns cleanly separated
- GitHub account: chutz68

# Infrastructure: Public Deployment via Synology DS218+

## Übersicht

Die Vaadin-App (`solar.softhenge.ch`) wird auf der **Synology DS218+** zu Hause gehostet —
kostenlos, da die Hardware bereits vorhanden ist. Die einzigen laufenden Kosten sind die
Domain-Registrierung (CHF 8.90/Jahr bei Infomaniak).

### Warum nicht Google Cloud Run?

Cloud Run mit `--min-instances=1` (notwendig wegen Vaadin Server-Side State) würde in Europa
ca. €10–15/Monat kosten. Da der 5-Minuten-Ingestion-Job bereits aus Kostengründen vom Cloud Run
auf den Raspberry Pi migriert wurde, war Synology die konsequente Wahl: gleiche Logik,
null Zusatzkosten.

---

## Komponenten und Warum

### 1. Domain: `softhenge.ch` (Infomaniak)

Registriert bei [Infomaniak](https://www.infomaniak.com) als reine Domain **ohne Webhosting**
(CHF 8.90/Jahr). Beim Checkout "Nein, ich benötige derzeit keine Website" wählen —
kein Hosting-Paket nötig.

Subdomain für die App: `solar.softhenge.ch`

### 2. Cloudflare (kostenloser Plan)

**Warum Cloudflare?**

Die Heim-IP-Adresse ist dynamisch (ändert sich periodisch). Damit `solar.softhenge.ch` immer
auf die aktuelle Heim-IP zeigt, braucht es einen DNS-Anbieter mit API — damit ein Script
die IP automatisch aktualisieren kann (DynDNS).

Cloudflare bietet:
- **Kostenloses DNS** mit weltweitem Anycast-Netzwerk (schneller als Infomaniaks DNS)
- **API für DynDNS-Updates** via API-Token
- **DNS-Challenge für Let's Encrypt** — Zertifikat-Ausstellung ohne offene Ports 80/443

**Einrichtung:**
1. Account erstellen auf [cloudflare.com](https://cloudflare.com)
2. Domain hinzufügen: Dashboard → "Add a domain" → `softhenge.ch` → Free Plan
3. Cloudflare weist zwei Nameserver zu (z.B. `nicolas.ns.cloudflare.com`, `peaches.ns.cloudflare.com`)
4. Bei Infomaniak: Domain → Nameserver → auf Cloudflare-Nameserver umstellen
5. Warten auf Aktivierungs-E-Mail von Cloudflare (~10–60 Minuten)

**API Token erstellen:**
- Cloudflare Dashboard → My Profile → API Tokens → Create Token
- Vorlage: "Edit zone DNS"
- Zone Resources: Include → Specific zone → `softhenge.ch`
- Permission: Zone / DNS / Edit
- Kein IP-Filter (Heim-IP ist dynamisch)
- Token sicher speichern — wird nur einmal angezeigt

**Wichtig:** Proxy-Status für `solar.softhenge.ch` auf **DNS only** (grauer Cloud) lassen,
da Vaadin WebSockets verwendet — Cloudflare-Proxy würde diese blockieren.

### 3. DynDNS Script auf Synology

Das Script `/volume1/scripts/cloudflare-dyndns.sh` läuft alle 5 Minuten via Synology
Task Scheduler und aktualisiert den DNS-A-Record in Cloudflare wenn sich die Heim-IP ändert.

**Funktionsweise:**
1. Externe IP via `api.ipify.org` abrufen
2. Gespeicherte IP aus `.last_ip` lesen
3. Bei Änderung: Cloudflare API aufrufen und A-Record für `solar.softhenge.ch` updaten
4. Neue IP in `.last_ip` speichern

**Script-Pfad:** `/volume1/scripts/cloudflare-dyndns.sh`
**Log:** `/volume1/scripts/dyndns.log`
**Gespeicherte IP:** `/volume1/scripts/.last_ip`

**Task Scheduler einrichten:**
DSM → Systemsteuerung → Aufgabenplaner → Erstellen → Geplante Aufgabe → Benutzerdefiniertes Script
- Benutzer: `root`
- Zeitplan: täglich, wiederholen alle 5 Minuten
- Script: `/volume1/scripts/cloudflare-dyndns.sh >> /volume1/scripts/dyndns.log 2>&1`

### 4. Let's Encrypt Zertifikat via acme.sh

**Warum acme.sh mit DNS-Challenge?**

Die übliche HTTP-Challenge für Let's Encrypt erfordert einen öffentlich erreichbaren
Webserver auf Port 80. Die DNS-Challenge via Cloudflare API funktioniert ohne offene Ports —
acme.sh schreibt einen temporären TXT-Record in Cloudflare, Let's Encrypt verifiziert diesen,
und das Zertifikat wird ausgestellt.

**Installation:**
```bash
curl https://get.acme.sh | sh -s email=deine@email.ch --force
source ~/.profile
```

**CA auf Let's Encrypt umstellen** (ZeroSSL hat Timeout-Probleme gehabt):
```bash
~/.acme.sh/acme.sh --set-default-ca --server letsencrypt
```

**Zertifikat ausstellen:**
```bash
export CF_Token="dein-cloudflare-api-token"
~/.acme.sh/acme.sh --issue --dns dns_cf -d solar.softhenge.ch
```

acme.sh speichert den `CF_Token` automatisch in `~/.acme.sh/account.conf` für künftige Renewals.

**Zertifikate nach `/volume1/scripts/ssl/` kopieren:**
```bash
mkdir -p /volume1/scripts/ssl
~/.acme.sh/acme.sh --install-cert -d solar.softhenge.ch \
  --cert-file /volume1/scripts/ssl/solar.softhenge.ch.cer \
  --key-file /volume1/scripts/ssl/solar.softhenge.ch.key \
  --fullchain-file /volume1/scripts/ssl/fullchain.cer
cp /var/services/homes/werneradmin/.acme.sh/solar.softhenge.ch_ecc/ca.cer /volume1/scripts/ssl/ca.cer
```

**Zertifikat in DSM importieren:**
DSM → Systemsteuerung → Sicherheit → Zertifikat → Hinzufügen → Zertifikat importieren
- Privatschlüssel: `solar.softhenge.ch.key`
- Zertifikat: `solar.softhenge.ch.cer`
- Zwischenzertifikat: `ca.cer` ← nicht fullchain.cer!

**Zertifikat zuweisen:**
DSM → Systemsteuerung → Sicherheit → Zertifikat → Einstellungen → Konfigurieren
→ Bei `solar.softhenge.ch` das Let's Encrypt Zertifikat auswählen

**Automatische Erneuerung** (Zertifikat läuft nach 90 Tagen ab):
Synology Task Scheduler → neuer Job:
```bash
/var/services/homes/werneradmin/.acme.sh/acme.sh --cron --home /var/services/homes/werneradmin/.acme.sh
```
Zeitplan: täglich (acme.sh erneuert nur wenn < 30 Tage bis Ablauf)

### 5. Reverse Proxy (Synology DSM)

Der Synology Reverse Proxy empfängt HTTPS-Anfragen auf Port 443 und leitet sie intern
an die Vaadin-App auf Port 8080 weiter.

**Einrichtung:**
DSM → Systemsteuerung → Anmeldeportal → Erweitert → Reverseproxy → Erstellen
- Name: `Solar Dashboard`
- Protokoll Quelle: `HTTPS`
- Hostname Quelle: `solar.softhenge.ch`
- Port Quelle: `443`
- Protokoll Ziel: `HTTP`
- Hostname Ziel: `localhost`
- Port Ziel: `8080`
- HSTS: deaktiviert (private App, unnötiges Risiko)

### 6. FritzBox Portweiterleitung

Die FritzBox (hinter der Sunrise Glasfaser Box im DMZ-Modus) leitet eingehende
HTTPS-Anfragen zur Synology weiter.

**FritzBox → Internet → Freigaben → Portfreigaben:**
- Protokoll: TCP
- Von Port: 443
- An Port: 443
- Zielgerät: Synology DS218+

**Wichtig:** Port 5001 (DSM) **nicht** von aussen freigeben — Sicherheitsrisiko.
DSM-Fernzugriff läuft über **QuickConnect** (DSM → Systemsteuerung → QuickConnect).

---

## Netzwerk-Topologie

```
Internet
  → solar.softhenge.ch (Cloudflare DNS, DNS-only)
  → Heim-IP 188.155.x.x (via DynDNS aktuell gehalten)
  → Sunrise Glasfaser Box (DMZ-Modus → FritzBox)
  → FritzBox (Port 443 → Synology)
  → Synology Reverse Proxy (HTTPS → localhost:8080)
  → Vaadin Docker Container (Port 8080)
  → BigQuery (GCP, bestehende Infrastruktur)
```

---

## Kosten

| Komponente | Kosten |
|---|---|
| Domain `softhenge.ch` | CHF 8.90 / Jahr |
| Cloudflare DNS | gratis |
| Let's Encrypt Zertifikat | gratis |
| Synology DS218+ Hosting | CHF 0 (bereits vorhanden) |
| **Total** | **~CHF 0.75 / Monat** |

---

## Offene Punkte / TODO

- [ ] Port 5001 in FritzBox schliessen (nach QuickConnect-Migration aller Geräte)
- [ ] Synology Drive auf allen Geräten auf QuickConnect-ID umstellen
- [ ] Dockerfile für Vaadin App erstellen
- [ ] GitHub Actions Deploy-Pipeline zur Synology einrichten
- [ ] PWA-Annotation in Vaadin App hinzufügen (`@PWA`)
