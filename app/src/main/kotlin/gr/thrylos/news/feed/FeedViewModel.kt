package gr.thrylos.news.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gr.thrylos.news.data.repo.ArticleRepository
import gr.thrylos.news.data.repo.FilterRepository
import gr.thrylos.news.data.repo.SourceRepository
import gr.thrylos.news.data.sync.SyncScheduler
import gr.thrylos.news.model.Article
import gr.thrylos.news.sources.filter.FilterEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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

data class FeedItem(val article: Article, val extraSourceCount: Int, val isImportant: Boolean = false)

private const val PAGE_SIZE = 20

data class FeedUiState(
    val items: List<FeedItem> = emptyList(),
    val sources: List<SourceChip> = emptyList(),
    val selectedSourceName: String? = null,
    val unreadOnly: Boolean = false,
    val isEmpty: Boolean = false,
    val page: Int = 0,
    val pageCount: Int = 1,
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
) : ViewModel() {

    private val selectedSourceName = MutableStateFlow<String?>(null)
    private val unreadOnly = MutableStateFlow(false)
    private val page = MutableStateFlow(0)

    private val sourcesFlow = sourceRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val computedFeed: StateFlow<ComputedFeed> = combine(
        articleRepository.observeAll(),
        filterRepository.observeAll(),
        sourcesFlow,
        selectedSourceName,
        unreadOnly,
    ) { articles, filters, sources, sourceName, onlyUnread ->
        val chips = sources.groupBy { it.name }.map { (name, group) -> SourceChip(name, group.map { it.id }.toSet()) }
        val selectedIds = chips.firstOrNull { it.name == sourceName }?.memberSourceIds

        val visible = articles
            .filter { FilterEngine.isVisible(it, filters) }
            .filter { selectedIds == null || it.sourceId in selectedIds }
            .filter { !onlyUnread || !it.isRead }

        // Collapse dedup groups: an article whose dedupGroupId points at another
        // article is a duplicate — only the group's primary (or an ungrouped
        // article) becomes its own card, tagged with how many others it absorbed.
        val byId = articles.associateBy { it.id }
        val primaries = visible.filter { it.dedupGroupId == null || byId[it.dedupGroupId] == null || !visible.any { v -> v.id == it.dedupGroupId } }
        val groupCounts = visible.groupingBy { it.dedupGroupId ?: it.id }.eachCount()

        val items = primaries
            .sortedWith(
                // Important articles are pinned to the top only while unread — once
                // read, they drop back into normal chronological order.
                compareByDescending<Article> { FilterEngine.isImportant(it, filters) && !it.isRead }
                    .thenByDescending { it.publishedAt ?: it.fetchedAt },
            )
            .map { FeedItem(it, (groupCounts[it.id] ?: 1) - 1, FilterEngine.isImportant(it, filters)) }

        ComputedFeed(items = items, sources = chips, selectedSourceName = sourceName, unreadOnly = onlyUnread)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ComputedFeed(emptyList(), emptyList(), null, false))

    /** Caps how many cards render at once — a long feed makes scrolling sluggish, so
     *  results are paged (PAGE_SIZE per page) instead of dumping everything into one list. */
    val uiState: StateFlow<FeedUiState> = combine(computedFeed, page) { computed, requestedPage ->
        val pageCount = maxOf(1, (computed.items.size + PAGE_SIZE - 1) / PAGE_SIZE)
        val clampedPage = requestedPage.coerceIn(0, pageCount - 1)
        FeedUiState(
            items = computed.items.drop(clampedPage * PAGE_SIZE).take(PAGE_SIZE),
            sources = computed.sources,
            selectedSourceName = computed.selectedSourceName,
            unreadOnly = computed.unreadOnly,
            isEmpty = computed.items.isEmpty(),
            page = clampedPage,
            pageCount = pageCount,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FeedUiState())

    val isSyncing: StateFlow<Boolean> = syncScheduler.observeSyncing()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun refresh() = syncScheduler.syncNow()

    fun markAllRead() {
        viewModelScope.launch { articleRepository.markAllRead() }
    }

    fun selectSource(name: String?) {
        selectedSourceName.value = name
        page.value = 0
    }

    fun toggleUnreadOnly() {
        unreadOnly.value = !unreadOnly.value
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
