package gr.thrylos.news.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import gr.thrylos.news.data.db.entity.ArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {

    @Query("SELECT * FROM articles ORDER BY COALESCE(publishedAt, fetchedAt) DESC")
    fun observeAll(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE isBookmarked = 1 ORDER BY COALESCE(publishedAt, fetchedAt) DESC")
    fun observeBookmarked(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE sourceId = :sourceId ORDER BY COALESCE(publishedAt, fetchedAt) DESC")
    fun observeBySource(sourceId: String): Flow<List<ArticleEntity>>

    /** Sportal-style grouped sources share a sourceName across several sourceIds, so
     *  a "source home" view queries by name to include every member's articles. */
    @Query("SELECT * FROM articles WHERE sourceName = :sourceName ORDER BY COALESCE(publishedAt, fetchedAt) DESC")
    fun observeBySourceName(sourceName: String): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE author = :author ORDER BY COALESCE(publishedAt, fetchedAt) DESC")
    fun observeByAuthor(author: String): Flow<List<ArticleEntity>>

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

    @Query("UPDATE articles SET isRead = 1 WHERE isRead = 0")
    suspend fun markAllRead()

    @Query("UPDATE articles SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun setBookmarked(id: String, isBookmarked: Boolean)

    @Query("UPDATE articles SET dedupGroupId = :groupId WHERE id = :id")
    suspend fun setDedupGroup(id: String, groupId: String?)

    @Query("SELECT * FROM articles")
    suspend fun getAllOnce(): List<ArticleEntity>

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
}
