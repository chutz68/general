# Solar Manager Pipeline

End-to-end solar data pipeline collecting 5-minute interval data from Solarmanager, OpenWeatherMap and a Stiebel Eltron ISG heat pump, storing everything in Google BigQuery.

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│  Every 5 minutes (xx:01, xx:06, ...)                    │
│                                                          │
│  Cloud Scheduler → Cloud Run Job (solar-bq-job)         │
│       │                                                  │
│       ├──► Solarmanager API    → Solar + Battery data   │
│       ├──► OpenWeatherMap API  → Weather data           │
│       ├──► GCS Bucket          → ISG heat pump data     │
│       │    (written by Raspy)                            │
│       ▼                                                  │
│  BigQuery: SolarManager.SolarManager_5m                 │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  Every 5 minutes (xx:04, xx:09, ...)                    │
│                                                          │
│  Raspberry Pi Cron                                       │
│       │                                                  │
│       ├──► ISG (Stiebel Eltron) HTML scraping           │
│       ▼                                                  │
│  GCS Bucket: solar-isg-data/isg_data.json               │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  Daily at 00:02                                          │
│                                                          │
│  Cloud Scheduler → Cloud Run Job (solar-daily-job)      │
│       │                                                  │
│       ├──► Aggregate SolarManager_5m → SolarManager_1d  │
│       └──► Data quality check                           │
└─────────────────────────────────────────────────────────┘
```

---

## Repository Structure

```
solarmanager/
├── ingest_5m/                  # 5-min data ingestion Cloud Run Job
│   ├── ingest_5m.py            # Main ingestion script
│   ├── Dockerfile
│   └── requirements.txt
├── aggregate_1d/               # Daily aggregation Cloud Run Job
│   ├── aggregate_1d.py         # Daily aggregation + quality check
│   ├── Dockerfile
│   └── requirements.txt
└── raspy/                      # Raspberry Pi scripts
    └── isg_to_gcs.py           # ISG scraper → GCS Bucket
```

---

## BigQuery Tables

### `SolarManager.SolarManager_5m`
5-minute interval data – the main source of truth.

| Field group | Fields | Source |
|---|---|---|
| Timestamps | `t`, `inserted` | Solarmanager |
| Solar | `pW`, `pWh`, `cW`, `cWh`, `iWh`, `eWh`, `scWh`, `cPvWh` | Solarmanager |
| Battery | `bcW`, `bcWh`, `bdW`, `bdWh`, `soc` | Solarmanager |
| Heat pump | `tHpWarmwaterC`, `tHpFlowC`, `tHpBackC`, `tHpOutC`, `sHp`, `sHpCurrent` | ISG via Raspy |
| Heat pump energy | `pHpHeatingDayWh`, `pHpHeatingTotalWh`, `pHpWarmwaterDayWh`, `pHpWarmwaterTotalWh` | ISG via Raspy |
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
- 2018-03-30 → 2025-10-19: Solarlog (`v=1` or `v=1100`)
- 2025-10-20 → today: Solarmanager (`v=1101` or `v=1111`)

---

### `SolarManager.SolarManager_1d`
Daily aggregation of `SolarManager_5m`. One row per day.

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
Raw Solarlog export from Synology MySQL database (2018–2025). Original field names preserved. Used as source for the Solarlog backfill into `SolarManager_5m`.

---

## GCP Resources

| Resource | Name | Purpose |
|---|---|---|
| Cloud Run Job | `solar-bq-job` | 5-min ingestion |
| Cloud Run Job | `solar-daily-job` | Daily aggregation |
| Cloud Scheduler | `solar-bq-scheduler` | Triggers `solar-bq-job` every 5 min |
| Cloud Scheduler | `solar-daily-scheduler` | Triggers `solar-daily-job` daily at 00:02 |
| GCS Bucket | `solar-isg-data` | ISG data exchange between Raspy and Cloud Run |
| Secret Manager | `solarmanager-token` | Solarmanager API token |
| Secret Manager | `openweathermap-apikey` | OpenWeatherMap API key |
| Artifact Registry | `solar` | Docker images |

---

## Schedules

| Job | Cron | Description |
|---|---|---|
| `solar-bq-job` | `1,6,11,16,21,26,31,36,41,46,51,56 * * * *` | Every 5 min, 1 min after interval |
| `solar-daily-job` | `2 0 * * *` | Daily at 00:02 UTC |
| Raspy ISG | `4,9,14,19,24,29,34,39,44,49,54,59 * * * *` | Every 5 min, 2 min before Cloud Run |

---

## Deployment

Every push to `master` triggers GitHub Actions which builds and deploys both Cloud Run Jobs automatically.

### Manual execution
```bash
# 5-min ingestion
gcloud run jobs execute solar-bq-job --region=europe-west6 --project=modern-cubist-412113

# Daily aggregation
gcloud run jobs execute solar-daily-job --region=europe-west6 --project=modern-cubist-412113
```

### View logs
```bash
# 5-min job
gcloud logging read "resource.type=cloud_run_job AND resource.labels.job_name=solar-bq-job" \
  --limit=50 --project=modern-cubist-412113

# Daily job
gcloud logging read "resource.type=cloud_run_job AND resource.labels.job_name=solar-daily-job" \
  --limit=50 --project=modern-cubist-412113
```

### Backfill tools (local)
| Script | Purpose |
|---|---|
| `backfill.py` | Full range backfill Solarmanager + OWM |
| `backfill_single_day.py` | Single day backfill with duplicate check |
| `patch_sunrise_sunset.py` | Patch missing sunrise/sunset values |
| `import_owm_and_patch.py` | Bulk import OWM file + mass UPDATE |
| `import_solarlog.py` | Import MySQL Solarlog dump into BigQuery |
| `backfill_1d.sql` | Backfill SolarManager_1d from 2018 |
