package gr.thrylos.news.settings.filters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gr.thrylos.news.data.repo.FilterRepository
import gr.thrylos.news.data.repo.SourceRepository
import gr.thrylos.news.model.FilterRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** [matchCount] is null while the count is still being worked out — the rule itself is
 *  known and shown immediately, the number catches up. */
data class FilterRow(val rule: FilterRule, val matchCount: Int?)

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
