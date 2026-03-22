#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Solar Manager Daily Aggregation Job
Aggregates yesterday's 5-min data into SolarManager_1d table.
Runs daily at 00:02 via Cloud Scheduler.

Also performs data quality checks:
- Missing rows (< 288)
- Duplicate timestamps
"""

from google.cloud import bigquery
import os
import sys
import logging
from datetime import datetime, timedelta, timezone

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
log = logging.getLogger(__name__)

# ---------------------------------------------------------
# Config
# ---------------------------------------------------------
PROJECT_ID  = os.environ.get("GCP_PROJECT_ID", "modern-cubist-412113")
DATASET_ID  = os.environ.get("BQ_DATASET_ID", "SolarManager")
SOURCE_TABLE = f"{PROJECT_ID}.{DATASET_ID}.SolarManager_5m"
TARGET_TABLE = f"{PROJECT_ID}.{DATASET_ID}.SolarManager_1d"


# ---------------------------------------------------------
# 1) Check if day already exists
# ---------------------------------------------------------
def day_exists(client: bigquery.Client, day: str) -> bool:
    query = f"SELECT 1 FROM `{TARGET_TABLE}` WHERE day = '{day}' LIMIT 1"
    results = list(client.query(query).result())
    return len(results) > 0


# ---------------------------------------------------------
# 2) Aggregate and insert
# ---------------------------------------------------------
def aggregate_day(client: bigquery.Client, day: str) -> None:
    """Aggregate 5-min data for a given day and insert into SolarManager_1d."""

    query = f"""
        INSERT INTO `{TARGET_TABLE}`
        SELECT
            DATE(t)                        AS day,
            CURRENT_TIMESTAMP()            AS inserted,

            -- Energy sums
            IFNULL(SUM(cWh), 0)            AS cWh,
            IFNULL(SUM(pWh), 0)            AS pWh,
            IFNULL(SUM(bcWh), 0)           AS bcWh,
            IFNULL(SUM(bdWh), 0)           AS bdWh,
            IFNULL(SUM(scWh), 0)           AS scWh,
            IFNULL(SUM(cPvWh), 0)          AS cPvWh,
            IFNULL(SUM(iWh), 0)            AS iWh,
            IFNULL(SUM(eWh), 0)            AS eWh,

            -- Heatpump
            IFNULL(MAX(pHpHeatingDayWh), 0)     AS pHpHeatingDayWh,
            IFNULL(MAX(pHpWarmwaterDayWh), 0)   AS pHpWarmwaterDayWh,
            IFNULL(MAX(pHpHeatingTotalWh), 0)   AS pHpHeatingTotalWh,
            IFNULL(MAX(pHpWarmwaterTotalWh), 0) AS pHpWarmwaterTotalWh,

            -- Power min/max
            IFNULL(MAX(pW), 0)                            AS pWmax,
            IFNULL(MIN(CASE WHEN pW > 0 THEN pW END), 0)  AS pWmin,
            IFNULL(MAX(cW), 0)                            AS cWmax,
            IFNULL(MIN(CASE WHEN cW > 0 THEN cW END), 0)  AS cWmin,

            -- Battery
            IFNULL(MAX(soc), 0)                               AS socMax,
            IFNULL(MIN(CASE WHEN soc > 0 THEN soc END), 0)    AS socMin,

            -- Weather
            IFNULL(MIN(tempReal), 0)       AS tempRealMin,
            IFNULL(MAX(tempReal), 0)       AS tempRealMax,
            IFNULL(SUM(rainAmount), 0)     AS rainAmountSum,
            IFNULL(SUM(snowAmount), 0)     AS snowAmountSum,
            MIN(sunriseTimestamp)          AS sunriseTimestamp,
            MAX(sunsetTimestamp)           AS sunsetTimestamp,

            -- Percentages
            CASE WHEN SUM(pWh) > 0 THEN ROUND((SUM(pWh) - SUM(eWh)) / SUM(pWh) * 100, 1) ELSE NULL END AS selfConsumptionPct,
            CASE WHEN SUM(cWh) > 0 THEN ROUND((SUM(cWh) - SUM(iWh)) / SUM(cWh) * 100, 1) ELSE NULL END AS autarkyPct,

            -- Data quality
            COUNT(*)                       AS rowCount,
            288 - COUNT(*)                 AS missingRows,
            COUNT(*) - COUNT(DISTINCT t)   AS duplicates,
            MIN(v)                         AS vMin

        FROM `{SOURCE_TABLE}`
        WHERE DATE(t) = '{day}'
        GROUP BY DATE(t)
    """

    job = client.query(query)
    job.result()
    log.info(f"✔ Aggregated day {day} into {TARGET_TABLE}")


# ---------------------------------------------------------
# 3) Data quality check
# ---------------------------------------------------------
def quality_check(client: bigquery.Client, day: str) -> None:
    """Log data quality results for the aggregated day."""
    query = f"""
        SELECT rowCount, missingRows, duplicates, vMin
        FROM `{TARGET_TABLE}`
        WHERE day = '{day}'
    """
    results = list(client.query(query).result())
    if not results:
        log.error(f"No aggregation found for {day}!")
        return

    r = results[0]
    log.info(f"Quality check for {day}:")
    log.info(f"  Rows:        {r.rowCount}/288")
    log.info(f"  Missing:     {r.missingRows}")
    log.info(f"  Duplicates:  {r.duplicates}")
    log.info(f"  vMin:        {r.vMin}")

    if r.missingRows > 0:
        log.warning(f"  ⚠️  {r.missingRows} missing rows for {day}!")
    if r.duplicates > 0:
        log.warning(f"  ⚠️  {r.duplicates} duplicate timestamps for {day}!")
    if r.rowCount == 288 and r.duplicates == 0:
        log.info(f"  ✔ Data quality OK for {day}")


# ---------------------------------------------------------
# MAIN
# ---------------------------------------------------------
def main():
    log.info("=== Daily Aggregation Job started ===")

    client = bigquery.Client(project=PROJECT_ID)

    # Default: aggregate yesterday
    yesterday = (datetime.now(timezone.utc) - timedelta(days=1)).strftime("%Y-%m-%d")

    # Allow override via env variable (useful for backfill)
    day = os.environ.get("AGGREGATE_DAY", yesterday)
    log.info(f"Aggregating day: {day}")

    # Check if already aggregated
    if day_exists(client, day):
        log.info(f"Day {day} already exists in {TARGET_TABLE} – skipping.")
        sys.exit(0)

    # Aggregate
    aggregate_day(client, day)

    # Quality check
    quality_check(client, day)

    log.info("=== Daily Aggregation Job finished ===")


if __name__ == "__main__":
    main()
