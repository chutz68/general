#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Stiebel Eltron ISG → GCS Bucket
Runs on Raspberry Pi via cron every 5 minutes (offset -2 min before Cloud Run)
Cron: 4,9,14,19,24,29,34,39,44,49,54,59 * * * *
"""

import requests
import json
from bs4 import BeautifulSoup
from google.cloud import storage
from datetime import datetime, timezone
import logging
import sys

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
log = logging.getLogger(__name__)

ISG_URL    = "http://192.168.1.180/?s=1,1"
PROJECT_ID = "modern-cubist-412113"
BUCKET     = "solar-isg-data"
BLOB_NAME  = "isg_data.json"


def parse_value(raw: str) -> float | None:
    try:
        number = raw.strip().split(" ")[0].replace(",", ".")
        value = float(number)
        if "MWh" in raw:
            value = value * 1_000_000
        elif "KWh" in raw:
            value = value * 1_000
        return value
    except (ValueError, IndexError):
        return None


def fetch_isg_data() -> dict | None:
    try:
        response = requests.get(ISG_URL, timeout=15)
        response.raise_for_status()
    except requests.RequestException as e:
        log.error(f"ISG request failed: {e}")
        return None

    soup = BeautifulSoup(response.text, "html.parser")
    data = {}
    for row in soup.select("table.info tr"):
        cells = row.select("td")
        if len(cells) == 2:
            key   = cells[0].get_text(strip=True).upper()
            value = cells[1].get_text(strip=True)
            data[key] = value

    log.info(f"ISG raw data: {data}")

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


def write_to_gcs(isg_data: dict) -> None:
    client = storage.Client(project=PROJECT_ID)
    bucket = client.bucket(BUCKET)
    blob   = bucket.blob(BLOB_NAME)
    payload = {
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "data": isg_data
    }
    blob.upload_from_string(json.dumps(payload), content_type="application/json")
    log.info(f"✔ ISG data written to gs://{BUCKET}/{BLOB_NAME}")


def main():
    log.info("=== ISG Raspy Job started ===")
    isg_data = fetch_isg_data()
    if isg_data is None:
        log.error("No ISG data – aborting.")
        sys.exit(1)
    log.info(f"ISG data: {isg_data}")
    write_to_gcs(isg_data)
    log.info("=== ISG Raspy Job finished ===")


if __name__ == "__main__":
    main()
