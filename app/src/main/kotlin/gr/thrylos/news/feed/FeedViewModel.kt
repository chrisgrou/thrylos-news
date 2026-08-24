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

data class SourceChip(val id: String, val name: String)

data class FeedItem(val article: Article, val extraSourceCount: Int)

data class FeedUiState(
    val items: List<FeedItem> = emptyList(),
    val sources: List<SourceChip> = emptyList(),
    val selectedSourceId: String? = null,
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

    private val selectedSourceId = MutableStateFlow<String?>(null)
    private val unreadOnly = MutableStateFlow(false)

    private val sourcesFlow = sourceRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<FeedUiState> = combine(
        articleRepository.observeAll(),
        filterRepository.observeAll(),
        sourcesFlow,
        selectedSourceId,
        unreadOnly,
    ) { articles, filters, sources, sourceId, onlyUnread ->
        val visible = articles
            .filterNot { FilterEngine.isHidden(it, filters) }
            .filter { sourceId == null || it.sourceId == sourceId }
            .filter { !onlyUnread || !it.isRead }

        // Collapse dedup groups: an article whose dedupGroupId points at another
        // article is a duplicate — only the group's primary (or an ungrouped
        // article) becomes its own card, tagged with how many others it absorbed.
        val byId = articles.associateBy { it.id }
        val primaries = visible.filter { it.dedupGroupId == null || byId[it.dedupGroupId] == null || !visible.any { v -> v.id == it.dedupGroupId } }
        val groupCounts = visible.groupingBy { it.dedupGroupId ?: it.id }.eachCount()

        val items = primaries
            .sortedByDescending { it.publishedAt ?: it.fetchedAt }
            .map { FeedItem(it, (groupCounts[it.id] ?: 1) - 1) }

        FeedUiState(
            items = items,
            sources = sources.map { SourceChip(it.id, it.name) },
            selectedSourceId = sourceId,
            unreadOnly = onlyUnread,
            isEmpty = items.isEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FeedUiState())

    fun refresh() = syncScheduler.syncNow()

    fun selectSource(id: String?) {
        selectedSourceId.value = id
    }

    fun toggleUnreadOnly() {
        unreadOnly.value = !unreadOnly.value
    }

    fun openArticle(id: String) {
        cursor.setContext(uiState.value.items.map { it.article.id })
        viewModelScope.launch { articleRepository.setRead(id, true) }
    }
}
