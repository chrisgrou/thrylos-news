package gr.thrylos.news.matches

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsSoccer
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesScreen(
    onBack: () -> Unit,
    viewModel: MatchesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showSportPicker by remember { mutableStateOf(false) }

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
                    horizontalArrangement = Arrangement.Start,
                ) {
                    FilterChip(
                        selected = s.selectedSport != null,
                        onClick = { showSportPicker = true },
                        label = { Text(s.selectedSport?.let { sportName(it) } ?: "Όλα τα αθλήματα") },
                        trailingIcon = { Icon(Icons.Filled.ExpandMore, contentDescription = null) },
                    )
                }
                if (s.pageMatches.isEmpty()) {
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
            SportPickerSheet(
                sports = (state as? MatchesUiState.Success)?.sports ?: emptyList(),
                onSelect = { sport ->
                    viewModel.selectSport(sport)
                    showSportPicker = false
                },
            )
        }
    }
}

@Composable
private fun SportPickerSheet(sports: List<String>, onSelect: (String?) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        ListItem(
            headlineContent = { Text("Όλα τα αθλήματα") },
            modifier = Modifier.fillMaxWidth().clickable { onSelect(null) },
        )
        sports.forEach { sport ->
            ListItem(
                headlineContent = { Text(sportName(sport)) },
                modifier = Modifier.fillMaxWidth().clickable { onSelect(sport) },
            )
        }
    }
}

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
 *  date run is now its own bordered card with a filled header strip. */
@Composable
private fun DateGroup(label: String, matches: List<Match>, onClick: (Match) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

/** Human label for a sport, no gender — used standalone in the sport filter. */
private fun sportName(sport: String): String = when (sport) {
    "football" -> "Ποδόσφαιρο"
    "basketball" -> "Μπάσκετ"
    "volleyball" -> "Βόλεϊ"
    "handball" -> "Χάντμπολ"
    else -> sport.replaceFirstChar { it.uppercase() }
}

private fun sportIcon(sport: String) = when (sport) {
    "basketball" -> Icons.Filled.SportsBasketball
    else -> Icons.Filled.SportsSoccer
}

private fun formatKickoff(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale("el", "GR")).format(Date(millis))
