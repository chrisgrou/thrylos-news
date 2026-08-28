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

/** Verifies the shipped rednews plugin against real page snapshots. */
class RednewsPluginTest {

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

    private fun shippedPlugin() = PluginTestSupport.shippedPlugin("rednews.json")

    @Test
    fun `discovers article stubs from the real latest-news page`() {
        server.enqueue(MockResponse().setBody(Fixtures.read("rednews-list.html")))
        val plugin = shippedPlugin().let { it.copy(discovery = it.discovery.copy(url = server.url("/latest-news/").toString())) }

        val stubs = HtmlListDiscovery().discover(plugin, HttpFetcher())

        assertEquals(40, stubs.size) // plugin caps discovery.maxItems at 40; the page itself has 45
        val target = stubs.firstOrNull { "protasi-9-ek-evro-stin-betis-gia-gkarsia" in it.url }
        assertTrue(target != null, "expected to find the Garcia transfer article, got: ${stubs.map { it.url }}")
        assertEquals("Πρόταση 9 εκ. ευρώ στην Μπέτις για Γκαρσία", target!!.title)
    }

    @Test
    fun `extracts a real rednews article cleanly`() {
        server.enqueue(MockResponse().setBody(Fixtures.read("rednews-article.html")))
        val plugin = shippedPlugin()
        val url = server.url("/protasi-9-ek-evro-stin-betis-gia-gkarsia/").toString()

        val article = ArticleExtractor(HttpFetcher()).extract(plugin, ArticleStub(plugin.id, url, "(stub)"))

        assertFalse(article.usedFallbackExtraction)
        assertEquals("Πρόταση 9 εκ. ευρώ στην Μπέτις για Γκαρσία", article.title)
        assertEquals("REDNEWS Team", article.author)
        assertEquals(Instant.parse("2026-08-28T15:36:56Z").toEpochMilli(), article.publishedAt)
        assertTrue(article.leadImageUrl!!.contains("imago1081998430"), "unexpected lead image: ${article.leadImageUrl}")

        val paragraphs = article.content.filterIsInstance<ContentBlock.Paragraph>()
        assertTrue(paragraphs.size >= 10, "expected the full article body, got ${paragraphs.size} paragraphs")
        val text = paragraphs.joinToString(" ") { it.text }
        assertTrue(text.contains("Πάμπλο Γκαρσία"), "expected article text, got: $text")
        assertFalse(text.contains("Contents"), "leaked the table-of-contents block")
    }
}
