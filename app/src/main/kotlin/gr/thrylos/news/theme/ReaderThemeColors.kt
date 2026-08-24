package gr.thrylos.news.theme

import androidx.compose.ui.graphics.Color
import gr.thrylos.news.model.ReaderTheme

data class ReaderColors(val background: Color, val text: Color, val secondaryText: Color)

fun colorsFor(theme: ReaderTheme): ReaderColors = when (theme) {
    ReaderTheme.LIGHT -> ReaderColors(Color(0xFFFDFBF6), Color(0xFF1C1A20), Color(0xFF6B6570))
    ReaderTheme.SEPIA -> ReaderColors(Color(0xFFF1E3C4), Color(0xFF3C2F1C), Color(0xFF7A6A4F))
    ReaderTheme.DARK -> ReaderColors(Color(0xFF1A1920), Color(0xFFE9E6E0), Color(0xFF9B96A3))
    ReaderTheme.BLACK -> ReaderColors(Color(0xFF000000), Color(0xFFEDEDED), Color(0xFF9B9B9B))
    ReaderTheme.HIGH_CONTRAST_BW -> ReaderColors(Color(0xFF000000), Color(0xFFFFFFFF), Color(0xFFFFFFFF))
}
