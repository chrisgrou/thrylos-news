package gr.thrylos.news.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import gr.thrylos.news.data.db.entity.UpdateHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UpdateHistoryDao {

    @Query("SELECT * FROM update_history ORDER BY versionCode DESC")
    fun observeAll(): Flow<List<UpdateHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UpdateHistoryEntity)
}
