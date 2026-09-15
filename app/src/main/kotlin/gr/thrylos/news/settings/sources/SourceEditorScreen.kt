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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gr.thrylos.news.sources.filter.FilterEngine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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

            // Only for a kind="youtube" plugin — a structured control bolted onto an
            // otherwise free-text editor, for the one setting common enough to deserve
            // a toggle rather than hand-editing JSON. Reads/writes discovery.excludeShorts
            // straight out of the live text (best-effort — hidden if that text doesn't
            // currently parse) rather than tracking separate state, so it never drifts
            // from what the field actually holds, including someone editing it by hand.
            if (remember(json) { isYouTubePlugin(json) }) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Εμφάνιση Shorts", modifier = Modifier.weight(1f))
                    Switch(
                        checked = remember(json) { showShortsFrom(json) },
                        onCheckedChange = { viewModel.updateJson(withShowShorts(json, it)) },
                    )
                }
            }

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

private fun isYouTubePlugin(json: String): Boolean =
    runCatching { Json.parseToJsonElement(json).jsonObject["kind"]?.jsonPrimitive?.contentOrNull }.getOrNull() == "youtube"

private fun showShortsFrom(json: String): Boolean =
    !(runCatching { Json.parseToJsonElement(json).jsonObject["discovery"]?.jsonObject?.get("excludeShorts")?.jsonPrimitive?.booleanOrNull }
        .getOrNull() ?: false)

private val PRETTY_PLUGIN_JSON = Json { prettyPrint = true; prettyPrintIndent = "  " }

/** Flips discovery.excludeShorts in place and re-serializes — reformats the whole
 *  document (this is a structured edit, not a text patch), but only ever runs when
 *  the text already parses as valid JSON (the toggle is hidden otherwise), so that's
 *  always a no-op on content, just formatting. Falls back to leaving [json] untouched
 *  if the shape is ever not what's expected — the toggle shouldn't be able to corrupt
 *  a document it can't fully understand. */
private fun withShowShorts(json: String, showShorts: Boolean): String {
    val root = runCatching { Json.parseToJsonElement(json).jsonObject }.getOrNull() ?: return json
    val discovery = (root["discovery"] as? JsonObject) ?: return json
    val newDiscovery = JsonObject(discovery + ("excludeShorts" to JsonPrimitive(!showShorts)))
    val newRoot = JsonObject(root + ("discovery" to newDiscovery))
    return PRETTY_PLUGIN_JSON.encodeToString(JsonObject.serializer(), newRoot)
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
