from google.cloud import bigquery

# Konfiguration
project_id = "modern-cubist-412113"
dataset_id = "SolarManager"
table_id = "SolarManager_5m_temp"

# Client initialisieren
client = bigquery.Client(project=project_id)

# Dataset erstellen (falls nicht vorhanden)
dataset_ref = bigquery.Dataset(f"{project_id}.{dataset_id}")
dataset_ref.location = "EU"
client.create_dataset(dataset_ref, exists_ok=True)

# Schema definieren
schema = [
    bigquery.SchemaField("t", "TIMESTAMP", mode="REQUIRED", description="Timestamp of solar data point"),
    bigquery.SchemaField("inserted", "TIMESTAMP", mode="REQUIRED", default_value_expression="CURRENT_TIMESTAMP()", description="Timestamp of the insert"),
    bigquery.SchemaField("v", "STRING", mode="REQUIRED", description="Interface version ([0-9][0-9][0-9][0-9], e.g. 1111=all available version 1, 1) Solardata, 2) Batterydata, 3) Heatpumpdata, 4) Weatherdata)"),
    bigquery.SchemaField("cW", "FLOAT", description="Consumption in [watt]"),
    bigquery.SchemaField("pW", "FLOAT", description="Production in [watt]"),
    bigquery.SchemaField("bcW", "FLOAT", description="Battery charging in [watt]"),
    bigquery.SchemaField("bdW", "FLOAT", description="Battery discharging in [watt]"),
    bigquery.SchemaField("cWh", "FLOAT", description="Consumption in [watt-hour]"),
    bigquery.SchemaField("pWh", "FLOAT", description="Production in [watt-hour]"),
    bigquery.SchemaField("bcWh", "FLOAT", description="Battery charging in [watt-hour]"),
    bigquery.SchemaField("bdWh", "FLOAT", description="Battery discharging in [watt-hour]"),
    bigquery.SchemaField("scWh", "FLOAT", description="Self-consumption in [watt-hour]"),
    bigquery.SchemaField("cPvWh", "FLOAT", description="Direct consumption from PV in [watt-hour]"),
    bigquery.SchemaField("iWh", "FLOAT", description="Import [watt-hour]"),
    bigquery.SchemaField("eWh", "FLOAT", description="Export [watt-hour]"),
    bigquery.SchemaField("sHp", "INTEGER", description="Heatpump Current state. 1=off, 2=starting/stopping, 3=running")
    bigquery.SchemaField("soc", "FLOAT", description="Battery capacity in percent"),
    bigquery.SchemaField("weatherMain", "STRING", description="Group of weather parameters (Rain, Snow, Extreme etc.)"),
    bigquery.SchemaField("weatherDetailed", "STRING", description="Weather condition within the group such as broken clouds, light snow"),
    bigquery.SchemaField("weatherIconId", "STRING", description="The iconid of the weather by openweathermap"),
    bigquery.SchemaField("tempReal", "FLOAT", description="The real temparature in degree celsius"),    
    bigquery.SchemaField("tempFeel", "FLOAT", description="The felt temparature in degree celsius"),
    bigquery.SchemaField("airPressure", "INTEGER", description="Air pressure in mbar"),
    bigquery.SchemaField("airHumidity", "INTEGER", description="Humidity as a percentage value"),
    bigquery.SchemaField("visibility", "INTEGER", description="The visibility in m"),
    bigquery.SchemaField("windSpeed", "FLOAT", description="Speed of the wind in m/s"),
    bigquery.SchemaField("windDegree", "INTEGER", description="The wind direction meteorological from 0 to 255"),
    bigquery.SchemaField("cloudiness", "INTEGER", description="Cloudiness as a percentage value"),
    bigquery.SchemaField("rainAmount", "FLOAT", description="Rain amount for the last 1 hour, mm"),
    bigquery.SchemaField("snowAmount", "FLOAT", description="Snow amount for the last 1 hour, mm"),
    bigquery.SchemaField("sunriseTimestamp", "TIMESTAMP", description="Date and time of the sunrise"),
    bigquery.SchemaField("sunsetTimestamp", "TIMESTAMP", description="Date and time of the sunset"),
    bigquery.SchemaField("tHpFlowC", "FLOAT", description="Heatpump Temparature Flow (Vorlauf) in Grac C"),
    bigquery.SchemaField("tHpBackC", "FLOAT", description="Heatpump Temparature Back (Ruecklauf) in Grac C"),
    bigquery.SchemaField("tHpWarmwaterC", "FLOAT", description="Heatpump Temparature Warmwater in Grac C"),
    bigquery.SchemaField("tHpOutC", "FLOAT", description="Heatpump Temparature Outside in Grac C"),
    bigquery.SchemaField("pHpHeatingDayWh", "FLOAT", description="Heatpump Heating Energy this day [watt-hour]"),
    bigquery.SchemaField("pHpHeatingTotalWh", "FLOAT", description="Heatpump Heating Energy total [watt-hour]"),
    bigquery.SchemaField("pHpWarmwaterDayWh", "FLOAT", description="Heatpump Warm water Energy this day [watt-hour]"),
    bigquery.SchemaField("pHpWarmwaterTotalWh", "FLOAT", description="Heatpump Warm water Energy total [watt-hour]"),
    bigquery.SchemaField("iHp", "FLOAT", description="Heatpump Warm water Energy total [watt-hour]"),
    bigquery.SchemaField("sHpCurrent", "FLOAT", description="Heatpump Current in Ampere. 0 if not running")
]

# Tabelle definieren und erstellen
table_ref = bigquery.Table(f"{project_id}.{dataset_id}.{table_id}", schema=schema)

# Set Daily Partitioning on column 't'
table_ref.time_partitioning = bigquery.TimePartitioning(
    type_=bigquery.TimePartitioningType.DAY,
    field="t",  # The column to partition by
)

client.create_table(table_ref, exists_ok=True)

print(f"Tabelle {dataset_id}.{table_id} erfolgreich erstellt.")
