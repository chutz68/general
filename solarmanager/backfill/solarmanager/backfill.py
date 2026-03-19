#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Backfill Script: Solarmanager + OpenWeatherMap → BigQuery
Fetches 5-min solar data from 2025-10-20 to 2026-03-14 21:45:00 UTC
Merges with hourly OWM weather data
Inserts into BigQuery (no duplicates)

Usage:
    pip install requests google-cloud-bigquery
    set GOOGLE_APPLICATION_CREDENTIALS=path/to/gcp-key.json   (Windows)
    python backfill.py
"""

import requests
import json
import time
from datetime import datetime, timedelta, timezone
from google.cloud import bigquery
import logging
import sys

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
log = logging.getLogger(__name__)

# ---------------------------------------------------------
# Config
# ---------------------------------------------------------
SM_URL        = "https://cloud.solar-manager.ch/v3/users/100000000B6E7363"
SM_TOKEN      = "Basic d2VybmVyLnJvZGVsQGdtYWlsLmNvbTpTb2xXZXJuSHkyNU9jdCQyNQ=="
OWM_FILE      = "openWeatherExportNhf-20251020-20260317.json"
PROJECT_ID    = "modern-cubist-412113"
DATASET_ID    = "SolarManager"
TABLE_ID      = "SolarManager_5m"

# Backfill range
BACKFILL_START = datetime(2025, 10, 20, 16, 0, 0, tzinfo=timezone.utc)
BACKFILL_END   = datetime(2026, 3, 14, 21, 45, 0, tzinfo=timezone.utc)

# Cutoff: rows before this timestamp need no duplicate check
DUPLICATE_CHECK_FROM = datetime(2026, 3, 14, 21, 50, 0, tzinfo=timezone.utc)

# API block size: 25 hours per call (300 x 5-min intervals)
BLOCK_HOURS = 25


LAT           = 47.449404
LNG           = 8.327495

# ---------------------------------------------------------
# 1) Sunrise/Sunset per day cache
# ---------------------------------------------------------
sunrise_sunset_cache = {}

def get_sunrise_sunset(date: datetime) -> tuple:
    """Fetch sunrise and sunset for a given date. Cached per day."""
    day_key = date.strftime("%Y-%m-%d")
    if day_key in sunrise_sunset_cache:
        return sunrise_sunset_cache[day_key]

    url = f"https://api.sunrise-sunset.org/json?lat={LAT}&lng={LNG}&date={day_key}&formatted=0"
    try:
        response = requests.get(url, timeout=10)
        response.raise_for_status()
        results = response.json().get("results", {})
        sunrise = results.get("sunrise")
        sunset  = results.get("sunset")
        sunrise_sunset_cache[day_key] = (sunrise, sunset)
        log.info(f"Sunrise/Sunset for {day_key}: {sunrise} / {sunset}")
        time.sleep(0.5)  # be nice to free API
        return (sunrise, sunset)
    except Exception as e:
        log.warning(f"Sunrise/Sunset API failed for {day_key}: {e}")
        sunrise_sunset_cache[day_key] = (None, None)
        return (None, None)


# ---------------------------------------------------------
# 2) Load OWM data into memory (keyed by hour timestamp)
# ---------------------------------------------------------
def load_owm_data(filepath: str) -> dict:
    """Load OWM hourly data into a dict keyed by UTC hour timestamp."""
    with open(filepath, "r", encoding="utf-8") as f:
        records = json.load(f)

    owm_by_hour = {}
    for r in records:
        # Parse dt_iso to datetime
        dt = datetime.strptime(r["dt_iso"], "%Y-%m-%d %H:%M:%S +0000 UTC").replace(tzinfo=timezone.utc)
        owm_by_hour[dt] = r

    log.info(f"Loaded {len(owm_by_hour)} OWM hourly records.")
    return owm_by_hour


def get_owm_for_timestamp(ts: datetime, owm_by_hour: dict) -> dict:
    """Find the closest OWM hourly record for a given 5-min timestamp."""
    # Round down to nearest hour
    hour = ts.replace(minute=0, second=0, microsecond=0)
    if hour in owm_by_hour:
        return owm_by_hour[hour]
    # Try next hour
    next_hour = hour + timedelta(hours=1)
    if next_hour in owm_by_hour:
        return owm_by_hour[next_hour]
    return {}


def parse_owm(r: dict) -> dict:
    """Extract relevant fields from OWM record."""
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
        "sunriseTimestamp":  None,
        "sunsetTimestamp":   None,
    }


# ---------------------------------------------------------
# 2) Fetch Solarmanager data in blocks
# ---------------------------------------------------------
def fetch_sm_block(start: datetime, end: datetime) -> list:
    """Fetch 5-min solar data for a time block."""
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
            return data
        except Exception as e:
            log.warning(f"SM API attempt {attempt}/3 failed: {e}")
            time.sleep(5)

    log.error(f"SM API failed for block {start} - {end}")
    return []


# ---------------------------------------------------------
# 3) BigQuery insert
# ---------------------------------------------------------
def insert_rows(client: bigquery.Client, rows: list, check_duplicates: bool) -> int:
    """Insert rows into BigQuery. Returns number of inserted rows."""
    table_ref = f"{PROJECT_ID}.{DATASET_ID}.{TABLE_ID}"
    inserted = 0

    for row in rows:
        ts = row["t"]

        if check_duplicates:
            query = f"SELECT 1 FROM `{table_ref}` WHERE t = TIMESTAMP('{ts}') LIMIT 1"
            results = list(client.query(query).result())
            if results:
                log.info(f"Duplicate {ts} – skipping.")
                continue

        errors = client.insert_rows_json(table_ref, [row])
        if errors:
            log.error(f"Insert error for {ts}: {errors}")
        else:
            inserted += 1

    return inserted


# ---------------------------------------------------------
# MAIN
# ---------------------------------------------------------
def main():
    log.info("=== Backfill started ===")
    log.info(f"Range: {BACKFILL_START} → {BACKFILL_END}")

    # Load OWM data
    owm_by_hour = load_owm_data(OWM_FILE)

    # BigQuery client
    bq_client = bigquery.Client(project=PROJECT_ID)

    total_inserted = 0
    total_skipped  = 0
    block_start    = BACKFILL_START

    while block_start < BACKFILL_END:
        block_end = min(block_start + timedelta(hours=BLOCK_HOURS), BACKFILL_END)
        log.info(f"Processing block: {block_start} → {block_end}")

        sm_records = fetch_sm_block(block_start, block_end)
        log.info(f"  SM records: {len(sm_records)}")

        rows = []
        for r in sm_records:
            ts_str = r.get("t")
            if not ts_str:
                continue

            ts = datetime.fromisoformat(ts_str.replace("Z", "+00:00"))

            # Get OWM data for this timestamp
            owm_record = get_owm_for_timestamp(ts, owm_by_hour)
            owm_data   = parse_owm(owm_record)

            # Get sunrise/sunset for this day
            sunrise, sunset = get_sunrise_sunset(ts)
            owm_data["sunriseTimestamp"] = sunrise
            owm_data["sunsetTimestamp"]  = sunset

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
            rows.append(row)

        # Insert rows
        check_dupes = block_start >= DUPLICATE_CHECK_FROM
        inserted = insert_rows(bq_client, rows, check_duplicates=check_dupes)
        skipped  = len(rows) - inserted
        total_inserted += inserted
        total_skipped  += skipped

        log.info(f"  Inserted: {inserted}, Skipped: {skipped}")

        block_start = block_end
        time.sleep(1)  # be nice to the API

    log.info(f"=== Backfill finished: {total_inserted} inserted, {total_skipped} skipped ===")


if __name__ == "__main__":
    main()
