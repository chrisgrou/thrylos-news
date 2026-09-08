package gr.thrylos.news.settings.filters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gr.thrylos.news.data.repo.FilterRepository
import gr.thrylos.news.model.FilterRule
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FiltersViewModel @Inject constructor(
    private val filterRepository: FilterRepository,
) : ViewModel() {

    /** null until the rules have been read — distinct from "there are no rules", which
     *  the screen renders as an explanatory message.
     *
     *  Deliberately just the rules. Each one used to carry a "matches N articles" count,
     *  which meant matching every rule against every stored article before the list
     *  could draw at all — and again on every return visit. That number now lives in the
     *  editor, where it describes the one rule being worked on and can be acted on. */
    val rules: StateFlow<List<FilterRule>?> = filterRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setEnabled(rule: FilterRule, enabled: Boolean) {
        viewModelScope.launch { filterRepository.upsert(rule.copy(enabled = enabled)) }
    }
}
