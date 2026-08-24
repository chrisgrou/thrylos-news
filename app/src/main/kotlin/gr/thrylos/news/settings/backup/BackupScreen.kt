package gr.thrylos.news.settings.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    val context = LocalContext.current

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
                title = { Text("Αντίγραφο ασφαλείας") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Πίσω") } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Text(
                "Εξαγωγή πηγών, φίλτρων, bookmarks και ρυθμίσεων σε ένα αρχείο — χρήσιμο πριν αλλάξεις συσκευή.",
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

            if (status != null) {
                Text(status ?: "", modifier = Modifier.padding(top = 20.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
