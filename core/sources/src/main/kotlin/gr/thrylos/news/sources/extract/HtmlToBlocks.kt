package gr.thrylos.news.sources.extract

import gr.thrylos.news.model.ContentBlock
import gr.thrylos.news.sources.plugin.ArticleSelectors
import gr.thrylos.news.sources.url.UrlNormalizer
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

private val HEADING_TAGS = setOf("h1", "h2", "h3", "h4", "h5", "h6")
private val SKIP_TAGS = setOf(
    "script", "style", "noscript", "form", "button",
    "aside", "nav", "ins", "svg", "figure-caption", "template",
)
private val CONTAINER_TAGS = setOf(
    "div", "span", "section", "article", "main", "p_wrapper", "content", "body",
)
/** Simple text-formatting tags whose content should join the running paragraph text
 *  instead of being recursed into as a block container (which, since they rarely
 *  wrap any *element* children, previously meant their text was silently dropped —
 *  walk() only ever looked at child Elements, never bare text nodes). */
private val INLINE_TAGS = setOf("a", "b", "strong", "i", "em", "u", "mark", "small", "sub", "sup", "abbr", "cite", "q")
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

    /** Many sites (especially WordPress-style bodies) separate paragraphs with bare
     *  text nodes and `<br>` tags instead of wrapping each one in a `<p>` — the old
     *  element-only walk saw none of that text at all (it only iterated child
     *  Elements), so it all either vanished or got flattened into one run-on
     *  paragraph by the Readability fallback. This walks every child *node*
     *  (text and element alike), buffering inline text and flushing it as a
     *  paragraph whenever a `<br>` or a block-level element is hit. */
    private fun walk(el: Element, baseUrl: String, out: MutableList<ContentBlock>) {
        val buffer = StringBuilder()
        // Deliberately no trimming/space-injection per fragment: the source's own
        // text nodes already carry whatever whitespace belongs between words and
        // around inline tags (e.g. "…</a>, σύμφωνα…" has no space before the comma
        // on purpose) — inserting our own would misplace it. Only collapse/trim the
        // fully assembled paragraph once, in flush().
        fun appendText(text: String) {
            if (text.isEmpty()) return
            buffer.append(text)
        }
        fun flush() {
            val text = buffer.toString().replace(Regex("\\s+"), " ").trim()
            if (text.length >= 2) out += ContentBlock.Paragraph(text)
            buffer.setLength(0)
        }

        for (node in el.childNodes()) {
            when (node) {
                is TextNode -> appendText(node.text())
                is Element -> {
                    val tag = node.tagName().lowercase()
                    when {
                        tag in SKIP_TAGS -> continue
                        tag == "br" -> flush()
                        tag in HEADING_TAGS -> {
                            flush()
                            val text = node.text().trim()
                            if (text.isNotBlank()) out += ContentBlock.Heading(text, tag.substring(1).toIntOrNull() ?: 2)
                        }
                        tag == "p" -> {
                            // Recurse rather than just taking node.text(): a <p> can
                            // itself contain <br>-separated runs (real-world markup,
                            // however invalid — e.g. one giant <p> the whole article
                            // body got dumped into with <br><br> between paragraphs),
                            // and .text() would silently flatten those into one block.
                            flush()
                            walk(node, baseUrl, out)
                        }
                        tag == "blockquote" -> {
                            flush()
                            val text = node.text().trim()
                            if (text.isNotBlank()) out += ContentBlock.Quote(text)
                        }
                        tag == "ul" || tag == "ol" -> {
                            flush()
                            val items = node.children().filter { it.tagName().equals("li", ignoreCase = true) }
                                .map { it.text().trim() }.filter { it.isNotBlank() }
                            if (items.isNotEmpty()) out += ContentBlock.ListBlock(items, ordered = tag == "ol")
                        }
                        tag == "figure" -> {
                            flush()
                            (videoFrom(node, baseUrl) ?: imageFrom(node, baseUrl))?.let { out += it }
                        }
                        tag == "img" -> {
                            flush()
                            imageFrom(node, baseUrl)?.let { out += it }
                        }
                        tag == "video" -> {
                            flush()
                            videoFrom(node, baseUrl)?.let { out += it }
                        }
                        tag == "iframe" -> {
                            flush()
                            videoFrom(node, baseUrl)?.let { out += it }
                        }
                        // node.text() trims the element's own leading/trailing
                        // whitespace, which can be meaningful here — e.g.
                        // "<strong>Μέλε </strong>είναι…" needs that trailing space to
                        // avoid gluing onto the following word. wholeText() preserves
                        // it; only internal whitespace runs get collapsed (in flush()).
                        tag in INLINE_TAGS -> appendText(node.wholeText().replace(Regex("\\s+"), " "))
                        tag in CONTAINER_TAGS || node.children().isNotEmpty() -> {
                            flush()
                            walk(node, baseUrl, out)
                        }
                        else -> {
                            flush()
                            val text = node.text().trim()
                            if (text.length >= 2) out += ContentBlock.Paragraph(text)
                        }
                    }
                }
            }
        }
        flush()
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
        val src = img?.let { pickImageSrc(it) } ?: return null
        if (src.isBlank()) return null
        val caption = el.selectFirst("figcaption")?.text()?.trim()?.ifBlank { null }
            ?: img.attr("alt").trim().ifBlank { null }
        return ContentBlock.Image(UrlNormalizer.resolve(baseUrl, src), caption)
    }

    /** Lazy-loading galleries commonly leave [src] pointing at a placeholder/logo
     *  and stash the real image URL in a data-* attribute or srcset, so those must
     *  be tried first — falling back to [src] only when nothing else is present. */
    private fun pickImageSrc(img: Element): String {
        val dataAttr = listOf("data-src", "data-lazy-src", "data-original", "data-lazy")
            .map { img.attr(it) }
            .firstOrNull { it.isNotBlank() }
        if (dataAttr != null) return dataAttr

        val srcset = img.attr("srcset").ifBlank { img.attr("data-srcset") }
        if (srcset.isNotBlank()) {
            val candidate = srcset.split(",").lastOrNull()?.trim()?.split(" ")?.firstOrNull()
            if (!candidate.isNullOrBlank()) return candidate
        }

        return img.attr("src")
    }
}
