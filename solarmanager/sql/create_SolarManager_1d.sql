-- =============================================================
-- DDL: SolarManager_1d
-- Daily aggregation table for SolarManager_5m
-- =============================================================

CREATE TABLE `modern-cubist-412113.SolarManager.SolarManager_1d` (
    day                 DATE      NOT NULL  OPTIONS(description="Date of the aggregation"),
    inserted            TIMESTAMP           OPTIONS(description="Timestamp of the insert"),

    -- Energy sums [Wh]
    cWh                 FLOAT64             OPTIONS(description="Consumption energy [Wh]"),
    pWh                 FLOAT64             OPTIONS(description="Production energy [Wh]"),
    bcWh                FLOAT64             OPTIONS(description="Battery charging energy [Wh]"),
    bdWh                FLOAT64             OPTIONS(description="Battery discharging energy [Wh]"),
    scWh                FLOAT64             OPTIONS(description="Self-consumption incl battery [Wh]"),
    cPvWh               FLOAT64             OPTIONS(description="Direct consumption from PV [Wh]"),
    iWh                 FLOAT64             OPTIONS(description="Import energy [Wh]"),
    eWh                 FLOAT64             OPTIONS(description="Export energy [Wh]"),

    -- Heatpump [Wh]
    pHpHeatingDayWh     FLOAT64             OPTIONS(description="Heatpump heating energy this day [Wh]"),
    pHpWarmwaterDayWh   FLOAT64             OPTIONS(description="Heatpump warm water energy this day [Wh]"),
    pHpHeatingTotalWh   FLOAT64             OPTIONS(description="Heatpump heating energy total [Wh]"),
    pHpWarmwaterTotalWh FLOAT64             OPTIONS(description="Heatpump warm water energy total [Wh]"),

    -- Power min/max [W]
    pWmax               FLOAT64             OPTIONS(description="Peak production power [W]"),
    pWmin               FLOAT64             OPTIONS(description="Min production power > 0 [W]"),
    cWmax               FLOAT64             OPTIONS(description="Peak consumption power [W]"),
    cWmin               FLOAT64             OPTIONS(description="Min consumption power > 0 [W]"),

    -- Battery [%]
    socMax              FLOAT64             OPTIONS(description="Max battery state of charge [%]"),
    socMin              FLOAT64             OPTIONS(description="Min battery state of charge > 0 [%]"),

    -- Weather
    tempRealMin         FLOAT64             OPTIONS(description="Min temperature [°C]"),
    tempRealMax         FLOAT64             OPTIONS(description="Max temperature [°C]"),
    rainAmountSum       FLOAT64             OPTIONS(description="Total rain amount [mm]"),
    snowAmountSum       FLOAT64             OPTIONS(description="Total snow amount [mm]"),
    sunriseTimestamp    TIMESTAMP           OPTIONS(description="Sunrise timestamp UTC"),
    sunsetTimestamp     TIMESTAMP           OPTIONS(description="Sunset timestamp UTC"),

    -- Percentages
    selfConsumptionPct  FLOAT64             OPTIONS(description="Self consumption rate [%]"),
    autarkyPct          FLOAT64             OPTIONS(description="Autarky degree [%]"),

    -- Data quality
    rowCount            INTEGER             OPTIONS(description="Number of 5-min rows for this day"),
    missingRows         INTEGER             OPTIONS(description="Missing 5-min rows (288 - rowCount)"),
    duplicates          INTEGER             OPTIONS(description="Number of duplicate timestamps"),
    vMin                INTEGER             OPTIONS(description="Minimum v flag of the day (data quality indicator)")
)
PARTITION BY day
OPTIONS (description = 'Daily aggregation of SolarManager 5-min data');
