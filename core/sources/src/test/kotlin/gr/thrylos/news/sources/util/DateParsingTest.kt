package gr.thrylos.news.sources.util

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DateParsingTest {

    @Test
    fun `parses strict RFC-822 dates`() {
        assertNotNull(DateParsing.parse("Mon, 24 Aug 2026 08:15:00 +0300"))
    }

    @Test
    fun `parses RFC-822 dates with a single-digit day, a real-world feed quirk`() {
        assertNotNull(DateParsing.parse("Tue, 4 Aug 2026 08:15:00 +0300"))
    }

    @Test
    fun `parses RFC-822 dates without seconds`() {
        assertNotNull(DateParsing.parse("Tue, 4 Aug 2026 08:15 +0300"))
    }

    @Test
    fun `parses ISO-8601 offset dates`() {
        assertNotNull(DateParsing.parse("2026-08-24T18:31:30+03:00"))
    }

    @Test
    fun `returns null for genuine garbage instead of throwing`() {
        assertNull(DateParsing.parse("not a date"))
    }
}
