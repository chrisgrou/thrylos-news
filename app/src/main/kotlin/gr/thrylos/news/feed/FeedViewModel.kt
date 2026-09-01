package gr.thrylos.news.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gr.thrylos.news.data.prefs.AppPreferences
import gr.thrylos.news.data.prefs.NewArticlesBoundary
import gr.thrylos.news.data.repo.ArticleRepository
import gr.thrylos.news.data.repo.FilterRepository
import gr.thrylos.news.data.repo.SourceRepository
import gr.thrylos.news.data.sync.SyncScheduler
import gr.thrylos.news.model.Article
import gr.thrylos.news.model.FilterRule
import gr.thrylos.news.sources.filter.FilterEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Strips the " — Ολυμπιακός"-style suffix bundled plugins add to their display name. */
fun stripSourceSuffix(name: String): String = name.substringBefore(" — ").trim()

/** Displayed source "chip". Two plugins sharing the same [name] (e.g. Sportal's
 *  separate football/basketball scrapers) collapse into a single chip whose
 *  selection filters by any of [memberSourceIds] — so the feed shows them as
 *  one unified source even though they're distinct plugins under the hood. */
data class SourceChip(val name: String, val memberSourceIds: Set<String>)

data class FeedItem(
    val article: Article,
    val extraSourceCount: Int,
    val isImportant: Boolean = false,
    /** Fetched after the app was last opened — see [NewArticlesBoundary]. */
    val isNew: Boolean = false,
)

data class FeedUiState(
    val items: List<FeedItem> = emptyList(),
    val sources: List<SourceChip> = emptyList(),
    val selectedSourceName: String? = null,
    val unreadOnly: Boolean = false,
    val isEmpty: Boolean = false,
    val page: Int = 0,
    val pageCount: Int = 1,
)

/** Everything that depends on articles/filters/sources/selected-source but NOT on the
 *  Όλα/Νέα toggle — kept as its own stage so flipping that toggle doesn't re-run
 *  FilterEngine (rule matching over every article) and dedup-collapse from scratch. */
private data class DedupedFeed(
    val items: List<FeedItem>,
    val sources: List<SourceChip>,
    val selectedSourceName: String?,
)

private data class ComputedFeed(
    val items: List<FeedItem>,
    val sources: List<SourceChip>,
    val selectedSourceName: String?,
    val unreadOnly: Boolean,
)

private data class FilterResult(val visible: Boolean, val important: Boolean)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val filterRepository: FilterRepository,
    sourceRepository: SourceRepository,
    private val syncScheduler: SyncScheduler,
    private val cursor: ArticleListCursor,
    appPreferences: AppPreferences,
    newArticlesBoundary: NewArticlesBoundary,
) : ViewModel() {

    private val selectedSourceName = MutableStateFlow<String?>(null)
    private val unreadOnly = MutableStateFlow(false)
    private val page = MutableStateFlow(0)

    /** What the user perceives has to change the instant they act, not once a DB
     *  write round-trips through Room's invalidation tracker and this whole
     *  filter/dedup/sort pipeline re-runs — that gap is exactly what made "mark all
     *  as read" feel laggy despite the write itself being fast. Ids added here render
     *  as read (and, under "Νέα", disappear) immediately; the real [setRead]/
     *  [setReadBatch] write still happens, it just isn't what the UI waits on. */
    private val optimisticReadIds = MutableStateFlow<Set<String>>(emptySet())

    /** Article rows are effectively immutable once synced — only isRead/isBookmarked/
     *  dedupGroupId change afterward via targeted UPDATEs, never contentJson, except
     *  when a broken article is re-extracted (which does bump fetchedAt). So keying on
     *  (id, fetchedAt) and rebuilding this map fresh each pass (reusing old entries,
     *  dropping ones for articles no longer present) skips re-decoding/re-joining a
     *  BODY/ANYWHERE rule's text for every already-seen article on every recomputation
     *  — including the several that fire back-to-back right after a sync starts, which
     *  was real, repeated cost behind both the sluggish-on-open feed and choppy
     *  scrolling while a sync is still catching up in the background. */
    private var filterResultCache: Map<String, FilterResult> = emptyMap()
    private var filterResultCacheFilters: List<FilterRule>? = null

    private val sourcesFlow = sourceRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** (highlight enabled, boundary timestamp — null until [NewArticlesBoundary] initializes,
     *  configured articles-per-page). */
    private val highlightConfig: Flow<Triple<Boolean, Long?, Int>> = combine(
        appPreferences.syncPrefs,
        newArticlesBoundary.threshold,
    ) { prefs, threshold -> Triple(prefs.highlightNewSinceRefresh, threshold, prefs.feedPageSize) }

    private val dedupedFeed: StateFlow<DedupedFeed> = combine(
        articleRepository.observeAll(),
        filterRepository.observeAll(),
        sourcesFlow,
        selectedSourceName,
        optimisticReadIds,
    ) { articles, filters, sources, sourceName, optimisticIds ->
        val chips = sources.groupBy { it.name }.map { (name, group) -> SourceChip(name, group.map { it.id }.toSet()) }
        val selectedIds = chips.firstOrNull { it.name == sourceName }?.memberSourceIds

        // A BODY/ANYWHERE filter rule needs an article's full joined body text, which —
        // for an article read back from Room — means decoding its stored content JSON.
        // Only recompute (id, fetchedAt) pairs not already in the cache from the last
        // pass; a real filter-rule edit invalidates the whole thing (new filters
        // instance), everything else (isRead toggles, a sync writing more articles)
        // reuses what's already known.
        if (filters !== filterResultCacheFilters) filterResultCache = emptyMap()
        filterResultCacheFilters = filters
        val needsBody = FilterEngine.rulesNeedBody(filters)
        val importantById = HashMap<String, Boolean>(articles.size)
        val newCache = HashMap<String, FilterResult>(articles.size)
        val visible = articles.filter { article ->
            if (selectedIds != null && article.sourceId !in selectedIds) return@filter false
            val cacheKey = "${article.id}:${article.fetchedAt}"
            val result = filterResultCache[cacheKey] ?: run {
                val body = if (needsBody) FilterEngine.bodyText(article) else null
                FilterResult(FilterEngine.isVisible(article, filters, body), FilterEngine.isImportant(article, filters, body))
            }
            newCache[cacheKey] = result
            if (!result.visible) return@filter false
            importantById[article.id] = result.important
            true
        }
        filterResultCache = newCache

        // Collapse dedup groups: an article whose dedupGroupId points at another
        // article is a duplicate — only the group's primary (or an ungrouped
        // article) becomes its own card, tagged with how many others it absorbed.
        val byId = articles.associateBy { it.id }
        val visibleIds = visible.mapTo(HashSet(visible.size)) { it.id }
        val primaries = visible.filter { it.dedupGroupId == null || byId[it.dedupGroupId] == null || it.dedupGroupId !in visibleIds }
        val groupCounts = visible.groupingBy { it.dedupGroupId ?: it.id }.eachCount()

        val items = primaries
            .sortedWith(
                // Important articles are pinned to the top only while unread — once
                // read, they drop back into normal chronological order.
                compareByDescending<Article> { importantById[it.id] == true && it.id !in optimisticIds && !it.isRead }
                    .thenByDescending { it.publishedAt ?: it.fetchedAt },
            )
            .map { article ->
                // Reflects an in-flight optimistic mark-as-read immediately, even
                // though the DB row (and thus this Article, straight from Room) may
                // not have caught up to it yet.
                val effective = if (!article.isRead && article.id in optimisticIds) article.copy(isRead = true) else article
                FeedItem(effective, (groupCounts[article.id] ?: 1) - 1, importantById[article.id] == true)
            }

        DedupedFeed(items = items, sources = chips, selectedSourceName = sourceName)
    }
        // This recomputes the full filter/dedup/sort pass over every stored article on
        // every single database change — combine()'s default dispatcher is whatever the
        // collector uses, which for viewModelScope is the main thread, so without this
        // it runs (and can jank scrolling/navigation animations) on the UI thread.
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DedupedFeed(emptyList(), emptyList(), null))

    /** Separate stage so toggling Όλα/Νέα is a cheap O(n) re-filter of the already
     *  deduped/sorted list instead of re-running FilterEngine + dedup from scratch —
     *  the Όλα/Νέα toggle doesn't change which articles pass the filter rules. */
    private val computedFeed: StateFlow<ComputedFeed> = combine(dedupedFeed, unreadOnly) { deduped, onlyUnread ->
        ComputedFeed(
            items = deduped.items.filter { !onlyUnread || !it.article.isRead },
            sources = deduped.sources,
            selectedSourceName = deduped.selectedSourceName,
            unreadOnly = onlyUnread,
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ComputedFeed(emptyList(), emptyList(), null, false))

    /** Caps how many cards render at once — a long feed makes scrolling sluggish, so
     *  results are paged (configurable page size, see [gr.thrylos.news.model.SyncPrefs.feedPageSize]). */
    val uiState: StateFlow<FeedUiState> = combine(computedFeed, page, highlightConfig) { computed, requestedPage, (highlightEnabled, threshold, pageSize) ->
        val items = if (highlightEnabled && threshold != null) {
            computed.items.map { it.copy(isNew = it.article.fetchedAt > threshold) }
        } else {
            computed.items
        }
        val pageCount = maxOf(1, (items.size + pageSize - 1) / pageSize)
        val clampedPage = requestedPage.coerceIn(0, pageCount - 1)
        FeedUiState(
            items = items.drop(clampedPage * pageSize).take(pageSize),
            sources = computed.sources,
            selectedSourceName = computed.selectedSourceName,
            unreadOnly = computed.unreadOnly,
            isEmpty = items.isEmpty(),
            page = clampedPage,
            pageCount = pageCount,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FeedUiState())

    val isSyncing: StateFlow<Boolean> = syncScheduler.observeSyncing()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** Whether the currently visible (filtered, current tab, current page) articles
     *  include anything unread — markAllRead() only acts on those, not the whole DB. */
    val hasUnread: StateFlow<Boolean> = uiState
        .map { state -> state.items.any { !it.article.isRead } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val lastSyncAt: StateFlow<Long?> = appPreferences.lastSyncCompletedAt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val lastSyncOutcome: StateFlow<String?> = appPreferences.lastSyncOutcome
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun refresh() = syncScheduler.syncNow()

    /** Marks only what's actually on screen right now (current tab, filters, source
     *  selection, and page) as read — not every unread article in the database,
     *  including ones on other pages or hidden by the current filters. */
    fun markAllRead() {
        val ids = uiState.value.items.map { it.article.id }
        optimisticReadIds.value = optimisticReadIds.value + ids
        viewModelScope.launch { articleRepository.setReadBatch(ids) }
    }

    fun markRead(id: String) {
        optimisticReadIds.value = optimisticReadIds.value + id
        viewModelScope.launch { articleRepository.setRead(id, true) }
    }

    fun selectSource(name: String?) {
        selectedSourceName.value = name
        page.value = 0
    }

    fun setUnreadOnly(value: Boolean) {
        unreadOnly.value = value
        page.value = 0
    }

    fun setPage(index: Int) {
        page.value = index.coerceAtLeast(0)
    }

    fun openArticle(id: String) {
        cursor.setContext(uiState.value.items.map { it.article.id })
        optimisticReadIds.value = optimisticReadIds.value + id
        viewModelScope.launch { articleRepository.setRead(id, true) }
    }
}
