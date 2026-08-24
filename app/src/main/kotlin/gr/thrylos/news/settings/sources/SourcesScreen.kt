package gr.thrylos.news.settings.sources

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesScreen(
    onBack: () -> Unit,
    onAddSource: () -> Unit,
    onEditSource: (String) -> Unit,
    onOpenSourceProfile: (sourceName: String) -> Unit,
    viewModel: SourcesViewModel = hiltViewModel(),
) {
    val groups by viewModel.groups.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Πηγές") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Πίσω") } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddSource) { Icon(Icons.Filled.Add, contentDescription = "Νέα πηγή") }
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(groups, key = { it.displayName }) { group ->
                var showEditMenu by remember { mutableStateOf(false) }

                fun editGroup() {
                    if (group.members.size == 1) onEditSource(group.members.first().id) else showEditMenu = true
                }

                ListItem(
                    headlineContent = { Text(group.displayName) },
                    leadingContent = {
                        Row {
                            IconButton(onClick = { viewModel.moveUp(group.displayName) }) {
                                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Πάνω")
                            }
                            IconButton(onClick = { viewModel.moveDown(group.displayName) }) {
                                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Κάτω")
                            }
                        }
                    },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { onOpenSourceProfile(group.displayName) }) {
                                Icon(Icons.Filled.OpenInNew, contentDescription = "Αρχική")
                            }
                            Switch(checked = group.enabled, onCheckedChange = { viewModel.setGroupEnabled(group, it) })
                            Box {
                                DropdownMenu(expanded = showEditMenu, onDismissRequest = { showEditMenu = false }) {
                                    group.members.forEach { member ->
                                        DropdownMenuItem(
                                            text = { Text(memberLabel(member.id)) },
                                            onClick = { showEditMenu = false; onEditSource(member.id) },
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { viewModel.removeGroup(group) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Διαγραφή")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().clickable { editGroup() },
                )
            }
        }
    }
}

private fun memberLabel(sourceId: String) = when {
    "football" in sourceId -> "Ποδόσφαιρο"
    "basket" in sourceId -> "Μπάσκετ"
    else -> sourceId
}
