@file:OptIn(kotlinx.coroutines.FlowPreview::class)

package gr.thrylos.news.settings.filters

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gr.thrylos.news.data.repo.FilterRepository
import gr.thrylos.news.data.repo.SourceRepository
import gr.thrylos.news.model.FilterAction
import gr.thrylos.news.model.FilterRule
import gr.thrylos.news.sources.plugin.SourceKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Long enough that typing a word doesn't re-scan the corpus per keystroke, short
 *  enough that the badge feels like it answers to what you just typed. */
private const val PREVIEW_DEBOUNCE_MS = 350L

/** A Πηγή condition's picker option — [kind] drives its icon, so a YouTube channel
 *  or Facebook page reads as one at a glance in the list rather than looking like
 *  a plain site. */
data class SourceOption(val name: String, val kind: SourceKind)

@HiltViewModel
class FilterEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val filterRepository: FilterRepository,
    private val preview: RuleMatchPreview,
    sourceRepository: SourceRepository,
) : ViewModel() {

    /** null when adding a rule rather than editing an existing one. */
    private val ruleId: String? = savedStateHandle.get<String>("ruleId")

    /** Only meaningful when [ruleId] is null (adding a new rule) — which tab
     *  "Νέος κανόνας" was tapped from, e.g. "SHOW_ONLY" when opened from Εμφάνιση,
     *  so the new rule's action starts there instead of always at Απόκρυψη. */
    val defaultAction: FilterAction? = savedStateHandle.get<String>("defaultAction")
        ?.let { name -> runCatching { FilterAction.valueOf(name) }.getOrNull() }

    private val _initialRule = MutableStateFlow<FilterRule?>(null)
    val initialRule: StateFlow<FilterRule?> = _initialRule.asStateFlow()

    /** False only until the existing rule (if any) has been read, so the editor doesn't
     *  briefly build its drafts from nothing and then discard them. */
    private val _ready = MutableStateFlow(ruleId == null)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    val sourceOptions: StateFlow<List<SourceOption>> = sourceRepository.observeAll()
        .map { sources ->
            sources.groupBy { it.name }
                .map { (name, group) -> SourceOption(name, group.firstNotNullOfOrNull { it.plugin?.kind } ?: SourceKind.SITE) }
                .sortedBy { it.name }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** How many articles the draft currently matches — null until the first pass ends. */
    val matchCount: StateFlow<Int?> = preview.matches
        .map { it?.articles?.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val draft = MutableStateFlow<FilterRule?>(null)

    init {
        if (ruleId != null) {
            viewModelScope.launch {
                _initialRule.value = filterRepository.observeAll().first().firstOrNull { it.id == ruleId }
                _ready.value = true
            }
        }
        viewModelScope.launch {
            draft.debounce(PREVIEW_DEBOUNCE_MS).collect { rule ->
                withContext(Dispatchers.Default) { preview.update(rule) }
            }
        }
    }

    /** Called by the editor as its draft changes, so the badge and the matched-articles
     *  list follow what's being typed rather than the last saved version. */
    fun onDraftChanged(rule: FilterRule?) {
        draft.value = rule
    }

    fun save(rule: FilterRule) {
        viewModelScope.launch { filterRepository.upsert(rule) }
    }

    fun delete(rule: FilterRule) {
        viewModelScope.launch { filterRepository.delete(rule) }
    }

    /** Fires when the editor is actually popped off the back stack — not when its
     *  composable is disposed, which also happens whenever the matched-articles list
     *  (which reads the very same preview) is pushed on top of it. */
    override fun onCleared() {
        super.onCleared()
        preview.clear()
    }
}
