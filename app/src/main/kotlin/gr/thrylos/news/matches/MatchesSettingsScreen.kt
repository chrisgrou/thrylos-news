package gr.thrylos.news.matches

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gr.thrylos.news.data.prefs.AppPreferences
import gr.thrylos.news.model.MatchesPrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class MatchesSettingsViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
) : ViewModel() {
    val prefs: StateFlow<MatchesPrefs> = appPreferences.matchesPrefs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MatchesPrefs())

    fun update(change: (MatchesPrefs) -> MatchesPrefs) {
        viewModelScope.launch { appPreferences.updateMatchesPrefs(change) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesSettingsScreen(
    onBack: () -> Unit,
    viewModel: MatchesSettingsViewModel = hiltViewModel(),
) {
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Πρόγραμμα αγώνων") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Πίσω") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        ) {
            Text(
                "Αθλήματα",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            )
            Text(
                "Ποιοι αγώνες εμφανίζονται στο κουμπί \"Πρόγραμμα αγώνων\" δίπλα στα Όλα/Νέα. Δεδομένα από το Sofascore.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            OLYMPIACOS_TEAMS.forEach { team ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(team.label, modifier = Modifier.padding(end = 12.dp))
                    Switch(
                        checked = team.id in prefs.enabledTeamIds,
                        onCheckedChange = { checked ->
                            viewModel.update { current ->
                                current.copy(
                                    enabledTeamIds = if (checked) current.enabledTeamIds + team.id else current.enabledTeamIds - team.id,
                                )
                            }
                        },
                    )
                }
            }

            Text(
                "Ανανέωση προγράμματος",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
            )
            Text(
                "Οι αγώνες αλλάζουν σπάνια — το πρόγραμμα ανανεώνεται αυτόματα μόνο κάθε τόσο. Υπάρχει πάντα κουμπί χειροκίνητης ανανέωσης στο ίδιο το πρόγραμμα.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(1 to "1ω", 3 to "3ω", 6 to "6ω", 12 to "12ω", 24 to "Ημερήσια").forEach { (hours, label) ->
                    FilterChip(
                        selected = prefs.refreshIntervalHours == hours,
                        onClick = { viewModel.update { it.copy(refreshIntervalHours = hours) } },
                        label = { Text(label) },
                    )
                }
            }

            Text(
                "Αγώνες ανά σελίδα",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
            )
            Text(
                "Πόσοι αγώνες χωράνε σε μια σελίδα του προγράμματος πριν χρειαστεί επόμενη — προσάρμοσέ το ώστε να χωράνε στην οθόνη σου χωρίς σκρολάρισμα.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = prefs.pageSize.toFloat(),
                    onValueChange = { newValue ->
                        viewModel.update { it.copy(pageSize = newValue.roundToInt()) }
                    },
                    valueRange = 5f..10f,
                    steps = 4,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    prefs.pageSize.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}
