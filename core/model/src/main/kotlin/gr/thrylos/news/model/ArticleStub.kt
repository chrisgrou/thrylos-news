package gr.thrylos.news.model

/**
 * A lightweight reference to an article discovered during sync (from RSS or an
 * HTML listing page), before its full body has been fetched and extracted.
 */
data class ArticleStub(
    val sourceId: String,
    val url: String,
    val title: String,
    val imageUrl: String? = null,
    val publishedAt: Long? = null,
)
