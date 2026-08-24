package gr.thrylos.news.feed

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers the ordered list of article ids currently visible in whichever
 * screen the user tapped an article from (feed or bookmarks), so the reader
 * can swipe through *that* list without re-encoding it into the nav route.
 */
@Singleton
class ArticleListCursor @Inject constructor() {
    @Volatile
    var currentIds: List<String> = emptyList()
        private set

    fun setContext(ids: List<String>) {
        currentIds = ids
    }

    fun indexOf(articleId: String): Int = currentIds.indexOf(articleId).coerceAtLeast(0)
}
