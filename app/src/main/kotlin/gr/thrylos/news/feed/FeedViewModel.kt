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

private const val PAGE_SIZE = 10

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

    private val sourcesFlow = sourceRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** (highlight enabled, boundary timestamp — null until [NewArticlesBoundary] initializes). */
    private val highlightConfig: Flow<Pair<Boolean, Long?>> = combine(
        appPreferences.syncPrefs,
        newArticlesBoundary.threshold,
    ) { prefs, threshold -> prefs.highlightNewSinceRefresh to threshold }

    private val dedupedFeed: StateFlow<DedupedFeed> = combine(
        articleRepository.observeAll(),
        filterRepository.observeAll(),
        sourcesFlow,
        selectedSourceName,
    ) { articles, filters, sources, sourceName ->
        val chips = sources.groupBy { it.name }.map { (name, group) -> SourceChip(name, group.map { it.id }.toSet()) }
        val selectedIds = chips.firstOrNull { it.name == sourceName }?.memberSourceIds

        val visible = articles
            .filter { FilterEngine.isVisible(it, filters) }
            .filter { selectedIds == null || it.sourceId in selectedIds }

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
                compareByDescending<Article> { FilterEngine.isImportant(it, filters) && !it.isRead }
                    .thenByDescending { it.publishedAt ?: it.fetchedAt },
            )
            .map { FeedItem(it, (groupCounts[it.id] ?: 1) - 1, FilterEngine.isImportant(it, filters)) }

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
     *  results are paged (PAGE_SIZE per page) instead of dumping everything into one list. */
    val uiState: StateFlow<FeedUiState> = combine(computedFeed, page, highlightConfig) { computed, requestedPage, (highlightEnabled, threshold) ->
        val items = if (highlightEnabled && threshold != null) {
            computed.items.map { it.copy(isNew = it.article.fetchedAt > threshold) }
        } else {
            computed.items
        }
        val pageCount = maxOf(1, (items.size + PAGE_SIZE - 1) / PAGE_SIZE)
        val clampedPage = requestedPage.coerceIn(0, pageCount - 1)
        FeedUiState(
            items = items.drop(clampedPage * PAGE_SIZE).take(PAGE_SIZE),
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

    /** Whether any article anywhere is unread — used to disable "mark all as read"
     *  when there's nothing for it to do (markAllRead() affects every article, not
     *  just the currently filtered/paged ones). */
    val hasUnread: StateFlow<Boolean> = articleRepository.observeAll()
        .map { articles -> articles.any { !it.isRead } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val lastSyncAt: StateFlow<Long?> = appPreferences.lastSyncCompletedAt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val lastSyncOutcome: StateFlow<String?> = appPreferences.lastSyncOutcome
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun refresh() = syncScheduler.syncNow()

    fun markAllRead() {
        viewModelScope.launch { articleRepository.markAllRead() }
    }

    fun markRead(id: String) {
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
        viewModelScope.launch { articleRepository.setRead(id, true) }
    }
}
