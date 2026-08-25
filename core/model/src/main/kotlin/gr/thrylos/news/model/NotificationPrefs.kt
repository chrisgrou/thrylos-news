package gr.thrylos.news.model

import kotlinx.serialization.Serializable

@Serializable
data class NotificationPrefs(
    val enabled: Boolean = true,
    /** Empty = all enabled sources notify; otherwise only these sourceIds do. */
    val onlySourceIds: Set<String> = emptySet(),
    /** Only notify when the article title/body matches one of these keywords. Empty = no keyword restriction. */
    val onlyKeywords: Set<String> = emptySet(),
    /** Only notify for articles a filter rule marks IMPORTANT — everything else syncs silently. */
    val onlyImportant: Boolean = true,
    val groupIntoSummary: Boolean = true,
)
