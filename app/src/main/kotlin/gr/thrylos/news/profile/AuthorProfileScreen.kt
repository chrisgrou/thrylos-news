package gr.thrylos.news.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.VisibilityOff
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import gr.thrylos.news.feed.stripSourceSuffix

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorProfileScreen(
    onBack: () -> Unit,
    onOpenArticle: (String) -> Unit,
    viewModel: AuthorProfileViewModel = hiltViewModel(),
) {
    val articles by viewModel.articles.collectAsStateWithLifecycle()
    val important by viewModel.isImportant.collectAsStateWithLifecycle()
    val ignored by viewModel.isIgnored.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viewModel.author) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Πίσω") } },
                actions = {
                    IconButton(onClick = viewModel::toggleImportant) {
                        Icon(
                            if (important) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = "Σημαντικός συντάκτης",
                        )
                    }
                    IconButton(onClick = viewModel::toggleIgnored) {
                        Icon(
                            if (ignored) Icons.Filled.VisibilityOff else Icons.Outlined.VisibilityOff,
                            contentDescription = "Αγνόησε",
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(articles, key = { it.id }) { article ->
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
                    supportingContent = { Text(stripSourceSuffix(article.sourceName)) },
                    modifier = Modifier.fillMaxWidth().clickable {
                        viewModel.setCursorContext(articles.map { it.id })
                        onOpenArticle(article.id)
                    },
                )
            }
        }
    }
}
