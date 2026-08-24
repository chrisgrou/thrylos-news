package gr.thrylos.news.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import gr.thrylos.news.model.ReaderFontFamily
import gr.thrylos.news.model.ReaderPrefs
import gr.thrylos.news.model.ReaderTheme
import gr.thrylos.news.model.TextAlign

@Composable
fun ReadingSettingsSheet(prefs: ReaderPrefs, onUpdate: ((ReaderPrefs) -> ReaderPrefs) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(20.dp)) {
        Text("Μέγεθος γραμματοσειράς", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
        Row(Modifier.padding(top = 8.dp, bottom = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onUpdate { it.copy(fontScale = (it.fontScale - 0.1f).coerceAtLeast(0.75f)) } }) {
                Icon(Icons.Filled.Remove, contentDescription = "Μικρότερο")
            }
            Text("Aa", modifier = Modifier.padding(horizontal = 12.dp))
            IconButton(onClick = { onUpdate { it.copy(fontScale = (it.fontScale + 0.1f).coerceAtMost(1.8f)) } }) {
                Icon(Icons.Filled.Add, contentDescription = "Μεγαλύτερο")
            }
        }

        Text("Γραμματοσειρά", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
        Row(Modifier.padding(top = 8.dp, bottom = 20.dp)) {
            ReaderFontFamily.entries.forEach { family ->
                FilterChip(
                    selected = prefs.fontFamily == family,
                    onClick = { onUpdate { it.copy(fontFamily = family) } },
                    label = { Text(labelFor(family)) },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }

        Text("Θέμα", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
        Row(Modifier.padding(top = 8.dp, bottom = 20.dp)) {
            ReaderTheme.entries.forEach { theme ->
                FilterChip(
                    selected = prefs.theme == theme,
                    onClick = { onUpdate { it.copy(theme = theme) } },
                    label = { Text(labelFor(theme)) },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }

        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
            Text("Πλήρης στοίχιση")
            Switch(
                checked = prefs.textAlign == TextAlign.JUSTIFY,
                onCheckedChange = { checked -> onUpdate { it.copy(textAlign = if (checked) TextAlign.JUSTIFY else TextAlign.START) } },
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
            Text("Οθόνη πάντα ενεργή")
            Switch(
                checked = prefs.keepScreenOn,
                onCheckedChange = { checked -> onUpdate { it.copy(keepScreenOn = checked) } },
            )
        }
    }
}

private fun labelFor(family: ReaderFontFamily) = when (family) {
    ReaderFontFamily.SERIF -> "Serif"
    ReaderFontFamily.SANS -> "Sans"
    ReaderFontFamily.DYSLEXIC -> "Ευανάγνωστη"
}

private fun labelFor(theme: ReaderTheme) = when (theme) {
    ReaderTheme.LIGHT -> "Light"
    ReaderTheme.SEPIA -> "Sepia"
    ReaderTheme.DARK -> "Dark"
    ReaderTheme.BLACK -> "Black"
    ReaderTheme.HIGH_CONTRAST_BW -> "B/W"
}
