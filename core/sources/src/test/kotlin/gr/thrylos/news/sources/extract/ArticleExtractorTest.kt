package gr.thrylos.news.sources.extract

import gr.thrylos.news.model.ArticleStub
import gr.thrylos.news.model.ContentBlock
import gr.thrylos.news.sources.http.HttpFetcher
import gr.thrylos.news.sources.plugin.ArticleSelectors
import gr.thrylos.news.sources.plugin.Discovery
import gr.thrylos.news.sources.plugin.DiscoveryType
import gr.thrylos.news.sources.plugin.SourcePlugin
import gr.thrylos.news.sources.testutil.Fixtures
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ArticleExtractorTest {

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

    private fun plugin() = SourcePlugin(
        schemaVersion = 1, id = "demo", name = "Demo Sports", homepage = server.url("/").toString(),
        discovery = Discovery(DiscoveryType.RSS, server.url("/rss").toString()),
        article = ArticleSelectors(
            title = "h1.article-title",
            author = "span.author-name",
            date = "time.published@datetime",
            leadImage = "figure.lead img@src",
            content = "div.article-body",
            remove = emptyList(),
        ),
    )

    @Test
    fun `extracts clean blocks and strips ads and related-content`() {
        server.enqueue(MockResponse().setBody(Fixtures.read("sample-article.html")))
        val stub = ArticleStub("demo", server.url("/football/olympiacos/plano-neas-sezon").toString(), "(stub title)")

        val article = ArticleExtractor(HttpFetcher()).extract(plugin(), stub)

        assertFalse(article.usedFallbackExtraction)
        assertEquals("Ολυμπιακός: το πλάνο του προπονητή για τη νέα σεζόν", article.title)
        assertEquals("Γ. Παπαδόπουλος", article.author)
        assertTrue(article.leadImageUrl!!.endsWith("/img/plano-lead.jpg"))

        val paragraphs = article.content.filterIsInstance<ContentBlock.Paragraph>()
        assertEquals(2, paragraphs.size)
        assertTrue(paragraphs.none { it.text.contains("διαφήμιση", ignoreCase = true) })

        val fullText = article.content.joinToString(" ") { block ->
            when (block) {
                is ContentBlock.Paragraph -> block.text
                is ContentBlock.Heading -> block.text
                is ContentBlock.Quote -> block.text
                is ContentBlock.ListBlock -> block.items.joinToString(" ")
                is ContentBlock.Image -> ""
                is ContentBlock.Video -> ""
            }
        }
        assertFalse(fullText.contains("Διαβάστε επίσης"))
        assertFalse(fullText.contains("newsletter", ignoreCase = true))

        assertTrue(article.content.any { it is ContentBlock.Quote })
        assertTrue(article.content.any { it is ContentBlock.ListBlock })
    }

    @Test
    fun `stable id is derived from canonical url`() {
        server.enqueue(MockResponse().setBody(Fixtures.read("sample-article.html")))
        val stub = ArticleStub("demo", server.url("/football/olympiacos/plano-neas-sezon").toString(), "(stub title)")
        val article = ArticleExtractor(HttpFetcher()).extract(plugin(), stub)
        assertEquals(24, article.id.length)
    }
}
