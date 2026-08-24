package gr.thrylos.news.profile

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SOURCE_IMPORTANT_PREFIX = "source-important-"

/**
 * "Home" view of one source (by display name, so grouped plugins like Sportal's
 * football/basketball scrapers share a single profile) — unfiltered, so articles
 * hidden by the user's filter rules are still visible here.
 */
@HiltViewModel
class SourceProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val articleRepository: ArticleRepository,
    private val sourceRepository: SourceRepository,
    private val filterRepository: FilterRepository,
    private val cursor: ArticleListCursor,
) : ViewModel() {

    val sourceName: String = Uri.decode(checkNotNull(savedStateHandle.get<String>("sourceName")))

    val articles: StateFlow<List<Article>> = articleRepository.observeBySourceName(sourceName)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val authors: StateFlow<List<String>> = articles
        .map { list -> list.mapNotNull { it.author }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isImportant: StateFlow<Boolean> = filterRepository.observeAll()
        .map { rules -> rules.any { it.id == SOURCE_IMPORTANT_PREFIX + sourceName && it.enabled } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun toggleImportant() {
        viewModelScope.launch {
            val id = SOURCE_IMPORTANT_PREFIX + sourceName
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
                    ),
                )
            }
        }
    }

    fun ignoreSource() {
        viewModelScope.launch {
            val members = sourceRepository.observeAll().first().filter { it.name == sourceName }
            members.forEach { sourceRepository.setEnabled(it.id, false) }
        }
    }

    fun setCursorContext(ids: List<String>) {
        cursor.setContext(ids)
    }
}
