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

    @Test
    fun `sends the CONSENT cookie so an EU request skips the interstitial`() {
        server.enqueue(MockResponse().setBody(channelPageHtml("UCGiTb1kleEoNRKPPhwBUDCg", "RedNews")))

        resolver.resolve(server.url("/@REDSPORTS7").toString())

        val recorded = server.takeRequest()
        org.junit.jupiter.api.Assertions.assertTrue(recorded.getHeader("Cookie")?.contains("CONSENT=YES") == true)
    }

    @Test
    fun `fails with a specific message when YouTube serves the consent wall instead of the channel`() {
        // Detected by the confirm form's fixed action URL, not by the wall's own text
        // — that text renders in whatever language the request asked for (Greek
        // here), not necessarily English.
        server.enqueue(
            MockResponse().setBody(
                """<html><head><title>Πριν μεταβείτε στο YouTube</title></head>
                   <body><form action="https://consent.youtube.com/save"></form></body></html>""",
            ),
        )

        val error = assertThrows(IllegalStateException::class.java) {
            resolver.resolve(server.url("/@REDSPORTS7").toString())
        }

        org.junit.jupiter.api.Assertions.assertTrue(error.message!!.contains("απορρήτου"))
    }

    @Test
    fun `an unresolved response includes its title and length for diagnosis`() {
        server.enqueue(MockResponse().setBody("<html><head><title>Κάτι άλλο</title></head><body></body></html>"))

        val error = assertThrows(IllegalStateException::class.java) {
            resolver.resolve(server.url("/@nobody").toString())
        }

        org.junit.jupiter.api.Assertions.assertTrue(error.message!!.contains("Κάτι άλλο"))
    }

    @Test
    fun `falls back to ytInitialData's channelMetadataRenderer when the tags aren't in the response`() {
        // No canonical link, no og:title — only what a real channel page's embedded
        // ytInitialData blob always carries. Includes a decoy channelId elsewhere on
        // the page (e.g. a "featured channels" shelf) to prove the resolver reads the
        // one specific, unambiguous field rather than the first UC... it finds.
        val html = """
            <!DOCTYPE html><html><head><title>bwinΣΠΟΡ FM 94.6 - YouTube</title></head>
            <body>
            <script>var ytInitialData = {"contents":{"twoColumnBrowseResultsRenderer":{"tabs":[]}},
            "metadata":{"channelMetadataRenderer":{"title":"bwinΣΠΟΡ FM 94.6",
            "description":"Contains \"quotes\" and {braces} in the description on purpose.",
            "externalId":"UCsporfm9460000000001"}},
            "header":{"c4TabbedHeaderRenderer":{"channelId":"UCdecoyDoNotUse00000001"}}};</script>
            </body></html>
        """.trimIndent()
        server.enqueue(MockResponse().setBody(html))

        val info = resolver.resolve(server.url("/@sporfm946").toString())

        assertEquals("UCsporfm9460000000001", info.channelId)
        assertEquals("bwinΣΠΟΡ FM 94.6", info.name)
    }
}
