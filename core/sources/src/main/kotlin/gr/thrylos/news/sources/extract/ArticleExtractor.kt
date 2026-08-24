package gr.thrylos.news.sources.extract

import gr.thrylos.news.model.Article
import gr.thrylos.news.model.ArticleStub
import gr.thrylos.news.sources.http.HttpFetcher
import gr.thrylos.news.sources.plugin.FallbackMode
import gr.thrylos.news.sources.plugin.SourcePlugin
import gr.thrylos.news.sources.plugin.elementOf
import gr.thrylos.news.sources.plugin.textOf
import gr.thrylos.news.sources.url.UrlNormalizer
import gr.thrylos.news.sources.util.Ids
import net.dankito.readability4j.Readability4J
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
            }
        }

    private fun extractWithSelectors(doc: Element, plugin: SourcePlugin, stub: ArticleStub, canonicalUrl: String): Article {
        val article = plugin.article
        val title = doc.textOf(article.title) ?: stub.title.ifBlank { error("Δεν βρέθηκε τίτλος") }
        val author = doc.textOf(article.author)
        val leadImage = article.leadImage?.let { doc.textOf(it) }?.let { UrlNormalizer.resolve(stub.url, it) }
        val contentEl = doc.elementOf(article.content) ?: error("Δεν βρέθηκε το content selector '${article.content}'")
        val blocks = HtmlToBlocks.convert(contentEl, article, stub.url)
        val publishedAt = doc.textOf(article.date)?.let { parseDate(it, article.dateFormat) } ?: stub.publishedAt

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

    /**
     * Parses a published date. Real sites publish dates in three shapes, and all
     * three show up across the bundled sources, so each is tried in turn:
     * with an explicit offset ("2026-08-24T18:31:30+03:00"), as a zoneless local
     * timestamp ("24.08.2026-18:31"), or as a bare date. Zoneless values are
     * resolved against [zone], since a site's local time is what it means.
     */
    private fun parseDate(text: String, pattern: String?, zone: ZoneId = ZoneId.systemDefault()): Long? {
        val formatters = buildList {
            if (pattern != null) runCatching { DateTimeFormatter.ofPattern(pattern) }.getOrNull()?.let { add(it) }
            add(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            // Some sites emit the offset without a colon ("+0300" instead of
            // "+03:00", e.g. gazzetta.gr's <time datetime> attribute), which
            // ISO_OFFSET_DATE_TIME rejects outright.
            add(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"))
            add(DateTimeFormatter.RFC_1123_DATE_TIME)
            add(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            add(DateTimeFormatter.ISO_LOCAL_DATE)
        }
        for (fmt in formatters) {
            val parsed = runCatching { fmt.parse(text) }.getOrNull() ?: continue
            // Narrow from the most specific interpretation to the least, so a value
            // that really does carry an offset never gets re-interpreted as local.
            runCatching { OffsetDateTime.from(parsed).toInstant().toEpochMilli() }
                .getOrNull()?.let { return it }
            runCatching { LocalDateTime.from(parsed).atZone(zone).toInstant().toEpochMilli() }
                .getOrNull()?.let { return it }
            runCatching { LocalDate.from(parsed).atStartOfDay(zone).toInstant().toEpochMilli() }
                .getOrNull()?.let { return it }
        }
        return null
    }
}
