package gr.thrylos.news.settings.filters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import gr.thrylos.news.model.FilterAction
import gr.thrylos.news.model.FilterCombinator
import gr.thrylos.news.model.FilterCondition
import gr.thrylos.news.model.FilterField
import gr.thrylos.news.model.FilterMatch
import gr.thrylos.news.model.FilterRule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersScreen(
    onBack: () -> Unit,
    viewModel: FiltersViewModel = hiltViewModel(),
) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val sourceNames by viewModel.sourceNames.collectAsStateWithLifecycle()
    var showEditor by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<FilterRule?>(null) }

    if (showEditor) {
        FilterEditorScreen(
            onBack = { showEditor = false },
            sources = sourceNames,
            initial = editingRule,
            onSave = { rule -> viewModel.save(rule); showEditor = false },
            onDelete = { rule -> viewModel.delete(rule); showEditor = false },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Φίλτρα") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Πίσω") } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editingRule = null; showEditor = true }) { Icon(Icons.Filled.Add, contentDescription = "Νέος κανόνας") }
        },
    ) { padding ->
        if (rows.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text("Δεν έχεις κανόνες φίλτρων ακόμα. Πρόσθεσε έναν για να κρύβεις άρθρα με συγκεκριμένες λέξεις-κλειδιά, συντάκτη ή πηγή.")
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
                items(rows, key = { it.rule.id }) { row ->
                    Card(
                        onClick = { editingRule = row.rule; showEditor = true },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    ) {
                        Row(
                            Modifier.padding(18.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(describe(row.rule), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "→ ${actionVerb(row.rule.action)} ${row.hiddenCount} άρθρα αυτή τη στιγμή" + (row.rule.scopeSourceId?.let { " · μόνο στην πηγή $it" } ?: ""),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                            Switch(
                                checked = row.rule.enabled,
                                onCheckedChange = { checked -> viewModel.setEnabled(row.rule, checked) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun describe(rule: FilterRule): String {
    val joiner = if (rule.combinator == FilterCombinator.AND) " ΚΑΙ " else " Ή "
    return rule.conditions.joinToString(joiner) { describe(it) }
}

private fun describe(condition: FilterCondition) =
    "${labelFor(condition.field)} ${labelFor(condition.match)} \"${condition.value}\""

private fun actionVerb(action: FilterAction) = when (action) {
    FilterAction.HIDE -> "κρύβει"
    FilterAction.SHOW_ONLY -> "αφήνει ορατά μόνο"
    FilterAction.IMPORTANT -> "μαρκάρει ως σημαντικά"
    FilterAction.HIGHLIGHT -> "επισημαίνει"
}

private fun labelFor(field: FilterField) = when (field) {
    FilterField.TITLE -> "Τίτλος"
    FilterField.BODY -> "Κείμενο"
    FilterField.AUTHOR -> "Συντάκτης"
    FilterField.URL -> "URL"
    FilterField.SOURCE -> "Πηγή"
}

private fun labelFor(match: FilterMatch) = when (match) {
    FilterMatch.CONTAINS -> "περιέχει"
    FilterMatch.NOT_CONTAINS -> "δεν περιέχει"
    FilterMatch.REGEX -> "ταιριάζει με regex"
    FilterMatch.EXACT -> "είναι ακριβώς"
}
