package gr.thrylos.news.profile

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gr.thrylos.news.data.repo.ArticleRepository
import gr.thrylos.news.feed.ArticleListCursor
import gr.thrylos.news.model.Article
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** "Home" view of one author — unfiltered, across every source they write for. */
@HiltViewModel
class AuthorProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    articleRepository: ArticleRepository,
    private val cursor: ArticleListCursor,
) : ViewModel() {

    val author: String = Uri.decode(checkNotNull(savedStateHandle.get<String>("author")))

    val articles: StateFlow<List<Article>> = articleRepository.observeByAuthor(author)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setCursorContext(ids: List<String>) {
        cursor.setContext(ids)
    }
}
