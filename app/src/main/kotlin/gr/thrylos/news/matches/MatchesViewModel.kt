package gr.thrylos.news.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gr.thrylos.news.data.prefs.AppPreferences
import gr.thrylos.news.model.Match
import gr.thrylos.news.model.MatchesPrefs
import gr.thrylos.news.sources.matches.SofascoreMatchesFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Sofascore team ids, from each team's URL (".../football/team/olympiacos-fc/3245",
 *  ".../basketball/team/olympiacos-bc/3501"). One entry per sport wired up so far. */
private val SPORT_TEAM_IDS: List<Pair<String, (MatchesPrefs) -> Boolean>> = listOf(
    "3245" to { prefs: MatchesPrefs -> prefs.football },
    "3501" to { prefs: MatchesPrefs -> prefs.basketball },
)

sealed class MatchesUiState {
    data object Loading : MatchesUiState()
    data class Success(
        val pageMatches: List<Match>,
        val page: Int,
        val pageCount: Int,
        val fetchedAt: Long,
        val sports: List<String>,
        val selectedSport: String?,
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
    private var selectedSport: String? = null
    private var pageSize = MatchesPrefs().pageSize

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

    fun selectSport(sport: String?) {
        selectedSport = sport
        page = 0
        publishSuccess()
    }

    fun refresh(force: Boolean = true) {
        viewModelScope.launch {
            val prefs = appPreferences.matchesPrefs.first()
            pageSize = prefs.pageSize
            val enabledTeamIds = SPORT_TEAM_IDS.filter { (_, enabled) -> enabled(prefs) }.map { it.first }
            if (enabledTeamIds.isEmpty()) {
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
                withContext(Dispatchers.IO) {
                    coroutineScope {
                        enabledTeamIds.map { teamId -> async { fetcher.fetchUpcoming(teamId) } }.awaitAll()
                    }.flatten().sortedBy { it.kickoffAt }
                }
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
        val sports = allMatches.map { it.sport }.distinct()
        if (selectedSport != null && selectedSport !in sports) selectedSport = null
        val filtered = selectedSport?.let { sport -> allMatches.filter { it.sport == sport } } ?: allMatches
        val pageCount = maxOf(1, (filtered.size + pageSize - 1) / pageSize)
        val clampedPage = page.coerceIn(0, pageCount - 1)
        page = clampedPage
        _state.value = MatchesUiState.Success(
            pageMatches = filtered.drop(clampedPage * pageSize).take(pageSize),
            page = clampedPage,
            pageCount = pageCount,
            fetchedAt = fetchedAt,
            sports = sports,
            selectedSport = selectedSport,
        )
    }
}
