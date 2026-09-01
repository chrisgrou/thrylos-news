package gr.thrylos.news.model

/** One boolean per team/sport didn't scale past two entries, so which Sofascore teams
 *  show up in Πρόγραμμα αγώνων is now a set of Sofascore numeric team ids (from each
 *  team's URL) — a new team is just a new id, no schema change needed. [DEFAULT_ENABLED_TEAM_IDS]
 *  must stay in sync with the labels in gr.thrylos.news.matches.OLYMPIACOS_TEAMS
 *  (:app) — this module can't reference that one (:app depends on :core:model, not
 *  the reverse), so the id↔label mapping only exists on the :app side. */
val DEFAULT_ENABLED_TEAM_IDS: Set<String> = setOf(
    "3245", // Ποδόσφαιρο Ανδρών
    "395730", // Ποδόσφαιρο Β' Ομάδα
    "90136", // Ποδόσφαιρο U19
    "3501", // Μπάσκετ Ανδρών
    "32395", // Βόλεϊ Ανδρών
    "176612", // Χάντμπολ Ανδρών
    "91728", // Πόλο Ανδρών
)

data class MatchesPrefs(
    val enabledTeamIds: Set<String> = DEFAULT_ENABLED_TEAM_IDS,
    /** Fixtures barely change within a day, so unlike article sync this defaults to
     *  a long interval — a cached result is served in between, with a manual refresh
     *  button in the overlay for whenever a fresher one is wanted right away. */
    val refreshIntervalHours: Int = 24,
    /** How many matches (across however many date-groups they fall into) show per
     *  page — screen heights vary a lot across devices, so this is user-adjustable
     *  rather than a fixed guess at what fits without scrolling. */
    val pageSize: Int = 5,
)
