package gr.thrylos.news.feed

import gr.thrylos.news.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gr.thrylos.news.matches.MatchesOverlay

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
    val lastSyncAt by viewModel.lastSyncAt.collectAsStateWithLifecycle()
    val lastSyncOutcome by viewModel.lastSyncOutcome.collectAsStateWithLifecycle()
    val hasUnread by viewModel.hasUnread.collectAsStateWithLifecycle()
    var showSourcePicker by remember { mutableStateOf(false) }
    var showMatches by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.olympiacos_logo),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                        )
                        Text(
                            "ΤΑ ΝΕΑ ΤΟΥ\nΟΛΥΜΠΙΑΚΟΥ",
                            style = MaterialTheme.typography.titleSmall,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(start = 10.dp),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.markAllRead() }, enabled = hasUnread) {
                        Icon(Icons.Filled.DoneAll, contentDescription = "Μαρκάρισμα όλων ως διαβασμένα")
                    }
                    IconButton(onClick = onOpenBookmarks) {
                        Icon(Icons.Filled.Bookmarks, contentDescription = "Bookmarks")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Ρυθμίσεις")
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
                    lastSyncAt = lastSyncAt,
                    lastSyncOutcome = lastSyncOutcome,
                    onOpenSourcePicker = { showSourcePicker = true },
                    onSetUnreadOnly = viewModel::setUnreadOnly,
                    onSetPage = viewModel::setPage,
                    onOpenMatches = { showMatches = true },
                )

                if (state.isEmpty) {
                    // PullToRefreshBox only detects the pull gesture through nested
                    // scroll deltas dispatched by a scrollable child — a plain static
                    // Box never dispatches any, so the pull silently does nothing here
                    // unless this is itself scrollable too.
                    Box(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Δεν υπάρχουν άρθρα ακόμα — τράβηξε για ανανέωση.")
                    }
                } else {
                    val listState = rememberLazyListState()
                    // Only scroll to top when the page number actually changes (real
                    // pagination) — not on every recomposition, which also happens when
                    // returning here from the reader. previousPage is rememberSaveable so
                    // it (and listState) survive that round trip with their real prior
                    // values instead of resetting to page 0 / scroll 0 every time.
                    var previousPage by rememberSaveable { mutableStateOf(state.page) }
                    LaunchedEffect(state.page) {
                        if (state.page != previousPage) listState.scrollToItem(0)
                        previousPage = state.page
                    }

                    val newCount = state.items.takeWhile { it.isNew }.size
                    val showNewSection = newCount in 1 until state.items.size

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        itemsIndexed(
                            state.items,
                            key = { _, item -> item.article.id },
                            contentType = { _, _ -> "article" },
                        ) { index, item ->
                            ArticleCard(item = item, onClick = {
                                viewModel.openArticle(item.article.id)
                                onOpenArticle(item.article.id)
                            })
                            if (showNewSection && index == newCount - 1) {
                                HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                            }
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

    if (showMatches) {
        ModalBottomSheet(onDismissRequest = { showMatches = false }) {
            MatchesOverlay()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedFilterBar(
    state: FeedUiState,
    lastSyncAt: Long?,
    lastSyncOutcome: String?,
    onOpenSourcePicker: () -> Unit,
    onSetUnreadOnly: (Boolean) -> Unit,
    onSetPage: (Int) -> Unit,
    onOpenMatches: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = state.selectedSourceName != null,
                onClick = onOpenSourcePicker,
                label = { Text(state.selectedSourceName?.let(::stripSourceSuffix) ?: "Όλες οι πηγές") },
                trailingIcon = { Icon(Icons.Filled.ExpandMore, contentDescription = null) },
                shape = FilterBarShape,
                modifier = Modifier.height(FilterBarHeight),
            )
            UnreadOnlyToggle(unreadOnly = state.unreadOnly, onSetUnreadOnly = onSetUnreadOnly)
            IconButton(onClick = onOpenMatches, modifier = Modifier.size(FilterBarHeight)) {
                Icon(Icons.Filled.SportsSoccer, contentDescription = "Πρόγραμμα αγώνων")
            }
            if (state.pageCount > 1) {
                CompactPager(state = state, onSetPage = onSetPage)
            }
        }
        val isNormal = lastSyncOutcome == null || lastSyncOutcome == "Ολοκληρώθηκε"
        Text(
            when {
                lastSyncAt == null -> "Καμία ενημέρωση ακόμα"
                isNormal -> "Τελευταία ενημέρωση: ${lastSyncLabel(lastSyncAt)}"
                else -> "Τελευταία προσπάθεια: ${lastSyncLabel(lastSyncAt)} — $lastSyncOutcome"
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (isNormal) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 6.dp, start = 4.dp),
        )
    }
}

/** "12:34" if it happened today, "χθες 12:34" if yesterday, else "24/8 12:34". */
private fun lastSyncLabel(millis: Long): String {
    val now = java.util.Calendar.getInstance()
    val then = java.util.Calendar.getInstance().apply { timeInMillis = millis }
    val timeFmt = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return when {
        now.get(java.util.Calendar.YEAR) == then.get(java.util.Calendar.YEAR) &&
            now.get(java.util.Calendar.DAY_OF_YEAR) == then.get(java.util.Calendar.DAY_OF_YEAR) -> timeFmt.format(then.time)
        now.get(java.util.Calendar.YEAR) == then.get(java.util.Calendar.YEAR) &&
            now.get(java.util.Calendar.DAY_OF_YEAR) - then.get(java.util.Calendar.DAY_OF_YEAR) == 1 -> "χθες ${timeFmt.format(then.time)}"
        else -> java.text.SimpleDateFormat("d/M HH:mm", java.util.Locale.getDefault()).format(then.time)
    }
}

/** Shared with [UnreadOnlyToggle] and [CompactPager] so every control in the filter
 *  bar — including the "Όλες οι πηγές" [FilterChip] — has the same squared corners
 *  and the same 32dp height. */
private val FilterBarShape = RoundedCornerShape(8.dp)
private val FilterBarHeight = 32.dp

/** A single control housing "Όλα"/"Νέα" as two joined segments, rather than two
 *  separate chips with a gap between them. */
@Composable
private fun UnreadOnlyToggle(unreadOnly: Boolean, onSetUnreadOnly: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .height(FilterBarHeight)
            .clip(FilterBarShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, FilterBarShape),
    ) {
        UnreadOnlySegment(label = "Όλα", selected = !unreadOnly, onClick = { onSetUnreadOnly(false) })
        UnreadOnlySegment(label = "Νέα", selected = unreadOnly, onClick = { onSetUnreadOnly(true) })
    }
}

@Composable
private fun RowScope.UnreadOnlySegment(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CompactPager(state: FeedUiState, onSetPage: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(FilterBarHeight)
            .clip(FilterBarShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, FilterBarShape),
    ) {
        IconButton(
            onClick = { onSetPage(state.page - 1) },
            enabled = state.page > 0,
            modifier = Modifier.size(FilterBarHeight),
        ) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "Προηγούμενη σελίδα")
        }
        Text(
            "${state.page + 1}/${state.pageCount}",
            style = MaterialTheme.typography.labelLarge,
        )
        IconButton(
            onClick = { onSetPage(state.page + 1) },
            enabled = state.page < state.pageCount - 1,
            modifier = Modifier.size(FilterBarHeight),
        ) {
            Icon(Icons.Filled.ChevronRight, contentDescription = "Επόμενη σελίδα")
        }
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
