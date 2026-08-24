package gr.thrylos.news.data.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/** Fired by AlarmManager for refresh intervals shorter than WorkManager's 15-minute
 *  floor (see [SyncScheduler]). Just enqueues a one-time sync — SyncWorker itself
 *  already re-checks the wifi-only and quiet-hours settings before doing anything. */
class SyncAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<SyncWorker>().build())
    }
}
