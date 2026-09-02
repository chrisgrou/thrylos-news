package gr.thrylos.news.matches

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsHandball
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import gr.thrylos.news.model.Match
import gr.thrylos.news.model.MatchStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private enum class MatchesViewMode { LIST, CALENDAR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesScreen(
    onBack: () -> Unit,
    viewModel: MatchesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showSportPicker by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(MatchesViewMode.LIST) }

    LaunchedEffect(Unit) { viewModel.loadIfNeeded() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Πρόγραμμα αγώνων") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Πίσω") } },
                actions = {
                    if (state is MatchesUiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp).padding(end = 12.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { viewModel.refresh(force = true) }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Ανανέωση προγράμματος")
                        }
                    }
                    IconButton(onClick = { viewMode = if (viewMode == MatchesViewMode.LIST) MatchesViewMode.CALENDAR else MatchesViewMode.LIST }) {
                        Icon(
                            if (viewMode == MatchesViewMode.LIST) Icons.Filled.CalendarMonth else Icons.Filled.ViewList,
                            contentDescription = if (viewMode == MatchesViewMode.LIST) "Προβολή ημερολογίου" else "Προβολή λίστας",
                        )
                    }
                },
            )
        },
    ) { padding ->
        when (val s = state) {
            is MatchesUiState.Loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is MatchesUiState.SportsDisabled -> Text(
                "Δεν έχεις ενεργοποιήσει κανένα άθλημα. Άνοιξε Ρυθμίσεις → Πρόγραμμα αγώνων.",
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            )

            is MatchesUiState.Error -> Column(
                Modifier.fillMaxSize().padding(padding).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Αποτυχία φόρτωσης: ${s.message}", color = MaterialTheme.colorScheme.error)
                Button(onClick = { viewModel.refresh(force = true) }, modifier = Modifier.padding(top = 12.dp)) { Text("Ξανά") }
            }

            is MatchesUiState.Success -> Column(Modifier.fillMaxSize().padding(padding)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = s.selectedTeamId != null,
                        onClick = { showSportPicker = true },
                        label = { Text(s.selectedTeamId?.let { teamLabel(it) } ?: "Όλα τα αθλήματα") },
                        trailingIcon = { Icon(Icons.Filled.ExpandMore, contentDescription = null) },
                    )
                    HomeAwayToggle(selected = s.homeAwayFilter, onSelect = viewModel::selectHomeAway)
                }
                if (viewMode == MatchesViewMode.CALENDAR) {
                    CalendarView(
                        matches = s.filteredMatches,
                        onOpenMatch = { match -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(match.matchUrl))) },
                        modifier = Modifier.weight(1f),
                    )
                } else if (s.pageMatches.isEmpty()) {
                    Text("Δεν βρέθηκαν προσεχείς αγώνες.", modifier = Modifier.padding(16.dp))
                } else {
                    val density = LocalDensity.current
                    var dragTotal by remember { mutableStateOf(0f) }
                    LazyColumn(
                        Modifier.weight(1f).pointerInput(s.page, s.pageCount) {
                            val threshold = with(density) { 56.dp.toPx() }
                            detectHorizontalDragGestures(
                                onDragStart = { dragTotal = 0f },
                                onHorizontalDrag = { change, dragAmount ->
                                    dragTotal += dragAmount
                                    change.consume()
                                },
                                onDragEnd = {
                                    if (abs(dragTotal) >= threshold) {
                                        // Swipe left (negative drag) advances, like the → chevron.
                                        viewModel.setPage(s.page + if (dragTotal < 0) 1 else -1)
                                    }
                                    dragTotal = 0f
                                },
                                onDragCancel = { dragTotal = 0f },
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        val groups = s.pageMatches.groupByConsecutiveDate()
                        groups.forEach { (dateLabel, matches) ->
                            item(key = "group-$dateLabel-${matches.first().id}") {
                                DateGroup(dateLabel, matches) { match ->
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(match.matchUrl)))
                                }
                            }
                        }
                    }
                    if (s.pageCount > 1) {
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = { viewModel.setPage(s.page - 1) }, enabled = s.page > 0) {
                                Icon(Icons.Filled.ChevronLeft, contentDescription = "Προηγούμενη σελίδα")
                            }
                            Text("${s.page + 1}/${s.pageCount}", style = MaterialTheme.typography.labelLarge)
                            IconButton(onClick = { viewModel.setPage(s.page + 1) }, enabled = s.page < s.pageCount - 1) {
                                Icon(Icons.Filled.ChevronRight, contentDescription = "Επόμενη σελίδα")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSportPicker) {
        ModalBottomSheet(onDismissRequest = { showSportPicker = false }) {
            TeamPickerSheet(
                teamIds = (state as? MatchesUiState.Success)?.teamIds ?: emptyList(),
                onSelect = { teamId ->
                    viewModel.selectTeam(teamId)
                    showSportPicker = false
                },
            )
        }
    }
}

@Composable
private fun TeamPickerSheet(teamIds: List<String>, onSelect: (String?) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        ListItem(
            headlineContent = { Text("Όλα τα αθλήματα") },
            modifier = Modifier.fillMaxWidth().clickable { onSelect(null) },
        )
        teamIds.forEach { teamId ->
            ListItem(
                headlineContent = { Text(teamLabel(teamId)) },
                modifier = Modifier.fillMaxWidth().clickable { onSelect(teamId) },
            )
        }
    }
}

/** Human label for one of our own tracked Sofascore team ids — falls back to the raw
 *  id itself for a team id not in [OLYMPIACOS_TEAMS] (shouldn't normally happen, since
 *  matches are only ever fetched for ids from that same registry). */
private fun teamLabel(teamId: String): String = OLYMPIACOS_TEAMS.firstOrNull { it.id == teamId }?.label ?: teamId

/** "Όλοι / Εντός / Εκτός" — a single control with three joined segments, matching the
 *  Όλα/Νέα toggle's style on the feed screen. */
@Composable
private fun HomeAwayToggle(selected: HomeAwayFilter, onSelect: (HomeAwayFilter) -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        Modifier.height(32.dp).clip(shape).border(1.dp, MaterialTheme.colorScheme.outline, shape),
    ) {
        HomeAwaySegment("Όλοι", selected == HomeAwayFilter.ALL) { onSelect(HomeAwayFilter.ALL) }
        HomeAwaySegment("Εντός", selected == HomeAwayFilter.HOME) { onSelect(HomeAwayFilter.HOME) }
        HomeAwaySegment("Εκτός", selected == HomeAwayFilter.AWAY) { onSelect(HomeAwayFilter.AWAY) }
    }
}

@Composable
private fun RowScope.HomeAwaySegment(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxHeight().background(if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick).padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Month-grid view, closer to what Sofascore's own "Ημερολόγιο" tab looks like than
 *  building a full native replica would justify right now: a day cell shows the
 *  opponent's crest, a small sport icon, and an outlined-vs-filled background for
 *  home/away — tapping a day reveals that day's matches below in the same bordered
 *  card style as the list view, rather than trying to cram full match rows into a
 *  ~48dp cell. */
@Composable
private fun CalendarView(matches: List<Match>, onOpenMatch: (Match) -> Unit, modifier: Modifier = Modifier) {
    var monthCursor by remember { mutableStateOf(Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }) }
    var selectedDayKey by remember { mutableStateOf<String?>(dayKey(System.currentTimeMillis())) }

    val matchesByDay = remember(matches) { matches.groupBy { dayKey(it.kickoffAt) } }

    fun changeMonth(delta: Int) {
        monthCursor = (monthCursor.clone() as Calendar).apply { add(Calendar.MONTH, delta) }
        selectedDayKey = null
    }

    val density = LocalDensity.current
    var dragTotal by remember { mutableStateOf(0f) }
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .pointerInput(Unit) {
                val threshold = with(density) { 64.dp.toPx() }
                detectHorizontalDragGestures(
                    onDragStart = { dragTotal = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        dragTotal += dragAmount
                        change.consume()
                    },
                    onDragEnd = {
                        // Swipe left (negative drag) advances, like the → chevron.
                        if (abs(dragTotal) >= threshold) changeMonth(if (dragTotal < 0) 1 else -1)
                        dragTotal = 0f
                    },
                    onDragCancel = { dragTotal = 0f },
                )
            }
            .padding(horizontal = 16.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { changeMonth(-1) }) { Icon(Icons.Filled.ChevronLeft, contentDescription = "Προηγούμενος μήνας") }
                Text(
                    SimpleDateFormat("LLLL yyyy", Locale("el", "GR")).format(monthCursor.time)
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                )
                IconButton(onClick = { changeMonth(1) }) { Icon(Icons.Filled.ChevronRight, contentDescription = "Επόμενος μήνας") }
            }

            Row(Modifier.fillMaxWidth()) {
                listOf("Δε", "Τρ", "Τε", "Πε", "Πα", "Σα", "Κυ").forEach { label ->
                    Text(
                        label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            val today = Calendar.getInstance()
            val firstOfMonth = (monthCursor.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
            // Calendar.DAY_OF_WEEK is SUNDAY=1..SATURDAY=7; shifted so MONDAY lands at 0.
            val leadingBlanks = (firstOfMonth.get(Calendar.DAY_OF_WEEK) + 5) % 7
            val daysInMonth = firstOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
            // Trailing blanks too, not just leading ones — without them the last week's
            // row (often fewer than 7 real days) had each of its cells stretch to fill
            // the whole row width via weight(1f), instead of staying a normal 1/7-width
            // cell aligned under its actual weekday column.
            val trailingBlanks = (7 - (leadingBlanks + daysInMonth) % 7) % 7
            val cells = List(leadingBlanks) { null } + (1..daysInMonth).toList() + List(trailingBlanks) { null }

            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        Box(Modifier.weight(1f).padding(2.dp)) {
                            if (day == null) {
                                Box(Modifier.fillMaxWidth().height(52.dp))
                            } else {
                                val cellCal = (firstOfMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }
                                val key = dayKey(cellCal.timeInMillis)
                                val dayMatches = matchesByDay[key].orEmpty()
                                DayCell(
                                    day = day,
                                    isToday = cellCal.isSameDay(today),
                                    matches = dayMatches,
                                    selected = key == selectedDayKey,
                                    onClick = { if (dayMatches.isNotEmpty()) selectedDayKey = if (key == selectedDayKey) null else key },
                                )
                            }
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LegendSwatch(color = MaterialTheme.colorScheme.tertiaryContainer, label = "Γηπεδούχος")
                LegendSwatch(color = MaterialTheme.colorScheme.secondaryContainer, label = "Εκτός έδρας")
                LegendSwatch(color = null, label = "Σήμερα")
            }
        }

        val selectedMatches = selectedDayKey?.let { matchesByDay[it] }.orEmpty()
        if (selectedMatches.isNotEmpty()) {
            Box(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp)) {
                DateGroup(dateLabelFor(selectedMatches.first().kickoffAt), selectedMatches, onOpenMatch)
            }
        }
    }
}

/** Home and away both mark a match day the same *type* of way — a filled background,
 *  just a different color — instead of the previous border-for-home/fill-for-away mix,
 *  which read as two different kinds of thing rather than two values of the same one.
 *  "Today" is its own, separate signal (an outline) so it stays visible on a day with
 *  no match, and layers correctly under the "selected" state (a solid fill) rather
 *  than competing with the home/away color for the same visual channel. */
@Composable
private fun DayCell(day: Int, isToday: Boolean, matches: List<Match>, selected: Boolean, onClick: () -> Unit) {
    val hasMatch = matches.isNotEmpty()
    val primary = matches.firstOrNull()
    val isAway = primary?.isHome == false
    val shape = RoundedCornerShape(8.dp)
    val fillColor = when {
        selected -> MaterialTheme.colorScheme.primary
        isAway -> MaterialTheme.colorScheme.secondaryContainer
        hasMatch -> MaterialTheme.colorScheme.tertiaryContainer
        else -> null
    }
    val contentColor = when {
        selected -> MaterialTheme.colorScheme.onPrimary
        isAway -> MaterialTheme.colorScheme.onSecondaryContainer
        hasMatch -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Column(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .let { if (fillColor != null) it.background(fillColor) else it }
            .let { if (isToday && !selected) it.border(1.5.dp, MaterialTheme.colorScheme.primary, shape) else it }
            .let { if (hasMatch) it.clickable(onClick = onClick) else it }
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            day.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = contentColor,
        )
        if (primary != null) {
            AsyncImage(
                model = if (primary.isHome) primary.awayTeamLogoUrl else primary.homeTeamLogoUrl,
                contentDescription = null,
                modifier = Modifier.size(20.dp).padding(top = 1.dp),
            )
            Icon(
                sportIcon(primary.sport),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(10.dp),
            )
        }
    }
}

/** [color] null draws the "today" swatch — an outline only, matching how [DayCell]
 *  marks today when it isn't also selected. */
@Composable
private fun LegendSwatch(color: androidx.compose.ui.graphics.Color?, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .let {
                    if (color != null) it.background(color)
                    else it.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                },
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

private fun dayKey(millis: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))

/** Groups already-sorted matches into consecutive same-date runs, preserving order —
 *  a plain groupBy would merge non-adjacent runs of the same date together. */
private fun List<Match>.groupByConsecutiveDate(): List<Pair<String, List<Match>>> {
    val result = mutableListOf<Pair<String, MutableList<Match>>>()
    for (match in this) {
        val label = dateLabelFor(match.kickoffAt)
        val last = result.lastOrNull()
        if (last != null && last.first == label) last.second.add(match) else result.add(label to mutableListOf(match))
    }
    return result
}

private fun dateLabelFor(millis: Long): String {
    val target = Calendar.getInstance().apply { timeInMillis = millis }
    val today = Calendar.getInstance()
    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    return when {
        target.isSameDay(today) -> "Σήμερα"
        target.isSameDay(tomorrow) -> "Αύριο"
        else -> SimpleDateFormat("EEEE d MMMM", Locale("el", "GR")).format(Date(millis))
    }
}

private fun Calendar.isSameDay(other: Calendar) =
    get(Calendar.YEAR) == other.get(Calendar.YEAR) && get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)

/** A date's matches as one visually-grouped, outlined block — a plain pill header with
 *  borderless rows underneath didn't read as "these matches belong together", so each
 *  date run is now its own bordered card with a filled header strip. Σήμερα/Αύριο
 *  groups get the app's accent color instead of the neutral one, so a match coming up
 *  very soon stands out from the rest of the list at a glance. */
@Composable
private fun DateGroup(label: String, matches: List<Match>, onClick: (Match) -> Unit) {
    val isSoon = label == "Σήμερα" || label == "Αύριο"
    val borderColor = if (isSoon) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val headerColor = if (isSoon) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val headerTextColor = if (isSoon) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(if (isSoon) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Box(Modifier.fillMaxWidth().background(headerColor).padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = headerTextColor)
        }
        matches.forEachIndexed { index, match ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            MatchRow(match, onClick = { onClick(match) })
        }
    }
}

@Composable
private fun MatchRow(match: Match, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TeamColumn(match.homeTeam, match.homeTeamLogoUrl, Alignment.Start, Modifier.weight(1f))
        CenterColumn(match)
        TeamColumn(match.awayTeam, match.awayTeamLogoUrl, Alignment.End, Modifier.weight(1f))
    }
}

@Composable
private fun TeamColumn(name: String, logoUrl: String, alignment: Alignment.Horizontal, modifier: Modifier) {
    Column(modifier, horizontalAlignment = alignment) {
        AsyncImage(model = logoUrl, contentDescription = name, modifier = Modifier.size(34.dp))
        Text(
            name,
            style = MaterialTheme.typography.bodySmall,
            textAlign = if (alignment == Alignment.Start) TextAlign.Start else TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun CenterColumn(match: Match) {
    Column(
        Modifier.padding(horizontal = 6.dp).width(96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (match.status) {
            MatchStatus.LIVE -> {
                LiveBadge()
                Text(
                    "${match.homeScore ?: "-"} - ${match.awayScore ?: "-"}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            MatchStatus.FINISHED -> Text(
                "${match.homeScore ?: "-"} - ${match.awayScore ?: "-"}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            else -> Text(formatKickoff(match.kickoffAt), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        Icon(
            sportIcon(match.sport),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(14.dp).padding(top = 2.dp),
        )
        Text(
            match.competition,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun LiveBadge() {
    Box(
        Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 8.dp, vertical = 1.dp),
    ) {
        Text("LIVE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

private fun sportIcon(sport: String) = when (sport) {
    "basketball" -> Icons.Filled.SportsBasketball
    "volleyball" -> Icons.Filled.SportsVolleyball
    "handball" -> Icons.Filled.SportsHandball
    "waterpolo", "water_polo", "water-polo" -> Icons.Filled.Pool
    else -> Icons.Filled.SportsSoccer
}

private fun formatKickoff(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale("el", "GR")).format(Date(millis))
