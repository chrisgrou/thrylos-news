package gr.thrylos.news.matches

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import gr.thrylos.news.model.Match
import gr.thrylos.news.model.MatchStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesScreen(
    onBack: () -> Unit,
    viewModel: MatchesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.loadIfNeeded() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Πρόγραμμα αγώνων")
                        (state as? MatchesUiState.Success)?.let {
                            Text(
                                "Ενημερώθηκε: ${formatUpdatedAt(it.fetchedAt)}",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                },
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
                if (s.pageMatches.isEmpty()) {
                    Text("Δεν βρέθηκαν προσεχείς αγώνες.", modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 8.dp)) {
                        val groups = s.pageMatches.groupByConsecutiveDate()
                        groups.forEach { (dateLabel, matches) ->
                            item(key = "header-$dateLabel") { DateHeader(dateLabel) }
                            items(matches, key = { it.id }) { match ->
                                MatchRow(match, onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(match.matchUrl))) })
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

@Composable
private fun DateHeader(label: String) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Box(
            Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MatchRow(match: Match, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(
            sportLabel(match.sport, match.gender) + " · " + match.competition,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TeamColumn(match.homeTeam, match.homeTeamLogoUrl, Modifier.weight(1f))
            CenterColumn(match)
            TeamColumn(match.awayTeam, match.awayTeamLogoUrl, Modifier.weight(1f))
        }
    }
}

@Composable
private fun TeamColumn(name: String, logoUrl: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(model = logoUrl, contentDescription = name, modifier = Modifier.size(36.dp))
        Text(
            name,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun CenterColumn(match: Match) {
    Column(
        Modifier.padding(horizontal = 8.dp),
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
            Icons.Filled.SportsSoccer,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(14.dp).padding(top = 2.dp),
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

/** Human label for a sport+gender pair — the app currently only ships football, but
 *  this keeps each match self-describing as more sports get added later. */
private fun sportLabel(sport: String, gender: String): String {
    val sportName = when (sport) {
        "football" -> "Ποδόσφαιρο"
        "basketball" -> "Μπάσκετ"
        "volleyball" -> "Βόλεϊ"
        "handball" -> "Χάντμπολ"
        else -> sport.replaceFirstChar { it.uppercase() }
    }
    val genderLabel = when (gender) {
        "M" -> "Ανδρών"
        "F" -> "Γυναικών"
        else -> null
    }
    return if (genderLabel != null) "$sportName $genderLabel" else sportName
}

private fun formatKickoff(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale("el", "GR")).format(Date(millis))

private fun formatUpdatedAt(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale("el", "GR")).format(Date(millis))
