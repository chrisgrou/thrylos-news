package gr.thrylos.news.profile

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gr.thrylos.news.data.repo.ArticleRepository
import gr.thrylos.news.data.repo.SourceRepository
import gr.thrylos.news.data.repo.SourceWithPlugin
import gr.thrylos.news.feed.ArticleListCursor
import gr.thrylos.news.model.Article
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * "Home" view of one source (by display name, so grouped plugins like Sportal's
 * football/basketball scrapers share a single profile) — unfiltered, so articles
 * hidden by the user's filter rules are still visible here. Also where that source
 * is managed (edit/delete), since the sources list itself only shows its toggle.
 */
@HiltViewModel
class SourceProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val articleRepository: ArticleRepository,
    private val sourceRepository: SourceRepository,
    private val cursor: ArticleListCursor,
) : ViewModel() {

    val sourceName: String = Uri.decode(checkNotNull(savedStateHandle.get<String>("sourceName")))

    val articles: StateFlow<List<Article>> = articleRepository.observeBySourceName(sourceName)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val authors: StateFlow<List<String>> = articles
        .map { list -> list.mapNotNull { it.author }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Two plugins can share a display name (e.g. Sportal's football/basketball
     *  scrapers) — edit needs to know which underlying plugin(s) back this source. */
    val members: StateFlow<List<SourceWithPlugin>> = sourceRepository.observeAll()
        .map { list -> list.filter { it.name == sourceName } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setCursorContext(ids: List<String>) {
        cursor.setContext(ids)
    }

    fun deleteSource() {
        viewModelScope.launch { members.value.forEach { sourceRepository.remove(it) } }
    }
}
