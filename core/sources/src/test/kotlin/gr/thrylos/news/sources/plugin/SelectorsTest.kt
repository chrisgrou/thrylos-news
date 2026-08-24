package gr.thrylos.news.sources.plugin

import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SelectorsTest {

    @Test
    fun `reads text via css selector`() {
        val doc = Jsoup.parse("<h1 class='t'>Γεια σου κόσμε</h1>")
        assertEquals("Γεια σου κόσμε", doc.textOf("h1.t"))
    }

    @Test
    fun `reads attribute when suffixed with at-attr`() {
        val doc = Jsoup.parse("<img src='/a.jpg'/>")
        assertEquals("/a.jpg", doc.textOf("img@src"))
    }

    @Test
    fun `returns null for missing selector`() {
        val doc = Jsoup.parse("<div></div>")
        assertNull(doc.textOf("h1.missing"))
    }

    @Test
    fun `returns null for null raw selector`() {
        val doc = Jsoup.parse("<div></div>")
        assertNull(doc.textOf(null))
    }

    @Test
    fun `at-ownText reads only the matched element's own text, skipping a child element's text`() {
        // Real-world pattern: a label element followed by the actual value as a
        // trailing plain-text sibling, e.g. sport-fm.gr's post-meta-date markup.
        val doc = Jsoup.parse(
            "<div class='meta'><span class='label'>ΗΜΕΡΟΜΗΝΙΑ:</span> 23/08/2026, 22:12</div>",
        )
        assertEquals("23/08/2026, 22:12", doc.textOf(".meta@ownText"))
    }
}
