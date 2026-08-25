package gr.thrylos.news.settings.filters

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gr.thrylos.news.feed.stripSourceSuffix
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
    var selectedTab by remember { mutableStateOf(0) }

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
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Fixed category order (matching how the user asked for them, not the enum's
            // declaration order); HIGHLIGHT has no editor entry anymore but still gets a
            // tab if any legacy rule of that type exists.
            val tabOrder = listOf(FilterAction.HIDE, FilterAction.SHOW_ONLY, FilterAction.IMPORTANT) +
                (if (rows.any { it.rule.action == FilterAction.HIGHLIGHT }) listOf(FilterAction.HIGHLIGHT) else emptyList())
            val clampedTab = selectedTab.coerceIn(0, tabOrder.lastIndex)

            TabRow(selectedTabIndex = clampedTab) {
                tabOrder.forEachIndexed { index, action ->
                    Tab(
                        selected = clampedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(sectionTitleFor(action)) },
                    )
                }
            }

            val tabRows = rows.filter { it.rule.action == tabOrder[clampedTab] }

            if (tabRows.isEmpty()) {
                Column(Modifier.fillMaxSize().padding(24.dp)) {
                    Text("Δεν υπάρχουν κανόνες σε αυτή την κατηγορία ακόμα.")
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
                    items(tabRows, key = { it.rule.id }) { row ->
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
                                    RuleDescription(row.rule)
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RuleDescription(rule: FilterRule) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        rule.conditions.forEachIndexed { index, condition ->
            if (index > 0) {
                Text(
                    if (rule.combinator == FilterCombinator.AND) "ΚΑΙ" else "Ή",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, end = 2.dp),
                )
            }
            ConditionBadge(condition)
        }
    }
}

@Composable
private fun ConditionBadge(condition: FilterCondition) {
    val (strongColor, onStrongColor, containerColor) = fieldColors(condition.field)
    val displayValue = if (condition.field == FilterField.SOURCE && condition.match == FilterMatch.EXACT) {
        stripSourceSuffix(condition.value)
    } else {
        condition.value
    }
    val strike = condition.match == FilterMatch.NOT_CONTAINS
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .clip(MaterialTheme.shapes.small)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small),
    ) {
        Text(
            uppercaseNoAccents(labelFor(condition.field)),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = onStrongColor,
            modifier = Modifier
                .background(strongColor)
                .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        )
        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            displayValue,
            style = MaterialTheme.typography.labelSmall.copy(
                textDecoration = if (strike) TextDecoration.LineThrough else TextDecoration.None,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .background(containerColor)
                .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        )
    }
}

private val GREEK_ACCENTS = mapOf(
    'ά' to 'α', 'έ' to 'ε', 'ή' to 'η', 'ί' to 'ι', 'ό' to 'ο', 'ύ' to 'υ', 'ώ' to 'ω',
    'ϊ' to 'ι', 'ΐ' to 'ι', 'ϋ' to 'υ', 'ΰ' to 'υ',
)

/** Greek typographic convention: monotonic accents are dropped when a word is
 *  written in all caps (e.g. "τίτλος" → "ΤΙΤΛΟΣ", not "ΤΊΤΛΟΣ"). */
private fun uppercaseNoAccents(text: String): String =
    text.map { GREEK_ACCENTS[it] ?: it }.joinToString("").uppercase()

/** Distinct badge colors per field so a rule's category is recognizable at a glance:
 *  a strong (label) half with high-contrast text, and a lighter (value) half. */
@Composable
private fun fieldColors(field: FilterField): Triple<Color, Color, Color> = when (field) {
    FilterField.TITLE -> Triple(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary, MaterialTheme.colorScheme.primaryContainer)
    FilterField.BODY -> Triple(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary, MaterialTheme.colorScheme.secondaryContainer)
    FilterField.AUTHOR -> Triple(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiary, MaterialTheme.colorScheme.tertiaryContainer)
    FilterField.URL -> Triple(MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant)
    FilterField.SOURCE -> Triple(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.onError, MaterialTheme.colorScheme.errorContainer)
}

private fun sectionTitleFor(action: FilterAction) = when (action) {
    FilterAction.HIDE -> "Απόκρυψη"
    FilterAction.SHOW_ONLY -> "Εμφάνιση"
    FilterAction.IMPORTANT -> "Σημαντικό"
    FilterAction.HIGHLIGHT -> "Επισήμανση"
}

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
