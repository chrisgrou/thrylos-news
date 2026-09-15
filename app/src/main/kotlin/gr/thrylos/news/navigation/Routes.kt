package gr.thrylos.news.navigation

import android.net.Uri

object Routes {
    const val FEED = "feed"
    const val BOOKMARKS = "bookmarks"
    const val READER = "reader/{articleId}"
    const val MEDIA_VIEWER = "media/{articleId}/{index}"
    const val SOURCE_PROFILE = "profile/source/{sourceName}"
    const val AUTHOR_PROFILE = "profile/author/{author}"
    const val SETTINGS = "settings"
    const val SETTINGS_SOURCES = "settings/sources"
    const val SETTINGS_SOURCES_ADD_YOUTUBE = "settings/sources/add-youtube"
    const val SETTINGS_SOURCE_EDITOR =
        "settings/sources/editor?sourceId={sourceId}&kind={kind}&ytName={ytName}&ytChannelId={ytChannelId}&ytExcludeShorts={ytExcludeShorts}"
    const val SETTINGS_FILTERS = "settings/filters"
    const val SETTINGS_FILTER_EDITOR = "settings/filters/editor?ruleId={ruleId}&defaultAction={defaultAction}"
    /** Lists what the rule open in the editor matches; reads it from
     *  [gr.thrylos.news.settings.filters.RuleMatchPreview], so it carries no argument. */
    const val SETTINGS_FILTER_MATCHES = "settings/filters/matches"
    const val SETTINGS_SYNC = "settings/sync"
    const val SETTINGS_BACKUP = "settings/backup"
    const val SETTINGS_UPDATE_HISTORY = "settings/update-history"
    const val SETTINGS_MATCHES = "settings/matches"
    const val MATCHES = "matches"

    fun reader(articleId: String) = "reader/$articleId"
    fun mediaViewer(articleId: String, index: Int) = "media/$articleId/$index"
    fun sourceProfile(sourceName: String) = "profile/source/${Uri.encode(sourceName)}"
    fun authorProfile(author: String) = "profile/author/${Uri.encode(author)}"
    /** [ruleId] null adds a new rule rather than editing an existing one — in that
     *  case [defaultAction] (a [gr.thrylos.news.model.FilterAction] name) preselects
     *  the new rule's action to match whichever tab "Νέος κανόνας" was tapped from,
     *  instead of always defaulting to "Απόκρυψη". Ignored when editing. */
    fun filterEditor(ruleId: String? = null, defaultAction: String? = null): String {
        val params = listOfNotNull(
            ruleId?.let { "ruleId=${Uri.encode(it)}" },
            defaultAction?.let { "defaultAction=$it" },
        )
        return if (params.isEmpty()) "settings/filters/editor" else "settings/filters/editor?${params.joinToString("&")}"
    }

    fun sourceEditor(
        sourceId: String? = null,
        kind: String? = null,
        /** Only meaningful with kind="youtube" — pre-fills the template from a
         *  channel already resolved by [gr.thrylos.news.sources.youtube.YouTubeChannelResolver],
         *  so the source editor doesn't need to know how that resolution happened. */
        ytName: String? = null,
        ytChannelId: String? = null,
        /** Also youtube-only — the "Εμφάνιση Shorts" toggle's value, carried through
         *  so the pre-filled template reflects what was chosen on the resolve screen. */
        ytExcludeShorts: Boolean? = null,
    ): String {
        val params = listOfNotNull(
            sourceId?.let { "sourceId=$it" },
            kind?.let { "kind=$it" },
            ytName?.let { "ytName=${Uri.encode(it)}" },
            ytChannelId?.let { "ytChannelId=${Uri.encode(it)}" },
            ytExcludeShorts?.let { "ytExcludeShorts=$it" },
        )
        return if (params.isEmpty()) "settings/sources/editor" else "settings/sources/editor?${params.joinToString("&")}"
    }
}
