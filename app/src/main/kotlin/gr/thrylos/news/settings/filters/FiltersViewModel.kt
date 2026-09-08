@file:OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package gr.thrylos.news.settings.filters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gr.thrylos.news.data.repo.FilterRepository
import gr.thrylos.news.data.repo.SourceRepository
import gr.thrylos.news.model.Article
import gr.thrylos.news.model.FilterRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** [matchCount] is null while the count is still being worked out — the rule itself is
 *  known and shown immediately, the number catches up. */
data class FilterRow(val rule: FilterRule, val matchCount: Int?)

/** What the rule currently open in the editor matches. [articles] is capped at
 *  [PREVIEW_LIMIT] for display; [count] is the real total. */
data class RuleMatchPreview(val count: Int, val articles: List<Article>)

private const val PREVIEW_LIMIT = 200

/** Long enough that typing a word doesn't re-scan the corpus per keystroke, short
 *  enough that the number feels like it answers to what you just typed. */
private const val PREVIEW_DEBOUNCE_MS = 350L

@HiltViewModel
class FiltersViewModel @Inject constructor(
    private val filterRepository: FilterRepository,
    private val matchCounts: FilterMatchCounts,
    sourceRepository: SourceRepository,
) : ViewModel() {

    private val counts = MutableStateFlow<Map<String, Int>?>(null)

    init {
        // Counting runs alongside the screen rather than in front of it. Deliberately
        // driven by the rules alone: an article arriving mid-visit doesn't redraw these
        // numbers, but re-opening the screen picks up the change — the alternative,
        // recomputing on every write to the articles table, is what made this screen
        // stall in the first place.
        viewModelScope.launch {
            filterRepository.observeAll().collectLatest { rules ->
                counts.value = withContext(Dispatchers.Default) { matchCounts.countsFor(rules) }
            }
        }
    }

    /** null until the rules themselves have been read — distinct from "there are no
     *  rules", which the screen renders as an explanatory message. */
    val rows: StateFlow<List<FilterRow>?> = combine(filterRepository.observeAll(), counts) { rules, byId ->
        rules.map { FilterRow(it, byId?.get(it.id)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val editedRule = MutableStateFlow<FilterRule?>(null)

    /** null while nothing is open in the editor, or while the current draft's matches
     *  are still being worked out. Driven by the live draft rather than the saved rule,
     *  so a rule can be judged — and a brand new one judged before it's ever saved — by
     *  what it actually catches. */
    val editorPreview: StateFlow<RuleMatchPreview?> = editedRule
        .debounce(PREVIEW_DEBOUNCE_MS)
        .mapLatest { rule ->
            if (rule == null) null else {
                val matched = matchCounts.articlesMatching(rule)
                RuleMatchPreview(matched.size, matched.take(PREVIEW_LIMIT))
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Called by the editor as its draft changes; null when it closes. */
    fun previewRule(rule: FilterRule?) {
        editedRule.value = rule
    }

    val sourceNames: StateFlow<List<String>> = sourceRepository.observeAll()
        .map { sources -> sources.map { it.name }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(rule: FilterRule) {
        viewModelScope.launch { filterRepository.upsert(rule) }
    }

    fun setEnabled(rule: FilterRule, enabled: Boolean) {
        viewModelScope.launch { filterRepository.upsert(rule.copy(enabled = enabled)) }
    }

    fun delete(rule: FilterRule) {
        viewModelScope.launch { filterRepository.delete(rule) }
    }
}
