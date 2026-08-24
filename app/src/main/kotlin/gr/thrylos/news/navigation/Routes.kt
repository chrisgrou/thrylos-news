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
    const val SETTINGS_SOURCE_EDITOR = "settings/sources/editor?sourceId={sourceId}"
    const val SETTINGS_FILTERS = "settings/filters"
    const val SETTINGS_SYNC = "settings/sync"
    const val SETTINGS_BACKUP = "settings/backup"
    const val SETTINGS_UPDATE_HISTORY = "settings/update-history"

    fun reader(articleId: String) = "reader/$articleId"
    fun mediaViewer(articleId: String, index: Int) = "media/$articleId/$index"
    fun sourceProfile(sourceName: String) = "profile/source/${Uri.encode(sourceName)}"
    fun authorProfile(author: String) = "profile/author/${Uri.encode(author)}"
    fun sourceEditor(sourceId: String? = null) =
        if (sourceId != null) "settings/sources/editor?sourceId=$sourceId" else "settings/sources/editor"
}
