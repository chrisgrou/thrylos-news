package gr.thrylos.news.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import gr.thrylos.news.data.db.entity.ArticleContent
import gr.thrylos.news.data.db.entity.ArticleEntity
import gr.thrylos.news.data.db.entity.ArticleSummary
import kotlinx.coroutines.flow.Flow

/** Column list backing every [ArticleSummary] projection — everything but the
 *  article body. Spelled out rather than `SELECT *` on purpose: that's the whole
 *  point of these queries (see [ArticleSummary]). */
private const val SUMMARY_COLUMNS =
    "id, sourceId, sourceName, url, title, author, publishedAt, fetchedAt, " +
        "leadImageUrl, usedFallbackExtraction, isRead, isBookmarked, dedupGroupId"

@Dao
interface ArticleDao {

    @Query("SELECT $SUMMARY_COLUMNS FROM articles ORDER BY COALESCE(publishedAt, fetchedAt) DESC")
    fun observeAllSummaries(): Flow<List<ArticleSummary>>

    @Query("SELECT $SUMMARY_COLUMNS FROM articles WHERE isBookmarked = 1 ORDER BY COALESCE(publishedAt, fetchedAt) DESC")
    fun observeBookmarkedSummaries(): Flow<List<ArticleSummary>>

    /** Sportal-style grouped sources share a sourceName across several sourceIds, so
     *  a "source home" view queries by name to include every member's articles. */
    @Query("SELECT $SUMMARY_COLUMNS FROM articles WHERE sourceName = :sourceName ORDER BY COALESCE(publishedAt, fetchedAt) DESC")
    fun observeBySourceNameSummaries(sourceName: String): Flow<List<ArticleSummary>>

    @Query("SELECT $SUMMARY_COLUMNS FROM articles WHERE author = :author ORDER BY COALESCE(publishedAt, fetchedAt) DESC")
    fun observeByAuthorSummaries(author: String): Flow<List<ArticleSummary>>

    @Query("SELECT $SUMMARY_COLUMNS FROM articles ORDER BY COALESCE(publishedAt, fetchedAt) DESC")
    suspend fun getAllSummariesOnce(): List<ArticleSummary>

    /** Bodies included, read once rather than observed — for a caller that must match
     *  rules against every article's text but doesn't need to be told about every
     *  subsequent write. */
    @Query("SELECT * FROM articles ORDER BY COALESCE(publishedAt, fetchedAt) DESC")
    suspend fun getAllWithContentOnce(): List<ArticleEntity>

    /** Cheap "has the article corpus changed?" probe: a count and the newest fetch
     *  timestamp. Both are aggregates over one column, so they cost nothing next to
     *  reading the rows themselves. */
    @Query("SELECT COUNT(*) FROM articles")
    suspend fun articleCount(): Int

    @Query("SELECT COALESCE(MAX(fetchedAt), 0) FROM articles")
    suspend fun latestFetchedAt(): Long

    /** Body text for a bounded, known set of articles — the only way a list screen
     *  should ever reach an article body (see [ArticleSummary]). Callers must chunk:
     *  SQLite caps a statement at 999 bound variables. */
    @Query("SELECT id, contentJson FROM articles WHERE id IN (:ids)")
    suspend fun contentFor(ids: List<String>): List<ArticleContent>

    /** Whole rows, bodies and all. Reserved for callers that must match filter rules
     *  against every stored article's text; a list screen wants a summary query. */
    @Query("SELECT * FROM articles ORDER BY COALESCE(publishedAt, fetchedAt) DESC")
    fun observeAll(): Flow<List<ArticleEntity>>

    /** Bodies included — a backup export carries the full article, so it can be
     *  restored and read offline. */
    @Query("SELECT * FROM articles WHERE isBookmarked = 1 ORDER BY COALESCE(publishedAt, fetchedAt) DESC")
    suspend fun getBookmarkedOnce(): List<ArticleEntity>

    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun getById(id: String): ArticleEntity?

    @Query("SELECT * FROM articles WHERE id = :id")
    fun observeById(id: String): Flow<ArticleEntity?>

    @Query("SELECT url FROM articles WHERE sourceId = :sourceId")
    suspend fun existingUrls(sourceId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(articles: List<ArticleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(article: ArticleEntity)

    @Query("UPDATE articles SET isRead = :isRead WHERE id = :id")
    suspend fun setRead(id: String, isRead: Boolean)

    /** Same single-transaction rationale as [setDedupGroups]: marking several articles
     *  read in a plain loop would trigger an observeAll() re-emission per article. */
    @Transaction
    suspend fun setReadBatch(ids: List<String>) {
        ids.forEach { id -> setRead(id, true) }
    }

    @Query("UPDATE articles SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun setBookmarked(id: String, isBookmarked: Boolean)

    @Query("UPDATE articles SET dedupGroupId = :groupId WHERE id = :id")
    suspend fun setDedupGroup(id: String, groupId: String?)

    /** Room's InvalidationTracker fires an observeAll() re-emission after every single
     *  write to the table — calling setDedupGroup() in a plain loop for dozens of
     *  articles after each sync meant the feed's whole filter/sort/dedup pipeline
     *  recomputed dozens of times in a row. Wrapping the batch in one @Transaction
     *  collapses that into a single re-emission once the batch commits. */
    @Transaction
    suspend fun setDedupGroups(updates: List<Pair<String, String?>>) {
        updates.forEach { (id, groupId) -> setDedupGroup(id, groupId) }
    }

    /** Bodies are needed here (the caller looks for suspiciously short ones), but only
     *  this source's — the re-extraction sweep runs once per source per sync, and
     *  reading every stored article's body once per source was pure waste. */
    @Query("SELECT * FROM articles WHERE sourceId = :sourceId ORDER BY fetchedAt DESC")
    suspend fun getBySourceOnce(sourceId: String): List<ArticleEntity>

    @Query(
        "DELETE FROM articles WHERE isBookmarked = 0 AND COALESCE(publishedAt, fetchedAt) < :cutoffMillis",
    )
    suspend fun deleteOlderThan(cutoffMillis: Long)

    @Query(
        """DELETE FROM articles WHERE id IN (
            SELECT id FROM articles WHERE isBookmarked = 0
            ORDER BY COALESCE(publishedAt, fetchedAt) DESC LIMIT -1 OFFSET :keep
        )""",
    )
    suspend fun trimToMostRecent(keep: Int)

    @Query("DELETE FROM articles WHERE sourceId = :sourceId")
    suspend fun deleteBySource(sourceId: String)

    /** Keeps bookmarks — those are the one thing a user explicitly chose to keep,
     *  everything else is just resynced from its source on the next refresh. */
    @Query("DELETE FROM articles WHERE isBookmarked = 0")
    suspend fun deleteAllUnbookmarked()
}
