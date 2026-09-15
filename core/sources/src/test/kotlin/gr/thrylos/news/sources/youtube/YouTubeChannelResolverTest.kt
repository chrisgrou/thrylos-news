package gr.thrylos.news.sources.youtube

import gr.thrylos.news.sources.http.HttpFetcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class YouTubeChannelResolverTest {

    private lateinit var server: MockWebServer
    private lateinit var resolver: YouTubeChannelResolver

    /** A real channel page's server-rendered HTML carries far more than this, but the
     *  resolver only ever reads these two tags. */
    private fun channelPageHtml(channelId: String, name: String) = """
        <!DOCTYPE html>
        <html><head>
        <link rel="canonical" href="https://www.youtube.com/channel/$channelId">
        <meta property="og:title" content="$name">
        <title>$name - YouTube</title>
        </head><body></body></html>
    """.trimIndent()

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        resolver = YouTubeChannelResolver(HttpFetcher())
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `resolves channel id and real name from the canonical link and og title`() {
        server.enqueue(MockResponse().setBody(channelPageHtml("UCGiTb1kleEoNRKPPhwBUDCg", "RedNews")))

        val info = resolver.resolve(server.url("/@REDSPORTS7").toString())

        assertEquals("UCGiTb1kleEoNRKPPhwBUDCg", info.channelId)
        assertEquals("RedNews", info.name)
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
        val error = assertThrows(IllegalStateException::class.java) { resolver.channelUrl("   ") }
        org.junit.jupiter.api.Assertions.assertTrue(error.message!!.contains("@handle"))
    }

    @Test
    fun `fails with a clear message when nothing looks like a channel`() {
        server.enqueue(MockResponse().setBody("<html><head><title>Not YouTube</title></head><body></body></html>"))

        val error = assertThrows(IllegalStateException::class.java) {
            resolver.resolve(server.url("/@nobody").toString())
        }

        org.junit.jupiter.api.Assertions.assertTrue(error.message!!.contains("Δεν βρέθηκε κανάλι"))
    }
}
