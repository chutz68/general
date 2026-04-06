package ch.softhenge.solar.util

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object TimeUtils {

    val ZONE: ZoneId = ZoneId.of("Europe/Zurich")

    private val DATE_FMT      = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val DATETIME_FMT  = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val TIME_FMT      = DateTimeFormatter.ofPattern("HH:mm")

    fun today(): LocalDate = LocalDate.now(ZONE)

    fun formatDate(date: LocalDate): String = date.format(DATE_FMT)

    fun formatDateTime(utcString: String): String {
        return try {
            val zdt = parseUtc(utcString)
            zdt.format(DATETIME_FMT)
        } catch (e: Exception) {
            utcString
        }
    }

    fun formatTime(utcString: String): String {
        return try {
            val zdt = parseUtc(utcString)
            zdt.format(TIME_FMT)
        } catch (e: Exception) {
            utcString
        }
    }

    fun zoneLabel(): String {
        val offset = ZonedDateTime.now(ZONE).offset
        return "UTC$offset"
    }

    private fun parseUtc(s: String): ZonedDateTime {
        // Handle formats: "2026-04-06 14:35" or "2026-04-06T14:35:00Z" or "2026-04-06T14:35:00.000Z"
        val cleaned = s.replace("T", " ").replace("Z", "").trimEnd('0').trimEnd('.')
        val base = cleaned.substring(0, minOf(16, cleaned.length))
        val local = java.time.LocalDateTime.parse(base,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        return local.atZone(ZoneId.of("UTC")).withZoneSameInstant(ZONE)
    }
}
