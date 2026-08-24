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
 * Verifies the *shipped* to10 plugin against real HTML captured from the live
 * site, so a regression in either the selectors or the extraction engine fails
 * the build. The fixtures are unmodified page snapshots — ads, related-article
 * widgets and all — which is exactly what the extractor has to cope with.
 */
class To10PluginTest {

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

    private fun shippedPlugin() = PluginTestSupport.shippedPlugin("to10.json")

    @Test
    fun `discovers article stubs from the real listing page`() {
        server.enqueue(MockResponse().setBody(Fixtures.read("to10-list.html")))
        val plugin = shippedPlugin().let { it.copy(discovery = it.discovery.copy(url = server.url("/team/olympiacos/").toString())) }

        val stubs = HtmlListDiscovery().discover(plugin, HttpFetcher())

        assertEquals(30, stubs.size, "maxItems=30 should cap the 31 items on the page")
        assertEquals("«Κοντά στον Ολυμπιακό ο Μέλε» (vid)", stubs[0].title)
        assertEquals(
            "https://www.to10.gr/podosfero/superleague/4309762/konta-ston-olybiako-o-mele/",
            stubs[0].url,
        )
        assertTrue(stubs[0].imageUrl!!.endsWith(".jpeg"), "expected a thumbnail, got ${stubs[0].imageUrl}")
        // The scoreboard widget links (/matchcenter/) sit outside the ItemList markup.
        assertTrue(stubs.none { "/matchcenter/" in it.url }, "matchcenter links must not be treated as articles")
    }

    @Test
    fun `extracts a real article cleanly`() {
        server.enqueue(MockResponse().setBody(Fixtures.read("to10-article.html")))
        val plugin = shippedPlugin()
        val url = server.url("/podosfero/superleague/4309762/konta-ston-olybiako-o-mele/").toString()

        val article = ArticleExtractor(HttpFetcher()).extract(plugin, ArticleStub(plugin.id, url, "(stub)"))

        assertFalse(article.usedFallbackExtraction, "the plugin's own selectors should have worked")
        assertEquals("«Κοντά στον Ολυμπιακό ο Μέλε» (vid)", article.title)
        assertEquals("Σαββας Λιαμιρας", article.author)
        assertEquals(
            Instant.parse("2026-08-24T15:31:30Z").toEpochMilli(),
            article.publishedAt,
            "article:published_time is 18:31:30+03:00",
        )
        assertTrue(
            article.leadImageUrl!!.endsWith("6f604b8f-0dae-49dc-8c39-deca423ef9ef.jpeg"),
            "unexpected lead image: ${article.leadImageUrl}",
        )

        val paragraphs = article.content.filterIsInstance<ContentBlock.Paragraph>()
        assertTrue(paragraphs.size >= 4, "expected the article body, got ${paragraphs.size} paragraphs")
        assertTrue(paragraphs.first().text.startsWith("Στην τελική ευθεία βρίσκεται η μεταγραφή"))

        val text = paragraphs.joinToString(" ") { it.text }
        listOf(
            "Ανακαλύψτε περισσότερα άρθρα", // .google-widget promo
            "TAGS",                          // tag list
            "Σχετικά Άρθρα",                 // .related-articles
            "MUST READ",                     // .articles-pop-now-must
        ).forEach { junk ->
            assertFalse(text.contains(junk, ignoreCase = true), "leaked non-article content: '$junk'")
        }
    }
}
