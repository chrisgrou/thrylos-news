package gr.thrylos.news.model

import kotlinx.serialization.Serializable

@Serializable
enum class FilterField { TITLE, BODY, AUTHOR, URL, SOURCE }

@Serializable
enum class FilterMatch { CONTAINS, REGEX, EXACT }

@Serializable
enum class FilterAction { HIDE, HIGHLIGHT }

/**
 * A user-defined rule for hiding (or highlighting) articles, e.g. "hide any
 * article whose title contains 'στοίχημα'". Applied both during sync (so
 * notifications never fire for hidden articles) and at query time in the feed.
 */
@Serializable
data class FilterRule(
    val id: String,
    val field: FilterField,
    val match: FilterMatch,
    val value: String,
    val caseSensitive: Boolean = false,
    val action: FilterAction = FilterAction.HIDE,
    /** Null = applies to every source; otherwise restricted to one sourceId. */
    val scopeSourceId: String? = null,
    val enabled: Boolean = true,
)
