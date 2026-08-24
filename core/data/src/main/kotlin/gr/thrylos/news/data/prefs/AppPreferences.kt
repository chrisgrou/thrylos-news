package gr.thrylos.news.data.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import gr.thrylos.news.model.AppThemeMode
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
    val quietHoursEnabled: Boolean = false,
    val quietHoursStartMinute: Int = 23 * 60,
    val quietHoursEndMinute: Int = 7 * 60,
    val highlightNewSinceRefresh: Boolean = true,
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
    private val appThemeKey = stringPreferencesKey("app_theme_mode")
    private val lastOpenedAtKey = longPreferencesKey("last_opened_at")
    private val lastSyncCompletedAtKey = longPreferencesKey("last_sync_completed_at")

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

    val appThemeMode: Flow<AppThemeMode> = context.dataStore.data.map { prefs ->
        prefs[appThemeKey]?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() } ?: AppThemeMode.SYSTEM
    }

    suspend fun setAppThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { prefs -> prefs[appThemeKey] = mode.name }
    }

    /** Returns the timestamp of the previous app open (0 the very first time), then
     *  immediately stamps "now" for the next call — so each process lifetime gets a
     *  fixed boundary for "new since I last opened the app" without it drifting
     *  while the app stays open. */
    suspend fun consumeAndAdvanceLastOpenedAt(): Long {
        var previous = 0L
        context.dataStore.edit { prefs ->
            previous = prefs[lastOpenedAtKey] ?: 0L
            prefs[lastOpenedAtKey] = System.currentTimeMillis()
        }
        return previous
    }

    /** Stamped by [gr.thrylos.news.data.sync.SyncWorker] once the per-source sync loop
     *  actually runs — not just enqueued — so the feed can show "when did this last
     *  really happen", which also doubles as a way to tell whether a refresh tap did
     *  anything at all (vs. WorkManager silently deferring/skipping it). */
    val lastSyncCompletedAt: Flow<Long?> = context.dataStore.data.map { prefs -> prefs[lastSyncCompletedAtKey] }

    suspend fun setLastSyncCompletedAt(millis: Long) {
        context.dataStore.edit { prefs -> prefs[lastSyncCompletedAtKey] = millis }
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

    private fun SyncPrefsDto.toDomain() = SyncPrefs(
        refreshInterval, syncOnlyOnWifi, downloadImagesOnlyOnWifi, offlineRetentionDays, offlineMaxArticles,
        prefetchImagesForOffline, quietHoursEnabled, quietHoursStartMinute, quietHoursEndMinute, highlightNewSinceRefresh,
    )
    private fun SyncPrefs.toDto() = SyncPrefsDto(
        refreshInterval, syncOnlyOnWifi, downloadImagesOnlyOnWifi, offlineRetentionDays, offlineMaxArticles,
        prefetchImagesForOffline, quietHoursEnabled, quietHoursStartMinute, quietHoursEndMinute, highlightNewSinceRefresh,
    )

    private fun NotificationPrefsDto.toDomain() = NotificationPrefs(enabled, onlySourceIds, onlyKeywords, groupIntoSummary)
    private fun NotificationPrefs.toDto() = NotificationPrefsDto(enabled, onlySourceIds, onlyKeywords, groupIntoSummary)
}
