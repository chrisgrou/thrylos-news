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
    val competition: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeScore: Int?,
    val awayScore: Int?,
    val status: MatchStatus,
    val kickoffAt: Long,
    val matchUrl: String,
)
