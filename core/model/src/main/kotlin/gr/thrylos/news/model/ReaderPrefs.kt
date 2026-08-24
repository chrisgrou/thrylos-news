package gr.thrylos.news.model

import kotlinx.serialization.Serializable

@Serializable
enum class ReaderTheme { LIGHT, SEPIA, DARK, BLACK, HIGH_CONTRAST_BW }

@Serializable
enum class ReaderFontFamily { SERIF, SANS, DYSLEXIC }

@Serializable
enum class TextAlign { START, JUSTIFY }

data class ReaderPrefs(
    val theme: ReaderTheme = ReaderTheme.LIGHT,
    val fontFamily: ReaderFontFamily = ReaderFontFamily.SERIF,
    /** Scale factor applied to the base reading font size, e.g. 0.85..1.6 */
    val fontScale: Float = 1.0f,
    val lineHeightScale: Float = 1.0f,
    val marginWidth: Int = 1, // 0 = narrow, 1 = normal, 2 = wide
    val textAlign: TextAlign = TextAlign.START,
    val keepScreenOn: Boolean = false,
)
