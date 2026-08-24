package gr.thrylos.news.theme

import androidx.compose.ui.text.font.FontFamily
import gr.thrylos.news.model.ReaderFontFamily

/** Uses Android's built-in generic font families so no font files need to be bundled. */
fun fontFamilyFor(family: ReaderFontFamily): FontFamily = when (family) {
    ReaderFontFamily.SERIF -> FontFamily.Serif
    ReaderFontFamily.SANS -> FontFamily.SansSerif
    ReaderFontFamily.DYSLEXIC -> FontFamily.SansSerif // closest built-in stand-in; a bundled dyslexia-friendly font can replace this later.
}

const val READER_BASE_BODY_SP = 17
const val READER_BASE_HEADING_SP = 22
