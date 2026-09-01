package gr.thrylos.news.model

/** Which sports show up in Πρόγραμμα αγώνων. One field per sport — more get added
 *  here as new sports are wired up, same as the rest of this app's settings. */
data class MatchesPrefs(
    val football: Boolean = true,
    val basketball: Boolean = true,
    /** Fixtures barely change within a day, so unlike article sync this defaults to
     *  a long interval — a cached result is served in between, with a manual refresh
     *  button in the overlay for whenever a fresher one is wanted right away. */
    val refreshIntervalHours: Int = 24,
    /** How many matches (across however many date-groups they fall into) show per
     *  page — screen heights vary a lot across devices, so this is user-adjustable
     *  rather than a fixed guess at what fits without scrolling. */
    val pageSize: Int = 5,
)
