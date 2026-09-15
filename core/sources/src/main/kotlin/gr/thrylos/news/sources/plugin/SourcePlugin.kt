package gr.thrylos.news.sources.plugin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The current schema version this app understands. Plugins with a higher
 *  version are rejected on import with a clear "update the app" message. */
const val PLUGIN_SCHEMA_VERSION = 1

enum class DiscoveryType {
    @SerialName("rss") RSS,
    @SerialName("html-list") HTML_LIST,
    @SerialName("sitemap") SITEMAP,
}

enum class FallbackMode {
    @SerialName("readability") READABILITY,
    @SerialName("none") NONE,
}

/** Which icon a source gets in the sources list — purely cosmetic for [SITE] and
 *  [FACEBOOK]; a Facebook page is scraped the same html-list way as any other site,
 *  just usually pointed at mbasic.facebook.com.
 *
 *  [YOUTUBE] is different: it also changes how a discovered item becomes an
 *  [gr.thrylos.news.model.Article]. A video's watch page is a JS application with no
 *  stable content to select — there's nothing there for [gr.thrylos.news.sources.extract.ArticleExtractor]
 *  to scrape — so a YouTube plugin is built around its channel's own Atom feed
 *  (`youtube.com/feeds/videos.xml?channel_id=...`) instead, which already carries a
 *  title, thumbnail, description and publish date per video. See
 *  [gr.thrylos.news.sources.extract.YouTubeVideoExtractor]. */
enum class SourceKind {
    @SerialName("site") SITE,
    @SerialName("facebook") FACEBOOK,
    @SerialName("youtube") YOUTUBE,
}

@Serializable
data class Discovery(
    val type: DiscoveryType,
    val url: String,
    val maxItems: Int = 40,
)

@Serializable
data class ListSelectors(
    val item: String,
    val link: String,
    val title: String? = null,
    val image: String? = null,
    val date: String? = null,
)

@Serializable
data class ArticleSelectors(
    val title: String,
    val author: String? = null,
    val date: String? = null,
    val dateFormat: String? = null,
    val leadImage: String? = null,
    /** Required for every [SourceKind] except [SourceKind.YOUTUBE], which never
     *  reaches [gr.thrylos.news.sources.extract.ArticleExtractor] (and so never reads
     *  this) at all — see [SourceKind]. */
    val content: String? = null,
    val remove: List<String> = emptyList(),
    val unwrap: List<String> = emptyList(),
)

@Serializable
data class UrlRules(
    val allow: List<String> = emptyList(),
    val deny: List<String> = emptyList(),
    val stripQueryParams: List<String> = emptyList(),
)

@Serializable
data class HttpConfig(
    val userAgent: String = "default",
    val headers: Map<String, String> = emptyMap(),
    val delayMs: Long = 300,
)

@Serializable
data class SourcePlugin(
    val schemaVersion: Int,
    val id: String,
    val name: String,
    val homepage: String,
    val enabled: Boolean = true,
    val kind: SourceKind = SourceKind.SITE,
    val discovery: Discovery,
    val listSelectors: ListSelectors? = null,
    val article: ArticleSelectors,
    val urlRules: UrlRules = UrlRules(),
    val http: HttpConfig = HttpConfig(),
    val fallback: FallbackMode = FallbackMode.READABILITY,
)
