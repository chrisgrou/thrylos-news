package gr.thrylos.news.theme

import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import gr.thrylos.news.model.ReaderFontFamily

/** Uses Android's built-in generic font families so no font files need to be bundled.
 *  "Ευανάγνωστη" asks the OS for its medium-weight sans face by name — heavier strokes
 *  read as more legible and, crucially, look visibly different from plain Sans. */
fun fontFamilyFor(family: ReaderFontFamily): FontFamily = when (family) {
    ReaderFontFamily.SERIF -> FontFamily.Serif
    ReaderFontFamily.SANS -> FontFamily.SansSerif
    ReaderFontFamily.DYSLEXIC -> FontFamily(Font(DeviceFontFamilyName("sans-serif-medium"), FontWeight.Medium))
}

const val READER_BASE_BODY_SP = 17
const val READER_BASE_HEADING_SP = 22
