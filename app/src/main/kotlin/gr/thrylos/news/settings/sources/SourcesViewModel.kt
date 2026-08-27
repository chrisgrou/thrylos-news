package gr.thrylos.news.settings.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gr.thrylos.news.data.repo.SourceRepository
import gr.thrylos.news.data.repo.SourceWithPlugin
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Two plugins can share a display name (e.g. Sportal's football/basketball
 *  scrapers), in which case they render — and toggle — as a single row. */
data class SourceGroupRow(
    val displayName: String,
    val rawName: String,
    val members: List<SourceWithPlugin>,
    val enabled: Boolean,
    val isBundled: Boolean,
)

@HiltViewModel
class SourcesViewModel @Inject constructor(
    private val repository: SourceRepository,
) : ViewModel() {

    private val sources: StateFlow<List<SourceWithPlugin>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groups: StateFlow<List<SourceGroupRow>> = sources.map { list ->
        list.groupBy { it.name }.map { (name, members) ->
            SourceGroupRow(
                displayName = name.substringBefore(" — ").trim(),
                rawName = name,
                members = members,
                enabled = members.all { it.enabled },
                isBundled = members.any { it.isBundled },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setGroupEnabled(group: SourceGroupRow, enabled: Boolean) {
        viewModelScope.launch { group.members.forEach { repository.setEnabled(it.id, enabled) } }
    }
}
