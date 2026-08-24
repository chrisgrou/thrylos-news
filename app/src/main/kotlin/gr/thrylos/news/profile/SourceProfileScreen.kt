package gr.thrylos.news.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SourceProfileScreen(
    onBack: () -> Unit,
    onOpenArticle: (String) -> Unit,
    onIgnored: () -> Unit,
    viewModel: SourceProfileViewModel = hiltViewModel(),
) {
    val articles by viewModel.articles.collectAsStateWithLifecycle()
    val authors by viewModel.authors.collectAsStateWithLifecycle()
    val important by viewModel.isImportant.collectAsStateWithLifecycle()
    var selectedAuthor by remember { mutableStateOf<String?>(null) }

    val visibleArticles = if (selectedAuthor == null) articles else articles.filter { it.author == selectedAuthor }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viewModel.sourceName) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Πίσω") } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { viewModel.toggleImportant() }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text(if (important) "Αφαίρεση σημαντικού" else "Σημαντική πηγή")
                }
                OutlinedButton(
                    onClick = { viewModel.ignoreSource(); onIgnored() },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.VisibilityOff, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text("Αγνόησε")
                }
            }

            if (authors.size > 1) {
                Text(
                    "Συντάκτης",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(selected = selectedAuthor == null, onClick = { selectedAuthor = null }, label = { Text("Όλοι") })
                    authors.forEach { author ->
                        FilterChip(
                            selected = selectedAuthor == author,
                            onClick = { selectedAuthor = if (selectedAuthor == author) null else author },
                            label = { Text(author) },
                        )
                    }
                }
            }

            LazyColumn(Modifier.fillMaxSize()) {
                items(visibleArticles, key = { it.id }) { article ->
                    ListItem(
                        headlineContent = { Text(article.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        supportingContent = article.author?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth().clickable {
                            viewModel.setCursorContext(visibleArticles.map { it.id })
                            onOpenArticle(article.id)
                        },
                    )
                }
            }
        }
    }
}
