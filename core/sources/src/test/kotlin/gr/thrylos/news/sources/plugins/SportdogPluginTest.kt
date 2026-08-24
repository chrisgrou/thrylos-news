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

/** Verifies the shipped sportdog plugin against real page snapshots. sportdog.gr has
 *  no RSS feed, so discovery is html-list against the team page. */
class SportdogPluginTest {

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

    private fun shippedPlugin() = PluginTestSupport.shippedPlugin("sportdog.json")

    @Test
    fun `discovers article stubs from the real team page`() {
        server.enqueue(MockResponse().setBody(Fixtures.read("sportdog-list.html")))
        val plugin = shippedPlugin().let { it.copy(discovery = it.discovery.copy(url = server.url("/teams/olympiakos").toString())) }

        val stubs = HtmlListDiscovery().discover(plugin, HttpFetcher())

        assertEquals(10, stubs.size)
        val target = stubs.firstOrNull { "olympiakos-poion-paikth-thelei-an-xalasei-toy-bargkas" in it.url }
        assertTrue(target != null, "expected to find the Vargas article, got: ${stubs.map { it.url }}")
        assertEquals("Ολυμπιακός: Ποιον παίκτη θέλει αν χαλάσει του Βάργκας!", target!!.title)
        assertTrue(target.imageUrl!!.contains("vargas-ap59215"), "unexpected image: ${target.imageUrl}")
    }

    @Test
    fun `extracts a real article cleanly`() {
        server.enqueue(MockResponse().setBody(Fixtures.read("sportdog-article.html")))
        val plugin = shippedPlugin()
        val url = server.url("/podosfairo/superleague/963476/olympiakos-poion-paikth-thelei-an-xalasei-toy-bargkas").toString()

        val article = ArticleExtractor(HttpFetcher()).extract(plugin, ArticleStub(plugin.id, url, "(stub)"))

        assertFalse(article.usedFallbackExtraction)
        assertEquals("Ολυμπιακός: Ποιον παίκτη θέλει αν χαλάσει του Βάργκας!", article.title)
        assertEquals("του Παναγιώτη Νικολάου", article.author)
        assertEquals(Instant.parse("2026-08-24T17:01:00Z").toEpochMilli(), article.publishedAt)
        assertTrue(article.leadImageUrl!!.contains("vargas-ap59215"), "unexpected lead image: ${article.leadImageUrl}")

        val paragraphs = article.content.filterIsInstance<ContentBlock.Paragraph>()
        assertTrue(paragraphs.isNotEmpty(), "expected the article body")
        val text = paragraphs.joinToString(" ") { it.text }
        assertTrue(text.contains("Μπράιαν Χιλ"))

        // Regression guard: the tag-pill row sits inside .article-body and would
        // otherwise leak into the extracted content as one run-on sentence. Each
        // individual name can legitimately appear in the prose too (Vargas is the
        // article's subject), so check the concatenated pill sequence instead.
        assertFalse(text.contains("Ρούμπεν Βάργκας Μπράιαν Χιλ Χοσέ Λουίς Μεντιλίμπαρ Τότεναμ"), "leaked the tag-pill row")
    }
}
