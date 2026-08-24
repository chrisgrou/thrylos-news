package gr.thrylos.news.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gr.thrylos.news.data.repo.UpdateHistoryEntry
import gr.thrylos.news.data.repo.UpdateHistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class UpdateHistoryViewModel @Inject constructor(
    repository: UpdateHistoryRepository,
) : ViewModel() {

    val entries: StateFlow<List<UpdateHistoryEntry>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
