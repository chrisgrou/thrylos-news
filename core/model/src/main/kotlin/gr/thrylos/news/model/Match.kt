package gr.thrylos.news.model

import kotlinx.serialization.Serializable

@Serializable
enum class MatchStatus {
    NOT_STARTED,
    LIVE,
    FINISHED,
    OTHER,
}

@Serializable
data class Match(
    val id: String,
    val sport: String,
    /** "M" / "F" / "" (unknown) — from the team roster, not the match itself, but
     *  home/away are always the same competition level in practice. */
    val gender: String,
    val competition: String,
    val homeTeam: String,
    val homeTeamLogoUrl: String,
    val awayTeam: String,
    val awayTeamLogoUrl: String,
    val homeScore: Int?,
    val awayScore: Int?,
    val status: MatchStatus,
    val kickoffAt: Long,
    val matchUrl: String,
)
