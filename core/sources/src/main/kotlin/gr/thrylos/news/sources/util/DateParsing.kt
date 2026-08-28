package gr.thrylos.news.sources.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.SignStyle
import java.time.format.TextStyle
import java.time.temporal.ChronoField
import java.util.Locale

private val GREEK: Locale = Locale.Builder().setLanguage("el").setRegion("GR").build()

/** Real RSS feeds routinely bend the RFC-822 spec [DateTimeFormatter.RFC_1123_DATE_TIME]
 *  enforces strictly — a single-digit day ("5 Aug" instead of "05 Aug"), a lowercase
 *  month, or stray extra whitespace are all common and otherwise silently drop the
 *  published date (falling back to "when we happened to sync" instead). */
private val LENIENT_RFC_1123 = DateTimeFormatterBuilder()
    .parseCaseInsensitive()
    .parseLenient()
    .optionalStart().appendPattern("EEE,").appendLiteral(' ').optionalEnd()
    .appendValue(ChronoField.DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
    .appendLiteral(' ')
    .appendText(ChronoField.MONTH_OF_YEAR, TextStyle.SHORT)
    .appendLiteral(' ')
    .appendValue(ChronoField.YEAR, 4)
    .appendLiteral(' ')
    .appendValue(ChronoField.HOUR_OF_DAY, 2)
    .appendLiteral(':')
    .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
    .optionalStart().appendLiteral(':').appendValue(ChronoField.SECOND_OF_MINUTE, 2).optionalEnd()
    .appendLiteral(' ')
    .appendOffset("+HHMM", "GMT")
    .toFormatter(Locale.ENGLISH)

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
        val trimmed = text.trim().replace(Regex("\\s+"), " ")
        val formatters = buildList {
            // Every bundled source is a Greek-language site, so a custom pattern is
            // always parsing Greek text — matters for patterns with textual month/day
            // names ("Παρασκευή, 28 Αυγούστου"), which the JVM's default locale (not
            // necessarily Greek, e.g. in CI or on a non-Greek device) would fail to
            // recognize even though the pattern itself is otherwise correct.
            if (pattern != null) runCatching { DateTimeFormatter.ofPattern(pattern, GREEK) }.getOrNull()?.let { add(it) }
            add(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            add(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"))
            add(DateTimeFormatter.RFC_1123_DATE_TIME)
            add(LENIENT_RFC_1123)
            add(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            add(DateTimeFormatter.ISO_LOCAL_DATE)
        }
        for (fmt in formatters) {
            val parsed = runCatching { fmt.parse(trimmed) }.getOrNull() ?: continue
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
