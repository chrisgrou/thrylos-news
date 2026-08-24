package gr.thrylos.news.sources.discovery

import gr.thrylos.news.model.ArticleStub
import gr.thrylos.news.sources.http.HttpFetcher
import gr.thrylos.news.sources.plugin.SourcePlugin
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** Parses RSS 2.0 and Atom feeds — the two formats real-world sites actually use. */
class RssDiscovery : ArticleDiscovery {

    override fun discover(plugin: SourcePlugin, http: HttpFetcher): List<ArticleStub> {
        val xml = http.fetchText(plugin.discovery.url, plugin.http)
        // A site without RSS typically serves its normal HTML page (or a 404 page)
        // at the guessed /feed URL. Detect that up front: otherwise the XML parser
        // fails on the HTML doctype with an error that means nothing to the user.
        if (looksLikeHtml(xml)) {
            error(
                "Το URL δεν επιστρέφει RSS feed αλλά ιστοσελίδα HTML. " +
                    "Ίσως το site δεν έχει RSS — δοκίμασε discovery.type=\"html-list\".",
            )
        }

        val doc = try {
            secureDocumentBuilder().parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
        } catch (e: Exception) {
            error("Το URL δεν επιστρέφει έγκυρο RSS/Atom XML (${e.message?.take(120)}).")
        }

        val items = doc.getElementsByTagName("item")
        val entries = doc.getElementsByTagName("entry")

        val stubs = when {
            items.length > 0 -> (0 until items.length).mapNotNull { parseRssItem(items.item(it) as Element, plugin.id) }
            entries.length > 0 -> (0 until entries.length).mapNotNull { parseAtomEntry(entries.item(it) as Element, plugin.id) }
            else -> error("Το feed δεν περιέχει άρθρα (κανένα <item> ή <entry>).")
        }
        return stubs.take(plugin.discovery.maxItems)
    }

    private fun looksLikeHtml(body: String): Boolean {
        val head = body.trimStart().take(600).lowercase()
        if (head.startsWith("<?xml") || head.contains("<rss") || head.contains("<feed")) return false
        return head.contains("<!doctype html") || head.contains("<html")
    }

    private fun Element.child(tag: String): String? =
        getElementsByTagName(tag).item(0)?.textContent?.trim()?.ifBlank { null }

    private fun parseRssItem(item: Element, sourceId: String): ArticleStub? {
        val link = item.child("link") ?: return null
        val title = item.child("title") ?: return null
        // Both <enclosure> and <media:content> are self-closing (their "text" is
        // blank) and carry the image URL as an attribute, not as element text.
        val image = (item.getElementsByTagName("enclosure").item(0) as? Element)?.getAttribute("url")?.ifBlank { null }
            ?: (item.getElementsByTagName("media:content").item(0) as? Element)?.getAttribute("url")?.ifBlank { null }
        val published = item.child("pubDate")?.let(::parseRfc822)
        return ArticleStub(sourceId, link.trim(), title, image?.ifBlank { null }, published)
    }

    private fun parseAtomEntry(entry: Element, sourceId: String): ArticleStub? {
        val links = entry.getElementsByTagName("link")
        var href: String? = null
        for (i in 0 until links.length) {
            val el = links.item(i) as Element
            val rel = el.getAttribute("rel")
            if (rel.isBlank() || rel == "alternate") {
                href = el.getAttribute("href").ifBlank { null }
                if (href != null) break
            }
        }
        val link = href ?: return null
        val title = entry.child("title") ?: return null
        val published = (entry.child("published") ?: entry.child("updated"))?.let(::parseIso8601)
        return ArticleStub(sourceId, link.trim(), title, null, published)
    }

    private fun parseRfc822(text: String): Long? = try {
        OffsetDateTime.parse(text, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
    } catch (e: DateTimeParseException) {
        null
    }

    private fun parseIso8601(text: String): Long? = try {
        OffsetDateTime.parse(text).toInstant().toEpochMilli()
    } catch (e: DateTimeParseException) {
        null
    }
}
