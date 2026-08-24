package gr.thrylos.news.model

import kotlinx.serialization.Serializable

@Serializable
enum class FilterField { TITLE, BODY, AUTHOR, URL, SOURCE }

@Serializable
enum class FilterMatch { CONTAINS, REGEX, EXACT }

@Serializable
enum class FilterAction { HIDE, HIGHLIGHT, IMPORTANT }

@Serializable
enum class FilterCombinator { AND, OR }

/** One clause inside a [FilterRule], e.g. "author is Χ" or "title contains Ψ". */
@Serializable
data class FilterCondition(
    val field: FilterField,
    val match: FilterMatch,
    val value: String,
    val caseSensitive: Boolean = false,
)

/**
 * A user-defined rule for hiding, highlighting, or marking articles as important,
 * e.g. "hide any article whose title contains 'στοίχημα'" or "mark as important
 * when author is Χ AND title contains Ψ". Applied both during sync (so
 * notifications never fire for hidden articles) and at query time in the feed.
 */
@Serializable
data class FilterRule(
    val id: String,
    val conditions: List<FilterCondition>,
    val combinator: FilterCombinator = FilterCombinator.AND,
    val action: FilterAction = FilterAction.HIDE,
    /** Null = applies to every source; otherwise restricted to one sourceId. */
    val scopeSourceId: String? = null,
    val enabled: Boolean = true,
) {
    /** Convenience constructor for the common single-condition case. */
    constructor(
        id: String,
        field: FilterField,
        match: FilterMatch,
        value: String,
        caseSensitive: Boolean = false,
        action: FilterAction = FilterAction.HIDE,
        scopeSourceId: String? = null,
        enabled: Boolean = true,
    ) : this(
        id = id,
        conditions = listOf(FilterCondition(field, match, value, caseSensitive)),
        combinator = FilterCombinator.AND,
        action = action,
        scopeSourceId = scopeSourceId,
        enabled = enabled,
    )
}
