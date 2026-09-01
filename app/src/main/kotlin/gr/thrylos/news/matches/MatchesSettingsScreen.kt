package gr.thrylos.news.matches

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
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
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Ποδόσφαιρο", modifier = Modifier.padding(end = 12.dp))
                Switch(
                    checked = prefs.football,
                    onCheckedChange = { checked -> viewModel.update { it.copy(football = checked) } },
                )
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
        }
    }
}
