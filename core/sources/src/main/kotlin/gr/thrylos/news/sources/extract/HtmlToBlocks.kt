package gr.thrylos.news.sources.extract

import gr.thrylos.news.model.ContentBlock
import gr.thrylos.news.sources.plugin.ArticleSelectors
import gr.thrylos.news.sources.url.UrlNormalizer
import org.jsoup.nodes.Element

private val HEADING_TAGS = setOf("h1", "h2", "h3", "h4", "h5", "h6")
private val SKIP_TAGS = setOf(
    "script", "style", "noscript", "form", "button",
    "aside", "nav", "ins", "svg", "figure-caption", "template",
)
private val CONTAINER_TAGS = setOf(
    "div", "span", "section", "article", "main", "p_wrapper", "content", "body",
)
private val VIDEO_EMBED_DOMAINS = listOf(
    "youtube.com", "youtu.be", "youtube-nocookie.com", "vimeo.com", "facebook.com", "fb.watch", "dailymotion.com",
)

/** Converts a cleaned article content [Element] into a flat, renderable block list. */
object HtmlToBlocks {

    fun convert(content: Element, selectors: ArticleSelectors, baseUrl: String): List<ContentBlock> {
        (AdBlockList.selectors + selectors.remove).forEach { sel ->
            runCatching { content.select(sel).remove() }
        }
        selectors.unwrap.forEach { tag ->
            runCatching { content.select(tag).forEach { it.unwrap() } }
        }

        val blocks = mutableListOf<ContentBlock>()
        walk(content, baseUrl, blocks)
        return blocks
    }

    private fun walk(el: Element, baseUrl: String, out: MutableList<ContentBlock>) {
        for (child in el.children()) {
            val tag = child.tagName().lowercase()
            when {
                tag in SKIP_TAGS -> continue
                tag in HEADING_TAGS -> {
                    val text = child.text().trim()
                    if (text.isNotBlank()) out += ContentBlock.Heading(text, tag.substring(1).toIntOrNull() ?: 2)
                }
                tag == "p" -> {
                    val text = child.text().trim()
                    if (text.length >= 2) out += ContentBlock.Paragraph(text)
                }
                tag == "blockquote" -> {
                    val text = child.text().trim()
                    if (text.isNotBlank()) out += ContentBlock.Quote(text)
                }
                tag == "ul" || tag == "ol" -> {
                    val items = child.children().filter { it.tagName().equals("li", ignoreCase = true) }
                        .map { it.text().trim() }.filter { it.isNotBlank() }
                    if (items.isNotEmpty()) out += ContentBlock.ListBlock(items, ordered = tag == "ol")
                }
                tag == "figure" -> {
                    (videoFrom(child, baseUrl) ?: imageFrom(child, baseUrl))?.let { out += it }
                }
                tag == "img" -> {
                    imageFrom(child, baseUrl)?.let { out += it }
                }
                tag == "video" -> {
                    videoFrom(child, baseUrl)?.let { out += it }
                }
                tag == "iframe" -> {
                    videoFrom(child, baseUrl)?.let { out += it }
                }
                tag in CONTAINER_TAGS || child.children().isNotEmpty() -> {
                    walk(child, baseUrl, out)
                }
                else -> {
                    val text = child.text().trim()
                    if (text.length >= 2) out += ContentBlock.Paragraph(text)
                }
            }
        }
    }

    private fun videoFrom(el: Element, baseUrl: String): ContentBlock.Video? {
        val src = when {
            el.tagName().equals("iframe", ignoreCase = true) -> el.attr("src")
            el.tagName().equals("video", ignoreCase = true) ->
                el.attr("src").ifBlank { el.selectFirst("source")?.attr("src").orEmpty() }
            else -> el.selectFirst("iframe")?.attr("src") ?: el.selectFirst("video")?.let {
                it.attr("src").ifBlank { it.selectFirst("source")?.attr("src").orEmpty() }
            }.orEmpty()
        }
        if (src.isBlank()) return null
        val resolved = UrlNormalizer.resolve(baseUrl, src)
        val isKnownVideoEmbed = el.tagName().equals("video", ignoreCase = true) ||
            VIDEO_EMBED_DOMAINS.any { resolved.contains(it, ignoreCase = true) }
        if (!isKnownVideoEmbed) return null
        val poster = el.attr("poster").ifBlank { null }
        val caption = el.selectFirst("figcaption")?.text()?.trim()?.ifBlank { null }
        return ContentBlock.Video(resolved, poster?.let { UrlNormalizer.resolve(baseUrl, it) }, caption)
    }

    private fun imageFrom(el: Element, baseUrl: String): ContentBlock.Image? {
        val img = if (el.tagName().equals("img", ignoreCase = true)) el else el.selectFirst("img")
        val src = img?.let { it.attr("src").ifBlank { it.attr("data-src") } } ?: return null
        if (src.isBlank()) return null
        val caption = el.selectFirst("figcaption")?.text()?.trim()?.ifBlank { null }
            ?: img.attr("alt").trim().ifBlank { null }
        return ContentBlock.Image(UrlNormalizer.resolve(baseUrl, src), caption)
    }
}
