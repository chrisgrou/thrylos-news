package gr.thrylos.news.settings.sources

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gr.thrylos.news.sources.plugin.SourceKind

/** The list is deliberately just a name + on/off toggle per source — tapping a row
 *  opens its (unfiltered) article list, where editing and deleting that source live
 *  instead (see [gr.thrylos.news.profile.SourceProfileScreen]). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesScreen(
    onBack: () -> Unit,
    onAddSource: (kind: String?) -> Unit,
    onOpenSourceProfile: (sourceName: String) -> Unit,
    viewModel: SourcesViewModel = hiltViewModel(),
) {
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    var showAddChooser by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Πηγές") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Πίσω") } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddChooser = true }) { Icon(Icons.Filled.Add, contentDescription = "Νέα πηγή") }
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(groups, key = { it.displayName }) { group ->
                val syncError = group.members.firstNotNullOfOrNull { it.lastSyncError }

                ListItem(
                    leadingContent = { SourceKindIcon(group.kind) },
                    headlineContent = { Text(group.displayName) },
                    supportingContent = syncError?.let {
                        { Text("Σφάλμα sync: $it", color = MaterialTheme.colorScheme.error) }
                    },
                    trailingContent = {
                        Switch(checked = group.enabled, onCheckedChange = { viewModel.setGroupEnabled(group, it) })
                    },
                    modifier = Modifier.fillMaxWidth().clickable { onOpenSourceProfile(group.rawName) },
                )
            }
        }
    }

    if (showAddChooser) {
        AddSourceChooserDialog(
            onDismiss = { showAddChooser = false },
            onChoose = { kind -> showAddChooser = false; onAddSource(kind) },
        )
    }
}

@Composable
private fun SourceKindIcon(kind: SourceKind) {
    when (kind) {
        SourceKind.SITE -> Icon(Icons.Filled.Public, contentDescription = "Site")
        SourceKind.FACEBOOK -> Icon(Icons.Filled.ThumbUp, contentDescription = "Facebook")
    }
}

@Composable
private fun AddSourceChooserDialog(onDismiss: () -> Unit, onChoose: (kind: String?) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Νέα πηγή") },
        text = {
            Column {
                ListItem(
                    leadingContent = { Icon(Icons.Filled.Public, contentDescription = null) },
                    headlineContent = { Text("Site") },
                    supportingContent = { Text("Άρθρα από ιστοσελίδα (RSS ή HTML)") },
                    modifier = Modifier.clickable { onChoose(null) },
                )
                ListItem(
                    leadingContent = { Icon(Icons.Filled.ThumbUp, contentDescription = null) },
                    headlineContent = { Text("Προφίλ/σελίδα Facebook") },
                    supportingContent = { Text("Δημοσιεύσεις από δημόσια σελίδα, χωρίς σύνδεση") },
                    modifier = Modifier.clickable { onChoose("facebook") },
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Άκυρο") } },
    )
}
