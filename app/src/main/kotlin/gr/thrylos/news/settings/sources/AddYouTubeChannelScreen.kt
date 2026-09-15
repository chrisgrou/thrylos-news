package gr.thrylos.news.settings.sources

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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

/** One text field ("κανάλι link ή @handle") standing in for the id/name-hunting a
 *  YouTube plugin would otherwise need by hand — see [gr.thrylos.news.sources.youtube.YouTubeChannelResolver].
 *  Never saves anything itself: a successful resolve hands off to the same JSON
 *  editor + "Δοκιμή" every other source goes through, pre-filled rather than blank. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddYouTubeChannelScreen(
    onBack: () -> Unit,
    onResolved: (name: String, channelId: String, showShorts: Boolean) -> Unit,
    onManual: (showShorts: Boolean) -> Unit,
    viewModel: AddYouTubeChannelViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    var showShorts by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Κανάλι YouTube") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Πίσω") } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Text(
                "Δώσε τον σύνδεσμο ή το @handle του καναλιού — π.χ. youtube.com/@REDSPORTS7 ή απλά REDSPORTS7.",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = input,
                onValueChange = { input = it; if (state !is ResolveState.Idle) viewModel.reset() },
                label = { Text("Σύνδεσμος ή @handle") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            )

            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Εμφάνιση Shorts", modifier = Modifier.weight(1f))
                Switch(checked = showShorts, onCheckedChange = { showShorts = it })
            }

            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { viewModel.resolve(input) },
                    enabled = input.isNotBlank() && state !is ResolveState.Loading,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Εύρεση καναλιού")
                }
            }

            ResolveResultCard(state = state, onAdd = { name, channelId -> onResolved(name, channelId, showShorts) })

            TextButton(onClick = { onManual(showShorts) }, modifier = Modifier.padding(top = 20.dp)) {
                Text("Χειροκίνητα (JSON) — αν δεν βρίσκεται αυτόματα")
            }
        }
    }
}

@Composable
private fun ResolveResultCard(state: ResolveState, onAdd: (name: String, channelId: String) -> Unit) {
    when (state) {
        is ResolveState.Idle -> {}
        is ResolveState.Loading -> Card(Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Row(Modifier.padding(16.dp)) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp))
                Text("Αναζήτηση καναλιού…")
            }
        }
        is ResolveState.Error -> Card(
            Modifier.fillMaxWidth().padding(top = 14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        ) {
            Text(state.message, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
        }
        is ResolveState.Success -> Card(
            Modifier.fillMaxWidth().padding(top = 14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("✓ Βρέθηκε κανάλι", style = MaterialTheme.typography.labelLarge)
                Text(state.info.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
                Text(
                    state.info.channelId,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Button(
                    onClick = { onAdd(state.info.name, state.info.channelId) },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) {
                    Text("Συνέχεια")
                }
            }
        }
    }
}
