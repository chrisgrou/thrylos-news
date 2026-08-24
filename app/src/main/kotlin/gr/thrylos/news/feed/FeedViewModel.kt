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

/** Displayed source "chip". Two plugins sharing the same [name] (e.g. Sportal's
 *  separate football/basketball scrapers) collapse into a single chip whose
 *  selection filters by any of [memberSourceIds] — so the feed shows them as
 *  one unified source even though they're distinct plugins under the hood. */
data class SourceChip(val name: String, val memberSourceIds: Set<String>)

data class FeedItem(val article: Article, val extraSourceCount: Int, val isImportant: Boolean = false)

data class FeedUiState(
    val items: List<FeedItem> = emptyList(),
    val sources: List<SourceChip> = emptyList(),
    val selectedSourceName: String? = null,
    val unreadOnly: Boolean = false,
    val isEmpty: Boolean = false,
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

    private val sourcesFlow = sourceRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<FeedUiState> = combine(
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
                compareByDescending<Article> { FilterEngine.isImportant(it, filters) }
                    .thenByDescending { it.publishedAt ?: it.fetchedAt },
            )
            .map { FeedItem(it, (groupCounts[it.id] ?: 1) - 1, FilterEngine.isImportant(it, filters)) }

        FeedUiState(
            items = items,
            sources = chips,
            selectedSourceName = sourceName,
            unreadOnly = onlyUnread,
            isEmpty = items.isEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FeedUiState())

    fun refresh() = syncScheduler.syncNow()

    fun selectSource(name: String?) {
        selectedSourceName.value = name
    }

    fun toggleUnreadOnly() {
        unreadOnly.value = !unreadOnly.value
    }

    fun openArticle(id: String) {
        cursor.setContext(uiState.value.items.map { it.article.id })
        viewModelScope.launch { articleRepository.setRead(id, true) }
    }
}
