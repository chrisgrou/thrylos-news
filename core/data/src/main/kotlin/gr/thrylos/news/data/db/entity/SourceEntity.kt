package gr.thrylos.news.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sources")
data class SourceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val homepage: String,
    /** The full plugin definition, stored verbatim so re-parsing/editing round-trips exactly. */
    val pluginJson: String,
    val enabled: Boolean = true,
    val sortOrder: Int = 0,
    val isBundled: Boolean = false,
    /** Set after every sync attempt for this source — null on success, the exception
     *  message on failure — so a broken source is visible in the UI instead of only
     *  ever failing silently (SyncWorker swallows per-source exceptions to keep one
     *  bad source from failing the whole sync run). */
    val lastSyncError: String? = null,
    val lastSyncAt: Long? = null,
)
