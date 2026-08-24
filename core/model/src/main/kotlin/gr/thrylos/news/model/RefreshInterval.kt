package gr.thrylos.news.model

import kotlinx.serialization.Serializable

/** Minimum enforced by WorkManager's PeriodicWorkRequest is 15 minutes. */
@Serializable
enum class RefreshInterval(val minutes: Long?) {
    NEVER(null),
    MIN_15(15),
    MIN_30(30),
    HOUR_1(60),
    HOUR_3(180),
    HOUR_6(360),
    HOUR_12(720),
    DAILY(1440),
}
