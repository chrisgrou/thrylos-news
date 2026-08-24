package gr.thrylos.news.sources.plugins

import gr.thrylos.news.model.ArticleStub
import gr.thrylos.news.model.ContentBlock
import gr.thrylos.news.sources.discovery.HtmlListDiscovery
import gr.thrylos.news.sources.extract.ArticleExtractor
import gr.thrylos.news.sources.http.HttpFetcher
import gr.thrylos.news.sources.testutil.Fixtures
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Verifies the shipped sport24 plugin against real page snapshots. Note:
 * sport24.gr returned HTTP 403 to the default request on-device — these
 * tests confirm the selectors are correct, not that the custom desktop
 * User-Agent in the plugin actually gets past whatever is blocking it.
 */
class Sport24PluginTest {

    private lateinit var server: MockWebServer

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    private fun shippedPlugin() = PluginTestSupport.shippedPlugin("sport24.json")

    @Test
    fun `discovers article stubs from the real listing page, excluding matchcenter`() {
        server.enqueue(MockResponse().setBody(Fixtures.read("sport24-list.html")))
        val plugin = shippedPlugin().let { it.copy(discovery = it.discovery.copy(url = server.url("/tag/olympiacos/").toString())) }

        val stubs = HtmlListDiscovery().discover(plugin, HttpFetcher())

        assertTrue(stubs.isNotEmpty())
        val target = stubs.firstOrNull { "olimpiakos-metagrafes-oi-peiraiotes" in it.url }
        assertTrue(target != null, "expected to find the Mæhle transfer article, got: ${stubs.map { it.url }}")
        assertEquals("Ο Ολυμπιακός πλησιάζει τον Μέλε, σύμφωνα με δημοσίευμα από την Δανία", target!!.title)
        assertTrue(stubs.none { "/matchcenter/" in it.url })
    }

    @Test
    fun `extracts a real sport24 article cleanly`() {
        server.enqueue(MockResponse().setBody(Fixtures.read("sport24-article.html")))
        val plugin = shippedPlugin()
        val url = server.url("/football/olimpiakos-metagrafes-oi-peiraiotes-plisiazoun-ton-mele-simfona-me-dimosievma-apo-tin-dania/").toString()

        val article = ArticleExtractor(HttpFetcher()).extract(plugin, ArticleStub(plugin.id, url, "(stub)"))

        assertFalse(article.usedFallbackExtraction)
        assertEquals(
            "Ολυμπιακός, μεταγραφές: Οι Πειραιώτες πλησιάζουν τον Μέλε, σύμφωνα με δημοσίευμα από την Δανία",
            article.title,
        )
        assertEquals("ΒΑΓΓΕΛΗΣ ΣΤΑΜΑΤΟΠΟΥΛΟΣ", article.author)
        assertEquals(Instant.parse("2026-08-24T15:00:45Z").toEpochMilli(), article.publishedAt)
        assertTrue(article.leadImageUrl!!.contains("joakim-maele"), "unexpected lead image: ${article.leadImageUrl}")

        val paragraphs = article.content.filterIsInstance<ContentBlock.Paragraph>()
        assertTrue(paragraphs.size >= 3, "expected the article body, got ${paragraphs.size} paragraphs")
        val text = paragraphs.joinToString(" ") { it.text }
        listOf("TAGS", "Ακολουθήστε στην Google", "Κάντε εγγραφή στο κανάλι").forEach { junk ->
            assertFalse(text.contains(junk, ignoreCase = true), "leaked non-article content: '$junk'")
        }
    }
}
