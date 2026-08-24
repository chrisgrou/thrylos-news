package gr.thrylos.news.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gr.thrylos.news.data.prefs.AppPreferences
import gr.thrylos.news.data.repo.ArticleRepository
import gr.thrylos.news.data.repo.FilterRepository
import gr.thrylos.news.data.repo.SourceRepository
import gr.thrylos.news.feed.ArticleListCursor
import gr.thrylos.news.model.Article
import gr.thrylos.news.model.FilterAction
import gr.thrylos.news.model.FilterCombinator
import gr.thrylos.news.model.FilterCondition
import gr.thrylos.news.model.FilterField
import gr.thrylos.news.model.FilterMatch
import gr.thrylos.news.model.FilterRule
import gr.thrylos.news.model.ReaderPrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SOURCE_IMPORTANT_PREFIX = "source-important-"

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val articleRepository: ArticleRepository,
    private val preferences: AppPreferences,
    private val sourceRepository: SourceRepository,
    private val filterRepository: FilterRepository,
    private val cursor: ArticleListCursor,
) : ViewModel() {

    private val articleId: String = checkNotNull(savedStateHandle["articleId"])

    val idList: List<String> = cursor.currentIds.ifEmpty { listOf(articleId) }
    val startIndex: Int = idList.indexOf(articleId).coerceAtLeast(0)

    val readerPrefs: StateFlow<ReaderPrefs> = preferences.readerPrefs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReaderPrefs())

    private val articleFlows = mutableMapOf<String, StateFlow<Article?>>()

    fun articleFlow(id: String): StateFlow<Article?> = articleFlows.getOrPut(id) {
        articleRepository.observeById(id).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    fun markRead(id: String) {
        viewModelScope.launch { articleRepository.setRead(id, true) }
    }

    fun toggleBookmark(id: String, currentlyBookmarked: Boolean) {
        viewModelScope.launch { articleRepository.setBookmarked(id, !currentlyBookmarked) }
    }

    fun updateReaderPrefs(update: (ReaderPrefs) -> ReaderPrefs) {
        viewModelScope.launch { preferences.updateReaderPrefs(update) }
    }

    fun articlesForSource(sourceId: String): Flow<List<Article>> = articleRepository.observeBySource(sourceId)

    fun isSourceImportant(sourceId: String): Flow<Boolean> = filterRepository.observeAll()
        .map { rules -> rules.any { it.id == SOURCE_IMPORTANT_PREFIX + sourceId && it.enabled } }

    fun toggleSourceImportant(sourceId: String, sourceName: String) {
        viewModelScope.launch {
            val id = SOURCE_IMPORTANT_PREFIX + sourceId
            val existing = filterRepository.getEnabled().firstOrNull { it.id == id }
            if (existing != null) {
                filterRepository.delete(existing)
            } else {
                filterRepository.upsert(
                    FilterRule(
                        id = id,
                        conditions = listOf(FilterCondition(FilterField.SOURCE, FilterMatch.EXACT, sourceName)),
                        combinator = FilterCombinator.AND,
                        action = FilterAction.IMPORTANT,
                        scopeSourceId = sourceId,
                    ),
                )
            }
        }
    }

    /** Point the swipe-navigation cursor at a different article list (e.g. one
     *  source's articles from the source banner) before navigating into it. */
    fun setCursorContext(ids: List<String>) {
        cursor.setContext(ids)
    }

    fun ignoreSource(sourceId: String) {
        viewModelScope.launch { sourceRepository.setEnabled(sourceId, false) }
    }
}
