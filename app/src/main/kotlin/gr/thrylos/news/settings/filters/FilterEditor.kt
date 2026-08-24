package gr.thrylos.news.settings.filters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import gr.thrylos.news.model.FilterAction
import gr.thrylos.news.model.FilterCombinator
import gr.thrylos.news.model.FilterCondition
import gr.thrylos.news.model.FilterField
import gr.thrylos.news.model.FilterMatch
import gr.thrylos.news.model.FilterRule
import java.util.UUID

private data class ConditionDraft(
    var field: FilterField = FilterField.TITLE,
    var match: FilterMatch = FilterMatch.CONTAINS,
    var value: String = "",
)

@Composable
fun FilterEditor(onSave: (FilterRule) -> Unit) {
    val conditions = remember { mutableStateListOf(ConditionDraft()) }
    var combinator by remember { mutableStateOf(FilterCombinator.AND) }
    var action by remember { mutableStateOf(FilterAction.HIDE) }

    Column(Modifier.fillMaxWidth().padding(20.dp)) {
        Text("Ενέργεια", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.padding(top = 8.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = action == FilterAction.HIDE, onClick = { action = FilterAction.HIDE }, label = { Text("Απόκρυψη") })
            FilterChip(selected = action == FilterAction.IMPORTANT, onClick = { action = FilterAction.IMPORTANT }, label = { Text("Σημαντικό") })
            FilterChip(selected = action == FilterAction.HIGHLIGHT, onClick = { action = FilterAction.HIGHLIGHT }, label = { Text("Επισήμανση") })
        }

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
                    Text("Πεδίο", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)) {
                        items(listOf(FilterField.TITLE, FilterField.AUTHOR, FilterField.SOURCE, FilterField.BODY, FilterField.URL)) {
                            FilterChip(selected = draft.field == it, onClick = { conditions[index] = draft.copy(field = it) }, label = { Text(labelFor(it)) })
                        }
                    }
                    Text("Κανόνας", style = MaterialTheme.typography.labelSmall)
                    Row(Modifier.padding(top = 4.dp, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterMatch.entries.forEach {
                            FilterChip(selected = draft.match == it, onClick = { conditions[index] = draft.copy(match = it) }, label = { Text(labelFor(it)) })
                        }
                    }
                    OutlinedTextField(
                        value = draft.value,
                        onValueChange = { conditions[index] = draft.copy(value = it) },
                        label = { Text("Τιμή (π.χ. στοίχημα)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Button(onClick = { conditions.add(ConditionDraft()) }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
            Text("Προσθήκη συνθήκης (${if (combinator == FilterCombinator.AND) "ΚΑΙ" else "Ή"})")
        }

        HorizontalDivider(Modifier.padding(vertical = 16.dp))

        Button(
            onClick = {
                val validConditions = conditions.filter { it.value.isNotBlank() }
                    .map { FilterCondition(it.field, it.match, it.value) }
                if (validConditions.isNotEmpty()) {
                    onSave(
                        FilterRule(
                            id = UUID.randomUUID().toString(),
                            conditions = validConditions,
                            combinator = combinator,
                            action = action,
                        ),
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Προσθήκη κανόνα")
        }
    }
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
    FilterMatch.REGEX -> "regex"
    FilterMatch.EXACT -> "ακριβώς"
}
