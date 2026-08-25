package gr.thrylos.news.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import gr.thrylos.news.data.db.entity.FilterRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FilterRuleDao {

    @Query("SELECT * FROM filter_rules")
    fun observeAll(): Flow<List<FilterRuleEntity>>

    @Query("SELECT * FROM filter_rules WHERE enabled = 1")
    suspend fun getEnabled(): List<FilterRuleEntity>

    /** A plain @Insert(REPLACE) deletes-then-reinserts an existing row on conflict,
     *  which gives it a new rowid and silently reshuffles the (unordered-by-design)
     *  list to the bottom on every edit or enable/disable toggle. Try a real UPDATE
     *  first so an existing rule keeps its position; only INSERT for a genuinely new
     *  rule. */
    @Transaction
    suspend fun upsert(rule: FilterRuleEntity) {
        if (update(rule) == 0) insert(rule)
    }

    @Update
    suspend fun update(rule: FilterRuleEntity): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(rule: FilterRuleEntity)

    @Delete
    suspend fun delete(rule: FilterRuleEntity)

    @Query("SELECT COUNT(*) FROM filter_rules WHERE id LIKE :prefix || '%'")
    suspend fun countByIdPrefix(prefix: String): Int

    @Query("DELETE FROM filter_rules WHERE id LIKE :prefix || '%'")
    suspend fun deleteByIdPrefix(prefix: String): Int
}
