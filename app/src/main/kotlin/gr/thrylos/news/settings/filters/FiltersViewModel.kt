package gr.thrylos.news.settings.filters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gr.thrylos.news.data.repo.ArticleRepository
import gr.thrylos.news.data.repo.FilterRepository
import gr.thrylos.news.model.FilterRule
import gr.thrylos.news.sources.filter.FilterEngine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FilterRow(val rule: FilterRule, val hiddenCount: Int)

@HiltViewModel
class FiltersViewModel @Inject constructor(
    private val filterRepository: FilterRepository,
    articleRepository: ArticleRepository,
) : ViewModel() {

    val rows: StateFlow<List<FilterRow>> = combine(
        filterRepository.observeAll(),
        articleRepository.observeAll(),
    ) { rules, articles ->
        rules.map { FilterRow(it, FilterEngine.countMatches(it, articles)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(rule: FilterRule) {
        viewModelScope.launch { filterRepository.upsert(rule) }
    }

    fun delete(rule: FilterRule) {
        viewModelScope.launch { filterRepository.delete(rule) }
    }
}
