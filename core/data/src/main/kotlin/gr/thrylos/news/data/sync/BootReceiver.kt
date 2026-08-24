package gr.thrylos.news.data.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import gr.thrylos.news.data.prefs.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/** AlarmManager alarms (used for sub-15-minute refresh intervals) are cleared on
 *  reboot, so re-apply the current schedule once the device comes back up. */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var appPreferences: AppPreferences
    @Inject lateinit var syncScheduler: SyncScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = appPreferences.currentSyncPrefs()
                syncScheduler.applySchedule(prefs.refreshInterval, prefs.syncOnlyOnWifi)
            } finally {
                pending.finish()
            }
        }
    }
}
