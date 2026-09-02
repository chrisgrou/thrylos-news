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

/** "Ποδόσφαιρο" alone can't tell Olympiacos FC's first team apart from its B' team or
 *  U19 side — Sofascore reports the same sport slug for all three — so filtering is by
 *  [Match.teamId] (one of our own tracked Sofascore team ids) instead of by sport. */
enum class HomeAwayFilter { ALL, HOME, AWAY }

sealed class MatchesUiState {
    data object Loading : MatchesUiState()
    data class Success(
        val pageMatches: List<Match>,
        val page: Int,
        val pageCount: Int,
        val fetchedAt: Long,
        val teamIds: List<String>,
        val selectedTeamId: String?,
        val homeAwayFilter: HomeAwayFilter,
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
    private var selectedTeamId: String? = null
    private var homeAwayFilter = HomeAwayFilter.ALL
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

    fun selectTeam(teamId: String?) {
        selectedTeamId = teamId
        page = 0
        publishSuccess()
    }

    fun selectHomeAway(filter: HomeAwayFilter) {
        homeAwayFilter = filter
        page = 0
        publishSuccess()
    }

    fun refresh(force: Boolean = true) {
        viewModelScope.launch {
            val prefs = appPreferences.matchesPrefs.first()
            pageSize = prefs.pageSize
            val enabledTeamIds = prefs.enabledTeamIds.toList()
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
                        // One team's request failing (e.g. a youth/reserve squad
                        // with no fixtures yet, or a transient error) must not lose
                        // every other team's matches — awaitAll() alone propagates
                        // the first failure and cancels the rest, which combined
                        // with the "keep the stale list on failure" handling below
                        // meant a single bad team silently blocked every team's
                        // matches from ever updating again.
                        enabledTeamIds.map { teamId ->
                            async { runCatching { fetcher.fetchUpcoming(teamId) }.getOrElse { emptyList() } }
                        }.awaitAll()
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
        val teamIds = allMatches.map { it.teamId }.distinct()
        if (selectedTeamId != null && selectedTeamId !in teamIds) selectedTeamId = null
        val filtered = allMatches
            .filter { selectedTeamId == null || it.teamId == selectedTeamId }
            .filter {
                when (homeAwayFilter) {
                    HomeAwayFilter.ALL -> true
                    HomeAwayFilter.HOME -> it.isHome
                    HomeAwayFilter.AWAY -> !it.isHome
                }
            }
        val pageCount = maxOf(1, (filtered.size + pageSize - 1) / pageSize)
        val clampedPage = page.coerceIn(0, pageCount - 1)
        page = clampedPage
        _state.value = MatchesUiState.Success(
            pageMatches = filtered.drop(clampedPage * pageSize).take(pageSize),
            page = clampedPage,
            pageCount = pageCount,
            fetchedAt = fetchedAt,
            teamIds = teamIds,
            selectedTeamId = selectedTeamId,
            homeAwayFilter = homeAwayFilter,
        )
    }
}
