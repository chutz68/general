#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Solarlog MySQL SQL Dump → BigQuery
Reads a large MySQL REPLACE INTO SQL file and inserts into BigQuery in batches.

Usage:
    export GOOGLE_APPLICATION_CREDENTIALS="C:/dev/backfill/solarmanager/gcp-key.json"
    python import_solarlog.py your_solarlog_dump.sql
"""

import re
import sys
import time
import logging
from datetime import datetime, timezone
from google.cloud import bigquery

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
log = logging.getLogger(__name__)

# ---------------------------------------------------------
# Config
# ---------------------------------------------------------
PROJECT_ID  = "modern-cubist-412113"
DATASET_ID  = "SolarlogData"
TABLE_ID    = "t_f_solardata_5m"
BATCH_SIZE  = 500

# Column names in order (must match SQL INSERT column list)
COLUMNS = [
    "RECORD_DATETIME", "UPDATE_DATETIME", "PAC_WRALL", "EAC_DAYSUM_WRALL",
    "PAC_TOT_CNT", "EAC_DAYSUM_CNT", "PAC_FROM_GRID", "EAC_DAYSUM_FROM_GRID",
    "PAC_OWNUSAGE", "EAC_DAYSUM_OWNUSAGE", "PAC_TO_GRID", "EAC_DAYSUM_TO_GRID",
    "EAC_MONTHSUM_WRALL", "EAC_YEARSUM_WRALL", "EAC_TOTAL_WRALL",
    "PAC_WR1", "UAC_WR1", "EAC_DAYSUM_WR1", "STATE_WR1", "ERROR_WR1", "TEMP_WR1",
    "PAC_WR2", "UAC_WR2", "EAC_DAYSUM_WR2", "STATE_WR2", "ERROR_WR2", "TEMP_WR2",
    "EAC_MONTHSUM_CNT", "EAC_YEARSUM_CNT", "EAC_TOTAL_CNT",
    "PAC_L1_CNT", "PAC_L2_CNT", "PAC_L3_CNT",
    "UAC_L1_CNT", "UAC_L2_CNT", "UAC_L3_CNT",
    "STATE_CNT", "ERROR_CNT", "ISG_ON",
    "WEATHER_MAIN", "WEATHER_DETAILED", "WEATHER_ICONID",
    "TEMP_REAL", "TEMP_FEEL", "AIR_PRESSURE", "AIR_HUMIDITY", "VISIBILITY",
    "WIND_SPEED", "WIND_DEGREE", "CLOUDINESS", "RAIN_AMOUNT", "SNOW_AMOUNT",
    "SUNRISE_DATETIME", "SUNSET_DATETIME", "AGGREGATION_SOURCE"
]

# Timestamp columns that need conversion
TIMESTAMP_COLS = {"RECORD_DATETIME", "UPDATE_DATETIME", "SUNRISE_DATETIME", "SUNSET_DATETIME"}

# BigQuery TIMESTAMP fields
BQ_TIMESTAMP_COLS = {"RECORD_DATETIME", "UPDATE_DATETIME", "SUNRISE_DATETIME", "SUNSET_DATETIME"}


# ---------------------------------------------------------
# 1) Parse a single VALUES row
# ---------------------------------------------------------
def parse_values_line(line: str) -> list | None:
    """Parse a single VALUES tuple from the SQL dump."""
    line = line.strip().rstrip(',').rstrip(';')

    # Extract content between outer parentheses
    match = re.match(r'^\((.+)\)$', line, re.DOTALL)
    if not match:
        return None

    content = match.group(1)

    # Split by comma but respect quoted strings
    values = []
    current = ""
    in_quotes = False
    quote_char = None

    for char in content:
        if in_quotes:
            current += char
            if char == quote_char:
                in_quotes = False
        elif char in ("'", '"'):
            in_quotes = True
            quote_char = char
            current += char
        elif char == ',':
            values.append(current.strip())
            current = ""
        else:
            current += char

    values.append(current.strip())
    return values


def convert_value(raw: str, col: str):
    """Convert a raw SQL value to Python/BigQuery compatible type."""
    # NULL
    if raw.upper() == "NULL":
        return None

    # Remove surrounding quotes
    if raw.startswith("'") and raw.endswith("'"):
        val = raw[1:-1]

        # Invalid MySQL date, empty string → NULL
        if not val or val.startswith("0000"):
            return None

        # Timestamp columns → ISO format
        if col in TIMESTAMP_COLS:
            try:
                dt = datetime.strptime(val, "%Y-%m-%d %H:%M:%S")
                return dt.strftime("%Y-%m-%d %H:%M:%S UTC")
            except ValueError:
                return None

        return val

    # Numeric
    try:
        if "." in raw:
            return float(raw)
        return int(raw)
    except ValueError:
        return None


def parse_row(values: list) -> dict | None:
    """Convert parsed values list to a BigQuery row dict."""
    if len(values) != len(COLUMNS):
        log.warning(f"Column count mismatch: expected {len(COLUMNS)}, got {len(values)}")
        return None

    row = {}
    for col, raw in zip(COLUMNS, values):
        val = convert_value(raw, col)
        # Extra safety: convert empty strings to None
        if val == "":
            val = None
        row[col] = val

    return row


# ---------------------------------------------------------
# 2) Insert batch into BigQuery
# ---------------------------------------------------------
def insert_batch(client: bigquery.Client, table_ref: str, batch: list) -> int:
    """Insert a batch of rows into BigQuery. Falls back to row-by-row on error."""
    errors = client.insert_rows_json(table_ref, batch)
    if not errors:
        return len(batch)

    # Batch failed – insert row by row to skip invalid rows
    log.warning(f"Batch failed – switching to row-by-row mode for {len(batch)} rows.")
    inserted = 0
    for row in batch:
        row_errors = client.insert_rows_json(table_ref, [row])
        if not row_errors:
            inserted += 1
        else:
            log.warning(f"Skipping invalid row {row.get('RECORD_DATETIME')}: {row_errors}")
    return inserted


# ---------------------------------------------------------
# MAIN
# ---------------------------------------------------------
def main():
    if len(sys.argv) < 2:
        print("Usage: python import_solarlog.py your_solarlog_dump.sql")
        sys.exit(1)

    sql_file   = sys.argv[1]
    table_ref  = f"{PROJECT_ID}.{DATASET_ID}.{TABLE_ID}"
    client     = bigquery.Client(project=PROJECT_ID)

    log.info(f"=== Solarlog Import started ===")
    log.info(f"File: {sql_file}")
    log.info(f"Target: {table_ref}")
    log.info(f"Batch size: {BATCH_SIZE}")

    total_inserted = 0
    total_errors   = 0
    batch          = []
    line_count     = 0
    in_values      = False

    with open(sql_file, "r", encoding="utf-8", errors="replace") as f:
        for line in f:
            line = line.strip()  # removes tabs and spaces at start/end

            # Skip empty lines
            if not line:
                continue

            # Parse any line starting with (
            if line.startswith("("):
                values = parse_values_line(line)
                if values:
                    row = parse_row(values)
                    if row:
                        batch.append(row)
                        line_count += 1

                # Insert batch when full
                if len(batch) >= BATCH_SIZE:
                    inserted = insert_batch(client, table_ref, batch)
                    total_inserted += inserted
                    total_errors   += len(batch) - inserted
                    batch = []
                    log.info(f"Progress: {total_inserted:,} inserted, {total_errors} errors")

    # Insert remaining rows
    if batch:
        inserted = insert_batch(client, table_ref, batch)
        total_inserted += inserted
        total_errors   += len(batch) - inserted

    log.info(f"=== Import finished ===")
    log.info(f"Total inserted: {total_inserted:,}")
    log.info(f"Total errors:   {total_errors:,}")


if __name__ == "__main__":
    main()
