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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gr.thrylos.news.model.Match
import gr.thrylos.news.model.MatchStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Content for the "Πρόγραμμα αγώνων" bottom sheet opened from the feed's filter bar. */
@Composable
fun MatchesOverlay(viewModel: MatchesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.loadIfNeeded() }

    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Πρόγραμμα αγώνων", style = MaterialTheme.typography.titleMedium)
                (state as? MatchesUiState.Success)?.let {
                    Text(
                        "Ενημερώθηκε: ${formatUpdatedAt(it.fetchedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (state is MatchesUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = { viewModel.refresh(force = true) }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Ανανέωση προγράμματος")
                }
            }
        }
        when (val s = state) {
            is MatchesUiState.Loading -> Box(
                Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is MatchesUiState.SportsDisabled -> Text(
                "Δεν έχεις ενεργοποιήσει κανένα άθλημα. Άνοιξε Ρυθμίσεις → Πρόγραμμα αγώνων.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
            )

            is MatchesUiState.Error -> Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Αποτυχία φόρτωσης: ${s.message}", color = MaterialTheme.colorScheme.error)
                Button(onClick = { viewModel.refresh(force = true) }, modifier = Modifier.padding(top = 12.dp)) { Text("Ξανά") }
            }

            is MatchesUiState.Success -> if (s.matches.isEmpty()) {
                Text(
                    "Δεν βρέθηκαν προσεχείς αγώνες.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 8.dp)) {
                    items(s.matches, key = { it.id }) { match ->
                        MatchRow(match, onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(match.matchUrl))) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchRow(match: Match, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(match.competition, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${match.homeTeam} — ${match.awayTeam}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
            when (match.status) {
                MatchStatus.LIVE -> LiveBadge()
                MatchStatus.FINISHED -> Text(
                    "${match.homeScore ?: "-"} - ${match.awayScore ?: "-"}",
                    style = MaterialTheme.typography.titleSmall,
                )
                else -> Text(formatKickoff(match.kickoffAt), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun LiveBadge() {
    Box(
        Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text("LIVE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

private fun formatKickoff(millis: Long): String =
    SimpleDateFormat("EEE d/M, HH:mm", Locale("el", "GR")).format(Date(millis))

private fun formatUpdatedAt(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale("el", "GR")).format(Date(millis))
