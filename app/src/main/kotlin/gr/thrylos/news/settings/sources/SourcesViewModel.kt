package gr.thrylos.news.settings.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gr.thrylos.news.data.repo.SourceRepository
import gr.thrylos.news.data.repo.SourceWithPlugin
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SourcesViewModel @Inject constructor(
    private val repository: SourceRepository,
) : ViewModel() {

    val sources: StateFlow<List<SourceWithPlugin>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch { repository.setEnabled(id, enabled) }
    }

    fun remove(source: SourceWithPlugin) {
        viewModelScope.launch { repository.remove(source) }
    }

    fun moveUp(index: Int) = reorder(index, index - 1)
    fun moveDown(index: Int) = reorder(index, index + 1)

    private fun reorder(from: Int, to: Int) {
        val current = sources.value.map { it.id }.toMutableList()
        if (to < 0 || to >= current.size) return
        val item = current.removeAt(from)
        current.add(to, item)
        viewModelScope.launch { repository.reorder(current) }
    }
}
