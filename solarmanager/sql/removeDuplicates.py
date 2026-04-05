import argparse
import sys
from google.cloud import bigquery
from datetime import datetime, timedelta

def cleanup_duplicates(project_id, dataset_id, table_id, days):
    client = bigquery.Client(project=project_id)
    table_ref = f"{project_id}.{dataset_id}.{table_id}"

# Die Logik: 
    # 1. Partitioniere nach dem Daten-Zeitstempel 't'.
    # 2. Sortiere innerhalb dieser Gruppe nach 'inserted' (dem Auto-Timestamp).
    # 3. Behalte nur die Zeile mit der Nummer 1.
    
    sql_query = f"""
    MERGE `{table_ref}` T
    USING (
        SELECT * EXCEPT(rn)
        FROM (
            SELECT 
                *, 
                ROW_NUMBER() OVER (
                    PARTITION BY t 
                    ORDER BY v DESC, inserted DESC
                ) as rn
            FROM `{table_ref}`
            WHERE t >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL {days} DAY)
        )
        WHERE rn = 1
    ) S
    ON T.t = S.t AND T.inserted = S.inserted
    
    -- Lösche alles im Zeitfenster, was NICHT unsere "Gewinner"-Zeile ist
    WHEN NOT MATCHED BY SOURCE AND T.t >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL {days} DAY) THEN
        DELETE
    """

    print(f"Bereinige Duplikate (kleinere Versionen) der letzten {days} Tage in: {table_ref}...")
    
    try:
        query_job = client.query(sql_query)
        query_job.result()
        print(f"Bereinigung abgeschlossen. Gelöschte Zeilen: {query_job.num_dml_affected_rows}")
    except Exception as e:
        print(f"Fehler: {e}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Bereinigt BigQuery-Duplikate basierend auf Zeitstempel und Version.")
    # Positionale oder benannte Argumente
    parser.add_argument("daysToCheck", type=int, help="Zeitfenster in Tage (z.B. 30)")

    args = parser.parse_args()

    project_id = "modern-cubist-412113"
    dataset_id = "SolarManager"
    table_id = "SolarManager_5m"
    
    cleanup_duplicates(project_id, dataset_id, table_id, args.daysToCheck)
    