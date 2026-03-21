-- =============================================================
-- Backfill SolarManager_1d from 2018-03-30 to 2026-03-20
-- Run once in BigQuery Console
-- =============================================================

INSERT INTO `modern-cubist-412113.SolarManager.SolarManager_1d`
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

FROM `modern-cubist-412113.SolarManager.SolarManager_5m`
WHERE DATE(t) BETWEEN '2018-03-30' AND '2026-03-20'
GROUP BY DATE(t)
ORDER BY day
