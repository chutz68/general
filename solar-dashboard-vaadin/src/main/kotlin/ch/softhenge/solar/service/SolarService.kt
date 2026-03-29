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
}
