package gr.thrylos.news.profile

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gr.thrylos.news.data.repo.ArticleRepository
import gr.thrylos.news.data.repo.FilterRepository
import gr.thrylos.news.feed.ArticleListCursor
import gr.thrylos.news.model.Article
import gr.thrylos.news.model.FilterAction
import gr.thrylos.news.model.FilterCombinator
import gr.thrylos.news.model.FilterCondition
import gr.thrylos.news.model.FilterField
import gr.thrylos.news.model.FilterMatch
import gr.thrylos.news.model.FilterRule
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val AUTHOR_IMPORTANT_PREFIX = "author-important-"
private const val AUTHOR_IGNORE_PREFIX = "author-ignore-"

/** "Home" view of one author — unfiltered, across every source they write for. */
@HiltViewModel
class AuthorProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    articleRepository: ArticleRepository,
    private val filterRepository: FilterRepository,
    private val cursor: ArticleListCursor,
) : ViewModel() {

    val author: String = Uri.decode(checkNotNull(savedStateHandle.get<String>("author")))

    val articles: StateFlow<List<Article>> = articleRepository.observeByAuthor(author)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isImportant: StateFlow<Boolean> = filterRepository.observeAll()
        .map { rules -> rules.any { it.id == AUTHOR_IMPORTANT_PREFIX + author && it.enabled } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isIgnored: StateFlow<Boolean> = filterRepository.observeAll()
        .map { rules -> rules.any { it.id == AUTHOR_IGNORE_PREFIX + author && it.enabled } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun toggleImportant() {
        viewModelScope.launch {
            toggleRule(AUTHOR_IMPORTANT_PREFIX + author, FilterAction.IMPORTANT)
        }
    }

    fun toggleIgnored() {
        viewModelScope.launch {
            toggleRule(AUTHOR_IGNORE_PREFIX + author, FilterAction.HIDE)
        }
    }

    private suspend fun toggleRule(id: String, action: FilterAction) {
        val existing = filterRepository.getEnabled().firstOrNull { it.id == id }
        if (existing != null) {
            filterRepository.delete(existing)
        } else {
            filterRepository.upsert(
                FilterRule(
                    id = id,
                    conditions = listOf(FilterCondition(FilterField.AUTHOR, FilterMatch.EXACT, author)),
                    combinator = FilterCombinator.AND,
                    action = action,
                ),
            )
        }
    }

    fun setCursorContext(ids: List<String>) {
        cursor.setContext(ids)
    }
}
