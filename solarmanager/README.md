# Solar Manager → BigQuery (Cloud Run Job)

## Architektur

```
Cloud Scheduler (every 5min, offset +1min)
        │
        ▼
Cloud Run Job (solar-bq-job)
        │
        ├──► Solarmanager API  →  Solar data
        ├──► OpenWeatherMap API → Weather data
        │
        ▼
BigQuery (SolarManager.SolarManager_5m)
```

## Dateien

| Datei | Beschreibung |
|---|---|
| `main.py` | Hauptlogik (refactored) |
| `Dockerfile` | Container für Cloud Run Job |
| `requirements.txt` | Python Dependencies |
| `cloudbuild.yaml` | CI/CD Pipeline |
| `setup.sh` | Einmaliges GCP Setup |

## Deployment

### Einmalig (Setup)
```bash
chmod +x setup.sh
./setup.sh
```

### Updates (nach Code-Änderungen)
```bash
gcloud builds submit . --config=cloudbuild.yaml --project=modern-cubist-412113
```

### Manuell testen
```bash
gcloud run jobs execute solar-bq-job --region=europe-west6 --project=modern-cubist-412113
```

### Logs ansehen
```bash
gcloud logging read \
  'resource.type=cloud_run_job AND resource.labels.job_name=solar-bq-job' \
  --limit=50 --project=modern-cubist-412113
```

## Änderungen gegenüber Original

| Original | Cloud Run Version |
|---|---|
| `wait_until_condition()` | ❌ Entfernt – Cloud Scheduler übernimmt Timing |
| Hardcoded Credentials | ✅ Secret Manager |
| `functions_framework` import | ❌ Entfernt – nicht benötigt |
| `sys.exit(5)` | ✅ Einheitlich `sys.exit(1)` |

## Cron-Schedule

`1,6,11,16,21,26,31,36,41,46,51,56 * * * *`

→ Startet 1 Minute nach jedem 5-Minuten-Intervall (xx:01, xx:06, xx:11, ...)  
→ Entspricht exakt der ursprünglichen `wait_until_condition()` Logik
