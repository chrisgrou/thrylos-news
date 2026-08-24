package gr.thrylos.news.model

import kotlinx.serialization.Serializable

/**
 * A fully extracted article, ready to be persisted and rendered by the reader.
 */
@Serializable
data class Article(
    val id: String,
    val sourceId: String,
    val sourceName: String,
    val url: String,
    val title: String,
    val author: String? = null,
    val publishedAt: Long? = null,
    val fetchedAt: Long,
    val leadImageUrl: String? = null,
    val content: List<ContentBlock>,
    val usedFallbackExtraction: Boolean = false,
    val isRead: Boolean = false,
    val isBookmarked: Boolean = false,
    val dedupGroupId: String? = null,
)
