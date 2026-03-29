# Solar Dashboard

Web application to visualize solar energy data from BigQuery.
Built with **Vaadin Hilla** (React frontend + Kotlin/Spring Boot backend).

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React + TypeScript (Vaadin Hilla) |
| Backend | Kotlin + Spring Boot |
| UI Components | Vaadin Design System (Lumo theme) |
| Data | Google BigQuery Java SDK |
| Auth | Spring Security (InMemoryUserDetails) |

## Project Structure

```
solar-dashboard/
├── pom.xml
├── src/main/kotlin/ch/softhenge/solar/
│   ├── SolarDashboardApplication.kt    # Spring Boot entry point
│   ├── service/
│   │   └── SolarService.kt             # BigQuery data access (@BrowserCallable)
│   └── security/
│       └── SecurityConfig.kt           # Spring Security config
├── src/main/resources/
│   └── application.properties
└── frontend/
    └── views/
        └── DashboardView.tsx            # Main React dashboard view
```

## Getting Started in IntelliJ

1. Open project: **File → Open → select `solar-dashboard` folder**
2. Maven will auto-import dependencies
3. Set up GCP credentials:
   ```bash
   gcloud auth application-default login
   ```
4. Run `SolarDashboardApplication.kt` → right-click → **Run**
5. Open browser: http://localhost:8080
6. Login: `werner` / `changeme` (change in `SecurityConfig.kt`!)

## Development

Frontend hot-reload is automatic in development mode.
Backend changes require restart.

## Production Build

```bash
mvn clean package -Pproduction
java -jar target/solar-dashboard-1.0.0-SNAPSHOT.jar
```

## Deploy to Cloud Run

```bash
# Build production jar
mvn clean package -Pproduction

# Build Docker image
docker build -t europe-west6-docker.pkg.dev/modern-cubist-412113/solar/solar-dashboard:latest .

# Push
docker push europe-west6-docker.pkg.dev/modern-cubist-412113/solar/solar-dashboard:latest
```

## BigQuery Tables Used

| Table | Purpose |
|---|---|
| `SolarManager.SolarManager_1d` | Daily aggregations (main dashboard) |
| `SolarManager.SolarManager_5m` | 5-min data (live view, charts) |
| `SolarManager.SolarManager_5m_v` | View with selfConsumptionPct + autarkyPct |
