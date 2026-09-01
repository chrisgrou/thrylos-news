package gr.thrylos.news

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import gr.thrylos.news.data.prefs.AppPreferences
import gr.thrylos.news.data.prefs.NewArticlesBoundary
import gr.thrylos.news.data.repo.SourceRepository
import gr.thrylos.news.data.sync.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** How long to hold off (re)scheduling sync work after a cold start. Applying the
 *  schedule can make WorkManager start a sync immediately, and that sync's concurrent
 *  network fetches + HTML parsing are CPU-heavy enough on modest devices to visibly
 *  delay the feed's own first read of already-cached articles — the app looked
 *  "sluggish" and blank on open instead of showing what was already there. Giving the
 *  UI a head start before any sync can kick in fixes that without changing what gets
 *  synced or how often. */
private const val SYNC_SCHEDULE_STARTUP_DELAY_MS = 8_000L

@HiltAndroidApp
class ThrylosApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var sourceRepository: SourceRepository

    @Inject
    lateinit var syncScheduler: SyncScheduler

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var newArticlesBoundary: NewArticlesBoundary

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            loadBundledPluginsIfNeeded()
            newArticlesBoundary.initializeOnce()
        }
        appScope.launch {
            // Deliberately on its own launch{}, after the two above — those two are
            // cheap/idempotent and don't compete for CPU, so they can run right away;
            // this delay is only about not letting a freshly (re)scheduled sync start
            // competing with the feed's very first paint.
            delay(SYNC_SCHEDULE_STARTUP_DELAY_MS)
            val syncPrefs = appPreferences.syncPrefs.first()
            syncScheduler.applySchedule(syncPrefs.refreshInterval, syncPrefs.syncOnlyOnWifi)
        }
    }

    /** Seeds the app on first run with the plugins in assets/plugins/ — never overwrites
     * a source the user has since edited or removed (see [SourceRepository.bundleIfAbsent]). */
    private suspend fun loadBundledPluginsIfNeeded() {
        val fileNames = assets.list("plugins") ?: return
        fileNames.filter { it.endsWith(".json") }.forEach { fileName ->
            runCatching {
                val json = assets.open("plugins/$fileName").bufferedReader(Charsets.UTF_8).use { it.readText() }
                sourceRepository.bundleIfAbsent(fileName, json)
            }
        }
    }
}
