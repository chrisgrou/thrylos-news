package gr.thrylos.news.sources.youtube

import gr.thrylos.news.sources.http.HttpFetcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class YouTubeChannelResolverTest {

    private lateinit var server: MockWebServer
    private lateinit var resolver: YouTubeChannelResolver

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        resolver = YouTubeChannelResolver(
            http = HttpFetcher(),
            resolveEndpoint = server.url("/resolve_url").toString(),
            feedUrl = { channelId -> server.url("/feeds/videos.xml?channel_id=$channelId").toString() },
        )
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    private fun resolveResponse(channelId: String) =
        """{"endpoint":{"browseEndpoint":{"browseId":"$channelId","params":"xyz"}}}"""

    private fun feedXml(channelName: String) = """
        <?xml version="1.0" encoding="UTF-8"?>
        <feed xmlns:yt="http://www.youtube.com/xml/schemas/2015" xmlns="http://www.w3.org/2005/Atom">
          <title>$channelName</title>
          <entry><title>Some video title, not the channel's</title></entry>
        </feed>
    """.trimIndent()

    @Test
    fun `resolves a handle end to end via the resolve API then the channel feed`() {
        server.enqueue(MockResponse().setBody(resolveResponse("UCGiTb1kleEoNRKPPhwBUDCg")))
        server.enqueue(MockResponse().setBody(feedXml("RedNews")))

        val info = resolver.resolve("@REDSPORTS7")

        assertEquals("UCGiTb1kleEoNRKPPhwBUDCg", info.channelId)
        assertEquals("RedNews", info.name)
    }

    @Test
    fun `skips the resolve API entirely for a bare channel id or a channel URL`() {
        server.enqueue(MockResponse().setBody(feedXml("Some Channel")))

        val info = resolver.resolve("https://www.youtube.com/channel/UCGiTb1kleEoNRKPPhwBUDCg")

        assertEquals("UCGiTb1kleEoNRKPPhwBUDCg", info.channelId)
        // Exactly one request — the feed lookup — proves the resolve API was never
        // called at all, not just that this test happened to still pass.
        val recorded = server.takeRequest()
        assertTrue(recorded.path!!.contains("/feeds/videos.xml"))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `normalizes a bare handle, an @handle, and a raw channel id to the right URL`() {
        assertEquals("https://www.youtube.com/@REDSPORTS7", resolver.channelUrl("REDSPORTS7"))
        assertEquals("https://www.youtube.com/@REDSPORTS7", resolver.channelUrl("@REDSPORTS7"))
        assertEquals(
            "https://www.youtube.com/channel/UCGiTb1kleEoNRKPPhwBUDCg",
            resolver.channelUrl("UCGiTb1kleEoNRKPPhwBUDCg"),
        )
        val fullUrl = "https://www.youtube.com/channel/UCGiTb1kleEoNRKPPhwBUDCg"
        assertEquals(fullUrl, resolver.channelUrl(fullUrl))
    }

    @Test
    fun `rejects a blank input up front, with no network call`() {
        val error = assertThrows(IllegalStateException::class.java) { resolver.resolve("   ") }
        assertTrue(error.message!!.contains("@handle"))
    }

    @Test
    fun `fails with the raw API response when the resolve API doesn't name a channel`() {
        server.enqueue(MockResponse().setBody("""{"endpoint":{"urlEndpoint":{"url":"https://example.com"}}}"""))

        val error = assertThrows(IllegalStateException::class.java) { resolver.resolve("@nobody") }

        assertTrue(error.message!!.contains("Δεν βρέθηκε κανάλι"))
        assertTrue(error.message!!.contains("urlEndpoint"))
    }

    @Test
    fun `extractBrowseId reads the browseId regardless of what else is in the response`() {
        assertEquals(
            "UCabc12345678901234567X",
            extractBrowseId("""{"endpoint":{"browseEndpoint":{"browseId":"UCabc12345678901234567X","canonicalBaseUrl":"/@x"}},"other":"noise"}"""),
        )
        assertNull(extractBrowseId("""{"endpoint":{}}"""))
        assertNull(extractBrowseId("not json"))
    }

    @Test
    fun `extractFeedTitle reads the feed's own title, not an entry's`() {
        assertEquals("RedNews", extractFeedTitle(feedXml("RedNews")))
        assertNull(extractFeedTitle("<not-xml"))
    }
}
