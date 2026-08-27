package gr.thrylos.news.settings.filters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gr.thrylos.news.data.repo.ArticleRepository
import gr.thrylos.news.data.repo.FilterRepository
import gr.thrylos.news.data.repo.SourceRepository
import gr.thrylos.news.model.FilterRule
import gr.thrylos.news.sources.filter.FilterEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FilterRow(val rule: FilterRule, val hiddenCount: Int)

@HiltViewModel
class FiltersViewModel @Inject constructor(
    private val filterRepository: FilterRepository,
    articleRepository: ArticleRepository,
    sourceRepository: SourceRepository,
) : ViewModel() {

    val rows: StateFlow<List<FilterRow>> = combine(
        filterRepository.observeAll(),
        articleRepository.observeAll(),
    ) { rules, articles ->
        val counts = FilterEngine.countMatchesBatch(rules, articles)
        rules.map { FilterRow(it, counts[it.id] ?: 0) }
    }
        // countMatches re-evaluates every rule against every stored article (and, for a
        // BODY/ANYWHERE rule, rejoins that article's content blocks into one string each
        // time) — on the main thread this is exactly the kind of blocking work that made
        // the Feed screen jank before it got the same fix; here it showed up as a visible
        // pause when opening Ρυθμίσεις → Φίλτρα.
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
