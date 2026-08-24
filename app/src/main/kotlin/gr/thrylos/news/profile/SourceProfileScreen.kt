package gr.thrylos.news.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceProfileScreen(
    onBack: () -> Unit,
    onOpenArticle: (String) -> Unit,
    viewModel: SourceProfileViewModel = hiltViewModel(),
) {
    val articles by viewModel.articles.collectAsStateWithLifecycle()
    val authors by viewModel.authors.collectAsStateWithLifecycle()
    var selectedAuthor by remember { mutableStateOf<String?>(null) }
    var authorMenuExpanded by remember { mutableStateOf(false) }

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
            if (authors.size > 1) {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    AssistChip(
                        onClick = { authorMenuExpanded = true },
                        label = { Text(selectedAuthor ?: "Όλοι οι συντάκτες") },
                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                    )
                    DropdownMenu(expanded = authorMenuExpanded, onDismissRequest = { authorMenuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Όλοι οι συντάκτες") },
                            onClick = { selectedAuthor = null; authorMenuExpanded = false },
                        )
                        authors.forEach { author ->
                            DropdownMenuItem(
                                text = { Text(author) },
                                onClick = { selectedAuthor = author; authorMenuExpanded = false },
                            )
                        }
                    }
                }
            }

            LazyColumn(Modifier.fillMaxSize()) {
                items(visibleArticles, key = { it.id }) { article ->
                    ListItem(
                        leadingContent = {
                            if (article.leadImageUrl != null) {
                                AsyncImage(
                                    model = article.leadImageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)),
                                )
                            } else {
                                Box(
                                    Modifier.size(52.dp).clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                )
                            }
                        },
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
