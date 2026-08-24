package gr.thrylos.news.profile

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
class AuthorsListViewModel @Inject constructor(
    articleRepository: ArticleRepository,
) : ViewModel() {

    val authors: StateFlow<List<String>> = articleRepository.observeAll()
        .map { articles -> articles.mapNotNull { it.author }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
