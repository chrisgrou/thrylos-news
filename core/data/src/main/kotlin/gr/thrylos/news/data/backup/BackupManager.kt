package gr.thrylos.news.data.backup

import gr.thrylos.news.data.db.dao.ArticleDao
import gr.thrylos.news.data.db.dao.FilterRuleDao
import gr.thrylos.news.data.db.dao.SourceDao
import gr.thrylos.news.data.db.entity.ArticleEntity
import gr.thrylos.news.data.db.entity.FilterRuleEntity
import gr.thrylos.news.data.db.entity.SourceEntity
import gr.thrylos.news.data.prefs.AppPreferences
import gr.thrylos.news.model.NotificationPrefs
import gr.thrylos.news.model.ReaderPrefs
import gr.thrylos.news.model.SyncPrefs
import gr.thrylos.news.sources.plugin.DiscoveryType
import gr.thrylos.news.sources.plugin.PluginParseResult
import gr.thrylos.news.sources.plugin.PluginParser
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class BackupSource(val pluginJson: String, val enabled: Boolean, val sortOrder: Int, val isBundled: Boolean)

@Serializable
data class BackupFilter(
    val id: String, val field: String, val match: String, val value: String,
    val caseSensitive: Boolean, val action: String, val scopeSourceId: String?, val enabled: Boolean,
)

@Serializable
data class BackupBundle(
    val formatVersion: Int = 1,
    val exportedAt: Long,
    val sources: List<BackupSource>,
    val filters: List<BackupFilter>,
    val bookmarkedArticles: List<gr.thrylos.news.model.Article>,
    val readerPrefs: ReaderPrefs,
    val syncPrefs: SyncPrefs,
    val notificationPrefs: NotificationPrefs,
)

sealed class RestoreResult {
    data class Success(val sourcesImported: Int, val filtersImported: Int, val bookmarksImported: Int) : RestoreResult()
    data class Failure(val message: String) : RestoreResult()
}

/**
 * Exports/imports everything that makes the app "yours": sources, filters,
 * bookmarks and preferences — a single JSON file, so switching phones doesn't
 * mean re-adding every plugin and filter by hand.
 */
@Singleton
class BackupManager @Inject constructor(
    private val sourceDao: SourceDao,
    private val filterDao: FilterRuleDao,
    private val articleDao: ArticleDao,
    private val preferences: AppPreferences,
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    suspend fun export(): String {
        val sources = sourceDao.observeAll().first().map {
            BackupSource(it.pluginJson, it.enabled, it.sortOrder, it.isBundled)
        }
        val filters = filterDao.observeAll().first().map {
            BackupFilter(it.id, it.field, it.match, it.value, it.caseSensitive, it.action, it.scopeSourceId, it.enabled)
        }
        val bookmarks = articleDao.observeBookmarked().first().map(gr.thrylos.news.data.repo.ArticleMapper::toDomain)

        val bundle = BackupBundle(
            exportedAt = System.currentTimeMillis(),
            sources = sources,
            filters = filters,
            bookmarkedArticles = bookmarks,
            readerPrefs = preferences.readerPrefs.first(),
            syncPrefs = preferences.syncPrefs.first(),
            notificationPrefs = preferences.notificationPrefs.first(),
        )
        return json.encodeToString(BackupBundle.serializer(), bundle)
    }

    suspend fun import(rawJson: String): RestoreResult {
        val bundle = try {
            json.decodeFromString(BackupBundle.serializer(), rawJson)
        } catch (e: Exception) {
            return RestoreResult.Failure("Μη έγκυρο αρχείο αντιγράφου ασφαλείας: ${e.message}")
        }

        var sourcesImported = 0
        bundle.sources.forEach { backupSource ->
            val parsed = PluginParser.parse(backupSource.pluginJson)
            if (parsed is PluginParseResult.Success) {
                sourceDao.upsert(
                    SourceEntity(
                        id = parsed.plugin.id,
                        name = parsed.plugin.name,
                        homepage = parsed.plugin.homepage,
                        pluginJson = backupSource.pluginJson,
                        enabled = backupSource.enabled,
                        sortOrder = backupSource.sortOrder,
                        isBundled = backupSource.isBundled,
                    ),
                )
                sourcesImported++
            }
        }

        bundle.filters.forEach { f ->
            filterDao.upsert(FilterRuleEntity(f.id, f.field, f.match, f.value, f.caseSensitive, f.action, f.scopeSourceId, f.enabled))
        }

        bundle.bookmarkedArticles.forEach { article ->
            articleDao.upsert(gr.thrylos.news.data.repo.ArticleMapper.toEntity(article.copy(isBookmarked = true)))
        }

        preferences.updateReaderPrefs { bundle.readerPrefs }
        preferences.updateSyncPrefs { bundle.syncPrefs }
        preferences.updateNotificationPrefs { bundle.notificationPrefs }

        return RestoreResult.Success(sourcesImported, bundle.filters.size, bundle.bookmarkedArticles.size)
    }

    /** OPML export of just the RSS-backed sources — for interop with other feed readers. */
    suspend fun exportOpml(): String {
        val sources = sourceDao.observeAll().first().mapNotNull { entity ->
            (PluginParser.parse(entity.pluginJson) as? PluginParseResult.Success)?.plugin
        }.filter { it.discovery.type == DiscoveryType.RSS }

        val body = sources.joinToString("\n") {
            """    <outline text="${it.name.xmlEscape()}" title="${it.name.xmlEscape()}" type="rss" xmlUrl="${it.discovery.url.xmlEscape()}" htmlUrl="${it.homepage.xmlEscape()}"/>"""
        }
        return """<?xml version="1.0" encoding="UTF-8"?>
<opml version="2.0">
  <head><title>Thrylos News — Πηγές</title></head>
  <body>
$body
  </body>
</opml>"""
    }

    private fun String.xmlEscape() = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}
