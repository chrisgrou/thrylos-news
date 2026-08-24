package gr.thrylos.news.sources.discovery

import gr.thrylos.news.model.ArticleStub
import gr.thrylos.news.sources.http.HttpFetcher
import gr.thrylos.news.sources.plugin.SourcePlugin
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Parses a `<urlset>` sitemap. Sitemaps carry no title, so the stub title is a
 * placeholder — it's always replaced once the article page is extracted.
 */
class SitemapDiscovery : ArticleDiscovery {

    override fun discover(plugin: SourcePlugin, http: HttpFetcher): List<ArticleStub> {
        val xml = http.fetchText(plugin.discovery.url, plugin.http)
        val doc = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }.newDocumentBuilder().parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))

        val urlNodes = doc.getElementsByTagName("url")
        return (0 until urlNodes.length).mapNotNull { i ->
            val el = urlNodes.item(i) as Element
            val loc = el.getElementsByTagName("loc").item(0)?.textContent?.trim() ?: return@mapNotNull null
            ArticleStub(plugin.id, loc, title = "", imageUrl = null, publishedAt = null)
        }.take(plugin.discovery.maxItems)
    }
}
