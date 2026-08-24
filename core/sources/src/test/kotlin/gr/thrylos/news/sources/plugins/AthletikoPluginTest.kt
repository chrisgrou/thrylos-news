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

/** Verifies the shipped athletiko plugin against real page snapshots. athletiko.gr
 * only declares a site-wide feed (no per-team one), so discovery is html-list. */
class AthletikoPluginTest {

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

    private fun shippedPlugin() = PluginTestSupport.shippedPlugin("athletiko.json")

    @Test
    fun `discovers article stubs from the real team page`() {
        server.enqueue(MockResponse().setBody(Fixtures.read("athletiko-list.html")))
        val plugin = shippedPlugin().let { it.copy(discovery = it.discovery.copy(url = server.url("/olympiakos").toString())) }

        val stubs = HtmlListDiscovery().discover(plugin, HttpFetcher())

        assertEquals(16, stubs.size)
        val target = stubs.firstOrNull { "vargkas-poyerta-kai-dexi-mpak-video-97368" in it.url }
        assertTrue(target != null, "expected to find the Vargas/Puerta article, got: ${stubs.map { it.url }}")
        assertEquals("«Βάργκας, Πουέρτα και δεξί μπακ στο προσκήνιο για Ολυμπιακό» (video)", target!!.title)
    }

    @Test
    fun `extracts a real athletiko article cleanly`() {
        server.enqueue(MockResponse().setBody(Fixtures.read("athletiko-article.html")))
        val plugin = shippedPlugin()
        val url = server.url("/vargkas-poyerta-kai-dexi-mpak-video-97368").toString()

        val article = ArticleExtractor(HttpFetcher()).extract(plugin, ArticleStub(plugin.id, url, "(stub)"))

        assertFalse(article.usedFallbackExtraction)
        assertEquals("«Βάργκας, Πουέρτα και δεξί μπακ στο προσκήνιο για Ολυμπιακό» (video)", article.title)
        assertEquals(Instant.parse("2026-08-24T15:37:00Z").toEpochMilli(), article.publishedAt)
        assertTrue(article.leadImageUrl!!.contains("imago1077063325"), "unexpected lead image: ${article.leadImageUrl}")

        val paragraphs = article.content.filterIsInstance<ContentBlock.Paragraph>()
        assertTrue(paragraphs.isNotEmpty(), "expected at least one paragraph")
        val text = paragraphs.joinToString(" ") { it.text }
        assertTrue(text.contains("Ο ρεπόρτερ των Πειραιωτών ανέλυσε"))
        assertFalse(text.contains("Μην χάνεις είδηση"), "leaked the 'follow us on Google' promo box")
    }
}
