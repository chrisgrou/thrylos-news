package gr.thrylos.news.reader.media

import androidx.lifecycle.SavedStateHandle
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
class MediaViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    articleRepository: ArticleRepository,
) : ViewModel() {

    private val articleId: String = checkNotNull(savedStateHandle["articleId"])
    val startIndex: Int = (savedStateHandle.get<String>("index")?.toIntOrNull() ?: 0)

    val media: StateFlow<List<MediaItem>> = articleRepository.observeById(articleId)
        .map { it?.mediaItems().orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
