package gr.thrylos.news.sources.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Parses a published-date string. Real sites publish dates in three shapes,
 * and all three show up across the bundled sources, so each is tried in
 * turn: with an explicit offset ("2026-08-24T18:31:30+03:00" or the
 * colon-less "+0300"), as a zoneless local timestamp ("24.08.2026-18:31"),
 * or as a bare date. Zoneless values are resolved against [zone], since a
 * site's local time is what it means.
 */
object DateParsing {
    fun parse(text: String, pattern: String? = null, zone: ZoneId = ZoneId.systemDefault()): Long? {
        val formatters = buildList {
            if (pattern != null) runCatching { DateTimeFormatter.ofPattern(pattern) }.getOrNull()?.let { add(it) }
            add(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            add(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"))
            add(DateTimeFormatter.RFC_1123_DATE_TIME)
            add(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            add(DateTimeFormatter.ISO_LOCAL_DATE)
        }
        for (fmt in formatters) {
            val parsed = runCatching { fmt.parse(text) }.getOrNull() ?: continue
            // Narrow from the most specific interpretation to the least, so a value
            // that really does carry an offset never gets re-interpreted as local.
            runCatching { OffsetDateTime.from(parsed).toInstant().toEpochMilli() }
                .getOrNull()?.let { return it }
            runCatching { LocalDateTime.from(parsed).atZone(zone).toInstant().toEpochMilli() }
                .getOrNull()?.let { return it }
            runCatching { LocalDate.from(parsed).atStartOfDay(zone).toInstant().toEpochMilli() }
                .getOrNull()?.let { return it }
        }
        return null
    }
}
