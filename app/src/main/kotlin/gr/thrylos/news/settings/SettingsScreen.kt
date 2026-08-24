package gr.thrylos.news.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenSources: () -> Unit,
    onOpenFilters: () -> Unit,
    onOpenSync: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenAuthors: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ρυθμίσεις") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Πίσω") } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SettingsRow("Πηγές", "Ενεργοποίηση, εισαγωγή και επεξεργασία plugins", onOpenSources)
            SettingsRow("Συντάκτες", "Περιήγηση σε άρθρα ανά συντάκτη", onOpenAuthors)
            SettingsRow("Φίλτρα", "Απόκρυψη ή προβολή άρθρων βάσει λέξεων-κλειδιών ή συντάκτη", onOpenFilters)
            SettingsRow("Ανανέωση & Ειδοποιήσεις", "Διάστημα, Wi-Fi, αποθηκευτικός χώρος", onOpenSync)
            SettingsRow("Αντίγραφο ασφαλείας", "Εξαγωγή/εισαγωγή πηγών, φίλτρων και bookmarks", onOpenBackup)
        }
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}
