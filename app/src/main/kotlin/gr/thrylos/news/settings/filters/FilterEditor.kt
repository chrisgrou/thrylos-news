package gr.thrylos.news.settings.filters

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import gr.thrylos.news.feed.stripSourceSuffix
import gr.thrylos.news.model.FilterAction
import gr.thrylos.news.model.FilterCombinator
import gr.thrylos.news.model.FilterCondition
import gr.thrylos.news.model.FilterField
import gr.thrylos.news.model.FilterMatch
import gr.thrylos.news.model.FilterRule
import java.util.UUID
import java.util.regex.Pattern

private data class ConditionDraft(
    var field: FilterField = FilterField.TITLE,
    var match: FilterMatch = FilterMatch.CONTAINS,
    var value: String = "",
    /** Only used when field == SOURCE: which of the known source names are checked. */
    var selectedSources: Set<String> = emptySet(),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterEditorScreen(
    onBack: () -> Unit,
    sources: List<String>,
    initial: FilterRule?,
    onSave: (FilterRule) -> Unit,
    onDelete: (FilterRule) -> Unit = {},
) {
    val conditions = remember {
        mutableStateListOf(
            *(initial?.conditions?.map { toDraft(it, sources) }?.toTypedArray() ?: arrayOf(ConditionDraft())),
        )
    }
    var combinator by remember { mutableStateOf(initial?.combinator ?: FilterCombinator.AND) }
    var action by remember { mutableStateOf(initial?.action ?: FilterAction.HIDE) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initial != null) "Επεξεργασία κανόνα" else "Νέος κανόνας") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Πίσω") } },
                actions = {
                    if (initial != null) {
                        IconButton(onClick = { onDelete(initial) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Διαγραφή κανόνα")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
        ) {
            Text("Ενέργεια", style = MaterialTheme.typography.titleSmall)
            FlowRow(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(FilterAction.HIDE, FilterAction.SHOW_ONLY, FilterAction.IMPORTANT).forEach {
                    FilterChip(selected = action == it, onClick = { action = it }, label = { Text(labelFor(it)) })
                }
            }
            Text(
                explanationFor(action),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            )

            conditions.forEachIndexed { index, draft ->
                if (index > 0) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.Center) {
                        FilterChip(selected = combinator == FilterCombinator.AND, onClick = { combinator = FilterCombinator.AND }, label = { Text("ΚΑΙ") }, modifier = Modifier.padding(end = 6.dp))
                        FilterChip(selected = combinator == FilterCombinator.OR, onClick = { combinator = FilterCombinator.OR }, label = { Text("Ή") })
                    }
                }
                Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Συνθήκη ${index + 1}", style = MaterialTheme.typography.labelMedium)
                            if (conditions.size > 1) {
                                IconButton(onClick = { conditions.removeAt(index) }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Αφαίρεση")
                                }
                            }
                        }
                        Text("Πεδίο", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp, bottom = 6.dp))
                        FieldPill(
                            selected = draft.field,
                            palette = fieldPalette(draft.field),
                            onSelect = { conditions[index] = draft.copy(field = it) },
                        )

                        if (draft.field == FilterField.SOURCE) {
                            Text("Πηγές (επιλογή πολλαπλών)", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 10.dp))
                            if (sources.isEmpty()) {
                                Text(
                                    "Δεν βρέθηκαν πηγές ακόμα.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            } else {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    sources.forEach { name ->
                                        val checked = name in draft.selectedSources
                                        FilterChip(
                                            selected = checked,
                                            onClick = {
                                                val next = if (checked) draft.selectedSources - name else draft.selectedSources + name
                                                conditions[index] = draft.copy(selectedSources = next)
                                            },
                                            label = { Text(stripSourceSuffix(name)) },
                                        )
                                    }
                                }
                            }
                        } else {
                            Text("Κανόνας & τιμή", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))
                            MatchValuePill(
                                match = draft.match,
                                value = draft.value,
                                palette = fieldPalette(draft.field),
                                onMatchChange = { conditions[index] = draft.copy(match = it) },
                                onValueChange = { conditions[index] = draft.copy(value = it) },
                            )
                        }
                    }
                }
            }

            OutlinedButton(onClick = { conditions.add(ConditionDraft()) }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                Text("Προσθήκη συνθήκης")
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            Button(
                onClick = {
                    val validConditions = conditions.mapNotNull { toCondition(it) }
                    if (validConditions.isNotEmpty()) {
                        onSave(
                            FilterRule(
                                id = initial?.id ?: UUID.randomUUID().toString(),
                                conditions = validConditions,
                                combinator = combinator,
                                action = action,
                                scopeSourceId = initial?.scopeSourceId,
                                enabled = initial?.enabled ?: true,
                            ),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (initial != null) "Αποθήκευση" else "Προσθήκη κανόνα")
            }
        }
    }
}

/** A single colored pill (matching the read-only rule list's badge style) that opens
 *  a dropdown of every [FilterField] when tapped. */
@Composable
private fun FieldPill(selected: FilterField, palette: FieldPalette, onSelect: (FilterField) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .background(palette.strong)
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                uppercaseNoAccents(labelFor(selected)),
                style = MaterialTheme.typography.labelMedium,
                color = palette.onStrong,
            )
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = palette.onStrong,
                modifier = Modifier.padding(start = 2.dp).size(18.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(FilterField.TITLE, FilterField.AUTHOR, FilterField.SOURCE, FilterField.BODY, FilterField.URL, FilterField.ANYWHERE).forEach { field ->
                DropdownMenuItem(text = { Text(labelFor(field)) }, onClick = { onSelect(field); expanded = false })
            }
        }
    }
}

/** A single bordered pill combining the match rule (dropdown segment) and the free-text
 *  value (inline text field segment) — the "[ ΚΑΝΟΝΑΣ | τιμή ]" concept, tinted by the
 *  condition's field so it visually pairs with the [FieldPill] above it. */
@Composable
private fun MatchValuePill(
    match: FilterMatch,
    value: String,
    palette: FieldPalette,
    onMatchChange: (FilterMatch) -> Unit,
    onValueChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(MaterialTheme.shapes.small)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small),
    ) {
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxHeight()
                    .background(palette.container)
                    .clickable { expanded = true }
                    .padding(horizontal = 10.dp),
            ) {
                Text(labelFor(match), style = MaterialTheme.typography.labelMedium, color = palette.onContainer)
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = palette.onContainer,
                    modifier = Modifier.padding(start = 2.dp).size(16.dp),
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                FilterMatch.entries.forEach { option ->
                    DropdownMenuItem(text = { Text(labelFor(option)) }, onClick = { onMatchChange(option); expanded = false })
                }
            }
        }
        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Box(Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 10.dp)) {
            if (value.isEmpty()) {
                Text(
                    "Τιμή (π.χ. στοίχημα)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun toCondition(draft: ConditionDraft): FilterCondition? {
    if (draft.field == FilterField.SOURCE) {
        return when (draft.selectedSources.size) {
            0 -> null
            1 -> FilterCondition(FilterField.SOURCE, FilterMatch.EXACT, draft.selectedSources.first())
            else -> FilterCondition(FilterField.SOURCE, FilterMatch.REGEX, sourceAlternationRegex(draft.selectedSources))
        }
    }
    return if (draft.value.isNotBlank()) FilterCondition(draft.field, draft.match, draft.value) else null
}

private fun sourceAlternationRegex(names: Set<String>) =
    "^(" + names.joinToString("|") { Pattern.quote(it) } + ")$"

/** Reconstructs the editor's draft state from a saved condition — a SOURCE condition
 *  is either a single EXACT name or our own generated alternation regex; anything
 *  else (e.g. hand-written regex/CONTAINS) falls back to the free-text form. */
private fun toDraft(condition: FilterCondition, sources: List<String>): ConditionDraft {
    if (condition.field == FilterField.SOURCE) {
        val selected = when (condition.match) {
            FilterMatch.EXACT -> setOf(condition.value)
            FilterMatch.REGEX -> {
                val inner = condition.value.removePrefix("^(").removeSuffix(")$")
                inner.split("|")
                    .map { it.removePrefix("\\Q").removeSuffix("\\E") }
                    .filter { it in sources }
                    .toSet()
            }
            else -> emptySet()
        }
        return ConditionDraft(field = FilterField.SOURCE, match = condition.match, value = condition.value, selectedSources = selected)
    }
    return ConditionDraft(field = condition.field, match = condition.match, value = condition.value)
}

private fun labelFor(field: FilterField) = when (field) {
    FilterField.TITLE -> "Τίτλος"
    FilterField.BODY -> "Κείμενο"
    FilterField.AUTHOR -> "Συντάκτης"
    FilterField.URL -> "URL"
    FilterField.SOURCE -> "Πηγή"
    FilterField.ANYWHERE -> "Οπουδήποτε"
}

private fun labelFor(match: FilterMatch) = when (match) {
    FilterMatch.CONTAINS -> "περιέχει"
    FilterMatch.NOT_CONTAINS -> "δεν περιέχει"
    FilterMatch.REGEX -> "regex"
    FilterMatch.EXACT -> "ακριβώς"
}

private fun labelFor(action: FilterAction) = when (action) {
    FilterAction.HIDE -> "Απόκρυψη"
    FilterAction.SHOW_ONLY -> "Εμφάνιση"
    FilterAction.IMPORTANT -> "Σημαντικό"
    FilterAction.HIGHLIGHT -> "Επισήμανση"
}

private fun explanationFor(action: FilterAction) = when (action) {
    FilterAction.HIDE -> "Κρύβει τελείως τα άρθρα που ταιριάζουν με τον κανόνα — δεν εμφανίζονται πουθενά στη ροή."
    FilterAction.SHOW_ONLY -> "Λειτουργία εστίασης: αφήνει ορατά ΜΟΝΟ τα άρθρα που ταιριάζουν, και κρύβει όλα τα υπόλοιπα."
    FilterAction.IMPORTANT -> "Μαρκάρει τα άρθρα που ταιριάζουν ως σημαντικά, ώστε να ξεχωρίζουν και να εμφανίζονται πρώτα στη ροή."
    FilterAction.HIGHLIGHT -> "Επισημαίνει οπτικά τα άρθρα που ταιριάζουν στη ροή, χωρίς να τα κρύβει ή να τα προτεραιοποιεί."
}
