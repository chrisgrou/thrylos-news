package gr.thrylos.news.data.repo

import gr.thrylos.news.data.db.entity.ArticleEntity
import gr.thrylos.news.model.Article
import gr.thrylos.news.model.ContentBlock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

object ArticleMapper {
    private val json = Json { ignoreUnknownKeys = true }
    private val blockListSerializer = ListSerializer(ContentBlock.serializer())

    fun toEntity(article: Article): ArticleEntity = ArticleEntity(
        id = article.id,
        sourceId = article.sourceId,
        sourceName = article.sourceName,
        url = article.url,
        title = article.title,
        author = article.author,
        publishedAt = article.publishedAt,
        fetchedAt = article.fetchedAt,
        leadImageUrl = article.leadImageUrl,
        contentJson = json.encodeToString(blockListSerializer, article.content),
        usedFallbackExtraction = article.usedFallbackExtraction,
        isRead = article.isRead,
        isBookmarked = article.isBookmarked,
        dedupGroupId = article.dedupGroupId,
    )

    fun toDomain(entity: ArticleEntity): Article = Article(
        id = entity.id,
        sourceId = entity.sourceId,
        sourceName = entity.sourceName,
        url = entity.url,
        title = entity.title,
        author = entity.author,
        publishedAt = entity.publishedAt,
        fetchedAt = entity.fetchedAt,
        leadImageUrl = entity.leadImageUrl,
        content = runCatching { json.decodeFromString(blockListSerializer, entity.contentJson) }.getOrDefault(emptyList()),
        usedFallbackExtraction = entity.usedFallbackExtraction,
        isRead = entity.isRead,
        isBookmarked = entity.isBookmarked,
        dedupGroupId = entity.dedupGroupId,
    )
}
