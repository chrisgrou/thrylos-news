package gr.thrylos.news.data.repo

import gr.thrylos.news.data.db.dao.SourceDao
import gr.thrylos.news.data.db.entity.SourceEntity
import gr.thrylos.news.sources.plugin.PluginParseResult
import gr.thrylos.news.sources.plugin.PluginParser
import gr.thrylos.news.sources.plugin.SourcePlugin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class SourceWithPlugin(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val sortOrder: Int,
    val isBundled: Boolean,
    val plugin: SourcePlugin?,
    val pluginJson: String,
    val lastSyncError: String?,
    val lastSyncAt: Long?,
)

@Singleton
class SourceRepository @Inject constructor(
    private val dao: SourceDao,
) {
    fun observeAll(): Flow<List<SourceWithPlugin>> = dao.observeAll().map { list -> list.map(::toDomain) }

    suspend fun getById(id: String): SourceWithPlugin? = dao.getById(id)?.let(::toDomain)

    suspend fun getEnabledPlugins(): List<SourcePlugin> =
        dao.getEnabled().mapNotNull { entity -> (PluginParser.parse(entity.pluginJson) as? PluginParseResult.Success)?.plugin }

    /** Imports (or updates) a plugin from raw JSON. Returns parse errors, if any, without touching the DB. */
    suspend fun importPlugin(rawJson: String, isBundled: Boolean = false): PluginParseResult {
        val result = PluginParser.parse(rawJson)
        if (result is PluginParseResult.Success) {
            val plugin = result.plugin
            val nextOrder = dao.count()
            dao.upsert(
                SourceEntity(
                    id = plugin.id,
                    name = plugin.name,
                    homepage = plugin.homepage,
                    pluginJson = rawJson,
                    enabled = plugin.enabled,
                    sortOrder = nextOrder,
                    isBundled = isBundled,
                ),
            )
        }
        return result
    }

    suspend fun setEnabled(id: String, enabled: Boolean) = dao.setEnabled(id, enabled)

    /** Called by SyncWorker after every per-source sync attempt — error is the
     *  exception message on failure, or null on success — so a broken source is
     *  visible in Settings instead of only ever failing silently. */
    suspend fun recordSyncResult(id: String, error: String?) = dao.setSyncStatus(id, error, System.currentTimeMillis())

    suspend fun reorder(idsInOrder: List<String>) {
        idsInOrder.forEachIndexed { index, id -> dao.setSortOrder(id, index) }
    }

    suspend fun remove(source: SourceWithPlugin) {
        dao.getById(source.id)?.let { dao.delete(it) }
    }

    suspend fun bundleIfAbsent(assetId: String, rawJson: String) {
        val result = PluginParser.parse(rawJson)
        if (result is PluginParseResult.Success) {
            val plugin = result.plugin
            dao.insertIfAbsent(
                SourceEntity(
                    id = plugin.id,
                    name = plugin.name,
                    homepage = plugin.homepage,
                    pluginJson = rawJson,
                    enabled = plugin.enabled,
                    sortOrder = Int.MAX_VALUE,
                    isBundled = true,
                ),
            )
        }
    }

    private fun toDomain(entity: SourceEntity): SourceWithPlugin {
        val parsed = (PluginParser.parse(entity.pluginJson) as? PluginParseResult.Success)?.plugin
        return SourceWithPlugin(
            id = entity.id,
            name = entity.name,
            enabled = entity.enabled,
            sortOrder = entity.sortOrder,
            isBundled = entity.isBundled,
            plugin = parsed,
            pluginJson = entity.pluginJson,
            lastSyncError = entity.lastSyncError,
            lastSyncAt = entity.lastSyncAt,
        )
    }
}
