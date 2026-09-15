package gr.thrylos.news.sources.extract

import gr.thrylos.news.model.ArticleStub
import gr.thrylos.news.model.ContentBlock
import gr.thrylos.news.sources.plugin.ArticleSelectors
import gr.thrylos.news.sources.plugin.Discovery
import gr.thrylos.news.sources.plugin.DiscoveryType
import gr.thrylos.news.sources.plugin.SourceKind
import gr.thrylos.news.sources.plugin.SourcePlugin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class YouTubeVideoExtractorTest {

    private val plugin = SourcePlugin(
        schemaVersion = 1,
        id = "demo-channel",
        name = "Demo Channel",
        homepage = "https://www.youtube.com/@demo",
        kind = SourceKind.YOUTUBE,
        discovery = Discovery(DiscoveryType.RSS, "https://www.youtube.com/feeds/videos.xml?channel_id=UCdemo"),
        article = ArticleSelectors(title = "unused"),
    )

    @Test
    fun `builds an article straight from the stub, with no network fetch`() {
        val stub = ArticleStub(
            sourceId = plugin.id,
            url = "https://www.youtube.com/watch?v=abc12345678",
            title = "Ολυμπιακός: highlights τελευταίου αγώνα",
            imageUrl = "https://i.ytimg.com/vi/abc12345678/hqdefault.jpg",
            publishedAt = 1_700_000_000_000L,
            description = "Τα καλύτερα στιγμιότυπα από τον αγώνα.",
        )

        val article = YouTubeVideoExtractor.extract(plugin, stub)

        assertEquals(stub.title, article.title)
        // The display source is the channel's real name (plugin.name) — never the
        // handle a plugin author resolved it from.
        assertEquals("Demo Channel", article.sourceName)
        assertEquals(stub.imageUrl, article.leadImageUrl)
        assertEquals(stub.publishedAt, article.publishedAt)
        assertNull(article.author)

        val video = article.content.filterIsInstance<ContentBlock.Video>().single()
        assertEquals("https://www.youtube.com/embed/abc12345678", video.url)
        assertEquals(stub.imageUrl, video.thumbnailUrl)

        val paragraph = article.content.filterIsInstance<ContentBlock.Paragraph>().single()
        assertEquals(stub.description, paragraph.text)
    }

    @Test
    fun `omits the paragraph entirely when the feed had no description`() {
        val stub = ArticleStub(
            sourceId = plugin.id,
            url = "https://www.youtube.com/watch?v=xyz98765432",
            title = "Video χωρίς περιγραφή",
        )

        val article = YouTubeVideoExtractor.extract(plugin, stub)

        assertTrue(article.content.none { it is ContentBlock.Paragraph })
        val video = article.content.filterIsInstance<ContentBlock.Video>().single()
        assertEquals("https://www.youtube.com/embed/xyz98765432", video.url)
        assertNull(video.thumbnailUrl)
    }

    @Test
    fun `extracts the video id from a Shorts URL just as well as a watch URL`() {
        val stub = ArticleStub(
            sourceId = plugin.id,
            url = "https://www.youtube.com/shorts/vqOlaO3I0VU",
            title = "Ένα Short",
        )

        val article = YouTubeVideoExtractor.extract(plugin, stub)

        val video = article.content.filterIsInstance<ContentBlock.Video>().single()
        assertEquals("https://www.youtube.com/embed/vqOlaO3I0VU", video.url)
    }

    @Test
    fun `fails clearly when the URL carries no recognizable video id`() {
        val stub = ArticleStub(sourceId = plugin.id, url = "https://www.youtube.com/watch", title = "Broken")

        val error = runCatching { YouTubeVideoExtractor.extract(plugin, stub) }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
    }
}
