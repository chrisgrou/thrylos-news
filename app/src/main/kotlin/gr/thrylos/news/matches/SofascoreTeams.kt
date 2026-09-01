package gr.thrylos.news.matches

/** One Olympiacos team/sport/level combo tracked on Sofascore — [id] is the numeric
 *  id from that team's Sofascore URL (e.g. ".../football/team/olympiacos-fc/3245").
 *  The set of ids enabled by default lives in [gr.thrylos.news.model.DEFAULT_ENABLED_TEAM_IDS]
 *  (:core:model, which can't reference this :app-level file) and must stay in sync
 *  with this list when a team is added or removed. */
data class SofascoreTeam(val id: String, val label: String)

val OLYMPIACOS_TEAMS: List<SofascoreTeam> = listOf(
    SofascoreTeam("3245", "Ποδόσφαιρο Ανδρών"),
    SofascoreTeam("395730", "Ποδόσφαιρο Β' Ομάδα"),
    SofascoreTeam("90136", "Ποδόσφαιρο U19"),
    SofascoreTeam("3501", "Μπάσκετ Ανδρών"),
    SofascoreTeam("32395", "Βόλεϊ Ανδρών"),
    SofascoreTeam("176612", "Χάντμπολ Ανδρών"),
    SofascoreTeam("91728", "Πόλο Ανδρών"),
)
