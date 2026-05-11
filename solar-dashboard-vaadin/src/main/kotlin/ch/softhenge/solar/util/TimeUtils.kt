package ch.softhenge.solar.util

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Zentrale Timezone-Verwaltung für das Solar-Dashboard.
 *
 * Die aktive Timezone wird einmalig pro Session gesetzt — entweder:
 *   a) Automatisch aus dem Browser (via EnergyFlowView / MainLayout beim Start)
 *   b) Manuell via setZone(zoneId)
 *   c) Fallback: "Europe/Zurich"
 *
 * Alle BigQuery-Queries in SolarService referenzieren TimeUtils.ZONE.id —
 * dadurch ist die Timezone genau einmal definiert und überall konsistent.
 *
 * Browser-Integration (in MainLayout.kt oder EnergyFlowView.kt):
 *
 *   // JavaScript liefert die Browser-Timezone:
 *   ui.page.executeJs("return Intl.DateTimeFormat().resolvedOptions().timeZone")
 *       .then(String::class.java) { tz ->
 *           TimeUtils.setZone(tz)
 *       }
 */
object TimeUtils {

    private const val DEFAULT_ZONE = "Europe/Zurich"

    @Volatile
    private var _zone: ZoneId = ZoneId.of(DEFAULT_ZONE)

    /** Aktive Timezone — wird von SolarService für alle BigQuery-Queries verwendet. */
    val ZONE: ZoneId get() = _zone

    private val DATE_FMT     = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val TIME_FMT     = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Setzt die aktive Timezone — typischerweise aus dem Browser.
     * Ungültige Zone-IDs werden ignoriert (Fallback bleibt aktiv).
     *
     * @param zoneId  IANA-Timezone-String, z.B. "Europe/Zurich", "America/New_York"
     */
    fun setZone(zoneId: String) {
        _zone = try {
            ZoneId.of(zoneId)
        } catch (e: Exception) {
            ZoneId.of(DEFAULT_ZONE)
        }
    }

    fun today(): LocalDate = LocalDate.now(_zone)

    fun formatDate(date: LocalDate): String = date.format(DATE_FMT)

    fun formatDateTime(utcString: String): String {
        return try {
            parseUtc(utcString).format(DATETIME_FMT)
        } catch (e: Exception) {
            utcString
        }
    }

    fun formatTime(utcString: String): String {
        return try {
            parseUtc(utcString).format(TIME_FMT)
        } catch (e: Exception) {
            utcString
        }
    }

    fun zoneLabel(): String = _zone.id

    private fun parseUtc(s: String): ZonedDateTime {
        val cleaned = s.replace("T", " ").replace("Z", "")
        val base = cleaned.substring(0, minOf(16, cleaned.length))
        val local = java.time.LocalDateTime.parse(
            base, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        )
        return local.atZone(ZoneId.of("UTC")).withZoneSameInstant(_zone)
    }
}
