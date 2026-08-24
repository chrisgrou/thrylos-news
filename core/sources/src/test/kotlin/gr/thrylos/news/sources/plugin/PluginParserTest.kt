package gr.thrylos.news.sources.plugin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PluginParserTest {

    private val validJson = """
        {
          "schemaVersion": 1,
          "id": "demo-sports",
          "name": "Demo Sports",
          "homepage": "https://demo-sports.example",
          "discovery": { "type": "rss", "url": "https://demo-sports.example/rss" },
          "article": { "title": "h1.article-title", "content": "div.article-body" }
        }
    """.trimIndent()

    @Test
    fun `parses a valid plugin`() {
        val result = PluginParser.parse(validJson)
        assertTrue(result is PluginParseResult.Success)
        val plugin = (result as PluginParseResult.Success).plugin
        assertEquals("demo-sports", plugin.id)
        assertEquals(DiscoveryType.RSS, plugin.discovery.type)
        assertEquals(FallbackMode.READABILITY, plugin.fallback)
    }

    @Test
    fun `rejects plugin from a newer schema version`() {
        val json = validJson.replace("\"schemaVersion\": 1", "\"schemaVersion\": 99")
        val result = PluginParser.parse(json)
        assertTrue(result is PluginParseResult.Failure)
    }

    @Test
    fun `rejects html-list plugin missing listSelectors`() {
        val json = validJson.replace("\"type\": \"rss\"", "\"type\": \"html-list\"")
        val result = PluginParser.parse(json)
        assertTrue(result is PluginParseResult.Failure)
        assertTrue((result as PluginParseResult.Failure).errors.any { it.contains("listSelectors") })
    }

    @Test
    fun `rejects malformed json`() {
        val result = PluginParser.parse("{ not json")
        assertTrue(result is PluginParseResult.Failure)
    }

    @Test
    fun `rejects blank id`() {
        val json = validJson.replace("\"id\": \"demo-sports\"", "\"id\": \"\"")
        val result = PluginParser.parse(json)
        assertTrue(result is PluginParseResult.Failure)
    }
}
