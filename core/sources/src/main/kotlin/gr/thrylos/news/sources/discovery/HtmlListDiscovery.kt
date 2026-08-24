package gr.thrylos.news.sources.discovery

import gr.thrylos.news.model.ArticleStub
import gr.thrylos.news.sources.http.HttpFetcher
import gr.thrylos.news.sources.plugin.SourcePlugin
import gr.thrylos.news.sources.plugin.textOf
import gr.thrylos.news.sources.url.UrlNormalizer
import org.jsoup.Jsoup

/** Scrapes a listing page (e.g. a category/tag page) using the plugin's [gr.thrylos.news.sources.plugin.ListSelectors]. */
class HtmlListDiscovery : ArticleDiscovery {

    override fun discover(plugin: SourcePlugin, http: HttpFetcher): List<ArticleStub> {
        val selectors = requireNotNull(plugin.listSelectors) {
            "Το plugin '${plugin.id}' έχει discovery.type=html-list αλλά δεν ορίζει listSelectors."
        }
        val html = http.fetchText(plugin.discovery.url, plugin.http)
        val doc = Jsoup.parse(html, plugin.discovery.url)

        return doc.select(selectors.item).mapNotNull { item ->
            val rawLink = item.textOf(selectors.link) ?: return@mapNotNull null
            val link = UrlNormalizer.resolve(plugin.discovery.url, rawLink)
            // Title is best-effort here; the definitive title comes from extracting
            // the article page itself, which always overrides this stub value.
            val title = selectors.title?.let { item.textOf(it) } ?: item.text().take(160)
            val image = selectors.image?.let { item.textOf(it) }?.let { UrlNormalizer.resolve(plugin.discovery.url, it) }
            ArticleStub(plugin.id, link, title.ifBlank { "(χωρίς τίτλο)" }, image, publishedAt = null)
        }.take(plugin.discovery.maxItems)
    }
}
