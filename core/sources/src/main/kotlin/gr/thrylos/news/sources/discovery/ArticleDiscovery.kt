package gr.thrylos.news.sources.discovery

import gr.thrylos.news.model.ArticleStub
import gr.thrylos.news.sources.http.HttpFetcher
import gr.thrylos.news.sources.plugin.DiscoveryType
import gr.thrylos.news.sources.plugin.SourcePlugin

interface ArticleDiscovery {
    fun discover(plugin: SourcePlugin, http: HttpFetcher): List<ArticleStub>
}

object DiscoveryFactory {
    fun forType(type: DiscoveryType): ArticleDiscovery = when (type) {
        DiscoveryType.RSS -> RssDiscovery()
        DiscoveryType.HTML_LIST -> HtmlListDiscovery()
        DiscoveryType.SITEMAP -> SitemapDiscovery()
    }
}
