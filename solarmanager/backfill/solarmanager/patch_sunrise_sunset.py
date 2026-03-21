#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Patch Script: Fix missing sunriseTimestamp / sunsetTimestamp in BigQuery
Finds all days with NULL sunrise/sunset and patches them via sunrise-sunset.org API

Usage:
    export GOOGLE_APPLICATION_CREDENTIALS="C:/dev/backfill/solarmanager/gcp-key.json"
    python patch_sunrise_sunset.py
"""

import requests
import time
from datetime import datetime, timezone
from google.cloud import bigquery
import logging

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
log = logging.getLogger(__name__)

PROJECT_ID = "modern-cubist-412113"
DATASET_ID = "SolarManager"
TABLE_ID   = "SolarManager_5m"
LAT        = 47.449404
LNG        = 8.327495


# ---------------------------------------------------------
# 1) Find days with missing sunrise/sunset
# ---------------------------------------------------------
def find_missing_days(client: bigquery.Client) -> list:
    """Find all distinct days with NULL sunrise or sunset."""
    table_ref = f"{PROJECT_ID}.{DATASET_ID}.{TABLE_ID}"
    query = f"""
        SELECT DISTINCT DATE(t) as day
        FROM `{table_ref}`
        WHERE (sunriseTimestamp IS NULL OR sunsetTimestamp IS NULL)
        ORDER BY day
    """
    results = list(client.query(query).result())
    days = [row.day for row in results]
    log.info(f"Found {len(days)} days with missing sunrise/sunset.")
    return days


# ---------------------------------------------------------
# 2) Fetch sunrise/sunset for a date
# ---------------------------------------------------------
def get_sunrise_sunset(date) -> tuple:
    """Fetch sunrise and sunset from sunrise-sunset.org API."""
    day_str = date.strftime("%Y-%m-%d")
    url = f"https://api.sunrise-sunset.org/json?lat={LAT}&lng={LNG}&date={day_str}&formatted=0"
    for attempt in range(1, 4):
        try:
            response = requests.get(url, timeout=10)
            response.raise_for_status()
            results = response.json().get("results", {})
            sunrise = results.get("sunrise")
            sunset  = results.get("sunset")
            log.info(f"  {day_str}: sunrise={sunrise}, sunset={sunset}")
            return (sunrise, sunset)
        except Exception as e:
            log.warning(f"  Attempt {attempt}/3 failed for {day_str}: {e}")
            time.sleep(5)
    return (None, None)


# ---------------------------------------------------------
# 3) Patch BigQuery rows for a day
# ---------------------------------------------------------
def patch_day(client: bigquery.Client, day, sunrise: str, sunset: str) -> int:
    """Update all rows for a given day with sunrise/sunset values."""
    if not sunrise or not sunset:
        log.warning(f"  Skipping {day} – no sunrise/sunset data.")
        return 0

    table_ref = f"{PROJECT_ID}.{DATASET_ID}.{TABLE_ID}"
    query = f"""
        UPDATE `{table_ref}`
        SET 
            sunriseTimestamp = TIMESTAMP('{sunrise}'),
            sunsetTimestamp  = TIMESTAMP('{sunset}')
        WHERE DATE(t) = '{day}'
          AND (sunriseTimestamp IS NULL OR sunsetTimestamp IS NULL)
    """
    job = client.query(query)
    job.result()
    rows_affected = job.num_dml_affected_rows
    log.info(f"  Updated {rows_affected} rows for {day}")
    return rows_affected


# ---------------------------------------------------------
# MAIN
# ---------------------------------------------------------
def main():
    log.info("=== Sunrise/Sunset Patch started ===")

    client = bigquery.Client(project=PROJECT_ID)

    missing_days = find_missing_days(client)
    if not missing_days:
        log.info("No missing values found – all good! ✅")
        return

    total_updated = 0
    for day in missing_days:
        log.info(f"Processing {day}...")
        sunrise, sunset = get_sunrise_sunset(day)
        updated = patch_day(client, day, sunrise, sunset)
        total_updated += updated
        time.sleep(1)  # be nice to free API

    log.info(f"=== Patch finished: {total_updated} rows updated ===")


if __name__ == "__main__":
    main()
