package gr.thrylos.news.sources.plugins

import gr.thrylos.news.model.ArticleStub
import gr.thrylos.news.model.ContentBlock
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

/**
 * Verifies extraction against a real sport-fm.gr article snapshot. Discovery
 * isn't covered here — sport-fm.gr declares a real Atom feed
 * (`<link rel=alternate type=application/atom+xml href=".../tag/olympiakos.feed">`)
 * but we only captured the tag *page*, not the feed body itself, so the RSS
 * parsing path for this source is unverified until tested on-device.
 */
class SportFmPluginTest {

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

    private fun shippedPlugin() = PluginTestSupport.shippedPlugin("sport-fm.json")

    @Test
    fun `extracts a real sport-fm article cleanly`() {
        server.enqueue(MockResponse().setBody(Fixtures.read("sportfm-article.html")))
        val plugin = shippedPlugin()
        val url = server.url("/article/podosfairo/superleague1/olumpiakos-koda-stin-apoktisi").toString()

        val article = ArticleExtractor(HttpFetcher()).extract(plugin, ArticleStub(plugin.id, url, "(stub)"))

        assertFalse(article.usedFallbackExtraction)
        assertEquals("«Κοντά στον Ολυμπιακό ο Μέλε»", article.title)
        assertTrue(
            article.leadImageUrl!!.contains("maehle_173126.jpg"),
            "unexpected lead image: ${article.leadImageUrl}",
        )

        val paragraphs = article.content.filterIsInstance<ContentBlock.Paragraph>()
        assertTrue(paragraphs.isNotEmpty(), "expected at least one paragraph")
        val text = paragraphs.joinToString(" ") { it.text }
        assertTrue(text.contains("Κοντά στην απόκτηση του"), "missing real article text: $text")
        assertFalse(text.contains("Google News"), "leaked the 'follow us' promo paragraph")

        // The real page wraps the whole body in one <p> with <br><br>-separated
        // paragraphs (plus stray ad/video divs mid-<p>, which Jsoup's HTML5 parser
        // implicitly closes the <p> in front of) — real paragraph breaks, not a
        // single run-on block of text.
        assertTrue(paragraphs.size > 1, "expected multiple paragraphs, got one run-on block: $text")
        assertTrue(
            paragraphs.any { it.text.startsWith("Όπως αναφέρει το δημοσίευμα") },
            "expected a distinct paragraph starting at the second <br><br>-separated run",
        )
    }
}
