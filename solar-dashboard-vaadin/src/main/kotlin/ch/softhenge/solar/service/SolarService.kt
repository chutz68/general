package ch.softhenge.solar.service

import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.QueryJobConfiguration
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

data class DailyData(
    val day: String,
    val pWh: Double,
    val cWh: Double,
    val iWh: Double,
    val eWh: Double,
    val scWh: Double,
    val bcWh: Double,
    val bdWh: Double,
    val pWmax: Double,
    val socMax: Double,
    val socMin: Double,
    val tempRealMin: Double,
    val tempRealMax: Double,
    val rainAmountSum: Double,
    val selfConsumptionPct: Double?,
    val autarkyPct: Double?,
    val rowCount: Int,
    val missingRows: Int
)

data class FiveMinData(
    val t: String,
    val pW: Double,
    val cW: Double,
    val bcW: Double,
    val bdW: Double,
    val soc: Double?
)

data class CurrentData(
    val t: String,
    val pW: Double,
    val cW: Double,
    val bcW: Double,
    val bdW: Double,
    val soc: Double?,
    val iWh: Double,
    val eWh: Double
)

data class TodaySums(
    val pWh: Double,
    val cWh: Double,
    val bcWh: Double,
    val bdWh: Double,
    val scWh: Double,
    val iWh: Double,
    val eWh: Double
)

data class MonthlyDayData(
    val day: String,
    val pWh: Double,
    val cWh: Double,
    val bcWh: Double,
    val bdWh: Double,
    val iWh: Double,
    val eWh: Double,
    val pHpHeatingDayWh: Double,
    val pHpWarmwaterDayWh: Double,
    val tempRealMin: Double,
    val tempRealMax: Double,
    val rainAmountSum: Double,
    val snowAmountSum: Double,
    val dayLengthMin: Int,
    val sunshineMin: Int,
    val selfConsumptionPct: Double?,
    val autarkyPct: Double?,
    val rowCount: Int,
    val missingRows: Int
)

data class MonthlyTotals(
    val pWh: Double,
    val cWh: Double,
    val bcWh: Double,
    val bdWh: Double,
    val iWh: Double,
    val eWh: Double,
    val pHpHeatingWh: Double,
    val pHpWarmwaterWh: Double,
    val rainAmountSum: Double,
    val snowAmountSum: Double,
    val sunshineMin: Int,
    val tempMin: Double?,
    val tempMax: Double?,
    val daysWithData: Int,
    val avgSelfConsumptionPct: Double?,
    val avgAutarkyPct: Double?
)

@Service
class SolarService {

    private val log = LoggerFactory.getLogger(SolarService::class.java)
    private val projectId = "modern-cubist-412113"
    private val bigQuery = BigQueryOptions.newBuilder()
        .setProjectId(projectId)
        .build()
        .service

    private fun FieldValue.toDoubleOrZero(): Double =
        if (this.isNull) 0.0 else this.doubleValue

    private fun FieldValue.toDoubleOrNull(): Double? =
        if (this.isNull) null else this.doubleValue

    fun getDailyData(fromDate: String, toDate: String): List<DailyData> {
        log.debug("getDailyData: $fromDate → $toDate")
        val query = """
            SELECT
                CAST(day AS STRING)    AS day,
                IFNULL(pWh, 0)         AS pWh,
                IFNULL(cWh, 0)         AS cWh,
                IFNULL(iWh, 0)         AS iWh,
                IFNULL(eWh, 0)         AS eWh,
                IFNULL(scWh, 0)        AS scWh,
                IFNULL(bcWh, 0)        AS bcWh,
                IFNULL(bdWh, 0)        AS bdWh,
                IFNULL(pWmax, 0)       AS pWmax,
                IFNULL(socMax, 0)      AS socMax,
                IFNULL(socMin, 0)      AS socMin,
                IFNULL(tempRealMin, 0) AS tempRealMin,
                IFNULL(tempRealMax, 0) AS tempRealMax,
                IFNULL(rainAmountSum, 0) AS rainAmountSum,
                selfConsumptionPct,
                autarkyPct,
                IFNULL(rowCount, 0)    AS rowCount,
                IFNULL(missingRows, 0) AS missingRows
            FROM `$projectId.SolarManager.SolarManager_1d`
            WHERE day BETWEEN '$fromDate' AND '$toDate'
            ORDER BY day DESC
        """.trimIndent()

        return try {
            val results = bigQuery.query(QueryJobConfiguration.newBuilder(query).build())
            log.debug("BigQuery returned ${results.totalRows} rows")
            results.iterateAll().map { row ->
                DailyData(
                    day              = row["day"].stringValue,
                    pWh              = row["pWh"].toDoubleOrZero(),
                    cWh              = row["cWh"].toDoubleOrZero(),
                    iWh              = row["iWh"].toDoubleOrZero(),
                    eWh              = row["eWh"].toDoubleOrZero(),
                    scWh             = row["scWh"].toDoubleOrZero(),
                    bcWh             = row["bcWh"].toDoubleOrZero(),
                    bdWh             = row["bdWh"].toDoubleOrZero(),
                    pWmax            = row["pWmax"].toDoubleOrZero(),
                    socMax           = row["socMax"].toDoubleOrZero(),
                    socMin           = row["socMin"].toDoubleOrZero(),
                    tempRealMin      = row["tempRealMin"].toDoubleOrZero(),
                    tempRealMax      = row["tempRealMax"].toDoubleOrZero(),
                    rainAmountSum    = row["rainAmountSum"].toDoubleOrZero(),
                    selfConsumptionPct = row["selfConsumptionPct"].toDoubleOrNull(),
                    autarkyPct       = row["autarkyPct"].toDoubleOrNull(),
                    rowCount         = row["rowCount"].longValue.toInt(),
                    missingRows      = row["missingRows"].longValue.toInt()
                )
            }.toList()
        } catch (e: Exception) {
            log.error("BigQuery error: ${e.message}", e)
            emptyList()
        }
    }

    fun getFiveMinData(fromDate: String, toDate: String = fromDate): List<FiveMinData> {
        log.debug("getFiveMinData: $fromDate → $toDate")
        val query = """
            SELECT
                FORMAT_TIMESTAMP('%Y-%m-%d %H:%M', t) AS t,
                IFNULL(pW, 0)  AS pW,
                IFNULL(cW, 0)  AS cW,
                IFNULL(bcW, 0) AS bcW,
                IFNULL(bdW, 0) AS bdW,
                soc
            FROM `$projectId.SolarManager.SolarManager_5m`
            WHERE DATE(t) BETWEEN '$fromDate' AND '$toDate'
            ORDER BY t ASC
        """.trimIndent()

        return try {
            val results = bigQuery.query(QueryJobConfiguration.newBuilder(query).build())
            results.iterateAll().map { row ->
                FiveMinData(
                    t   = row["t"].stringValue,
                    pW  = row["pW"].toDoubleOrZero(),
                    cW  = row["cW"].toDoubleOrZero(),
                    bcW = row["bcW"].toDoubleOrZero(),
                    bdW = row["bdW"].toDoubleOrZero(),
                    soc = row["soc"].toDoubleOrNull()
                )
            }.toList()
        } catch (e: Exception) {
            log.error("BigQuery error: ${e.message}", e)
            emptyList()
        }
    }

    fun getCurrentData(): CurrentData? {
        log.debug("getCurrentData")
        val query = """
            SELECT
                FORMAT_TIMESTAMP('%Y-%m-%d %H:%M', t) AS t,
                IFNULL(pW, 0)  AS pW,
                IFNULL(cW, 0)  AS cW,
                IFNULL(bcW, 0) AS bcW,
                IFNULL(bdW, 0) AS bdW,
                soc,
                IFNULL(iWh, 0) AS iWh,
                IFNULL(eWh, 0) AS eWh
            FROM `$projectId.SolarManager.SolarManager_5m`
            ORDER BY t DESC
            LIMIT 1
        """.trimIndent()

        return try {
            val results = bigQuery.query(QueryJobConfiguration.newBuilder(query).build())
            results.iterateAll().map { row ->
                CurrentData(
                    t   = row["t"].stringValue,
                    pW  = row["pW"].toDoubleOrZero(),
                    cW  = row["cW"].toDoubleOrZero(),
                    bcW = row["bcW"].toDoubleOrZero(),
                    bdW = row["bdW"].toDoubleOrZero(),
                    soc = row["soc"].toDoubleOrNull(),
                    iWh = row["iWh"].toDoubleOrZero(),
                    eWh = row["eWh"].toDoubleOrZero()
                )
            }.firstOrNull()
        } catch (e: Exception) {
            log.error("BigQuery error: ${e.message}", e)
            null
        }
    }

    /**
     * Returns daily values for the given month, joined with sunshine duration
     * derived from the 5-min table (count of intervals with pW > 100 W, * 5 min).
     */
    fun getMonthlyData(fromDate: String, toDate: String): List<MonthlyDayData> {
        log.debug("getMonthlyData: $fromDate → $toDate")
        val query = """
            WITH sunshine AS (
                -- Sunshine = sum of 5-min intervals where OpenWeatherMap reports
                -- clear sky (01d) or few clouds (02d). Night codes (01n/02n) are
                -- naturally excluded so we don't count "sunshine" after sunset.
                SELECT
                    DATE(t, 'Europe/Zurich') AS day,
                    COUNT(*) * 5             AS sunshineMin
                FROM `$projectId.SolarManager.SolarManager_5m`
                WHERE weatherIconId IN ('01d', '02d')
                  AND DATE(t, 'Europe/Zurich') BETWEEN '$fromDate' AND '$toDate'
                GROUP BY day
            )
            SELECT
                CAST(d.day AS STRING)         AS day,
                IFNULL(d.pWh, 0)              AS pWh,
                IFNULL(d.cWh, 0)              AS cWh,
                IFNULL(d.bcWh, 0)             AS bcWh,
                IFNULL(d.bdWh, 0)             AS bdWh,
                IFNULL(d.iWh, 0)              AS iWh,
                IFNULL(d.eWh, 0)              AS eWh,
                IFNULL(d.pHpHeatingDayWh, 0)  AS pHpHeatingDayWh,
                IFNULL(d.pHpWarmwaterDayWh, 0) AS pHpWarmwaterDayWh,
                IFNULL(d.tempRealMin, 0)      AS tempRealMin,
                IFNULL(d.tempRealMax, 0)      AS tempRealMax,
                IFNULL(d.rainAmountSum, 0)    AS rainAmountSum,
                IFNULL(d.snowAmountSum, 0)    AS snowAmountSum,
                -- Day length: compare only time-of-day in Europe/Zurich; the stored
                -- sunsetTimestamp may belong to the following day, which would otherwise
                -- add a spurious 24 h. TIME_DIFF on the time component is date-agnostic.
                IFNULL(TIME_DIFF(
                    TIME(d.sunsetTimestamp,  'Europe/Zurich'),
                    TIME(d.sunriseTimestamp, 'Europe/Zurich'),
                    MINUTE
                ), 0) AS dayLengthMin,
                IFNULL(s.sunshineMin, 0)      AS sunshineMin,
                d.selfConsumptionPct,
                d.autarkyPct,
                IFNULL(d.rowCount, 0)         AS rowCount,
                IFNULL(d.missingRows, 0)      AS missingRows
            FROM `$projectId.SolarManager.SolarManager_1d` d
            LEFT JOIN sunshine s ON s.day = d.day
            WHERE d.day BETWEEN '$fromDate' AND '$toDate'
            ORDER BY d.day ASC
        """.trimIndent()

        return try {
            val results = bigQuery.query(QueryJobConfiguration.newBuilder(query).build())
            log.debug("BigQuery returned ${results.totalRows} rows")
            results.iterateAll().map { row ->
                MonthlyDayData(
                    day                = row["day"].stringValue,
                    pWh                = row["pWh"].toDoubleOrZero(),
                    cWh                = row["cWh"].toDoubleOrZero(),
                    bcWh               = row["bcWh"].toDoubleOrZero(),
                    bdWh               = row["bdWh"].toDoubleOrZero(),
                    iWh                = row["iWh"].toDoubleOrZero(),
                    eWh                = row["eWh"].toDoubleOrZero(),
                    pHpHeatingDayWh    = row["pHpHeatingDayWh"].toDoubleOrZero(),
                    pHpWarmwaterDayWh  = row["pHpWarmwaterDayWh"].toDoubleOrZero(),
                    tempRealMin        = row["tempRealMin"].toDoubleOrZero(),
                    tempRealMax        = row["tempRealMax"].toDoubleOrZero(),
                    rainAmountSum      = row["rainAmountSum"].toDoubleOrZero(),
                    snowAmountSum      = row["snowAmountSum"].toDoubleOrZero(),
                    dayLengthMin       = row["dayLengthMin"].longValue.toInt(),
                    sunshineMin        = row["sunshineMin"].longValue.toInt(),
                    selfConsumptionPct = row["selfConsumptionPct"].toDoubleOrNull(),
                    autarkyPct         = row["autarkyPct"].toDoubleOrNull(),
                    rowCount           = row["rowCount"].longValue.toInt(),
                    missingRows        = row["missingRows"].longValue.toInt()
                )
            }.toList()
        } catch (e: Exception) {
            log.error("BigQuery error: ${e.message}", e)
            emptyList()
        }
    }

    /** Aggregates a month's daily rows into totals/averages for the KPI cards. */
    fun summarize(days: List<MonthlyDayData>): MonthlyTotals {
        if (days.isEmpty()) {
            return MonthlyTotals(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, null, null, 0, null, null)
        }
        val sc = days.mapNotNull { it.selfConsumptionPct }
        val au = days.mapNotNull { it.autarkyPct }
        // tempRealMin/Max = 0 typically means "no data" (IFNULL fallback in SQL); ignore those.
        val mins = days.map { it.tempRealMin }.filter { it != 0.0 }
        val maxs = days.map { it.tempRealMax }.filter { it != 0.0 }
        return MonthlyTotals(
            pWh                   = days.sumOf { it.pWh },
            cWh                   = days.sumOf { it.cWh },
            bcWh                  = days.sumOf { it.bcWh },
            bdWh                  = days.sumOf { it.bdWh },
            iWh                   = days.sumOf { it.iWh },
            eWh                   = days.sumOf { it.eWh },
            pHpHeatingWh          = days.sumOf { it.pHpHeatingDayWh },
            pHpWarmwaterWh        = days.sumOf { it.pHpWarmwaterDayWh },
            rainAmountSum         = days.sumOf { it.rainAmountSum },
            snowAmountSum         = days.sumOf { it.snowAmountSum },
            sunshineMin           = days.sumOf { it.sunshineMin },
            tempMin               = mins.minOrNull(),
            tempMax               = maxs.maxOrNull(),
            daysWithData          = days.count { it.rowCount > 0 },
            avgSelfConsumptionPct = if (sc.isNotEmpty()) sc.average() else null,
            avgAutarkyPct         = if (au.isNotEmpty()) au.average() else null
        )
    }

    fun getTodaySums(): TodaySums {
        log.debug("getTodaySums")
        val today = java.time.LocalDate.now().toString()
        val query = """
            SELECT
                IFNULL(SUM(pWh), 0)  AS pWh,
                IFNULL(SUM(cWh), 0)  AS cWh,
                IFNULL(SUM(bcWh), 0) AS bcWh,
                IFNULL(SUM(bdWh), 0) AS bdWh,
                IFNULL(SUM(scWh), 0) AS scWh,
                IFNULL(SUM(iWh), 0)  AS iWh,
                IFNULL(SUM(eWh), 0)  AS eWh
            FROM `$projectId.SolarManager.SolarManager_5m`
            WHERE DATE(t) = '$today'
        """.trimIndent()

        return try {
            val results = bigQuery.query(QueryJobConfiguration.newBuilder(query).build())
            results.iterateAll().map { row ->
                TodaySums(
                    pWh  = row["pWh"].toDoubleOrZero(),
                    cWh  = row["cWh"].toDoubleOrZero(),
                    bcWh = row["bcWh"].toDoubleOrZero(),
                    bdWh = row["bdWh"].toDoubleOrZero(),
                    scWh = row["scWh"].toDoubleOrZero(),
                    iWh  = row["iWh"].toDoubleOrZero(),
                    eWh  = row["eWh"].toDoubleOrZero()
                )
            }.firstOrNull() ?: TodaySums(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        } catch (e: Exception) {
            log.error("BigQuery error: ${e.message}", e)
            TodaySums(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }
    }
}
