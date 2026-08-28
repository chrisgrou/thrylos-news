package gr.thrylos.news.model

import kotlinx.serialization.Serializable

@Serializable
data class SyncPrefs(
    val refreshInterval: RefreshInterval = RefreshInterval.HOUR_1,
    val syncOnlyOnWifi: Boolean = false,
    val downloadImagesOnlyOnWifi: Boolean = true,
    val offlineRetentionDays: Int = 14,
    val offlineMaxArticles: Int = 500,
    val prefetchImagesForOffline: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    /** Minute-of-day (0..1439), local time. */
    val quietHoursStartMinute: Int = 23 * 60,
    val quietHoursEndMinute: Int = 7 * 60,
    /** Shows a "Νέα άρθρα" divider in the feed above articles fetched since the
     *  app was last opened. */
    val highlightNewSinceRefresh: Boolean = true,
    /** How many articles the feed shows per page. */
    val feedPageSize: Int = 10,
) {
    /** Handles overnight windows where start > end (e.g. 23:00 → 07:00). */
    fun isQuietAt(minuteOfDay: Int): Boolean {
        if (!quietHoursEnabled) return false
        return if (quietHoursStartMinute <= quietHoursEndMinute) {
            minuteOfDay in quietHoursStartMinute until quietHoursEndMinute
        } else {
            minuteOfDay >= quietHoursStartMinute || minuteOfDay < quietHoursEndMinute
        }
    }
}
