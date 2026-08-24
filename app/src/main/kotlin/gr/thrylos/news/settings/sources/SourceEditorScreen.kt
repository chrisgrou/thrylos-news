package gr.thrylos.news.settings.sources

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gr.thrylos.news.sources.filter.FilterEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceEditorScreen(
    onBack: () -> Unit,
    viewModel: SourceEditorViewModel = hiltViewModel(),
) {
    val json by viewModel.jsonText.collectAsStateWithLifecycle()
    val errors by viewModel.saveErrors.collectAsStateWithLifecycle()
    val testState by viewModel.testState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isNew) "Νέα πηγή" else "Επεξεργασία πηγής") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Πίσω") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
        ) {
            Text(
                "Ορισμός πηγής (plugin JSON) — δες το docs/PLUGIN_FORMAT.md για όλα τα πεδία.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = json,
                onValueChange = viewModel::updateJson,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(360.dp),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
            )

            if (errors.isNotEmpty()) {
                Card(
                    Modifier.fillMaxWidth().padding(top = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        errors.forEach { Text(it, color = MaterialTheme.colorScheme.onErrorContainer) }
                    }
                }
            }

            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = viewModel::test, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Text("  Δοκιμή")
                }
                Button(onClick = { viewModel.save(onBack) }, modifier = Modifier.weight(1f)) {
                    Text("Αποθήκευση")
                }
            }

            TestResultCard(testState)
        }
    }
}

@Composable
private fun TestResultCard(state: TestState) {
    when (state) {
        is TestState.Idle -> {}
        is TestState.Loading -> Card(Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Row(Modifier.padding(16.dp)) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp))
                Text("Δοκιμή σε πραγματικό άρθρο…")
            }
        }
        is TestState.Error -> Card(
            Modifier.fillMaxWidth().padding(top = 14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        ) {
            Text(state.message, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
        }
        is TestState.Success -> Card(
            Modifier.fillMaxWidth().padding(top = 14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("✓ Επιτυχής εξαγωγή", style = MaterialTheme.typography.labelLarge)
                Text(state.article.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 6.dp))
                val wordCount = FilterEngine.bodyText(state.article).split(Regex("\\s+")).count { it.isNotBlank() }
                Text(
                    "${state.articleCount} άρθρα βρέθηκαν · ${wordCount} λέξεις · " +
                        (state.article.author?.let { "συντάκτης: $it" } ?: "χωρίς συντάκτη") +
                        if (state.article.usedFallbackExtraction) " · χρησιμοποιήθηκε Readability fallback" else "",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
