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
)
