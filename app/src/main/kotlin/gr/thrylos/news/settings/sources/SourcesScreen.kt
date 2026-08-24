package gr.thrylos.news.settings.sources

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesScreen(
    onBack: () -> Unit,
    onAddSource: () -> Unit,
    onEditSource: (String) -> Unit,
    viewModel: SourcesViewModel = hiltViewModel(),
) {
    val sources by viewModel.sources.collectAsStateWithLifecycle()

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
            items(sources, key = { it.id }) { source ->
                val index = sources.indexOf(source)
                ListItem(
                    headlineContent = { Text(source.name) },
                    supportingContent = { Text(source.plugin?.homepage.orEmpty()) },
                    leadingContent = {
                        Column {
                            IconButton(onClick = { viewModel.moveUp(index) }, modifier = Modifier) {
                                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Πάνω")
                            }
                            IconButton(onClick = { viewModel.moveDown(index) }) {
                                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Κάτω")
                            }
                        }
                    },
                    trailingContent = {
                        Row {
                            Switch(checked = source.enabled, onCheckedChange = { viewModel.setEnabled(source.id, it) })
                            IconButton(onClick = { onEditSource(source.id) }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Επεξεργασία")
                            }
                            IconButton(onClick = { viewModel.remove(source) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Διαγραφή")
                            }
                        }
                    },
                )
            }
        }
    }
}
