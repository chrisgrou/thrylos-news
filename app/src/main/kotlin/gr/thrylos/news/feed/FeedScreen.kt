package gr.thrylos.news.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
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
    onOpenSourceProfile: (sourceName: String) -> Unit,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val syncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    var showSourcePicker by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thrylos News") },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Μενού")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Μαρκάρισμα όλων ως διαβασμένα") },
                            leadingIcon = { Icon(Icons.Filled.DoneAll, contentDescription = null) },
                            onClick = { showMenu = false; viewModel.markAllRead() },
                        )
                        DropdownMenuItem(
                            text = { Text("Bookmarks") },
                            leadingIcon = { Icon(Icons.Filled.Bookmarks, contentDescription = null) },
                            onClick = { showMenu = false; onOpenBookmarks() },
                        )
                        DropdownMenuItem(
                            text = { Text("Ρυθμίσεις") },
                            leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                            onClick = { showMenu = false; onOpenSettings() },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.refresh() }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Ανανέωση")
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = syncing,
            onRefresh = viewModel::refresh,
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
                        verticalArrangement = Arrangement.spacedBy(12.dp),
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
                onOpenProfile = { name ->
                    showSourcePicker = false
                    onOpenSourceProfile(name)
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
            label = { Text(state.selectedSourceName?.let(::stripSourceSuffix) ?: "Όλες οι πηγές") },
            trailingIcon = { Icon(Icons.Filled.ExpandMore, contentDescription = null) },
        )
        FilterChip(selected = state.unreadOnly, onClick = onToggleUnread, label = { Text("Αδιάβαστα") })
    }
}

@Composable
private fun SourcePickerSheet(state: FeedUiState, onSelect: (String?) -> Unit, onOpenProfile: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        ListItem(
            headlineContent = { Text("Όλα") },
            modifier = Modifier.fillMaxWidth().clickable { onSelect(null) },
        )
        state.sources.forEach { source ->
            ListItem(
                headlineContent = { Text(stripSourceSuffix(source.name)) },
                trailingContent = {
                    IconButton(onClick = { onOpenProfile(source.name) }) {
                        Icon(Icons.Filled.OpenInNew, contentDescription = "Αρχική ${source.name}")
                    }
                },
                modifier = Modifier.fillMaxWidth().clickable { onSelect(source.name) },
            )
        }
    }
}
