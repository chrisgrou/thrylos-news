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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Verifies the shipped gavros plugin against real page snapshots. */
class GavrosPluginTest {

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

    private fun shippedPlugin() = PluginTestSupport.shippedPlugin("gavros.json")

    @Test
    fun `discovers article stubs from the real timeline page`() {
        server.enqueue(MockResponse().setBody(Fixtures.read("gavros-list.html")))
        val plugin = shippedPlugin().let { it.copy(discovery = it.discovery.copy(url = server.url("/timeline/").toString())) }

        val stubs = HtmlListDiscovery().discover(plugin, HttpFetcher())

        assertEquals(20, stubs.size)
        val target = stubs.firstOrNull { "torense-h-omada" in it.url }
        assertTrue(target != null, "expected to find the Torrense article, got: ${stubs.map { it.url }}")
    }

    @Test
    fun `extracts a real gavros article cleanly`() {
        server.enqueue(MockResponse().setBody(Fixtures.read("gavros-article.html")))
        val plugin = shippedPlugin()
        val url = server.url("/article/podosfairo/torense-i-omada-tis-v-portogalias").toString()

        val article = ArticleExtractor(HttpFetcher()).extract(plugin, ArticleStub(plugin.id, url, "(stub)"))

        assertFalse(article.usedFallbackExtraction)
        assertTrue(article.title.startsWith("Τορένσε"), "unexpected title: ${article.title}")
        // Zoneless "Παρασκευή, 28 Αυγούστου 2026 - 20:08" — just confirm it parses at
        // all (Greek weekday/month names via DateParsing's el-GR formatter); the
        // resulting instant depends on the running JVM's default zone, so not asserted.
        assertNotNull(article.publishedAt, "expected the Greek-formatted date to parse")
        assertTrue(article.leadImageUrl!!.contains("othd1057"), "unexpected lead image: ${article.leadImageUrl}")

        val paragraphs = article.content.filterIsInstance<ContentBlock.Paragraph>()
        assertTrue(paragraphs.isNotEmpty(), "expected at least one paragraph")
        val text = paragraphs.joinToString(" ") { it.text }
        assertTrue(text.contains("Τορένσε"), "expected article text, got: $text")
        assertFalse(text.contains("Google News"), "leaked the Google News promo block")
        assertFalse(text.contains("TAGS"), "leaked the tags block")
    }
}
