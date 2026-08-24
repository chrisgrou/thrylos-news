package gr.thrylos.news.data.sync

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import gr.thrylos.news.model.RefreshInterval
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val PERIODIC_WORK_NAME = "gr.thrylos.news.periodic_sync"
private const val MANUAL_WORK_NAME = "gr.thrylos.news.manual_sync"
private const val ALARM_REQUEST_CODE = 4201

/** WorkManager's PeriodicWorkRequest enforces a 15-minute minimum interval. */
private const val MIN_WORKMANAGER_MINUTES = 15L

@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Call whenever the refresh interval or the wifi-only setting changes. */
    fun applySchedule(interval: RefreshInterval, wifiOnly: Boolean) {
        val workManager = WorkManager.getInstance(context)
        val minutes = interval.minutes
        cancelAlarm()

        if (minutes == null) {
            workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
            return
        }

        if (minutes < MIN_WORKMANAGER_MINUTES) {
            workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
            scheduleAlarm(minutes)
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<SyncWorker>(minutes, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    /** Manual pull-to-refresh: runs once, ignoring the wifi-only setting (the user asked explicitly).
     *  Uses a unique work name so [observeSyncing] can report real completion instead of the
     *  fire-and-forget instant true/false toggle a naive enqueue would give the UI. */
    fun syncNow() {
        WorkManager.getInstance(context).enqueueUniqueWork(
            MANUAL_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<SyncWorker>().build(),
        )
    }

    /** True while the manual sync triggered by [syncNow] is enqueued or running. */
    fun observeSyncing(): Flow<Boolean> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(MANUAL_WORK_NAME)
            .map { infos -> infos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING } }

    private fun alarmPendingIntent(): PendingIntent {
        val intent = Intent(context, SyncAlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, ALARM_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * Sub-15-minute refresh can't use WorkManager (15-minute platform floor), so it
     * falls back to AlarmManager instead. This is an inexact repeating alarm — no
     * special permission needed, but Android's Doze mode can batch or delay delivery
     * while the device is idle, so this is "as often as possible", not a guarantee.
     */
    private fun scheduleAlarm(minutes: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intervalMillis = minutes * 60_000L
        val triggerAt = SystemClock.elapsedRealtime() + intervalMillis
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, intervalMillis, alarmPendingIntent())
    }

    private fun cancelAlarm() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarmManager.cancel(alarmPendingIntent())
    }
}
