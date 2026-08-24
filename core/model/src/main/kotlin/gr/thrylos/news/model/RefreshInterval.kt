package gr.thrylos.news.model

import kotlinx.serialization.Serializable

/** WorkManager's PeriodicWorkRequest enforces a 15-minute minimum, so anything
 *  shorter than that is scheduled with AlarmManager instead — see SyncScheduler. */
@Serializable
enum class RefreshInterval(val minutes: Long?) {
    NEVER(null),
    MIN_1(1),
    MIN_5(5),
    MIN_10(10),
    MIN_15(15),
    MIN_30(30),
    HOUR_1(60),
    HOUR_3(180),
    HOUR_6(360),
    HOUR_12(720),
    DAILY(1440),
}
