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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

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
