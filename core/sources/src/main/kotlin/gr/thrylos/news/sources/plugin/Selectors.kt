package gr.thrylos.news.sources.plugin

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Selector syntax used throughout plugin JSON: a Jsoup CSS selector, with an
 * optional trailing "@attrName" to read an attribute instead of text content,
 * e.g. "img@src" or "time.published@datetime".
 */
data class ParsedSelector(val css: String, val attr: String?) {
    companion object {
        fun parse(raw: String): ParsedSelector {
            val at = raw.lastIndexOf('@')
            return if (at > 0) ParsedSelector(raw.substring(0, at), raw.substring(at + 1))
            else ParsedSelector(raw, null)
        }
    }
}

private fun Element.readValue(selector: ParsedSelector): String? {
    val target = if (selector.css.isBlank()) this else selectFirst(selector.css) ?: return null
    val value = if (selector.attr != null) target.attr(selector.attr) else target.text()
    return value.trim().ifBlank { null }
}

fun Element.textOf(rawSelector: String?): String? =
    rawSelector?.let { readValue(ParsedSelector.parse(it)) }

fun Document.textOf(rawSelector: String?): String? = (this as Element).textOf(rawSelector)

fun Element.elementOf(rawSelector: String): Element? {
    val selector = ParsedSelector.parse(rawSelector)
    return if (selector.css.isBlank()) this else selectFirst(selector.css)
}
