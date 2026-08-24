package gr.thrylos.news.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey val id: String,
    val sourceId: String,
    val sourceName: String,
    val url: String,
    val title: String,
    val author: String?,
    val publishedAt: Long?,
    val fetchedAt: Long,
    val leadImageUrl: String?,
    /** JSON-encoded List<ContentBlock>; see [gr.thrylos.news.data.repo.ArticleMapper]. */
    val contentJson: String,
    val usedFallbackExtraction: Boolean,
    val isRead: Boolean = false,
    val isBookmarked: Boolean = false,
    val dedupGroupId: String? = null,
)
