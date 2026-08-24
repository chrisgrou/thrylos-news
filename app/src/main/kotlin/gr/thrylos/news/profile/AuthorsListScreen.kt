package gr.thrylos.news.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorsListScreen(
    onBack: () -> Unit,
    onOpenAuthor: (String) -> Unit,
    viewModel: AuthorsListViewModel = hiltViewModel(),
) {
    val authors by viewModel.authors.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Συντάκτες") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Πίσω") } },
            )
        },
    ) { padding ->
        if (authors.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text("Δεν υπάρχουν ακόμα άρθρα με γνωστό συντάκτη.")
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(authors, key = { it }) { author ->
                    ListItem(
                        headlineContent = { Text(author) },
                        modifier = Modifier.fillMaxWidth().clickable { onOpenAuthor(author) },
                    )
                }
            }
        }
    }
}
