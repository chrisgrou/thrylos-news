package gr.thrylos.news.data.repo

import gr.thrylos.news.data.db.entity.ArticleEntity
import gr.thrylos.news.model.Article
import gr.thrylos.news.model.ContentBlock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.Collections
import java.util.LinkedHashMap

object ArticleMapper {
    private val json = Json { ignoreUnknownKeys = true }
    private val blockListSerializer = ListSerializer(ContentBlock.serializer())

    private const val CONTENT_CACHE_CAPACITY = 3000

    /** Room re-emits (and this re-maps) every stored row on every write to the table —
     *  even one to a single unrelated article's isRead flag — and a sync run fires
     *  several such writes in quick succession right after app launch. Re-parsing
     *  every article's full content JSON on every one of those emissions, even though
     *  almost none of them actually changed, was the main cost behind the feed
     *  appearing briefly empty (and the page count slow to settle) on open. Cache the
     *  decoded blocks per article id, keyed by the exact JSON that produced them, so
     *  an unchanged row's content is never re-parsed; bounded (LRU) so it can't grow
     *  unboundedly across a long-running process as articles get replaced over time. */
    private val contentCache: MutableMap<String, Pair<String, List<ContentBlock>>> = Collections.synchronizedMap(
        object : LinkedHashMap<String, Pair<String, List<ContentBlock>>>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<String, List<ContentBlock>>>): Boolean =
                size > CONTENT_CACHE_CAPACITY
        },
    )

    private fun decodeContent(entity: ArticleEntity): List<ContentBlock> {
        val cached = contentCache[entity.id]
        if (cached != null && cached.first == entity.contentJson) return cached.second
        val decoded = runCatching { json.decodeFromString(blockListSerializer, entity.contentJson) }.getOrDefault(emptyList())
        contentCache[entity.id] = entity.contentJson to decoded
        return decoded
    }

    /** Defers the actual JSON decode until something (the reader, a BODY/ANYWHERE
     *  filter rule) really reads an article's content — the feed list itself only
     *  ever touches title/source/image/time. Without this, mapping a whole page of
     *  stored rows into [Article] decoded every article's full body content JSON
     *  unconditionally, every time, which is real cost on a cold start with hundreds
     *  of previously-synced articles and was the actual reason old articles took
     *  seconds to appear instead of showing instantly. [equals]/[hashCode] compare
     *  the raw JSON instead of the decoded list, so callers that just compare two
     *  Articles for change (StateFlow's conflation, Compose's remember(key)) never
     *  force a decode either. */
    private class LazyContentBlocks(
        private val rawJson: String,
        private val supplier: () -> List<ContentBlock>,
    ) : AbstractList<ContentBlock>() {
        private val decoded by lazy(LazyThreadSafetyMode.PUBLICATION) { supplier() }
        override val size: Int get() = decoded.size
        override fun get(index: Int): ContentBlock = decoded[index]
        override fun equals(other: Any?): Boolean = other is LazyContentBlocks && other.rawJson == rawJson
        override fun hashCode(): Int = rawJson.hashCode()
    }

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
        content = LazyContentBlocks(entity.contentJson) { decodeContent(entity) },
        usedFallbackExtraction = entity.usedFallbackExtraction,
        isRead = entity.isRead,
        isBookmarked = entity.isBookmarked,
        dedupGroupId = entity.dedupGroupId,
    )
}
