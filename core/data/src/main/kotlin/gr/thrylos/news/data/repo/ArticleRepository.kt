package gr.thrylos.news.data.repo

import gr.thrylos.news.data.db.dao.ArticleDao
import gr.thrylos.news.data.db.entity.ArticleState
import gr.thrylos.news.model.Article
import gr.thrylos.news.sources.filter.FilterEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRepository @Inject constructor(
    private val dao: ArticleDao,
) {
    /** Body-free — see [gr.thrylos.news.data.db.entity.ArticleSummary]. Every list
     *  screen wants one of these, not [observeAllWithContent]. */
    fun observeAllSummaries(): Flow<List<Article>> = dao.observeAllSummaries().map { it.map(ArticleMapper::toDomain) }

    fun observeBookmarked(): Flow<List<Article>> = dao.observeBookmarkedSummaries().map { it.map(ArticleMapper::toDomain) }

    fun observeBySourceName(sourceName: String): Flow<List<Article>> =
        dao.observeBySourceNameSummaries(sourceName).map { it.map(ArticleMapper::toDomain) }

    fun observeByAuthor(author: String): Flow<List<Article>> = dao.observeByAuthorSummaries(author).map { it.map(ArticleMapper::toDomain) }

    suspend fun getAllSummariesOnce(): List<Article> = dao.getAllSummariesOnce().map(ArticleMapper::toDomain)

    suspend fun getAllWithContentOnce(): List<Article> = dao.getAllWithContentOnce().map(ArticleMapper::toDomain)

    /** Loads whole article bodies — only for the handful of ids asked for, and only
     *  from a caller that genuinely needs body text (a BODY/"Οπουδήποτε" filter rule).
     *  Chunked to stay under SQLite's 999-bound-variable limit. */
    suspend fun bodyTextByIds(ids: List<String>): Map<String, String> {
        if (ids.isEmpty()) return emptyMap()
        val result = HashMap<String, String>(ids.size)
        ids.chunked(400).forEach { chunk ->
            dao.contentFor(chunk).forEach { row ->
                result[row.id] = FilterEngine.bodyTextOf(ArticleMapper.decodeBlocks(row.contentJson))
            }
        }
        return result
    }

    /** Full rows, bodies included, and re-read on every write to the table. The one
     *  caller left is the widget's refresh, which must apply filter rules — body rules
     *  among them — to every stored article; anything list-shaped wants
     *  [observeAllSummaries] instead. */
    fun observeAllWithContent(): Flow<List<Article>> = dao.observeAll().map { it.map(ArticleMapper::toDomain) }

    suspend fun getById(id: String): Article? = dao.getById(id)?.let(ArticleMapper::toDomain)

    fun observeById(id: String): Flow<Article?> = dao.observeById(id).map { it?.let(ArticleMapper::toDomain) }

    /** The plain, as-stored url of every article for this source — *not* canonicalized;
     *  see [gr.thrylos.news.sources.sync.SourceSyncCoordinator.discoverNew], which does
     *  that itself against the same [gr.thrylos.news.sources.plugin.UrlRules] it applies
     *  to freshly discovered urls, so both sides of the "already known?" check agree. */
    suspend fun existingUrls(sourceId: String): Set<String> = dao.existingUrls(sourceId).toSet()

    /** A "new" article from discovery can turn out to already exist — most often a
     *  transient mismatch during discovery's own known-url check re-surfacing an
     *  already-synced article as if it were unseen (see SourceSyncCoordinator.
     *  discoverNew). Room's REPLACE conflict strategy would otherwise blow away that
     *  existing row's isRead/isBookmarked/dedupGroupId wholesale — silently marking a
     *  read article unread and un-collapsing it from its dedup group — every single
     *  time that happens. Carrying those three fields forward from whatever's already
     *  stored (when anything is) makes a spurious "rediscovery" harmless either way. */
    suspend fun upsertAll(articles: List<Article>) {
        if (articles.isEmpty()) return
        val existing = dao.existingState(articles.map { it.id }).associateBy { it.id }
        val merged = articles.map { article -> preserveExistingState(article, existing[article.id]) }
        dao.upsertAll(merged.map(ArticleMapper::toEntity))
    }

    suspend fun upsert(article: Article) {
        val existing = dao.existingState(listOf(article.id)).firstOrNull()
        dao.upsert(ArticleMapper.toEntity(preserveExistingState(article, existing)))
    }

    private fun preserveExistingState(article: Article, existing: ArticleState?): Article =
        if (existing == null) article
        else article.copy(isRead = existing.isRead, isBookmarked = existing.isBookmarked, dedupGroupId = existing.dedupGroupId)

    suspend fun setRead(id: String, isRead: Boolean) = dao.setRead(id, isRead)

    suspend fun setReadBatch(ids: List<String>) {
        if (ids.isNotEmpty()) dao.setReadBatch(ids)
    }

    suspend fun setBookmarked(id: String, isBookmarked: Boolean) = dao.setBookmarked(id, isBookmarked)

    suspend fun setDedupGroup(id: String, groupId: String?) = dao.setDedupGroup(id, groupId)

    suspend fun setDedupGroups(updates: List<Pair<String, String?>>) {
        if (updates.isNotEmpty()) dao.setDedupGroups(updates)
    }

    suspend fun getBySourceOnce(sourceId: String): List<Article> = dao.getBySourceOnce(sourceId).map(ArticleMapper::toDomain)

    suspend fun runOfflineCleanup(retentionDays: Int, maxArticles: Int) {
        val cutoff = System.currentTimeMillis() - retentionDays * 24 * 60 * 60 * 1000L
        dao.deleteOlderThan(cutoff)
        dao.trimToMostRecent(maxArticles)
    }

    suspend fun deleteBySource(sourceId: String) = dao.deleteBySource(sourceId)

    /** Clears everything except bookmarks, and (via [existingUrls] then
     *  finding nothing known) forces every source to be fully re-discovered and
     *  re-extracted on the next sync — useful after a data-quality fix (e.g. a
     *  published-date parsing bug) that only affects newly-synced articles, since
     *  existing ones are otherwise never re-fetched once already known. */
    suspend fun clearHistory() = dao.deleteAllUnbookmarked()
}
