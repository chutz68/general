#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Single Day Backfill Script: Solarmanager + OWM → BigQuery
Fetches 5-min solar data for a single day and inserts missing rows only.
Always checks for duplicates regardless of date.

Usage:
    export GOOGLE_APPLICATION_CREDENTIALS="C:/dev/backfill/solarmanager/gcp-key.json"
    python backfill_single_day.py 2025-11-15
"""

import requests
import json
import time
import sys
from datetime import datetime, timedelta, timezone
from google.cloud import bigquery
import logging

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
log = logging.getLogger(__name__)

SM_URL     = "https://cloud.solar-manager.ch/v3/users/100000000B6E7363"
SM_TOKEN   = "Basic d2VybmVyLnJvZGVsQGdtYWlsLmNvbTpTb2xXZXJuSHkyNU9jdCQyNQ=="
OWM_FILE   = "openWeatherExportNhf-20251020-20260317.json"
PROJECT_ID = "modern-cubist-412113"
DATASET_ID = "SolarManager"
TABLE_ID   = "SolarManager_5m"
LAT        = 47.449404
LNG        = 8.327495


def get_sunrise_sunset(date: datetime) -> tuple:
    day_str = date.strftime("%Y-%m-%d")
    url = f"https://api.sunrise-sunset.org/json?lat={LAT}&lng={LNG}&date={day_str}&formatted=0"
    for attempt in range(1, 4):
        try:
            response = requests.get(url, timeout=10)
            response.raise_for_status()
            results = response.json().get("results", {})
            sunrise = results.get("sunrise")
            sunset  = results.get("sunset")
            log.info(f"Sunrise/Sunset for {day_str}: {sunrise} / {sunset}")
            return (sunrise, sunset)
        except Exception as e:
            log.warning(f"Attempt {attempt}/3 failed: {e}")
            time.sleep(5)
    return (None, None)


def load_owm_data(filepath: str) -> dict:
    with open(filepath, "r", encoding="utf-8") as f:
        records = json.load(f)
    owm_by_hour = {}
    for r in records:
        dt = datetime.strptime(r["dt_iso"], "%Y-%m-%d %H:%M:%S +0000 UTC").replace(tzinfo=timezone.utc)
        owm_by_hour[dt] = r
    log.info(f"Loaded {len(owm_by_hour)} OWM hourly records.")
    return owm_by_hour


def get_owm_for_timestamp(ts: datetime, owm_by_hour: dict) -> dict:
    hour = ts.replace(minute=0, second=0, microsecond=0)
    if hour in owm_by_hour:
        return owm_by_hour[hour]
    next_hour = hour + timedelta(hours=1)
    if next_hour in owm_by_hour:
        return owm_by_hour[next_hour]
    return {}


def parse_owm(r: dict, sunrise: str, sunset: str) -> dict:
    if not r:
        return {}
    main    = r.get("main", {})
    weather = r.get("weather", [{}])[0]
    wind    = r.get("wind", {})
    return {
        "weatherMain":       weather.get("main"),
        "weatherDetailed":   weather.get("description"),
        "weatherIconId":     weather.get("icon"),
        "tempReal":          main.get("temp"),
        "tempFeel":          main.get("feels_like"),
        "airPressure":       main.get("pressure"),
        "airHumidity":       main.get("humidity"),
        "visibility":        r.get("visibility"),
        "windSpeed":         wind.get("speed"),
        "windDegree":        wind.get("deg"),
        "cloudiness":        r.get("clouds", {}).get("all"),
        "rainAmount":        r.get("rain", {}).get("1h", 0),
        "snowAmount":        r.get("snow", {}).get("1h", 0),
        "sunriseTimestamp":  sunrise,
        "sunsetTimestamp":   sunset,
    }


def fetch_sm_day(start: datetime, end: datetime) -> list:
    url = (
        f"{SM_URL}/data/range"
        f"?from={start.strftime('%Y-%m-%dT%H:%M:%S')}"
        f"&to={end.strftime('%Y-%m-%dT%H:%M:%S')}&interval=300"
    )
    headers = {"accept": "application/json", "authorization": SM_TOKEN}
    for attempt in range(1, 4):
        try:
            response = requests.get(url, headers=headers, timeout=30)
            response.raise_for_status()
            data = response.json().get("data", [])
            log.info(f"SM API returned {len(data)} records.")
            return data
        except Exception as e:
            log.warning(f"SM API attempt {attempt}/3 failed: {e}")
            time.sleep(5)
    log.error("SM API failed after all retries.")
    return []


def get_existing_timestamps(client: bigquery.Client, day: datetime) -> set:
    table_ref = f"{PROJECT_ID}.{DATASET_ID}.{TABLE_ID}"
    query = f"""
        SELECT t FROM `{table_ref}`
        WHERE DATE(t) = '{day.strftime('%Y-%m-%d')}'
    """
    results = list(client.query(query).result())
    existing = {row.t.replace(tzinfo=timezone.utc) for row in results}
    log.info(f"Found {len(existing)} existing rows for {day.strftime('%Y-%m-%d')}.")
    return existing


def main():
    if len(sys.argv) < 2:
        print("Usage: python backfill_single_day.py YYYY-MM-DD")
        sys.exit(1)

    try:
        target_date = datetime.strptime(sys.argv[1], "%Y-%m-%d").replace(tzinfo=timezone.utc)
    except ValueError:
        print("Invalid date format. Use YYYY-MM-DD (e.g. 2025-11-15)")
        sys.exit(1)

    day_start = target_date
    day_end   = target_date + timedelta(days=1)

    log.info(f"=== Single Day Backfill: {target_date.strftime('%Y-%m-%d')} ===")

    owm_by_hour      = load_owm_data(OWM_FILE)
    sunrise, sunset  = get_sunrise_sunset(target_date)
    sm_records       = fetch_sm_day(day_start, day_end)

    if not sm_records:
        log.error("No SM data – aborting.")
        sys.exit(1)

    client      = bigquery.Client(project=PROJECT_ID)
    existing_ts = get_existing_timestamps(client, target_date)
    table_ref   = f"{PROJECT_ID}.{DATASET_ID}.{TABLE_ID}"
    inserted    = 0
    skipped     = 0

    for r in sm_records:
        ts_str = r.get("t")
        if not ts_str:
            continue

        ts = datetime.fromisoformat(ts_str.replace("Z", "+00:00"))

        if ts in existing_ts:
            log.info(f"Duplicate {ts} – skipping.")
            skipped += 1
            continue

        owm_record = get_owm_for_timestamp(ts, owm_by_hour)
        owm_data   = parse_owm(owm_record, sunrise, sunset)

        row = {
            "t":      ts_str,
            "v":      "1101",
            "cW":     r.get("cW"),    "pW":    r.get("pW"),
            "bcW":    r.get("bcW"),   "bdW":   r.get("bdW"),
            "cWh":    r.get("cWh"),   "pWh":   r.get("pWh"),
            "bcWh":   r.get("bcWh"),  "bdWh":  r.get("bdWh"),
            "scWh":   r.get("scWh"),  "cPvWh": r.get("cPvWh"),
            "iWh":    r.get("iWh"),   "eWh":   r.get("eWh"),
            "soc":    r.get("soc"),
            **owm_data,
        }

        errors = client.insert_rows_json(table_ref, [row])
        if errors:
            log.error(f"Insert error for {ts}: {errors}")
        else:
            inserted += 1
            log.info(f"✔ Inserted {ts}")

    log.info(f"=== Finished: {inserted} inserted, {skipped} skipped ===")


if __name__ == "__main__":
    main()
