#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Solar Manager → BigQuery
Cloud Run Job (triggered by Cloud Scheduler every 5 minutes)
"""

import requests
from google.cloud import bigquery, secretmanager
import os
import sys
import time
from datetime import datetime, timedelta, timezone
import logging

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
log = logging.getLogger(__name__)

# ---------------------------------------------------------
# Config (non-sensitive – via Env Variables)
# ---------------------------------------------------------
PROJECT_ID   = os.environ.get("GCP_PROJECT_ID", "modern-cubist-412113")
DATASET_ID   = os.environ.get("BQ_DATASET_ID", "SolarManager")
TABLE_ID     = os.environ.get("BQ_TABLE_ID", "SolarManager_5m")
SM_URL       = os.environ.get("SOLARMANAGER_URL", "https://cloud.solar-manager.ch/v3/users/100000000B6E7363")
SM_STREAM_URL = f"{SM_URL}/data/stream"
WEATHER_URL  = os.environ.get("WEATHER_URL", "https://api.openweathermap.org/data/3.0/onecall?lat=47.45&lon=8.32&units=metric&exclude=minutely,hourly,daily,alerts")

# Secret names in GCP Secret Manager
SECRET_SM_TOKEN     = os.environ.get("SECRET_SM_TOKEN", "solarmanager-token")
SECRET_OWM_APIKEY   = os.environ.get("SECRET_OWM_APIKEY", "openweathermap-apikey")


# ---------------------------------------------------------
# 1) Secrets from GCP Secret Manager
# ---------------------------------------------------------
def get_secret(secret_id: str) -> str:
    """Fetch latest version of a secret from GCP Secret Manager."""
    client = secretmanager.SecretManagerServiceClient()
    name = f"projects/{PROJECT_ID}/secrets/{secret_id}/versions/latest"
    response = client.access_secret_version(request={"name": name})
    return response.payload.data.decode("UTF-8").strip()


# ---------------------------------------------------------
# 2) Solarmanager API
# ---------------------------------------------------------
def fetch_solarmanager_data(token: str) -> dict | None:
    """
    Fetch the latest 5-min data point from Solarmanager API.
    Retries up to 6x (every 30s) in case data isn't ready yet.
    """
    now = datetime.now(timezone.utc).replace(second=0, microsecond=0)
    start_time = now - timedelta(minutes=5)
    end_time   = now

    solar_url = (
        f"{SM_URL}/data/range"
        f"?from={start_time.strftime('%Y-%m-%dT%H:%M:%S')}"
        f"&to={end_time.strftime('%Y-%m-%dT%H:%M:%S')}&interval=300"
    )
    headers = {"accept": "application/json", "authorization": token}

    for attempt in range(1, 7):
        log.info(f"Solarmanager fetch attempt {attempt}/6 → {solar_url}")
        try:
            response = requests.get(solar_url, headers=headers, timeout=15)
            response.raise_for_status()
            json_data = response.json()
        except (requests.RequestException, ValueError) as e:
            log.warning(f"Request/JSON error: {e}. Retrying in 30s...")
            time.sleep(30)
            continue

        data_list = json_data.get("data", [])
        if not data_list:
            log.warning("Empty data from Solarmanager. Retrying in 30s...")
            time.sleep(30)
            continue

        r = data_list[0]
        return {
            "t":     r.get("t"),
            "v":     "1101",
            "cW":    r.get("cW"),   "pW":   r.get("pW"),
            "bcW":   r.get("bcW"),  "bdW":  r.get("bdW"),
            "cWh":   r.get("cWh"),  "pWh":  r.get("pWh"),
            "bcWh":  r.get("bcWh"), "bdWh": r.get("bdWh"),
            "scWh":  r.get("scWh"), "cPvWh":r.get("cPvWh"),
            "iWh":   r.get("iWh"),  "eWh":  r.get("eWh"),
            "soc":   r.get("soc"),
        }

    log.error("Solarmanager: all retries exhausted.")
    return None


# ---------------------------------------------------------
# 3) Solarmanager Stream API (Echtzeit-Momentanwerte)
# ---------------------------------------------------------
def fetch_solarmanager_stream(token: str) -> dict:
    """
    Fetch real-time stream data from Solarmanager API.
    Extracts temperature from the heatpump device (operationState field).
    """
    headers = {"accept": "application/json", "authorization": token}

    try:
        response = requests.get(SM_STREAM_URL, headers=headers, timeout=15)
        response.raise_for_status()
        r = response.json()
        log.info(f"Stream data: {r}")

        # Find heatpump device (has "temperature" field)
        hp_temp = None
        for device in r.get("devices", []):
            if "temperature" in device:
                hp_temp = device["temperature"]
                break

        return {"tHpWarmwaterC": hp_temp}

    except (requests.RequestException, ValueError) as e:
        log.warning(f"Stream API error: {e} – returning empty stream data.")
        return {"tHpWarmwaterC": None}


# ---------------------------------------------------------
# 3) OpenWeatherMap API
# ---------------------------------------------------------
def fetch_weather_data(apikey: str) -> dict:
    """Fetch current weather data from OpenWeatherMap One Call API."""
    url = f"{WEATHER_URL}&appid={apikey}"
    response = requests.get(url, timeout=15)
    response.raise_for_status()
    owm = response.json()

    current  = owm.get("current", {})
    weather  = current.get("weather", [{}])[0]
    sunrise  = current.get("sunrise")
    sunset   = current.get("sunset")

    return {
        "weatherMain":       weather.get("main"),
        "weatherDetailed":   weather.get("description"),
        "weatherIconId":     weather.get("icon"),
        "tempReal":          current.get("temp"),
        "tempFeel":          current.get("feels_like"),
        "airPressure":       current.get("pressure"),
        "airHumidity":       current.get("humidity"),
        "visibility":        current.get("visibility"),
        "windSpeed":         current.get("wind_speed"),
        "windDegree":        current.get("wind_deg"),
        "cloudiness":        current.get("clouds"),
        "rainAmount":        current.get("rain", {}).get("1h", 0),
        "snowAmount":        current.get("snow", {}).get("1h", 0),
        "sunriseTimestamp":  datetime.fromtimestamp(sunrise, timezone.utc).isoformat() if sunrise else None,
        "sunsetTimestamp":   datetime.fromtimestamp(sunset,  timezone.utc).isoformat() if sunset  else None,
    }


# ---------------------------------------------------------
# 4) BigQuery – deduplicated insert
# ---------------------------------------------------------
def write_to_bigquery(rows: list) -> None:
    """Insert rows into BigQuery, skipping duplicates by timestamp."""
    client   = bigquery.Client(project=PROJECT_ID)
    table_ref = f"{PROJECT_ID}.{DATASET_ID}.{TABLE_ID}"
    ts = rows[0]["t"]

    query = f"SELECT 1 FROM `{table_ref}` WHERE t = TIMESTAMP('{ts}') LIMIT 1"
    results = list(client.query(query).result())

    if results:
        log.info(f"Duplicate timestamp {ts} – skipping insert.")
        return

    errors = client.insert_rows_json(table_ref, rows)
    if errors:
        raise RuntimeError(f"BigQuery insert errors: {errors}")
    log.info(f"✔ Row inserted into {table_ref} (t={ts})")


# ---------------------------------------------------------
# MAIN
# ---------------------------------------------------------
def main():
    log.info("=== Solar Cloud Run Job started ===")

    # Load secrets
    sm_token  = get_secret(SECRET_SM_TOKEN)
    owm_key   = get_secret(SECRET_OWM_APIKEY)

    # Fetch data
    sm_data = fetch_solarmanager_data(sm_token)
    if sm_data is None:
        log.error("No Solarmanager data – aborting.")
        sys.exit(1)

    stream_data  = fetch_solarmanager_stream(sm_token)
    weather_data = fetch_weather_data(owm_key)

    # Merge & write
    row = {**sm_data, **stream_data, **weather_data}
    log.info(f"Row to insert: {row}")
    write_to_bigquery([row])

    log.info("=== Job finished successfully ===")


if __name__ == "__main__":
    main()

