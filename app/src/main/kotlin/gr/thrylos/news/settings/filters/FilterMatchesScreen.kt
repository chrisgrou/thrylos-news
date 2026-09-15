package gr.thrylos.news.settings.filters

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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import gr.thrylos.news.feed.stripSourceSuffix
import gr.thrylos.news.sources.filter.FilterEngine

/** The articles a rule catches, laid out exactly like a source's or an author's own
 *  article list — same rows, and tapping one opens it in the reader. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterMatchesScreen(
    onBack: () -> Unit,
    onOpenArticle: (String) -> Unit,
    viewModel: FilterMatchesViewModel = hiltViewModel(),
) {
    val articles by viewModel.articles.collectAsStateWithLifecycle()
    val rule by viewModel.rule.collectAsStateWithLifecycle() // TEMPORARY — see FilterEngine.debugConditionResults.

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(articles?.let { "Ταιριάζουν ${it.size} άρθρα" } ?: "Άρθρα που ταιριάζουν") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Πίσω") } },
            )
        },
    ) { padding ->
        val list = articles
        when {
            list == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            list.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Κανένα αποθηκευμένο άρθρο δεν ταιριάζει με αυτόν τον κανόνα.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(list, key = { it.id }) { article ->
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
                        supportingContent = {
                            Column {
                                Text(stripSourceSuffix(article.sourceName))
                                // TEMPORARY diagnostic — see FilterEngine.debugConditionResults.
                                val r = rule
                                if (r != null) {
                                    val results = FilterEngine.debugConditionResults(r, article)
                                    Text(
                                        "DEBUG: " + r.conditions.zip(results).joinToString("  ") { (c, ok) -> "${c.field}=${ok}" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().clickable {
                            viewModel.setCursorContext(list.map { it.id })
                            onOpenArticle(article.id)
                        },
                    )
                }
            }
        }
    }
}
