package gr.thrylos.news.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gr.thrylos.news.data.repo.ArticleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    articleRepository: ArticleRepository,
    private val cursor: ArticleListCursor,
) : ViewModel() {

    val items: StateFlow<List<FeedItem>> = articleRepository.observeBookmarked()
        .map { list -> list.sortedByDescending { it.publishedAt ?: it.fetchedAt }.map { FeedItem(it, 0) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun openArticle(id: String) {
        cursor.setContext(items.value.map { it.article.id })
    }
}
