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

private const val PAGE_SIZE = 10

sealed class MatchesUiState {
    data object Loading : MatchesUiState()
    data class Success(
        val pageMatches: List<Match>,
        val page: Int,
        val pageCount: Int,
        val fetchedAt: Long,
    ) : MatchesUiState()
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

    private var allMatches: List<Match> = emptyList()
    private var fetchedAt: Long = 0L
    private var page = 0
    private var loaded = false

    /** Called once when the screen first opens. Fixtures barely change within a day,
     *  so this reuses whatever's cached until [gr.thrylos.news.model.MatchesPrefs.refreshIntervalHours]
     *  has passed — only [refresh] with force=true (the manual button) always re-fetches. */
    fun loadIfNeeded() {
        if (loaded) return
        loaded = true
        refresh(force = false)
    }

    fun setPage(index: Int) {
        page = index.coerceAtLeast(0)
        publishSuccess()
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
                    val (cachedAt, matches) = cached
                    allMatches = matches
                    fetchedAt = cachedAt
                    page = 0
                    publishSuccess()
                    val ageMs = System.currentTimeMillis() - cachedAt
                    if (ageMs < prefs.refreshIntervalHours * 60 * 60 * 1000L) return@launch
                }
            }

            if (_state.value !is MatchesUiState.Success) _state.value = MatchesUiState.Loading
            runCatching {
                withContext(Dispatchers.IO) { fetcher.fetchUpcoming(OLYMPIACOS_FOOTBALL_TEAM_ID) }
            }.onSuccess { matches ->
                appPreferences.cacheMatches(matches)
                allMatches = matches
                fetchedAt = System.currentTimeMillis()
                page = 0
                publishSuccess()
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

    private fun publishSuccess() {
        val pageCount = maxOf(1, (allMatches.size + PAGE_SIZE - 1) / PAGE_SIZE)
        val clampedPage = page.coerceIn(0, pageCount - 1)
        page = clampedPage
        _state.value = MatchesUiState.Success(
            pageMatches = allMatches.drop(clampedPage * PAGE_SIZE).take(PAGE_SIZE),
            page = clampedPage,
            pageCount = pageCount,
            fetchedAt = fetchedAt,
        )
    }
}
