package gr.thrylos.news.sources.plugins

import gr.thrylos.news.model.ContentBlock
import gr.thrylos.news.sources.discovery.RssDiscovery
import gr.thrylos.news.sources.extract.YouTubeVideoExtractor
import gr.thrylos.news.sources.http.HttpFetcher
import gr.thrylos.news.sources.plugin.SourceKind
import gr.thrylos.news.sources.testutil.Fixtures
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Verifies the shipped RedNews YouTube-channel plugin. */
class RednewsYoutubePluginTest {

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

    private fun shippedPlugin() = PluginTestSupport.shippedPlugin("rednews-youtube.json")

    @Test
    fun `ships as a youtube-kind plugin pointed at the channel's Atom feed by channel_id`() {
        val plugin = shippedPlugin()

        assertEquals(SourceKind.YOUTUBE, plugin.kind)
        assertEquals("RedNews", plugin.name)
        assertTrue(
            plugin.discovery.url.startsWith("https://www.youtube.com/feeds/videos.xml?channel_id=UC"),
            "expected a channel_id feed URL, got: ${plugin.discovery.url}",
        )
    }

    @Test
    fun `discovers and builds video articles from the channel feed, with no page fetch`() {
        server.enqueue(MockResponse().setBody(Fixtures.read("sample-youtube-feed.xml")))
        val plugin = shippedPlugin().let { it.copy(discovery = it.discovery.copy(url = server.url("/feed").toString())) }

        val stubs = RssDiscovery().discover(plugin, HttpFetcher())
        assertEquals(1, stubs.size)

        val article = YouTubeVideoExtractor.extract(plugin, stubs[0])

        // The display source is the plugin's real channel name, never the @handle.
        assertEquals("RedNews", article.sourceName)
        assertEquals("Ολυμπιακός: highlights τελευταίου αγώνα", article.title)
        assertEquals("https://i.ytimg.com/vi/abc12345678/hqdefault.jpg", article.leadImageUrl)

        val video = article.content.filterIsInstance<ContentBlock.Video>().single()
        assertEquals("https://www.youtube.com/embed/abc12345678", video.url)
    }
}
