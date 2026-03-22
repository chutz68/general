package ch.softhenge.solar.service

import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.QueryJobConfiguration
import com.vaadin.flow.server.auth.AnonymousAllowed
import com.vaadin.hilla.BrowserCallable
import org.springframework.stereotype.Service
import java.time.LocalDate
import org.slf4j.LoggerFactory


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

data class RecentData(
    val t: String,
    val pW: Double,
    val cW: Double,
    val soc: Double?,
    val tempReal: Double?,
    val tHpWarmwaterC: Double?,
    val v: Int
)


@BrowserCallable
@AnonymousAllowed
@Service
class SolarService {
    private val log = LoggerFactory.getLogger(SolarService::class.java)
    private val projectId = "modern-cubist-412113"
    private val bigQuery = BigQueryOptions.newBuilder()
        .setProjectId(projectId)
        .build()
        .service

    fun getDailyData(fromDate: String, toDate: String): List<DailyData> {
        log.debug("getDailyData called: $fromDate → $toDate")
        val query = """
            SELECT
                CAST(day AS STRING) as day,
                IFNULL(pWh, 0) as pWh,
                IFNULL(cWh, 0) as cWh,
                IFNULL(iWh, 0) as iWh,
                IFNULL(eWh, 0) as eWh,
                IFNULL(scWh, 0) as scWh,
                IFNULL(bcWh, 0) as bcWh,
                IFNULL(bdWh, 0) as bdWh,
                IFNULL(pWmax, 0) as pWmax,
                IFNULL(socMax, 0) as socMax,
                IFNULL(socMin, 0) as socMin,
                IFNULL(tempRealMin, 0) as tempRealMin,
                IFNULL(tempRealMax, 0) as tempRealMax,
                IFNULL(rainAmountSum, 0) as rainAmountSum,
                selfConsumptionPct,
                autarkyPct,
                rowCount,
                missingRows
            FROM `$projectId.SolarManager.SolarManager_1d`
            WHERE day BETWEEN '$fromDate' AND '$toDate'
            ORDER BY day DESC
        """.trimIndent()

        try {
            val config = QueryJobConfiguration.newBuilder(query).build()
            log.debug("Executing BigQuery query...")
            val results = bigQuery.query(config)
            log.debug("BigQuery returned ${results.totalRows} rows")

        return results.iterateAll().map { row ->
            DailyData(
                day = row["day"].stringValue,
                pWh = row["pWh"].doubleValue,
                cWh = row["cWh"].doubleValue,
                iWh = row["iWh"].doubleValue,
                eWh = row["eWh"].doubleValue,
                scWh = row["scWh"].doubleValue,
                bcWh = row["bcWh"].doubleValue,
                bdWh = row["bdWh"].doubleValue,
                pWmax = row["pWmax"].doubleValue,
                socMax = row["socMax"].doubleValue,
                socMin = row["socMin"].doubleValue,
                tempRealMin = row["tempRealMin"].doubleValue,
                tempRealMax = row["tempRealMax"].doubleValue,
                rainAmountSum = row["rainAmountSum"].doubleValue,
                selfConsumptionPct = if (row["selfConsumptionPct"].isNull) null else row["selfConsumptionPct"].doubleValue,
                autarkyPct = if (row["autarkyPct"].isNull) null else row["autarkyPct"].doubleValue,
                rowCount = row["rowCount"].longValue.toInt(),
                missingRows = row["missingRows"].longValue.toInt()
            )
            }.toList()
        } catch (e: Exception) {
            log.error("BigQuery error: ${e.message}", e)
            return emptyList()
        }
    }

    fun getRecentData(limit: Int = 288): List<RecentData> {
        val query = """
            SELECT
                FORMAT_TIMESTAMP('%Y-%m-%dT%H:%M:%S', t) as t,
                IFNULL(pW, 0) as pW,
                IFNULL(cW, 0) as cW,
                soc,
                tempReal,
                tHpWarmwaterC,
                v
            FROM `$projectId.SolarManager.SolarManager_5m`
            ORDER BY t DESC
            LIMIT $limit
        """.trimIndent()

        val config = QueryJobConfiguration.newBuilder(query).build()
        val results = bigQuery.query(config)

        return results.iterateAll().map { row ->
            RecentData(
                t = row["t"].stringValue,
                pW = row["pW"].doubleValue,
                cW = row["cW"].doubleValue,
                soc = if (row["soc"].isNull) null else row["soc"].doubleValue,
                tempReal = if (row["tempReal"].isNull) null else row["tempReal"].doubleValue,
                tHpWarmwaterC = if (row["tHpWarmwaterC"].isNull) null else row["tHpWarmwaterC"].doubleValue,
                v = row["v"].longValue.toInt()
            )
        }.toList()
    }

    fun getTodaySummary(): DailyData? {
        val today = LocalDate.now().toString()
        return getDailyData(today, today).firstOrNull()
    }
}
