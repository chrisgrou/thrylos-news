package gr.thrylos.news.settings.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gr.thrylos.news.sources.youtube.YouTubeChannelInfo
import gr.thrylos.news.sources.youtube.YouTubeChannelResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class ResolveState {
    data object Idle : ResolveState()
    data object Loading : ResolveState()
    data class Success(val info: YouTubeChannelInfo) : ResolveState()
    data class Error(val message: String) : ResolveState()
}

@HiltViewModel
class AddYouTubeChannelViewModel @Inject constructor(
    private val resolver: YouTubeChannelResolver,
) : ViewModel() {

    private val _state = MutableStateFlow<ResolveState>(ResolveState.Idle)
    val state: StateFlow<ResolveState> = _state.asStateFlow()

    fun resolve(input: String) {
        viewModelScope.launch {
            _state.value = ResolveState.Loading
            _state.value = withContext(Dispatchers.IO) {
                runCatching { resolver.resolve(input) }
                    .fold(
                        onSuccess = { ResolveState.Success(it) },
                        onFailure = { ResolveState.Error(it.message ?: "Άγνωστο σφάλμα.") },
                    )
            }
        }
    }

    /** Back to a blank field after a failed attempt, or to try a different channel
     *  after a successful one without leaving this screen. */
    fun reset() {
        _state.value = ResolveState.Idle
    }
}
