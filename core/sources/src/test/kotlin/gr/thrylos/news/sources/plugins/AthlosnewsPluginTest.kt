package gr.thrylos.news.sources.plugins

import gr.thrylos.news.model.ArticleStub
import gr.thrylos.news.model.ContentBlock
import gr.thrylos.news.sources.discovery.RssDiscovery
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
 * athlosnews.gr declares a real WordPress category RSS feed
 * (/category/omada/olympiakos/feed/) — our first guess had used
 * .../big-5/olympiakos/, a category slug that doesn't exist. Since we only
 * captured the category *page* (not the feed body), discovery itself is
 * covered generically by RssDiscoveryTest; this test focuses on extraction
 * against the real article snapshot.
 */
class AthlosnewsPluginTest {

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

    private fun shippedPlugin() = PluginTestSupport.shippedPlugin("athlosnews.json")

    @Test
    fun `discovery url is a real category feed, not a guessed path`() {
        val plugin = shippedPlugin()
        assertTrue(plugin.discovery.url.endsWith("/category/omada/olympiakos/feed/"))
        assertEquals(gr.thrylos.news.sources.plugin.DiscoveryType.RSS, plugin.discovery.type)
    }

    @Test
    fun `extracts a real athlosnews article cleanly`() {
        server.enqueue(MockResponse().setBody(Fixtures.read("athlosnews-article.html")))
        val plugin = shippedPlugin()
        val url = server.url("/1043115/oristike-i-3i-kai-4i-agonistiki-tis-super-league/").toString()

        val article = ArticleExtractor(HttpFetcher()).extract(plugin, ArticleStub(plugin.id, url, "(stub)"))

        assertFalse(article.usedFallbackExtraction)
        assertEquals("Ορίστηκε η 3η και 4η αγωνιστική της Super League", article.title)
        assertEquals("ΑΘΛΟΣ NEWSROOM", article.author)
        assertEquals(Instant.parse("2026-08-24T15:44:16Z").toEpochMilli(), article.publishedAt)
        assertTrue(article.leadImageUrl!!.contains("Carmo-El-Kaabi"), "unexpected lead image: ${article.leadImageUrl}")

        val paragraphs = article.content.filterIsInstance<ContentBlock.Paragraph>()
        assertTrue(paragraphs.size >= 4, "expected the article body, got ${paragraphs.size} paragraphs")
        val text = paragraphs.joinToString(" ") { it.text }
        assertTrue(text.contains("Ορίστηκαν τα παιχνίδια"))
    }
}
