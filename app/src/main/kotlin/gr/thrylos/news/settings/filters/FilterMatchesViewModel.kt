package gr.thrylos.news.settings.filters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gr.thrylos.news.feed.ArticleListCursor
import gr.thrylos.news.model.Article
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Reads whatever the rule editor last matched — see [RuleMatchPreview]; this screen
 *  never runs the pass itself, and stays in step with the draft if it keeps changing. */
@HiltViewModel
class FilterMatchesViewModel @Inject constructor(
    preview: RuleMatchPreview,
    private val cursor: ArticleListCursor,
) : ViewModel() {

    val articles: StateFlow<List<Article>?> = preview.matches
        .map { it?.articles }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Same as every other article list: opening one makes the rest swipeable in the
     *  reader instead of stranding it on a single article. */
    fun setCursorContext(ids: List<String>) {
        cursor.setContext(ids)
    }
}
