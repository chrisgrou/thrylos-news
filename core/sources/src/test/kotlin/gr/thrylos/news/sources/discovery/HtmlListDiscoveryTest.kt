package gr.thrylos.news.sources.discovery

import gr.thrylos.news.sources.http.HttpFetcher
import gr.thrylos.news.sources.plugin.ArticleSelectors
import gr.thrylos.news.sources.plugin.Discovery
import gr.thrylos.news.sources.plugin.DiscoveryType
import gr.thrylos.news.sources.plugin.ListSelectors
import gr.thrylos.news.sources.plugin.SourcePlugin
import gr.thrylos.news.sources.testutil.Fixtures
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HtmlListDiscoveryTest {

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
    fun `scrapes teaser cards into stubs with resolved links`() {
        server.enqueue(MockResponse().setBody(Fixtures.read("sample-list.html")))
        val listUrl = server.url("/team/olympiacos").toString()
        val plugin = SourcePlugin(
            schemaVersion = 1, id = "demo", name = "Demo", homepage = listUrl,
            discovery = Discovery(DiscoveryType.HTML_LIST, listUrl),
            listSelectors = ListSelectors(item = "article.teaser", link = "a@href", title = "h3", image = "img@src", date = "time@datetime"),
            article = ArticleSelectors(title = "h1", content = "div"),
        )

        val stubs = HtmlListDiscovery().discover(plugin, HttpFetcher())

        assertEquals(2, stubs.size)
        assertTrue(stubs[0].url.startsWith(server.url("/").toString()))
        assertEquals("Ολυμπιακός: το πλάνο του προπονητή για τη νέα σεζόν", stubs[0].title)
    }
}
