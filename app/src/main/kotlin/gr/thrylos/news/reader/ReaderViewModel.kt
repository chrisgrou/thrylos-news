package gr.thrylos.news.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gr.thrylos.news.data.prefs.AppPreferences
import gr.thrylos.news.data.repo.ArticleRepository
import gr.thrylos.news.feed.ArticleListCursor
import gr.thrylos.news.model.Article
import gr.thrylos.news.model.ReaderPrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val articleRepository: ArticleRepository,
    private val preferences: AppPreferences,
    cursor: ArticleListCursor,
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
}
