package gr.thrylos.news.data.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import gr.thrylos.news.model.AppThemeMode
import gr.thrylos.news.model.Match
import gr.thrylos.news.model.MatchesPrefs
import gr.thrylos.news.model.NotificationPrefs
import gr.thrylos.news.model.ReaderFontFamily
import gr.thrylos.news.model.ReaderPrefs
import gr.thrylos.news.model.ReaderTheme
import gr.thrylos.news.model.RefreshInterval
import gr.thrylos.news.model.SyncPrefs
import gr.thrylos.news.model.TextAlign
import gr.thrylos.news.model.WidgetPrefs
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
    val feedPageSize: Int = 10,
)

@Serializable
private data class NotificationPrefsDto(
    val enabled: Boolean = true,
    val onlySourceIds: Set<String> = emptySet(),
    val onlyKeywords: Set<String> = emptySet(),
    val onlyImportant: Boolean = true,
    val groupIntoSummary: Boolean = true,
)

@Serializable
private data class WidgetPrefsDto(
    val showOnlyImportant: Boolean = false,
)

@Serializable
private data class MatchesPrefsDto(
    val football: Boolean = true,
    val basketball: Boolean = true,
    val refreshIntervalHours: Int = 24,
)

@Serializable
private data class MatchesCacheDto(
    val fetchedAt: Long,
    val matches: List<Match>,
)

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val readerKey = stringPreferencesKey("reader_prefs")
    private val syncKey = stringPreferencesKey("sync_prefs")
    private val notificationKey = stringPreferencesKey("notification_prefs")
    private val widgetKey = stringPreferencesKey("widget_prefs")
    private val matchesKey = stringPreferencesKey("matches_prefs")
    private val matchesCacheKey = stringPreferencesKey("matches_cache")
    private val appThemeKey = stringPreferencesKey("app_theme_mode")
    private val lastOpenedAtKey = longPreferencesKey("last_opened_at")
    private val lastSyncCompletedAtKey = longPreferencesKey("last_sync_completed_at")
    private val lastSyncOutcomeKey = stringPreferencesKey("last_sync_outcome")

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

    val widgetPrefs: Flow<WidgetPrefs> = context.dataStore.data.map { prefs ->
        val dto = prefs[widgetKey]?.let { runCatching { json.decodeFromString<WidgetPrefsDto>(it) }.getOrNull() } ?: WidgetPrefsDto()
        dto.toDomain()
    }

    val matchesPrefs: Flow<MatchesPrefs> = context.dataStore.data.map { prefs ->
        val dto = prefs[matchesKey]?.let { runCatching { json.decodeFromString<MatchesPrefsDto>(it) }.getOrNull() } ?: MatchesPrefsDto()
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

    /** Stamped by [gr.thrylos.news.data.sync.SyncWorker] on every doWork() exit —
     *  including its early-return guards (quiet hours / offline / wifi-only) — so the
     *  feed can show "when did this last really happen, and why did it stop there" as
     *  a single glance-able diagnostic instead of a silent no-op. [lastSyncOutcome] is
     *  "Ολοκληρώθηκε" on a normal completion, or the guard's reason otherwise. */
    val lastSyncCompletedAt: Flow<Long?> = context.dataStore.data.map { prefs -> prefs[lastSyncCompletedAtKey] }
    val lastSyncOutcome: Flow<String?> = context.dataStore.data.map { prefs -> prefs[lastSyncOutcomeKey] }

    suspend fun recordSyncAttempt(outcome: String) {
        context.dataStore.edit { prefs ->
            prefs[lastSyncCompletedAtKey] = System.currentTimeMillis()
            prefs[lastSyncOutcomeKey] = outcome
        }
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

    suspend fun updateWidgetPrefs(update: (WidgetPrefs) -> WidgetPrefs) {
        context.dataStore.edit { prefs ->
            val current = prefs[widgetKey]?.let { runCatching { json.decodeFromString<WidgetPrefsDto>(it) }.getOrNull() } ?: WidgetPrefsDto()
            val next = update(current.toDomain()).toDto()
            prefs[widgetKey] = json.encodeToString(WidgetPrefsDto.serializer(), next)
        }
    }

    suspend fun updateMatchesPrefs(update: (MatchesPrefs) -> MatchesPrefs) {
        context.dataStore.edit { prefs ->
            val current = prefs[matchesKey]?.let { runCatching { json.decodeFromString<MatchesPrefsDto>(it) }.getOrNull() } ?: MatchesPrefsDto()
            val next = update(current.toDomain()).toDto()
            prefs[matchesKey] = json.encodeToString(MatchesPrefsDto.serializer(), next)
        }
    }

    /** The last successfully fetched match list, with when it was fetched — fixtures
     *  barely change within a day, so the overlay reuses this instead of always
     *  hitting Sofascore, refreshing only once [MatchesPrefs.refreshIntervalHours] has
     *  passed or the user explicitly taps refresh. */
    suspend fun cachedMatches(): Pair<Long, List<Match>>? {
        val raw = context.dataStore.data.first()[matchesCacheKey] ?: return null
        val dto = runCatching { json.decodeFromString<MatchesCacheDto>(raw) }.getOrNull() ?: return null
        return dto.fetchedAt to dto.matches
    }

    suspend fun cacheMatches(matches: List<Match>) {
        context.dataStore.edit { prefs ->
            prefs[matchesCacheKey] = json.encodeToString(
                MatchesCacheDto.serializer(),
                MatchesCacheDto(fetchedAt = System.currentTimeMillis(), matches = matches),
            )
        }
    }

    private fun ReaderPrefsDto.toDomain() = ReaderPrefs(theme, fontFamily, fontScale, lineHeightScale, marginWidth, textAlign, keepScreenOn)
    private fun ReaderPrefs.toDto() = ReaderPrefsDto(theme, fontFamily, fontScale, lineHeightScale, marginWidth, textAlign, keepScreenOn)

    private fun SyncPrefsDto.toDomain() = SyncPrefs(
        refreshInterval, syncOnlyOnWifi, downloadImagesOnlyOnWifi, offlineRetentionDays, offlineMaxArticles,
        prefetchImagesForOffline, quietHoursEnabled, quietHoursStartMinute, quietHoursEndMinute, highlightNewSinceRefresh,
        feedPageSize,
    )
    private fun SyncPrefs.toDto() = SyncPrefsDto(
        refreshInterval, syncOnlyOnWifi, downloadImagesOnlyOnWifi, offlineRetentionDays, offlineMaxArticles,
        prefetchImagesForOffline, quietHoursEnabled, quietHoursStartMinute, quietHoursEndMinute, highlightNewSinceRefresh,
        feedPageSize,
    )

    private fun NotificationPrefsDto.toDomain() = NotificationPrefs(enabled, onlySourceIds, onlyKeywords, onlyImportant, groupIntoSummary)
    private fun NotificationPrefs.toDto() = NotificationPrefsDto(enabled, onlySourceIds, onlyKeywords, onlyImportant, groupIntoSummary)

    private fun WidgetPrefsDto.toDomain() = WidgetPrefs(showOnlyImportant)
    private fun WidgetPrefs.toDto() = WidgetPrefsDto(showOnlyImportant)

    private fun MatchesPrefsDto.toDomain() = MatchesPrefs(football, basketball, refreshIntervalHours)
    private fun MatchesPrefs.toDto() = MatchesPrefsDto(football, basketball, refreshIntervalHours)
}
