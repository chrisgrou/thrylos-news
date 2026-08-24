package gr.thrylos.news.data.repo

import gr.thrylos.news.data.db.dao.UpdateHistoryDao
import gr.thrylos.news.data.db.entity.UpdateHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateHistoryEntry(val versionCode: Int, val notes: String, val installedAt: Long)

@Singleton
class UpdateHistoryRepository @Inject constructor(
    private val dao: UpdateHistoryDao,
) {
    fun observeAll(): Flow<List<UpdateHistoryEntry>> =
        dao.observeAll().map { list -> list.map { UpdateHistoryEntry(it.versionCode, it.notes, it.installedAt) } }

    suspend fun record(versionCode: Int, notes: String) {
        dao.upsert(UpdateHistoryEntity(versionCode, notes, System.currentTimeMillis()))
    }
}
