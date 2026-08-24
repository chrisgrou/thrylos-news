package gr.thrylos.news.data.sync

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import gr.thrylos.news.model.RefreshInterval
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val PERIODIC_WORK_NAME = "gr.thrylos.news.periodic_sync"

@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Call whenever the refresh interval or the wifi-only setting changes. */
    fun applySchedule(interval: RefreshInterval, wifiOnly: Boolean) {
        val workManager = WorkManager.getInstance(context)
        val minutes = interval.minutes
        if (minutes == null) {
            workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
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

    /** Manual pull-to-refresh: runs once, ignoring the wifi-only setting (the user asked explicitly). */
    fun syncNow() {
        WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<SyncWorker>().build())
    }
}
