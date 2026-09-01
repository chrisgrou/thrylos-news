package gr.thrylos.news.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gr.thrylos.news.data.prefs.AppPreferences
import gr.thrylos.news.model.Match
import gr.thrylos.news.sources.matches.SofascoreMatchesFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Olympiacos FC's numeric id on Sofascore — from the team URL
 *  (".../football/team/olympiacos-fc/3245"). The only team/sport wired up so far. */
private const val OLYMPIACOS_FOOTBALL_TEAM_ID = "3245"

sealed class MatchesUiState {
    data object Loading : MatchesUiState()
    data class Success(val matches: List<Match>, val fetchedAt: Long) : MatchesUiState()
    data class Error(val message: String) : MatchesUiState()
    data object SportsDisabled : MatchesUiState()
}

@HiltViewModel
class MatchesViewModel @Inject constructor(
    private val fetcher: SofascoreMatchesFetcher,
    private val appPreferences: AppPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow<MatchesUiState>(MatchesUiState.Loading)
    val state: StateFlow<MatchesUiState> = _state.asStateFlow()

    private var loaded = false

    /** Called on every overlay open. Fixtures barely change within a day, so this
     *  reuses whatever's cached until [MatchesPrefs.refreshIntervalHours] has passed —
     *  only [refresh] with force=true (the overlay's manual button) always re-fetches. */
    fun loadIfNeeded() {
        if (loaded) return
        loaded = true
        refresh(force = false)
    }

    fun refresh(force: Boolean = true) {
        viewModelScope.launch {
            val prefs = appPreferences.matchesPrefs.first()
            if (!prefs.football) {
                _state.value = MatchesUiState.SportsDisabled
                return@launch
            }

            if (!force) {
                val cached = appPreferences.cachedMatches()
                if (cached != null) {
                    val (fetchedAt, matches) = cached
                    _state.value = MatchesUiState.Success(matches, fetchedAt)
                    val ageMs = System.currentTimeMillis() - fetchedAt
                    if (ageMs < prefs.refreshIntervalHours * 60 * 60 * 1000L) return@launch
                }
            }

            if (_state.value !is MatchesUiState.Success) _state.value = MatchesUiState.Loading
            runCatching {
                withContext(Dispatchers.IO) { fetcher.fetchUpcoming(OLYMPIACOS_FOOTBALL_TEAM_ID) }
            }.onSuccess { matches ->
                appPreferences.cacheMatches(matches)
                _state.value = MatchesUiState.Success(matches, System.currentTimeMillis())
            }.onFailure { e ->
                // Keep showing a stale cached list rather than replacing it with an
                // error if we already had one on screen — only surface the error when
                // there was nothing to show in the first place.
                if (_state.value !is MatchesUiState.Success) {
                    _state.value = MatchesUiState.Error(e.message ?: "Άγνωστο σφάλμα δικτύου")
                }
            }
        }
    }
}
