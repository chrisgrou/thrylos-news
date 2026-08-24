package gr.thrylos.news.sources.extract

import gr.thrylos.news.model.Article
import gr.thrylos.news.model.ArticleStub
import gr.thrylos.news.sources.http.HttpFetcher
import gr.thrylos.news.sources.plugin.FallbackMode
import gr.thrylos.news.sources.plugin.SourcePlugin
import gr.thrylos.news.sources.plugin.elementOf
import gr.thrylos.news.sources.plugin.textOf
import gr.thrylos.news.sources.url.UrlNormalizer
import gr.thrylos.news.sources.util.DateParsing
import gr.thrylos.news.sources.util.Ids
import net.dankito.readability4j.Readability4J
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/** Minimum combined paragraph text length below which we consider the plugin's
 * selectors to have failed and fall back to Readability. */
private const val MIN_CONTENT_CHARS = 200

class ArticleExtractor(private val http: HttpFetcher = HttpFetcher()) {

    /** @throws IllegalStateException if extraction fails and no usable fallback exists. */
    fun extract(plugin: SourcePlugin, stub: ArticleStub): Article {
        val html = http.fetchText(stub.url, plugin.http)
        val doc = Jsoup.parse(html, stub.url)
        val canonicalUrl = UrlNormalizer.canonicalize(stub.url, plugin.urlRules)

        val primary = runCatching { extractWithSelectors(doc, plugin, stub, canonicalUrl) }.getOrNull()
        if (primary != null && contentLength(primary) >= MIN_CONTENT_CHARS) return primary

        if (plugin.fallback == FallbackMode.READABILITY) {
            val fallback = runCatching { extractWithReadability(doc, plugin, stub, canonicalUrl) }.getOrNull()
            if (fallback != null && contentLength(fallback) >= MIN_CONTENT_CHARS) return fallback
        }

        // Return whichever attempt produced the most content, even below the threshold,
        // rather than losing a short-but-real article (e.g. a brief news flash).
        return primary ?: extractWithReadability(doc, plugin, stub, canonicalUrl)
    }

    private fun contentLength(article: Article) =
        article.content.sumOf { block ->
            when (block) {
                is gr.thrylos.news.model.ContentBlock.Paragraph -> block.text.length
                is gr.thrylos.news.model.ContentBlock.Heading -> block.text.length
                is gr.thrylos.news.model.ContentBlock.Quote -> block.text.length
                is gr.thrylos.news.model.ContentBlock.ListBlock -> block.items.sumOf { it.length }
                is gr.thrylos.news.model.ContentBlock.Image -> 0
                is gr.thrylos.news.model.ContentBlock.Video -> 0
            }
        }

    private fun extractWithSelectors(doc: Element, plugin: SourcePlugin, stub: ArticleStub, canonicalUrl: String): Article {
        val article = plugin.article
        val title = doc.textOf(article.title) ?: stub.title.ifBlank { error("Δεν βρέθηκε τίτλος") }
        val author = doc.textOf(article.author)
        val leadImage = article.leadImage?.let { doc.textOf(it) }?.let { UrlNormalizer.resolve(stub.url, it) }
        val contentEl = doc.elementOf(article.content) ?: error("Δεν βρέθηκε το content selector '${article.content}'")
        val blocks = HtmlToBlocks.convert(contentEl, article, stub.url)
        val publishedAt = doc.textOf(article.date)?.let { DateParsing.parse(it, article.dateFormat) } ?: stub.publishedAt

        return Article(
            id = Ids.forArticle(canonicalUrl),
            sourceId = plugin.id,
            sourceName = plugin.name,
            url = canonicalUrl,
            title = title,
            author = author,
            publishedAt = publishedAt,
            fetchedAt = System.currentTimeMillis(),
            leadImageUrl = leadImage ?: stub.imageUrl,
            content = blocks,
            usedFallbackExtraction = false,
        )
    }

    private fun extractWithReadability(doc: Element, plugin: SourcePlugin, stub: ArticleStub, canonicalUrl: String): Article {
        val readability = Readability4J(stub.url, doc.outerHtml())
        val parsed = readability.parse()
        val contentHtml = parsed.content ?: error("Το Readability δεν εξήγαγε περιεχόμενο")
        val contentEl = Jsoup.parse(contentHtml, stub.url).body()
        val blocks = HtmlToBlocks.convert(contentEl, plugin.article, stub.url)

        return Article(
            id = Ids.forArticle(canonicalUrl),
            sourceId = plugin.id,
            sourceName = plugin.name,
            url = canonicalUrl,
            title = parsed.title?.ifBlank { null } ?: stub.title,
            author = parsed.byline,
            publishedAt = stub.publishedAt,
            fetchedAt = System.currentTimeMillis(),
            leadImageUrl = stub.imageUrl,
            content = blocks,
            usedFallbackExtraction = true,
        )
    }
}
