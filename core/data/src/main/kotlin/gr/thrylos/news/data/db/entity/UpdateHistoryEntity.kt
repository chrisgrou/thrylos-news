package gr.thrylos.news.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One installed app update, recorded locally when the user installs it through the
 *  in-app updater — there's no GitHub-side history to read back from, since CI
 *  replaces the single rolling "latest" release on every push (see build.yml). */
@Entity(tableName = "update_history")
data class UpdateHistoryEntity(
    @PrimaryKey val versionCode: Int,
    val notes: String,
    val installedAt: Long,
)
