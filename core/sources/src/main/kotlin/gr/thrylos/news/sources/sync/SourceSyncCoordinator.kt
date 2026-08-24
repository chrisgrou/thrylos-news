package gr.thrylos.news.sources.sync

import gr.thrylos.news.model.Article
import gr.thrylos.news.model.ArticleStub
import gr.thrylos.news.sources.discovery.DiscoveryFactory
import gr.thrylos.news.sources.extract.ArticleExtractor
import gr.thrylos.news.sources.http.HttpFetcher
import gr.thrylos.news.sources.plugin.SourcePlugin
import gr.thrylos.news.sources.url.UrlNormalizer

/**
 * Orchestrates one plugin's sync: discovery → URL filtering/dedup against
 * already-known articles → (caller extracts bodies). Kept network-call-free
 * except for the two steps that inherently need it, so most of the pipeline
 * logic here is unit-testable without a live network.
 */
class SourceSyncCoordinator(
    private val http: HttpFetcher = HttpFetcher(),
    private val extractor: ArticleExtractor = ArticleExtractor(http),
) {

    /** Returns discovered stubs that pass urlRules and aren't already known, newest-effort first. */
    fun discoverNew(plugin: SourcePlugin, knownCanonicalUrls: Set<String>): List<ArticleStub> {
        val discovery = DiscoveryFactory.forType(plugin.discovery.type)
        val stubs = discovery.discover(plugin, http)
        return stubs
            .map { it.copy(url = UrlNormalizer.resolve(plugin.discovery.url, it.url)) }
            .filter { UrlNormalizer.isAllowed(it.url, plugin.urlRules) }
            .distinctBy { UrlNormalizer.canonicalize(it.url, plugin.urlRules) }
            .filterNot { UrlNormalizer.canonicalize(it.url, plugin.urlRules) in knownCanonicalUrls }
    }

    fun extractArticle(plugin: SourcePlugin, stub: ArticleStub): Article = extractor.extract(plugin, stub)
}
