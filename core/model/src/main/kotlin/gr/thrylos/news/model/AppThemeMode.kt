package gr.thrylos.news.model

import kotlinx.serialization.Serializable

/** Controls the whole app's Material3 theme (feed, settings, etc.) — separate from
 *  the reader's own theme, which already offers Light/Sepia/Dark/Black/B&W. */
@Serializable
enum class AppThemeMode { SYSTEM, LIGHT, DARK }
