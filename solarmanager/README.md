# Solar Data Pipeline

End-to-end solar energy data pipeline collecting 5-minute interval data from multiple sources, storing everything in Google BigQuery, and visualizing it via a Vaadin Flow web dashboard.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│  Every 5 minutes (xx:01)                                        │
│                                                                  │
│  Raspberry Pi 5 (RuRosRaspy5)                                   │
│       │                                                          │
│       ├──► Solarmanager API    → Solar + Battery data           │
│       ├──► Solarmanager Stream → Warmwater + HP state           │
│       ├──► OpenWeatherMap API  → Weather data                   │
│       ├──► ISG (192.168.1.180) → Heat pump data (HTML scraping) │
│       │                                                          │
│       ├──► BigQuery: SolarManager.SolarManager_5m               │
│       └──► GCS: solar-isg-data/raspy_status.json (heartbeat)   │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  Every 5 minutes (xx:02) – Standby                              │
│                                                                  │
│  Synology DS218+ (RuRosNAS218)                                  │
│       │                                                          │
│       ├──► Reads raspy_status.json from GCS                     │
│       ├──► If Raspy OK (< 6 min ago) → exit, do nothing         │
│       └──► If Raspy down → takes over full ingestion            │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  Daily at 00:04 (Europe/Zurich)                                  │
│                                                                  │
│  Cloud Scheduler → Cloud Run Job (solar-daily-job)              │
│       │                                                          │
│       ├──► Aggregates SolarManager_5m → SolarManager_1d         │
│       └──► Data quality check (missing rows, duplicates)        │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  Web Dashboard                                                   │
│                                                                  │
│  Vaadin Flow + Kotlin (solar-dashboard)                         │
│       │                                                          │
│       ├──► DashboardView  → 30-day table (SolarManager_1d)      │
│       └──► LiveView       → 5-min chart (SolarManager_5m)       │
└─────────────────────────────────────────────────────────────────┘
```

---

## Data Sources

| Source | Data | Method |
|---|---|---|
| Solarmanager API | Solar production, consumption, battery | REST API (5-min intervals) |
| Solarmanager Stream | Warmwater temperature, HP operation state | REST API (realtime) |
| OpenWeatherMap | Weather, temperature, wind, rain | REST API |
| Stiebel Eltron ISG | Heat pump temperatures, energy counters | HTML scraping |

---

## Repository Structure

```
general/
└── solarmanager/
    ├── aggregate_1d/               # Cloud Run Job – daily aggregation
    │   ├── aggregate_1d.py
    │   ├── Dockerfile
    │   └── requirements.txt
    ├── raspy/                      # Raspberry Pi scripts
    │   ├── ingest_5m.py            # Main 5-min ingestion (Raspy)
    │   ├── standby_ingest.py       # Standby ingestion (Synology)
    │   └── isg_to_gcs.py           # Legacy: ISG → GCS (no longer used)
    └── solar-dashboard/            # Vaadin Flow web dashboard
        ├── pom.xml
        ├── frontend/themes/solar-dashboard/styles.css
        └── src/main/kotlin/ch/softhenge/solar/
            ├── SolarDashboardApplication.kt
            ├── service/SolarService.kt
            └── view/
                ├── DashboardView.kt
                └── LiveView.kt
```

---

## BigQuery Tables

### `SolarManager.SolarManager_5m`
5-minute interval data – the main source of truth. Partitioned by `t`.

| Field group | Fields | Source |
|---|---|---|
| Timestamp | `t`, `inserted` | Solarmanager |
| Solar | `pW`, `pWh`, `cW`, `cWh`, `iWh`, `eWh`, `scWh`, `cPvWh` | Solarmanager |
| Battery | `bcW`, `bcWh`, `bdW`, `bdWh`, `soc` | Solarmanager |
| HP Stream | `tHpWarmwaterC`, `sHp` | Solarmanager Stream |
| Heat pump temps | `tHpFlowC`, `tHpBackC`, `tHpOutC` | ISG |
| Heat pump energy | `pHpHeatingDayWh`, `pHpHeatingTotalWh`, `pHpWarmwaterDayWh`, `pHpWarmwaterTotalWh`, `sHpCurrent` | ISG |
| Weather | `tempReal`, `tempFeel`, `airPressure`, `airHumidity`, `visibility`, `windSpeed`, `windDegree`, `cloudiness`, `rainAmount`, `snowAmount` | OpenWeatherMap |
| Weather detail | `weatherMain`, `weatherDetailed`, `weatherIconId`, `sunriseTimestamp`, `sunsetTimestamp` | OpenWeatherMap |
| Quality | `v` | Pipeline |

**Data quality flag `v`:**
| Value | Meaning |
|---|---|
| `1111` | Solar + Battery + Weather + ISG all present |
| `1101` | ISG missing |
| `1100` | ISG + Battery missing (Solarlog era) |
| `1` | Legacy Solarlog data |

**Data history:**
- 2018-03-30 → 2025-10-19: Solarlog data (`v=1` or `v=1100`)
- 2025-10-20 → today: Solarmanager data (`v=1101` or `v=1111`)

---

### `SolarManager.SolarManager_1d`
Daily aggregation of `SolarManager_5m`. One row per day. Partitioned by `day`.

| Field group | Fields |
|---|---|
| Date | `day`, `inserted` |
| Energy [Wh] | `cWh`, `pWh`, `bcWh`, `bdWh`, `scWh`, `cPvWh`, `iWh`, `eWh` |
| Heat pump [Wh] | `pHpHeatingDayWh`, `pHpWarmwaterDayWh`, `pHpHeatingTotalWh`, `pHpWarmwaterTotalWh` |
| Power [W] | `pWmax`, `pWmin`, `cWmax`, `cWmin` |
| Battery [%] | `socMax`, `socMin` |
| Weather | `tempRealMin`, `tempRealMax`, `rainAmountSum`, `snowAmountSum`, `sunriseTimestamp`, `sunsetTimestamp` |
| KPIs | `selfConsumptionPct`, `autarkyPct` |
| Quality | `rowCount`, `missingRows`, `duplicates`, `vMin` |

---

### `SolarManager.SolarManager_5m_v`
View on `SolarManager_5m` with two additional calculated fields:
- `selfConsumptionPct` – % of production consumed locally
- `autarkyPct` – % of consumption from own production

---

### `SolarlogData.t_f_solardata_5m`
Raw Solarlog export from Synology MySQL (2018–2025). Original field names preserved.

---

## GCP Resources

| Resource | Name | Purpose |
|---|---|---|
| Cloud Run Job | `solar-daily-job` | Daily aggregation |
| Cloud Scheduler | `solar-daily-scheduler` | Triggers daily job at 00:04 Europe/Zurich |
| GCS Bucket | `solar-isg-data` | Raspy heartbeat (`raspy_status.json`) |
| Secret Manager | `solarmanager-token` | Solarmanager API token (legacy) |
| Secret Manager | `openweathermap-apikey` | OWM API key (legacy) |
| Artifact Registry | `solar` | Docker images |
| Service Account | `solar-runner` | Cloud Run + BigQuery access |

---

## Raspberry Pi Setup (RuRosRaspy5)

**User:** `wern`
**Location:** `/home/wern/isg/`

**Files:**
```
/home/wern/isg/
├── ingest_5m.py        # Main ingestion script
├── gcp-key.json        # GCP Service Account key (never commit!)
├── .env                # Environment variables (never commit!)
└── ingest_5m.log       # Log file (rotated weekly)
```

**`.env` file:**
```bash
export GOOGLE_APPLICATION_CREDENTIALS=/home/wern/isg/gcp-key.json
export GCP_PROJECT_ID=modern-cubist-412113
export SOLARMANAGER_TOKEN="Basic xxxx"
export OWM_APIKEY=your_key_here
```

**Crontab** (`/etc/crontab` not applicable – user crontab):
```
1,6,11,16,21,26,31,36,41,46,51,56 * * * * python3 /home/wern/isg/ingest_5m.py >> /home/wern/isg/ingest_5m.log 2>&1
```

**Log rotation** (`/etc/logrotate.d/solar-isg`):
```
/home/wern/isg/*.log {
    weekly
    rotate 12
    compress
    delaycompress
    missingok
    notifempty
    dateext
    dateformat -%Y-%m-%d
}
```

**IPv4 fix** (prevents 2-min timeout):
In `/etc/gai.conf` – uncomment:
```
precedence ::ffff:0:0/96  100
```

---

## Synology DS218+ Setup (RuRosNAS218)

**User:** `werneradmin`
**Location:** `/volume1/Daten/dev/solarmanager/`
**Python:** `python3.14`

**Files:**
```
/volume1/Daten/dev/solarmanager/
├── standby_ingest.py   # Standby ingestion script
├── gcp-key.json        # GCP Service Account key (never commit!)
├── .env                # Environment variables (never commit!)
└── standby.log         # Log file (rotated weekly)
```

**`.env` file:**
```bash
export GOOGLE_APPLICATION_CREDENTIALS=/volume1/Daten/dev/solarmanager/gcp-key.json
export GCP_PROJECT_ID=modern-cubist-412113
export SOLARMANAGER_TOKEN="Basic xxxx"
export OWM_APIKEY=your_key_here
```

**Crontab** (`/etc/crontab`):
```
2,7,12,17,22,27,32,37,42,47,52,57 * * * * werneradmin cd /volume1/Daten/dev/solarmanager && python3.14 standby_ingest.py >> /volume1/Daten/dev/solarmanager/standby.log 2>&1
```

**Log rotation** (`/etc/logrotate.d/solar-standby`):
```
/volume1/Daten/dev/solarmanager/*.log {
    weekly
    rotate 12
    compress
    delaycompress
    missingok
    notifempty
    dateext
    dateformat -%Y-%m-%d
}
```

---

## Failover Logic

```
xx:01 → Raspy runs ingest_5m.py
         → Inserts into BigQuery
         → Writes raspy_status.json to GCS (timestamp + status)

xx:02 → Synology runs standby_ingest.py
         → Reads raspy_status.json from GCS
         → If timestamp < 6 min ago → exit (Raspy OK)
         → If timestamp > 6 min ago → full ingestion takeover
```

**When Synology takes over:**
- Fetches all data sources (Solarmanager, OWM, ISG)
- Inserts with duplicate check
- `v=1111` if ISG available, `v=1101` if not
- Log entry: `[SYNOLOGY STANDBY] Row inserted`

---

## Schedules

| Job | Schedule | Description |
|---|---|---|
| Raspy `ingest_5m.py` | `1,6,11,16,21,26,31,36,41,46,51,56 * * * *` | Every 5 min, 1 min after interval |
| Synology `standby_ingest.py` | `2,7,12,17,22,27,32,37,42,47,52,57 * * * *` | Every 5 min, 2 min after interval |
| Cloud Run `solar-daily-job` | `4 0 * * *` (Europe/Zurich) | Daily at 00:04 |

---

## Web Dashboard

**Tech Stack:** Vaadin Flow + Kotlin + Spring Boot + BigQuery Java SDK

**Views:**
- `/` → **DashboardView** – 30-day table with daily KPIs
- `/live` → **LiveView** – 5-min chart (Apache ECharts) with day/week/month switching and battery toggle

**Run locally:**
```bash
# Set GCP credentials
gcloud auth application-default login

# Run in IntelliJ
# Open solar-dashboard/ → Run SolarDashboardApplication.kt
# Browser: http://localhost:8080
```

**Requirements:**
- Java 21 (Azul Zulu recommended)
- Maven 3.9.x
- Node.js LTS
- GCP Application Default Credentials

---

## GitHub Actions CI/CD

Every push to `master` → builds and deploys `solar-daily-job` automatically.

**Secrets required:**
- `WIF_PROVIDER` – Workload Identity Federation provider
- `WIF_SERVICE_ACCOUNT` – GCP Service Account email

---

## Backfill & Maintenance Tools (local PC)

| Script | Purpose |
|---|---|
| `backfill.py` | Full range backfill Solarmanager + OWM (2025-10-20 → present) |
| `backfill_single_day.py` | Single day backfill with duplicate check |
| `patch_sunrise_sunset.py` | Patch missing sunrise/sunset values via API |
| `import_owm_and_patch.py` | Bulk import OWM file + mass UPDATE weather data |
| `import_solarlog.py` | Import MySQL Solarlog dump (2018–2025) into BigQuery |
| `backfill_1d.sql` | Backfill SolarManager_1d from 2018 |
