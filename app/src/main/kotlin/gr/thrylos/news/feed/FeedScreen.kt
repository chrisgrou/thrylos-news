package gr.thrylos.news.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onOpenArticle: (String) -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var refreshing by remember { mutableStateOf(false) }
    var showSourcePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thrylos News") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Ανανέωση")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Ροή") },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onOpenBookmarks,
                    icon = { Icon(Icons.Filled.Bookmarks, contentDescription = null) },
                    label = { Text("Bookmarks") },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onOpenSettings,
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("Ρυθμίσεις") },
                )
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                refreshing = true
                viewModel.refresh()
                refreshing = false
            },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            Column(Modifier.fillMaxSize()) {
                FeedFilterBar(
                    state = state,
                    onOpenSourcePicker = { showSourcePicker = true },
                    onToggleUnread = viewModel::toggleUnreadOnly,
                )

                if (state.isEmpty) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Δεν υπάρχουν άρθρα ακόμα — τράβηξε για ανανέωση.")
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.items, key = { it.article.id }) { item ->
                            ArticleCard(item = item, onClick = {
                                viewModel.openArticle(item.article.id)
                                onOpenArticle(item.article.id)
                            })
                        }
                    }
                }
            }
        }
    }

    if (showSourcePicker) {
        ModalBottomSheet(onDismissRequest = { showSourcePicker = false }) {
            SourcePickerSheet(
                state = state,
                onSelect = { name ->
                    viewModel.selectSource(name)
                    showSourcePicker = false
                },
            )
        }
    }
}

@Composable
private fun FeedFilterBar(state: FeedUiState, onOpenSourcePicker: () -> Unit, onToggleUnread: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = state.selectedSourceName != null,
            onClick = onOpenSourcePicker,
            label = { Text(state.selectedSourceName ?: "Όλες οι πηγές") },
            trailingIcon = { Icon(Icons.Filled.ExpandMore, contentDescription = null) },
        )
        FilterChip(selected = state.unreadOnly, onClick = onToggleUnread, label = { Text("Αδιάβαστα") })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SourcePickerSheet(state: FeedUiState, onSelect: (String?) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(20.dp)) {
        Text("Πηγή", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = state.selectedSourceName == null, onClick = { onSelect(null) }, label = { Text("Όλα") })
            state.sources.forEach { source ->
                FilterChip(
                    selected = state.selectedSourceName == source.name,
                    onClick = { onSelect(source.name) },
                    label = { Text(source.name) },
                )
            }
        }
    }
}
