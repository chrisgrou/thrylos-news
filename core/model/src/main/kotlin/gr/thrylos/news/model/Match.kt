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
    /** The Sofascore team id this fixture was fetched for — [sport] alone can't tell
     *  apart e.g. Olympiacos FC's first team from its B' team or U19 side, since
     *  Sofascore reports the same "football" sport slug for all of them. Lets the UI
     *  filter by team, not just by sport. */
    val teamId: String,
    /** Whether [teamId]'s team is playing at home in this fixture. */
    val isHome: Boolean,
)
