package gr.thrylos.news.data.repo

import gr.thrylos.news.data.db.dao.ArticleDao
import gr.thrylos.news.model.Article
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRepository @Inject constructor(
    private val dao: ArticleDao,
) {
    fun observeAll(): Flow<List<Article>> = dao.observeAll().map { it.map(ArticleMapper::toDomain) }

    fun observeBookmarked(): Flow<List<Article>> = dao.observeBookmarked().map { it.map(ArticleMapper::toDomain) }

    fun observeBySource(sourceId: String): Flow<List<Article>> = dao.observeBySource(sourceId).map { it.map(ArticleMapper::toDomain) }

    fun observeBySourceName(sourceName: String): Flow<List<Article>> = dao.observeBySourceName(sourceName).map { it.map(ArticleMapper::toDomain) }

    fun observeByAuthor(author: String): Flow<List<Article>> = dao.observeByAuthor(author).map { it.map(ArticleMapper::toDomain) }

    suspend fun getById(id: String): Article? = dao.getById(id)?.let(ArticleMapper::toDomain)

    fun observeById(id: String): Flow<Article?> = dao.observeById(id).map { it?.let(ArticleMapper::toDomain) }

    suspend fun existingCanonicalUrls(sourceId: String): Set<String> = dao.existingUrls(sourceId).toSet()

    suspend fun upsertAll(articles: List<Article>) = dao.upsertAll(articles.map(ArticleMapper::toEntity))

    suspend fun upsert(article: Article) = dao.upsert(ArticleMapper.toEntity(article))

    suspend fun setRead(id: String, isRead: Boolean) = dao.setRead(id, isRead)

    suspend fun markAllRead() = dao.markAllRead()

    suspend fun setBookmarked(id: String, isBookmarked: Boolean) = dao.setBookmarked(id, isBookmarked)

    suspend fun setDedupGroup(id: String, groupId: String?) = dao.setDedupGroup(id, groupId)

    suspend fun setDedupGroups(updates: List<Pair<String, String?>>) {
        if (updates.isNotEmpty()) dao.setDedupGroups(updates)
    }

    suspend fun getAllOnce(): List<Article> = dao.getAllOnce().map(ArticleMapper::toDomain)

    suspend fun runOfflineCleanup(retentionDays: Int, maxArticles: Int) {
        val cutoff = System.currentTimeMillis() - retentionDays * 24 * 60 * 60 * 1000L
        dao.deleteOlderThan(cutoff)
        dao.trimToMostRecent(maxArticles)
    }

    suspend fun deleteBySource(sourceId: String) = dao.deleteBySource(sourceId)

    /** Clears everything except bookmarks, and (via [existingCanonicalUrls] then
     *  finding nothing known) forces every source to be fully re-discovered and
     *  re-extracted on the next sync — useful after a data-quality fix (e.g. a
     *  published-date parsing bug) that only affects newly-synced articles, since
     *  existing ones are otherwise never re-fetched once already known. */
    suspend fun clearHistory() = dao.deleteAllUnbookmarked()
}
