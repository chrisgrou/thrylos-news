package gr.thrylos.news.sources.url

import gr.thrylos.news.sources.plugin.UrlRules
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UrlNormalizerTest {

    @Test
    fun `resolves relative urls against base`() {
        assertEquals(
            "https://example.com/football/a",
            UrlNormalizer.resolve("https://example.com/team/olympiacos", "/football/a"),
        )
    }

    @Test
    fun `strips configured query params and trailing slash`() {
        val rules = UrlRules(stripQueryParams = listOf("utm_source", "utm_medium"))
        val result = UrlNormalizer.canonicalize(
            "https://Example.com/a/b/?utm_source=rss&utm_medium=feed&id=42",
            rules,
        )
        assertEquals("https://example.com/a/b?id=42", result)
    }

    @Test
    fun `deny rule blocks even without allow rules`() {
        val rules = UrlRules(deny = listOf("/live/"))
        assertFalse(UrlNormalizer.isAllowed("https://example.com/live/x", rules))
        assertTrue(UrlNormalizer.isAllowed("https://example.com/football/x", rules))
    }

    @Test
    fun `allow rule restricts to matching paths`() {
        val rules = UrlRules(allow = listOf("^https://example\\.com/football/"))
        assertTrue(UrlNormalizer.isAllowed("https://example.com/football/x", rules))
        assertFalse(UrlNormalizer.isAllowed("https://example.com/basket/x", rules))
    }
}
