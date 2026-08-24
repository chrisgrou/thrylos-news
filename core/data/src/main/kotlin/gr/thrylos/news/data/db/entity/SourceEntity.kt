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
)
