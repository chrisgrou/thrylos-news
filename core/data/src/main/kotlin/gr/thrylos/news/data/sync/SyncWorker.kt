package gr.thrylos.news.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import gr.thrylos.news.data.notifications.NotificationHelper
import gr.thrylos.news.data.prefs.AppPreferences
import gr.thrylos.news.data.repo.ArticleRepository
import gr.thrylos.news.data.repo.FilterRepository
import gr.thrylos.news.data.repo.SourceRepository
import gr.thrylos.news.data.widget.WidgetUpdater
import gr.thrylos.news.model.Article
import gr.thrylos.news.model.ArticleStub
import gr.thrylos.news.model.ContentBlock
import gr.thrylos.news.sources.dedup.Dedup
import gr.thrylos.news.sources.filter.FilterEngine
import gr.thrylos.news.sources.plugin.SourcePlugin
import gr.thrylos.news.sources.sync.SourceSyncCoordinator
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/** Caps how many new articles are fetched per source per run so one very active
 * source can't starve the others or make a single sync run unbounded. */
private const val MAX_NEW_PER_SOURCE = 25
private const val MAX_CONCURRENT_SOURCES = 3
private const val DEDUP_WINDOW_MS = 24 * 60 * 60 * 1000L

/** Bounds how many already-known-but-empty articles get retried per source per run —
 *  see [SyncWorker.reextractEmptyArticles]. */
private const val MAX_REEXTRACT_PER_SOURCE = 5

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sourceRepository: SourceRepository,
    private val articleRepository: ArticleRepository,
    private val filterRepository: FilterRepository,
    private val appPreferences: AppPreferences,
    private val notificationHelper: NotificationHelper,
    private val coordinator: SourceSyncCoordinator,
    private val widgetUpdater: WidgetUpdater,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val syncPrefs = appPreferences.currentSyncPrefs()
        if (!NetworkUtil.isOnline(applicationContext)) {
            appPreferences.recordSyncAttempt("Καμία σύνδεση δικτύου")
            return@withContext Result.retry()
        }
        if (syncPrefs.syncOnlyOnWifi && !NetworkUtil.isOnWifi(applicationContext)) {
            appPreferences.recordSyncAttempt("Ρύθμιση \"Μόνο σε Wi-Fi\" ενεργή, όχι σε Wi-Fi")
            return@withContext Result.success()
        }

        val plugins = sourceRepository.getEnabledPlugins()
        val filters = filterRepository.getEnabled()
        val semaphore = Semaphore(MAX_CONCURRENT_SOURCES)

        val newArticles = coroutineScope {
            plugins.map { plugin ->
                async {
                    semaphore.withPermit {
                        runCatching { syncOneSource(plugin, filters) }
                            .onSuccess { sourceRepository.recordSyncResult(plugin.id, error = null) }
                            .onFailure { e -> sourceRepository.recordSyncResult(plugin.id, error = e.message ?: e.toString()) }
                            .getOrElse { emptyList() }
                    }
                }
            }.flatMap { it.await() }
        }

        appPreferences.recordSyncAttempt("Ολοκληρώθηκε")
        recomputeDedupGroups()

        // Quiet hours mute notifications only — sync itself must keep running, or the
        // feed simply stops updating (silently, from the user's point of view) for
        // however long the quiet window lasts.
        val now = Calendar.getInstance()
        val minuteOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val isQuiet = syncPrefs.isQuietAt(minuteOfDay)

        if (newArticles.isNotEmpty()) {
            val notificationPrefs = appPreferences.currentNotificationPrefs()
            if (notificationPrefs.enabled && !isQuiet) {
                val eligible = newArticles.filter { article ->
                    (notificationPrefs.onlySourceIds.isEmpty() || article.sourceId in notificationPrefs.onlySourceIds) &&
                        (notificationPrefs.onlyKeywords.isEmpty() || notificationPrefs.onlyKeywords.any { article.title.contains(it, ignoreCase = true) }) &&
                        (!notificationPrefs.onlyImportant || FilterEngine.isImportant(article, filters)) &&
                        FilterEngine.isVisible(article, filters)
                }
                notificationHelper.notifyNewArticles(eligible, notificationPrefs.groupIntoSummary)
            }
        }

        articleRepository.runOfflineCleanup(syncPrefs.offlineRetentionDays, syncPrefs.offlineMaxArticles)
        widgetUpdater.requestUpdate()
        Result.success()
    }

    private suspend fun syncOneSource(plugin: SourcePlugin, filters: List<gr.thrylos.news.model.FilterRule>): List<Article> {
        val known = articleRepository.existingCanonicalUrls(plugin.id)
        val stubs = coordinator.discoverNew(plugin, known)
            .filterNot { FilterEngine.isHiddenStub(it.title, plugin.id, plugin.name, it.url, filters) }
            .take(MAX_NEW_PER_SOURCE)

        val extracted = stubs.mapNotNull { stub ->
            runCatching { coordinator.extractArticle(plugin, stub) }.getOrNull()
        }
        val reextracted = reextractEmptyArticles(plugin)
        val upserts = extracted + reextracted
        if (upserts.isNotEmpty()) articleRepository.upsertAll(upserts)

        return extracted.filter { FilterEngine.isVisible(it, filters) }
    }

    /** Some publishers (Sport24 in particular) briefly publish a "developing story"
     *  page with a headline but no body text yet, then fill it in within minutes —
     *  but a URL that's already known is never revisited by [coordinator]'s
     *  discovery, so an article synced during that empty window stays permanently
     *  empty. Retries extraction for a bounded number of the most recently fetched
     *  already-known articles that ended up with no paragraph content, keeping the
     *  stored read/bookmark/dedup state and only overwriting when the retry actually
     *  found real content this time (an article that's genuinely just short, or a
     *  source whose selectors are broken, isn't retried forever — only up to
     *  [MAX_REEXTRACT_PER_SOURCE] per run). */
    private suspend fun reextractEmptyArticles(plugin: SourcePlugin): List<Article> {
        val empty = articleRepository.getAllOnce()
            .filter { it.sourceId == plugin.id && it.content.none { block -> block is ContentBlock.Paragraph } }
            .sortedByDescending { it.fetchedAt }
            .take(MAX_REEXTRACT_PER_SOURCE)
        if (empty.isEmpty()) return emptyList()

        return empty.mapNotNull { existing ->
            runCatching {
                val stub = ArticleStub(plugin.id, existing.url, existing.title)
                val fresh = coordinator.extractArticle(plugin, stub)
                fresh.copy(isRead = existing.isRead, isBookmarked = existing.isBookmarked, dedupGroupId = existing.dedupGroupId)
            }.getOrNull()
        }.filter { it.content.any { block -> block is ContentBlock.Paragraph } }
    }

    private suspend fun recomputeDedupGroups() {
        val recent = articleRepository.getAllOnce().filter {
            val time = it.publishedAt ?: it.fetchedAt
            System.currentTimeMillis() - time <= DEDUP_WINDOW_MS
        }
        if (recent.size < 2) return
        val groups = Dedup.groupDuplicates(recent, windowMs = DEDUP_WINDOW_MS)
        val changed = recent.mapNotNull { article ->
            val groupId = groups[article.id]?.takeIf { it != article.id }
            if (groupId != article.dedupGroupId) article.id to groupId else null
        }
        articleRepository.setDedupGroups(changed)
    }
}
