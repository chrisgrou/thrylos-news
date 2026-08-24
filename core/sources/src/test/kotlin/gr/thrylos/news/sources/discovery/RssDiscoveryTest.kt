package gr.thrylos.news.sources.discovery

import gr.thrylos.news.sources.http.HttpFetcher
import gr.thrylos.news.sources.plugin.Discovery
import gr.thrylos.news.sources.plugin.DiscoveryType
import gr.thrylos.news.sources.plugin.ArticleSelectors
import gr.thrylos.news.sources.plugin.SourcePlugin
import gr.thrylos.news.sources.testutil.Fixtures
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RssDiscoveryTest {

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
    fun `parses rss items into stubs`() {
        server.enqueue(MockResponse().setBody(Fixtures.read("sample-rss.xml")))
        val plugin = SourcePlugin(
            schemaVersion = 1, id = "demo", name = "Demo", homepage = server.url("/").toString(),
            discovery = Discovery(DiscoveryType.RSS, server.url("/rss").toString()),
            article = ArticleSelectors(title = "h1", content = "div"),
        )

        val stubs = RssDiscovery().discover(plugin, HttpFetcher())

        assertEquals(3, stubs.size)
        assertEquals("Ολυμπιακός: το πλάνο του προπονητή για τη νέα σεζόν", stubs[0].title)
        assertTrue(stubs[0].publishedAt != null)
        assertEquals("https://demo-sports.example/img/plano.jpg", stubs[0].imageUrl)
    }

    @Test
    fun `parses feeds that declare a DOCTYPE for HTML entities (real WordPress RSS quirk)`() {
        server.enqueue(MockResponse().setBody(Fixtures.read("sample-rss-with-doctype.xml")))
        val plugin = SourcePlugin(
            schemaVersion = 1, id = "demo", name = "Demo", homepage = server.url("/").toString(),
            discovery = Discovery(DiscoveryType.RSS, server.url("/rss").toString()),
            article = ArticleSelectors(title = "h1", content = "div"),
        )

        val stubs = RssDiscovery().discover(plugin, HttpFetcher())

        assertEquals(1, stubs.size)
        assertTrue(stubs[0].title.contains("ανακοίνωση"))
    }
}
