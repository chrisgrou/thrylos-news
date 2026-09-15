package gr.thrylos.news.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gr.thrylos.news.data.prefs.AppPreferences
import gr.thrylos.news.data.repo.ArticleRepository
import gr.thrylos.news.data.repo.SourceRepository
import gr.thrylos.news.feed.ArticleListCursor
import gr.thrylos.news.model.Article
import gr.thrylos.news.model.ArticleStub
import gr.thrylos.news.model.ReaderPrefs
import gr.thrylos.news.sources.plugin.SourceKind
import gr.thrylos.news.sources.sync.SourceSyncCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val articleRepository: ArticleRepository,
    private val sourceRepository: SourceRepository,
    private val coordinator: SourceSyncCoordinator,
    private val preferences: AppPreferences,
    cursor: ArticleListCursor,
) : ViewModel() {

    private val articleId: String = checkNotNull(savedStateHandle["articleId"])

    val idList: List<String> = cursor.currentIds.ifEmpty { listOf(articleId) }
    val startIndex: Int = idList.indexOf(articleId).coerceAtLeast(0)

    val readerPrefs: StateFlow<ReaderPrefs> = preferences.readerPrefs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReaderPrefs())

    private val articleFlows = mutableMapOf<String, StateFlow<Article?>>()

    private val _refetching = MutableStateFlow(false)
    val refetching: StateFlow<Boolean> = _refetching.asStateFlow()

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

    /** Manual re-fetch for a page a publisher first put up as a "developing story" — a
     *  placeholder that got filled in later but that the automatic sync-time retry
     *  (bounded per source per run) hasn't gotten to yet, or a page that's grown stale. */
    fun refetch(id: String) {
        viewModelScope.launch {
            _refetching.value = true
            try {
                withContext(Dispatchers.IO) {
                    val existing = articleRepository.observeById(id).first() ?: return@withContext
                    val plugin = sourceRepository.getById(existing.sourceId)?.plugin ?: return@withContext
                    // Nothing to refetch: a YOUTUBE-kind plugin never fetches the
                    // video's own page to begin with (see YouTubeVideoExtractor), and
                    // this 3-arg stub carries neither the thumbnail nor the description
                    // that only the original RSS entry ever had — re-running it would
                    // silently strip both from the stored article.
                    if (plugin.kind == SourceKind.YOUTUBE) return@withContext
                    val stub = ArticleStub(existing.sourceId, existing.url, existing.title)
                    val fresh = coordinator.extractArticle(plugin, stub).copy(
                        isRead = existing.isRead,
                        isBookmarked = existing.isBookmarked,
                        dedupGroupId = existing.dedupGroupId,
                    )
                    articleRepository.upsert(fresh)
                }
            } finally {
                _refetching.value = false
            }
        }
    }
}
