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

/** Verifies the shipped redaroume plugin against real page snapshots. */
class RedaroumePluginTest {

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

    private fun shippedPlugin() = PluginTestSupport.shippedPlugin("redaroume.json")

    @Test
    fun `discovers article stubs from the real category page`() {
        server.enqueue(MockResponse().setBody(Fixtures.read("redaroume-list.html")))
        val plugin = shippedPlugin().let { it.copy(discovery = it.discovery.copy(url = server.url("/category/teleytaia-nea/").toString())) }

        val stubs = HtmlListDiscovery().discover(plugin, HttpFetcher())

        assertEquals(10, stubs.size)
        val target = stubs.firstOrNull { "xypnisan-mnimes-fortoyni-fetfa-apenanti-se-milan-marseig" in it.url }
        assertTrue(target != null, "expected to find the Fortounis/Fetfatzidis article, got: ${stubs.map { it.url }}")
        assertEquals("Ξύπνησαν μνήμες από Φορτούνη και «Φέτφα» απέναντι σε Μίλαν και Μαρσέιγ! [Videos]", target!!.title)
    }

    @Test
    fun `extracts a real redaroume article cleanly`() {
        server.enqueue(MockResponse().setBody(Fixtures.read("redaroume-article.html")))
        val plugin = shippedPlugin()
        val url = server.url("/xypnisan-mnimes-fortoyni-fetfa-apenanti-se-milan-marseig/").toString()

        val article = ArticleExtractor(HttpFetcher()).extract(plugin, ArticleStub(plugin.id, url, "(stub)"))

        assertFalse(article.usedFallbackExtraction)
        assertEquals("Ξύπνησαν μνήμες από Φορτούνη και «Φέτφα» απέναντι σε Μίλαν και Μαρσέιγ! [Videos]", article.title)
        assertEquals("Κώστας Παλαιολόγος", article.author)
        assertEquals(Instant.parse("2026-08-28T16:02:28Z").toEpochMilli(), article.publishedAt)
        assertTrue(article.leadImageUrl!!.contains("4653437-full"), "unexpected lead image: ${article.leadImageUrl}")

        val paragraphs = article.content.filterIsInstance<ContentBlock.Paragraph>()
        assertTrue(paragraphs.isNotEmpty(), "expected at least one paragraph")
        val text = paragraphs.joinToString(" ") { it.text }
        assertTrue(text.contains("Ολυμπιακό"), "expected article text, got: $text")
        assertFalse(text.contains("EUROPA LEAGUE"), "leaked the tags block")
    }
}
