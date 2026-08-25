package gr.thrylos.news.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gr.thrylos.news.update.UpdateState
import gr.thrylos.news.update.UpdateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenSources: () -> Unit,
    onOpenFilters: () -> Unit,
    onOpenSync: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenUpdateHistory: () -> Unit,
    updateViewModel: UpdateViewModel = hiltViewModel(),
) {
    val updateState by updateViewModel.state.collectAsStateWithLifecycle()

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
            SettingsRow("Φίλτρα", "Απόκρυψη ή προβολή άρθρων βάσει λέξεων-κλειδιών ή συντάκτη", onOpenFilters)
            SettingsRow("Εφαρμογή", "Εμφάνιση, ανανέωση, ειδοποιήσεις, αποθηκευτικός χώρος", onOpenSync)
            SettingsRow("Δεδομένα", "Αντίγραφο ασφαλείας, προτεινόμενα φίλτρα, ιστορικό άρθρων", onOpenBackup)
            ListItem(
                headlineContent = { Text("Έλεγχος για ενημερώσεις") },
                supportingContent = {
                    Text(if (updateState is UpdateState.Checking) "Έλεγχος..." else "Νέα έκδοση της εφαρμογής από το GitHub")
                },
                trailingContent = {
                    IconButton(onClick = onOpenUpdateHistory) {
                        Icon(Icons.Filled.History, contentDescription = "Ιστορικό ενημερώσεων")
                    }
                },
                modifier = Modifier.fillMaxWidth().clickable(onClick = updateViewModel::checkForUpdate),
            )
        }
    }

    when (val s = updateState) {
        is UpdateState.Available -> AlertDialog(
            onDismissRequest = updateViewModel::dismiss,
            title = { Text("Νέα έκδοση διαθέσιμη") },
            text = {
                Column {
                    Text("Βρέθηκε νέα έκδοση (build ${s.info.versionCode}). Λήψη και εγκατάσταση;")
                    if (!s.info.releaseNotes.isNullOrBlank()) {
                        Text(
                            "Τι άλλαξε:",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                        )
                        Text(s.info.releaseNotes)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { updateViewModel.downloadAndInstall(s.info) }) { Text("Λήψη & εγκατάσταση") } },
            dismissButton = { TextButton(onClick = updateViewModel::dismiss) { Text("Άκυρο") } },
        )
        is UpdateState.Downloading -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Λήψη ενημέρωσης...") },
            text = {
                Column {
                    if (s.progress > 0f) {
                        LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth())
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {},
        )
        is UpdateState.Error -> AlertDialog(
            onDismissRequest = updateViewModel::dismiss,
            title = { Text("Σφάλμα ενημέρωσης") },
            text = { Text(s.message) },
            confirmButton = {
                if (s.needsInstallPermission) {
                    TextButton(onClick = updateViewModel::openInstallPermissionSettings) { Text("Ρυθμίσεις") }
                } else {
                    TextButton(onClick = updateViewModel::dismiss) { Text("OK") }
                }
            },
            dismissButton = { TextButton(onClick = updateViewModel::dismiss) { Text("Άκυρο") } },
        )
        UpdateState.UpToDate -> AlertDialog(
            onDismissRequest = updateViewModel::dismiss,
            title = { Text("Ενημερωμένη έκδοση") },
            text = { Text("Έχεις ήδη την τελευταία έκδοση.") },
            confirmButton = { TextButton(onClick = updateViewModel::dismiss) { Text("OK") } },
        )
        UpdateState.Checking, UpdateState.Idle -> Unit
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, onClick: () -> Unit, showChevron: Boolean = true) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = if (showChevron) {
            { Icon(Icons.Filled.ChevronRight, contentDescription = null) }
        } else {
            null
        },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}
