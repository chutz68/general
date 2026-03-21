#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Weather Patch Script: Update missing weather data in BigQuery from OWM file
Finds all rows with missing weather data and updates them from the OWM export file.

Usage:
    export GOOGLE_APPLICATION_CREDENTIALS="C:/dev/backfill/solarmanager/gcp-key.json"
    python patch_weather.py owm_file.json
"""

import json
import sys
import time
from datetime import datetime, timedelta, timezone
from google.cloud import bigquery
import logging

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
log = logging.getLogger(__name__)

# ---------------------------------------------------------
# Config
# ---------------------------------------------------------
PROJECT_ID = "modern-cubist-412113"
DATASET_ID = "SolarManager"
TABLE_ID   = "SolarManager_5m"
BATCH_SIZE = 50  # hours to process per BigQuery UPDATE


# ---------------------------------------------------------
# 1) Load OWM data
# ---------------------------------------------------------
def load_owm_data(filepath: str) -> dict:
    """Load OWM hourly data into a dict keyed by UTC hour timestamp."""
    with open(filepath, "r", encoding="utf-8") as f:
        records = json.load(f)

    owm_by_hour = {}
    for r in records:
        dt = datetime.strptime(r["dt_iso"], "%Y-%m-%d %H:%M:%S +0000 UTC").replace(tzinfo=timezone.utc)
        owm_by_hour[dt] = r

    log.info(f"Loaded {len(owm_by_hour)} OWM hourly records.")
    return owm_by_hour


def get_owm_for_timestamp(ts: datetime, owm_by_hour: dict) -> dict:
    """Find the closest OWM hourly record for a given timestamp."""
    hour = ts.replace(minute=0, second=0, microsecond=0)
    if hour in owm_by_hour:
        return owm_by_hour[hour]
    next_hour = hour + timedelta(hours=1)
    if next_hour in owm_by_hour:
        return owm_by_hour[next_hour]
    return {}


# ---------------------------------------------------------
# 2) Find hours with missing weather data
# ---------------------------------------------------------
def find_missing_hours(client: bigquery.Client) -> list:
    """Find all distinct hours with NULL weather data."""
    table_ref = f"{PROJECT_ID}.{DATASET_ID}.{TABLE_ID}"
    query = f"""
        SELECT DISTINCT TIMESTAMP_TRUNC(t, HOUR) as hour
        FROM `{table_ref}`
        WHERE weatherMain IS NULL
        ORDER BY hour
    """
    results = list(client.query(query).result())
    hours = [row.hour.replace(tzinfo=timezone.utc) for row in results]
    log.info(f"Found {len(hours)} hours with missing weather data.")
    return hours


# ---------------------------------------------------------
# 3) Update weather for a specific hour
# ---------------------------------------------------------
def patch_hour(client: bigquery.Client, hour: datetime, owm_data: dict) -> int:
    """Update weather fields for all rows within a specific hour."""
    if not owm_data:
        log.warning(f"No OWM data for {hour} – skipping.")
        return 0

    table_ref = f"{PROJECT_ID}.{DATASET_ID}.{TABLE_ID}"

    main    = owm_data.get("main", {})
    weather = owm_data.get("weather", [{}])[0]
    wind    = owm_data.get("wind", {})
    rain    = owm_data.get("rain", {}).get("1h", 0)
    snow    = owm_data.get("snow", {}).get("1h", 0)

    query = f"""
        UPDATE `{table_ref}`
        SET
            weatherMain     = '{weather.get("main", "")}',
            weatherDetailed = '{weather.get("description", "")}',
            weatherIconId   = '{weather.get("icon", "")}',
            tempReal        = {main.get("temp") or "NULL"},
            tempFeel        = {main.get("feels_like") or "NULL"},
            airPressure     = {main.get("pressure") or "NULL"},
            airHumidity     = {main.get("humidity") or "NULL"},
            visibility      = {owm_data.get("visibility") or "NULL"},
            windSpeed       = {wind.get("speed") or "NULL"},
            windDegree      = {wind.get("deg") or "NULL"},
            cloudiness      = {owm_data.get("clouds", {}).get("all") or "NULL"},
            rainAmount      = {rain},
            snowAmount      = {snow}
        WHERE TIMESTAMP_TRUNC(t, HOUR) = TIMESTAMP('{hour.strftime("%Y-%m-%d %H:%M:%S")} UTC')
          AND weatherMain IS NULL
    """

    job = client.query(query)
    job.result()
    rows = job.num_dml_affected_rows
    return rows


# ---------------------------------------------------------
# MAIN
# ---------------------------------------------------------
def main():
    if len(sys.argv) < 2:
        print("Usage: python patch_weather.py owm_file.json")
        sys.exit(1)

    owm_file    = sys.argv[1]
    owm_by_hour = load_owm_data(owm_file)
    client      = bigquery.Client(project=PROJECT_ID)

    log.info("=== Weather Patch started ===")

    missing_hours = find_missing_hours(client)
    if not missing_hours:
        log.info("No missing weather data found – all good! ✅")
        return

    total_updated = 0
    total_skipped = 0

    for i, hour in enumerate(missing_hours):
        owm_record = get_owm_for_timestamp(hour, owm_by_hour)
        if not owm_record:
            log.warning(f"No OWM data for {hour} – skipping.")
            total_skipped += 1
            continue

        updated = patch_hour(client, hour, owm_record)
        total_updated += updated

        if (i + 1) % 10 == 0:
            log.info(f"Progress: {i+1}/{len(missing_hours)} hours processed, {total_updated} rows updated")

    log.info(f"=== Patch finished: {total_updated} rows updated, {total_skipped} hours skipped ===")


if __name__ == "__main__":
    main()
