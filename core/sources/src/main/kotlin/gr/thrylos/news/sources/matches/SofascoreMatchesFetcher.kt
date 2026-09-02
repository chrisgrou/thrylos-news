package gr.thrylos.news.sources.matches

import gr.thrylos.news.model.Match
import gr.thrylos.news.model.MatchStatus
import gr.thrylos.news.sources.http.HttpFetcher
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

@Serializable
private data class EventsResponse(val events: List<EventDto> = emptyList())

@Serializable
private data class EventDto(
    val id: Long,
    val slug: String? = null,
    val customId: String? = null,
    val startTimestamp: Long,
    val status: StatusDto,
    val homeTeam: TeamDto,
    val awayTeam: TeamDto,
    val homeScore: ScoreDto = ScoreDto(),
    val awayScore: ScoreDto = ScoreDto(),
    val tournament: TournamentDto,
)

@Serializable
private data class StatusDto(val type: String)

@Serializable
private data class TeamDto(val id: Long, val name: String, val gender: String = "")

@Serializable
private data class ScoreDto(val current: Int? = null)

@Serializable
private data class TournamentDto(
    val name: String,
    val category: CategoryDto,
)

@Serializable
private data class CategoryDto(val sport: SportDto)

@Serializable
private data class SportDto(@SerialName("slug") val slug: String)

/** Reads a team's fixtures from Sofascore's own (undocumented but publicly
 *  reachable, no-login-required) JSON API — the same one their web/app client
 *  calls. Far more stable than scraping Sofascore's own page HTML, which is a
 *  client-rendered Next.js app with no server-rendered match data to select
 *  against; this endpoint returns real match data directly. */
class SofascoreMatchesFetcher(
    private val http: HttpFetcher = HttpFetcher(),
    private val baseUrl: String = "https://api.sofascore.com",
) {

    /** Upcoming fixtures for a team, soonest first. [teamId] is the numeric id from
     *  the team's Sofascore URL (e.g. ".../olympiacos-fc/3245" → "3245"). */
    fun fetchUpcoming(teamId: String): List<Match> {
        val body = http.fetchText("$baseUrl/api/v1/team/$teamId/events/next/0")
        val response = json.decodeFromString(EventsResponse.serializer(), body)
        return response.events
            .map { it.toMatch(teamId) }
            .sortedBy { it.kickoffAt }
    }

    private fun EventDto.toMatch(queriedTeamId: String): Match {
        val sport = tournament.category.sport.slug
        val matchUrl = if (slug != null && customId != null) {
            "https://www.sofascore.com/$sport/match/$slug/$customId#id:$id"
        } else {
            "https://www.sofascore.com/event/$id"
        }
        return Match(
            id = id.toString(),
            sport = sport,
            gender = homeTeam.gender,
            competition = tournament.name,
            homeTeam = homeTeam.name,
            homeTeamLogoUrl = "$baseUrl/api/v1/team/${homeTeam.id}/image",
            awayTeam = awayTeam.name,
            awayTeamLogoUrl = "$baseUrl/api/v1/team/${awayTeam.id}/image",
            homeScore = homeScore.current,
            awayScore = awayScore.current,
            status = when (status.type) {
                "notstarted" -> MatchStatus.NOT_STARTED
                "inprogress" -> MatchStatus.LIVE
                "finished" -> MatchStatus.FINISHED
                else -> MatchStatus.OTHER
            },
            kickoffAt = startTimestamp * 1000,
            matchUrl = matchUrl,
            teamId = queriedTeamId,
            isHome = homeTeam.id.toString() == queriedTeamId,
        )
    }
}
