package gr.thrylos.news.data.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import gr.thrylos.news.model.NotificationPrefs
import gr.thrylos.news.model.ReaderFontFamily
import gr.thrylos.news.model.ReaderPrefs
import gr.thrylos.news.model.ReaderTheme
import gr.thrylos.news.model.RefreshInterval
import gr.thrylos.news.model.SyncPrefs
import gr.thrylos.news.model.TextAlign
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "thrylos_news_prefs")

@Serializable
private data class ReaderPrefsDto(
    val theme: ReaderTheme = ReaderTheme.LIGHT,
    val fontFamily: ReaderFontFamily = ReaderFontFamily.SERIF,
    val fontScale: Float = 1.0f,
    val lineHeightScale: Float = 1.0f,
    val marginWidth: Int = 1,
    val textAlign: TextAlign = TextAlign.START,
    val keepScreenOn: Boolean = false,
)

@Serializable
private data class SyncPrefsDto(
    val refreshInterval: RefreshInterval = RefreshInterval.HOUR_1,
    val syncOnlyOnWifi: Boolean = false,
    val downloadImagesOnlyOnWifi: Boolean = true,
    val offlineRetentionDays: Int = 14,
    val offlineMaxArticles: Int = 500,
    val prefetchImagesForOffline: Boolean = true,
)

@Serializable
private data class NotificationPrefsDto(
    val enabled: Boolean = true,
    val onlySourceIds: Set<String> = emptySet(),
    val onlyKeywords: Set<String> = emptySet(),
    val groupIntoSummary: Boolean = true,
)

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val readerKey = stringPreferencesKey("reader_prefs")
    private val syncKey = stringPreferencesKey("sync_prefs")
    private val notificationKey = stringPreferencesKey("notification_prefs")

    val readerPrefs: Flow<ReaderPrefs> = context.dataStore.data.map { prefs ->
        val dto = prefs[readerKey]?.let { runCatching { json.decodeFromString<ReaderPrefsDto>(it) }.getOrNull() } ?: ReaderPrefsDto()
        dto.toDomain()
    }

    val syncPrefs: Flow<SyncPrefs> = context.dataStore.data.map { prefs ->
        val dto = prefs[syncKey]?.let { runCatching { json.decodeFromString<SyncPrefsDto>(it) }.getOrNull() } ?: SyncPrefsDto()
        dto.toDomain()
    }

    val notificationPrefs: Flow<NotificationPrefs> = context.dataStore.data.map { prefs ->
        val dto = prefs[notificationKey]?.let { runCatching { json.decodeFromString<NotificationPrefsDto>(it) }.getOrNull() } ?: NotificationPrefsDto()
        dto.toDomain()
    }

    suspend fun currentSyncPrefs(): SyncPrefs = syncPrefs.first()
    suspend fun currentNotificationPrefs(): NotificationPrefs = notificationPrefs.first()

    suspend fun updateReaderPrefs(update: (ReaderPrefs) -> ReaderPrefs) {
        context.dataStore.edit { prefs ->
            val current = prefs[readerKey]?.let { runCatching { json.decodeFromString<ReaderPrefsDto>(it) }.getOrNull() } ?: ReaderPrefsDto()
            val next = update(current.toDomain()).toDto()
            prefs[readerKey] = json.encodeToString(ReaderPrefsDto.serializer(), next)
        }
    }

    suspend fun updateSyncPrefs(update: (SyncPrefs) -> SyncPrefs) {
        context.dataStore.edit { prefs ->
            val current = prefs[syncKey]?.let { runCatching { json.decodeFromString<SyncPrefsDto>(it) }.getOrNull() } ?: SyncPrefsDto()
            val next = update(current.toDomain()).toDto()
            prefs[syncKey] = json.encodeToString(SyncPrefsDto.serializer(), next)
        }
    }

    suspend fun updateNotificationPrefs(update: (NotificationPrefs) -> NotificationPrefs) {
        context.dataStore.edit { prefs ->
            val current = prefs[notificationKey]?.let { runCatching { json.decodeFromString<NotificationPrefsDto>(it) }.getOrNull() } ?: NotificationPrefsDto()
            val next = update(current.toDomain()).toDto()
            prefs[notificationKey] = json.encodeToString(NotificationPrefsDto.serializer(), next)
        }
    }

    private fun ReaderPrefsDto.toDomain() = ReaderPrefs(theme, fontFamily, fontScale, lineHeightScale, marginWidth, textAlign, keepScreenOn)
    private fun ReaderPrefs.toDto() = ReaderPrefsDto(theme, fontFamily, fontScale, lineHeightScale, marginWidth, textAlign, keepScreenOn)

    private fun SyncPrefsDto.toDomain() = SyncPrefs(refreshInterval, syncOnlyOnWifi, downloadImagesOnlyOnWifi, offlineRetentionDays, offlineMaxArticles, prefetchImagesForOffline)
    private fun SyncPrefs.toDto() = SyncPrefsDto(refreshInterval, syncOnlyOnWifi, downloadImagesOnlyOnWifi, offlineRetentionDays, offlineMaxArticles, prefetchImagesForOffline)

    private fun NotificationPrefsDto.toDomain() = NotificationPrefs(enabled, onlySourceIds, onlyKeywords, groupIntoSummary)
    private fun NotificationPrefs.toDto() = NotificationPrefsDto(enabled, onlySourceIds, onlyKeywords, groupIntoSummary)
}
