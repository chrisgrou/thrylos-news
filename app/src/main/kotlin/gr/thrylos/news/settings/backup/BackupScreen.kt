package gr.thrylos.news.settings.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    val bundledFiltersActive by viewModel.bundledFiltersActive.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmingClearHistory by remember { mutableStateOf(false) }

    val exportJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            viewModel.export { content ->
                context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
            }
        }
    }
    val exportOpmlLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/x-opml")) { uri ->
        if (uri != null) {
            viewModel.exportOpml { content ->
                context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (text != null) viewModel.import(text)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Δεδομένα") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Πίσω") } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Text(
                "Εξαγωγή/εισαγωγή πηγών, φίλτρων, bookmarks και ρυθμίσεων, προτεινόμενα φίλτρα, και εκκαθάριση ιστορικού άρθρων.",
                style = MaterialTheme.typography.bodyMedium,
            )

            Button(
                onClick = { exportJsonLauncher.launch("thrylos-news-backup.json") },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) { Text("Εξαγωγή αντιγράφου ασφαλείας") }

            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json")) },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            ) { Text("Εισαγωγή από αρχείο") }

            OutlinedButton(
                onClick = { exportOpmlLauncher.launch("thrylos-news-sources.opml") },
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            ) { Text("Εξαγωγή πηγών RSS ως OPML") }

            HorizontalDivider(modifier = Modifier.padding(top = 28.dp, bottom = 4.dp))
            Text(
                "Ένα σετ προτεινόμενων φίλτρων, έτοιμων προς εισαγωγή με ένα κλικ — αντί να τα δημιουργήσεις ένα-ένα. Απενεργοποίηση = αφαίρεση ακριβώς αυτού του σετ.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Προτεινόμενα φίλτρα", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = bundledFiltersActive, onCheckedChange = viewModel::setBundledFiltersActive)
            }

            HorizontalDivider(modifier = Modifier.padding(top = 28.dp, bottom = 4.dp))
            Text(
                "Διαγράφει όλα τα αποθηκευμένα άρθρα (εκτός από τα bookmarks) και τα φέρνει ξανά στην επόμενη ανανέωση — χρήσιμο όταν μια πηγή έχει δείξει λάθος στοιχεία (π.χ. ώρα δημοσίευσης) που δεν διορθώνονται μόνα τους.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
            OutlinedButton(
                onClick = { confirmingClearHistory = true },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            ) { Text("Εκκαθάριση ιστορικού άρθρων") }

            if (status != null) {
                Text(status ?: "", modifier = Modifier.padding(top = 20.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    if (confirmingClearHistory) {
        AlertDialog(
            onDismissRequest = { confirmingClearHistory = false },
            title = { Text("Εκκαθάριση ιστορικού άρθρων;") },
            text = { Text("Όλα τα άρθρα εκτός από τα bookmarks θα διαγραφούν και θα ξαναφορτωθούν στην επόμενη ανανέωση.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearArticleHistory(); confirmingClearHistory = false }) { Text("Εκκαθάριση") }
            },
            dismissButton = { TextButton(onClick = { confirmingClearHistory = false }) { Text("Άκυρο") } },
        )
    }
}
