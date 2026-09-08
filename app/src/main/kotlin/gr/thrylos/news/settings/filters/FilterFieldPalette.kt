package gr.thrylos.news.settings.filters

import androidx.compose.ui.graphics.Color
import gr.thrylos.news.model.FilterField

/** Shared between the read-only rule list ([FiltersScreen]) and the rule editor
 *  ([FilterEditorScreen]) so a field's color is consistent everywhere it appears. */
internal data class FieldPalette(val strong: Color, val onStrong: Color, val container: Color, val onContainer: Color)

// Fixed (theme-independent) hues, deliberately far from the app's red/pink brand
// color so a badge's category reads clearly instead of blending into the rest
// of the UI (buttons, switches, the app bar are all shades of Olympiacos red).
private val FIELD_PALETTES = mapOf(
    FilterField.TITLE to FieldPalette(Color(0xFF1565C0), Color.White, Color(0xFFBBDEFB), Color(0xFF0D2B4E)),
    FilterField.BODY to FieldPalette(Color(0xFF2E7D32), Color.White, Color(0xFFC8E6C9), Color(0xFF13351A)),
    FilterField.AUTHOR to FieldPalette(Color(0xFFEF6C00), Color.White, Color(0xFFFFE0B2), Color(0xFF5C3300)),
    FilterField.URL to FieldPalette(Color(0xFF37474F), Color.White, Color(0xFFCFD8DC), Color(0xFF1B2529)),
    FilterField.SOURCE to FieldPalette(Color(0xFF00695C), Color.White, Color(0xFFB2DFDB), Color(0xFF00332C)),
    FilterField.ANYWHERE to FieldPalette(Color(0xFF6A1B9A), Color.White, Color(0xFFE1BEE7), Color(0xFF3B0764)),
)

internal fun fieldPalette(field: FilterField): FieldPalette = FIELD_PALETTES.getValue(field)

private val GREEK_ACCENTS = mapOf(
    'ά' to 'α', 'έ' to 'ε', 'ή' to 'η', 'ί' to 'ι', 'ό' to 'ο', 'ύ' to 'υ', 'ώ' to 'ω',
    'ϊ' to 'ι', 'ΐ' to 'ι', 'ϋ' to 'υ', 'ΰ' to 'υ',
)

/** Greek typographic convention: monotonic accents are dropped when a word is
 *  written in all caps (e.g. "τίτλος" → "ΤΙΤΛΟΣ", not "ΤΊΤΛΟΣ"). */
internal fun uppercaseNoAccents(text: String): String =
    text.map { GREEK_ACCENTS[it] ?: it }.joinToString("").uppercase()

/** Every badge label there can ever be, built once. There are six fields, and the
 *  labels never change — rebuilding one character-by-character for every badge on
 *  every composition was pure waste in a scrolling list. */
internal val FIELD_BADGE_LABELS: Map<FilterField, String> =
    FilterField.entries.associateWith { uppercaseNoAccents(labelForField(it)) }

/** Shared by the badge labels above and by both screens' visible field names. */
internal fun labelForField(field: FilterField) = when (field) {
    FilterField.TITLE -> "Τίτλος"
    FilterField.BODY -> "Κείμενο"
    FilterField.AUTHOR -> "Συντάκτης"
    FilterField.URL -> "URL"
    FilterField.SOURCE -> "Πηγή"
    FilterField.ANYWHERE -> "Οπουδήποτε"
}
