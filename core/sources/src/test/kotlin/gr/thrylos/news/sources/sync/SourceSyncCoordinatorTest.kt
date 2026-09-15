package gr.thrylos.news.sources.sync

import gr.thrylos.news.sources.http.HttpFetcher
import gr.thrylos.news.sources.plugin.ArticleSelectors
import gr.thrylos.news.sources.plugin.Discovery
import gr.thrylos.news.sources.plugin.DiscoveryType
import gr.thrylos.news.sources.plugin.SourceKind
import gr.thrylos.news.sources.plugin.SourcePlugin
import gr.thrylos.news.sources.testutil.Fixtures
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SourceSyncCoordinatorTest {

    private lateinit var server: MockWebServer
    private lateinit var coordinator: SourceSyncCoordinator

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        coordinator = SourceSyncCoordinator(HttpFetcher())
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    private fun plugin(excludeShorts: Boolean) = SourcePlugin(
        schemaVersion = 1,
        id = "demo-channel",
        name = "Demo Channel",
        homepage = "https://www.youtube.com/@demo",
        kind = SourceKind.YOUTUBE,
        discovery = Discovery(DiscoveryType.RSS, server.url("/feed").toString(), excludeShorts = excludeShorts),
        article = ArticleSelectors(title = "unused"),
    )

    @Test
    fun `excludeShorts drops Shorts entries from discovery entirely`() {
        server.enqueue(MockResponse().setBody(Fixtures.read("sample-youtube-feed-with-short.xml")))

        val stubs = coordinator.discoverNew(plugin(excludeShorts = true), emptySet())

        assertEquals(1, stubs.size)
        assertTrue(stubs[0].url.contains("watch?v=regular12345"))
    }

    @Test
    fun `Shorts are included by default`() {
        server.enqueue(MockResponse().setBody(Fixtures.read("sample-youtube-feed-with-short.xml")))

        val stubs = coordinator.discoverNew(plugin(excludeShorts = false), emptySet())

        assertEquals(2, stubs.size)
    }
}
