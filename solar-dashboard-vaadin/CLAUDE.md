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
