#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Solar Manager Standby Ingest – Synology DS218+
Runs every 5 minutes at xx:02 (1 min after Raspy at xx:01)
Checks if Raspy inserted successfully – if not, takes over.

Cron: 2,7,12,17,22,27,32,37,42,47,52,57 * * * *

Environment variables (.env):
    GOOGLE_APPLICATION_CREDENTIALS  Path to GCP service account key
    SOLARMANAGER_TOKEN              Basic Auth token
    OWM_APIKEY                      OpenWeatherMap API key
"""

import socket
# Force IPv4
_orig_getaddrinfo = socket.getaddrinfo
def _ipv4_getaddrinfo(host, port, family=0, *args, **kwargs):
    return _orig_getaddrinfo(host, port, socket.AF_INET, *args, **kwargs)
socket.getaddrinfo = _ipv4_getaddrinfo

import requests
import json
import time
import os
import sys
from datetime import datetime, timedelta, timezone
from google.cloud import bigquery, storage
import logging

# ---------------------------------------------------------
# Load .env file
# ---------------------------------------------------------
_env_file = os.path.join(os.path.dirname(os.path.abspath(__file__)), '.env')
if os.path.exists(_env_file):
    with open(_env_file) as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith('#') and '=' in line:
                key, _, value = line.partition('=')
                value = value.strip().strip('"').strip("'")
                os.environ.setdefault(key.strip().replace('export ', ''), value)

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
log = logging.getLogger(__name__)

# ---------------------------------------------------------
# Config
# ---------------------------------------------------------
PROJECT_ID    = os.environ.get("GCP_PROJECT_ID", "modern-cubist-412113")
DATASET_ID    = os.environ.get("BQ_DATASET_ID", "SolarManager")
TABLE_ID      = os.environ.get("BQ_TABLE_ID", "SolarManager_5m")
SM_URL        = os.environ.get("SOLARMANAGER_URL", "https://cloud.solar-manager.ch/v3/users/100000000B6E7363")
SM_STREAM_URL = f"{SM_URL}/data/stream"
WEATHER_URL   = os.environ.get("WEATHER_URL", "https://api.openweathermap.org/data/3.0/onecall?lat=47.45&lon=8.32&units=metric&exclude=minutely,hourly,daily,alerts")
GCS_BUCKET    = os.environ.get("GCS_BUCKET", "solar-isg-data")
STATUS_BLOB   = "raspy_status.json"
ISG_URL       = os.environ.get("ISG_URL", "http://192.168.1.180/?s=1,1")

# Max age of Raspy status before Synology takes over (6 minutes)
MAX_STATUS_AGE_MINUTES = 6

SM_TOKEN   = os.environ.get("SOLARMANAGER_TOKEN")
OWM_APIKEY = os.environ.get("OWM_APIKEY")


# ---------------------------------------------------------
# 1) Check Raspy status from GCS
# ---------------------------------------------------------
def raspy_is_ok() -> bool:
    """Returns True if Raspy inserted successfully within the last 6 minutes."""
    try:
        client  = storage.Client(project=PROJECT_ID)
        bucket  = client.bucket(GCS_BUCKET)
        blob    = bucket.blob(STATUS_BLOB)
        payload = json.loads(blob.download_as_text())

        ts  = datetime.fromisoformat(payload["timestamp"])
        age = (datetime.now(timezone.utc) - ts).total_seconds() / 60

        if age <= MAX_STATUS_AGE_MINUTES:
            log.info(f"Raspy OK – last insert {age:.1f} min ago. Standby not needed.")
            return True
        else:
            log.warning(f"Raspy status is {age:.1f} min old – taking over!")
            return False
    except Exception as e:
        log.warning(f"Could not read Raspy status: {e} – taking over!")
        return False


# ---------------------------------------------------------
# 2) Solarmanager API
# ---------------------------------------------------------
def fetch_solarmanager_data() -> dict | None:
    now        = datetime.now(timezone.utc).replace(second=0, microsecond=0)
    start_time = now - timedelta(minutes=5)
    end_time   = now

    solar_url = (
        f"{SM_URL}/data/range"
        f"?from={start_time.strftime('%Y-%m-%dT%H:%M:%S')}"
        f"&to={end_time.strftime('%Y-%m-%dT%H:%M:%S')}&interval=300"
    )
    headers = {"accept": "application/json", "authorization": SM_TOKEN}

    for attempt in range(1, 7):
        log.info(f"Solarmanager fetch attempt {attempt}/6")
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
            "cW":    r.get("cW"),   "pW":    r.get("pW"),
            "bcW":   r.get("bcW"),  "bdW":   r.get("bdW"),
            "cWh":   r.get("cWh"),  "pWh":   r.get("pWh"),
            "bcWh":  r.get("bcWh"), "bdWh":  r.get("bdWh"),
            "scWh":  r.get("scWh"), "cPvWh": r.get("cPvWh"),
            "iWh":   r.get("iWh"),  "eWh":   r.get("eWh"),
            "soc":   r.get("soc"),
        }

    log.error("Solarmanager: all retries exhausted.")
    return None


# ---------------------------------------------------------
# 3) Solarmanager Stream API
# ---------------------------------------------------------
def fetch_solarmanager_stream() -> dict:
    headers = {"accept": "application/json", "authorization": SM_TOKEN}
    try:
        response = requests.get(SM_STREAM_URL, headers=headers, timeout=15)
        response.raise_for_status()
        r = response.json()
        hp_temp  = None
        hp_state = None
        for device in r.get("devices", []):
            if "temperature" in device:
                hp_temp  = device.get("temperature")
                hp_state = device.get("operationState")
                break
        return {"tHpWarmwaterC": hp_temp, "sHp": hp_state}
    except (requests.RequestException, ValueError) as e:
        log.warning(f"Stream API error: {e}")
        return {"tHpWarmwaterC": None, "sHp": None}


# ---------------------------------------------------------
# 4) OpenWeatherMap API
# ---------------------------------------------------------
def fetch_weather_data() -> dict:
    url = f"{WEATHER_URL}&appid={OWM_APIKEY}"
    response = requests.get(url, timeout=15)
    response.raise_for_status()
    owm     = response.json()
    current = owm.get("current", {})
    weather = current.get("weather", [{}])[0]
    sunrise = current.get("sunrise")
    sunset  = current.get("sunset")
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
# 5) ISG – direct HTML scraping
# ---------------------------------------------------------
def parse_value(raw: str) -> float | None:
    try:
        number = raw.strip().split(" ")[0].replace(",", ".")
        value  = float(number)
        if "MWh" in raw:
            value *= 1_000_000
        elif "KWh" in raw:
            value *= 1_000
        return value
    except (ValueError, IndexError):
        return None


def fetch_isg_data() -> dict:
    try:
        from bs4 import BeautifulSoup
        response = requests.get(ISG_URL, timeout=15)
        response.raise_for_status()
        soup = BeautifulSoup(response.text, "html.parser")
        data = {}
        for row in soup.select("table.info tr"):
            cells = row.select("td")
            if len(cells) == 2:
                key   = cells[0].get_text(strip=True).upper()
                value = cells[1].get_text(strip=True)
                data[key] = value
        log.info("ISG data fetched successfully")
        return {
            "tHpBackC":            parse_value(data.get("RÜCKLAUFTEMPERATUR", "")),
            "tHpFlowC":            parse_value(data.get("VORLAUFTEMPERATUR", "")),
            "tHpOutC":             parse_value(data.get("AUSSENTEMPERATUR", "")),
            "sHpCurrent":          parse_value(data.get("STROM INVERTER", "")),
            "pHpHeatingDayWh":     parse_value(data.get("VD HEIZEN TAG", "")),
            "pHpHeatingTotalWh":   parse_value(data.get("VD HEIZEN SUMME", "")),
            "pHpWarmwaterDayWh":   parse_value(data.get("VD WARMWASSER TAG", "")),
            "pHpWarmwaterTotalWh": parse_value(data.get("VD WARMWASSER SUMME", "")),
        }
    except Exception as e:
        log.warning(f"ISG error: {e} – using empty values.")
        return {}


# ---------------------------------------------------------
# 6) BigQuery – deduplicated insert
# ---------------------------------------------------------
def write_to_bigquery(rows: list) -> None:
    client    = bigquery.Client(project=PROJECT_ID)
    table_ref = f"{PROJECT_ID}.{DATASET_ID}.{TABLE_ID}"
    ts        = rows[0]["t"]

    query   = f"SELECT 1 FROM `{table_ref}` WHERE t = TIMESTAMP('{ts}') LIMIT 1"
    results = list(client.query(query).result())

    if results:
        log.info(f"Duplicate timestamp {ts} – skipping insert.")
        return

    errors = client.insert_rows_json(table_ref, rows)
    if errors:
        raise RuntimeError(f"BigQuery insert errors: {errors}")
    log.info(f"✔ [SYNOLOGY STANDBY] Row inserted into {table_ref} (t={ts})")


# ---------------------------------------------------------
# MAIN
# ---------------------------------------------------------
def main():
    log.info("=== Synology Standby Check started ===")

    if not SM_TOKEN:
        log.error("SOLARMANAGER_TOKEN not set!")
        sys.exit(1)
    if not OWM_APIKEY:
        log.error("OWM_APIKEY not set!")
        sys.exit(1)

    # Check if Raspy is OK – if yes, do nothing
    if raspy_is_ok():
        sys.exit(0)

    log.info("=== Raspy down – Synology taking over! ===")

    sm_data = fetch_solarmanager_data()
    if sm_data is None:
        log.error("No Solarmanager data – aborting.")
        sys.exit(1)

    stream_data  = fetch_solarmanager_stream()
    weather_data = fetch_weather_data()
    isg_data     = fetch_isg_data()

    has_isg = bool(isg_data)
    version = 1111 if has_isg else 1101

    row = {**sm_data, **stream_data, **weather_data, **isg_data, "v": version}
    log.info(f"Row to insert: {row}")
    write_to_bigquery([row])

    log.info("=== Synology Standby Job finished ===")


if __name__ == "__main__":
    main()
