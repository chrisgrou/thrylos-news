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
 * Verifies both shipped sportal plugins (football and basketball are the same
 * WordPress theme, just different team pages) against real page snapshots.
 * sportal.gr only declares a site-wide feed, so both use html-list discovery.
 */
class SportalPluginTest {

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

    @Test
    fun `discovers article stubs from the real football team page`() {
        server.enqueue(MockResponse().setBody(Fixtures.read("sportal-football-list.html")))
        val plugin = PluginTestSupport.shippedPlugin("sportal-football.json")
            .let { it.copy(discovery = it.discovery.copy(url = server.url("/podosfairo/olympiakos-554").toString())) }

        val stubs = HtmlListDiscovery().discover(plugin, HttpFetcher())

        assertEquals(20, stubs.size)
        val target = stubs.firstOrNull { "oi-danoi-vlepoun-ton-mele-mia-anasa-apo-ton-olybiako" in it.url }
        assertTrue(target != null, "expected to find the Mæhle article, got: ${stubs.map { it.url }}")
        assertEquals("Οι Δανοί «βλέπουν» τον Μέλε μία ανάσα από τον Ολυμπιακό!", target!!.title)
        assertTrue(target.publishedAt != null, "list page <time datetime> should give a stub date")
    }

    @Test
    fun `extracts a real football article cleanly`() {
        server.enqueue(MockResponse().setBody(Fixtures.read("sportal-football-article.html")))
        val plugin = PluginTestSupport.shippedPlugin("sportal-football.json")
        val url = server.url("/podosfairo/article/oi-danoi-vlepoun-ton-mele-mia-anasa-apo-ton-olybiako").toString()

        val article = ArticleExtractor(HttpFetcher()).extract(plugin, ArticleStub(plugin.id, url, "(stub)"))

        assertFalse(article.usedFallbackExtraction)
        assertEquals("Οι Δανοί «βλέπουν» τον Μέλε μία ανάσα από τον Ολυμπιακό!", article.title)
        assertEquals("Αλέξης Βιρβίλης", article.author)
        assertEquals(Instant.parse("2026-08-24T14:41:32Z").toEpochMilli(), article.publishedAt)
        assertTrue(article.leadImageUrl!!.contains("mele-1"), "unexpected lead image: ${article.leadImageUrl}")

        val paragraphs = article.content.filterIsInstance<ContentBlock.Paragraph>()
        assertTrue(paragraphs.size >= 4, "expected the article body, got ${paragraphs.size} paragraphs")
        val text = paragraphs.joinToString(" ") { it.text }
        assertTrue(text.contains("Ο Ολυμπιακός ανεβάζει στροφές"))
        assertFalse(text.contains("Share on"), "leaked the share-button labels")
    }

    @Test
    fun `extracts a real basketball article cleanly`() {
        server.enqueue(MockResponse().setBody(Fixtures.read("sportal-basket-article.html")))
        val plugin = PluginTestSupport.shippedPlugin("sportal-basket.json")
        val url = server.url("/basket/article/ti-tha-ginei-me-tis-omades-tis-euroleague-sto-nba2k-2027-ti-apofasise-telika-i-2k").toString()

        val article = ArticleExtractor(HttpFetcher()).extract(plugin, ArticleStub(plugin.id, url, "(stub)"))

        assertFalse(article.usedFallbackExtraction)
        assertEquals("Τι θα γίνει με τις ομάδες της EuroLeague στο NBA2K 2027 – Τι αποφάσισε τελικά η 2K", article.title)
        assertEquals("Παναγιώτης Νομικός", article.author)
        assertEquals(Instant.parse("2026-08-24T14:53:41Z").toEpochMilli(), article.publishedAt)

        val paragraphs = article.content.filterIsInstance<ContentBlock.Paragraph>()
        assertTrue(paragraphs.isNotEmpty())
        assertTrue(paragraphs.joinToString(" ") { it.text }.contains("Παρά την έντονη φημολογία"))
    }

    @Test
    fun `basketball plugin homepage url has exactly one 'b' in olympiakos slug`() {
        // Regression guard: the id segment is case-sensitive and easy to mistype
        // (it was originally copied as "...MbbG5sh" with a double b, which 404s).
        val plugin = PluginTestSupport.shippedPlugin("sportal-basket.json")
        assertTrue(plugin.discovery.url.endsWith("olympiakos-1kPxqw9m37heUsQmMbG5sh"))
    }
}
