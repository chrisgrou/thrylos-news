package gr.thrylos.news.sources.extract

import gr.thrylos.news.model.Article
import gr.thrylos.news.model.ArticleStub
import gr.thrylos.news.model.ContentBlock
import gr.thrylos.news.sources.plugin.SourcePlugin
import gr.thrylos.news.sources.url.UrlNormalizer
import gr.thrylos.news.sources.util.Ids

/**
 * Builds an [Article] straight from a [gr.thrylos.news.sources.plugin.SourceKind.YOUTUBE]
 * channel feed's own per-video data instead of fetching and scraping the video's watch
 * page the way [ArticleExtractor] does for every other plugin.
 *
 * A watch page is a JS application, not server-rendered article HTML — there's no
 * stable content selector to write, and Readability finds nothing usable there either.
 * The channel's own Atom feed (`youtube.com/feeds/videos.xml?channel_id=...`) already
 * carries everything a video "article" needs per entry, via its `media:` namespace —
 * [gr.thrylos.news.sources.discovery.RssDiscovery] captures that into
 * [ArticleStub.imageUrl]/[ArticleStub.description], and this just reads it back rather
 * than making any network call of its own.
 */
object YouTubeVideoExtractor {

    private val VIDEO_ID_REGEX = Regex("""[?&]v=([\w-]{11})""")

    fun extract(plugin: SourcePlugin, stub: ArticleStub): Article {
        val canonicalUrl = UrlNormalizer.canonicalize(stub.url, plugin.urlRules)
        val videoId = VIDEO_ID_REGEX.find(stub.url)?.groupValues?.get(1)
            ?: error("Δεν βρέθηκε video id στο URL '${stub.url}'")

        val blocks = buildList {
            // youtube.com is in ContentBlockRenderer's IFRAME_EMBED_HOSTS, so this
            // plays inline (tap-to-play) exactly like a video embedded in a regular
            // article — no separate video UI needed.
            add(ContentBlock.Video(url = "https://www.youtube.com/embed/$videoId", thumbnailUrl = stub.imageUrl))
            stub.description?.trim()?.takeIf { it.isNotEmpty() }?.let { add(ContentBlock.Paragraph(it)) }
        }

        return Article(
            id = Ids.forArticle(canonicalUrl),
            sourceId = plugin.id,
            sourceName = plugin.name,
            url = canonicalUrl,
            title = stub.title,
            author = null,
            publishedAt = stub.publishedAt,
            fetchedAt = System.currentTimeMillis(),
            leadImageUrl = stub.imageUrl,
            content = blocks,
            usedFallbackExtraction = false,
        )
    }
}
