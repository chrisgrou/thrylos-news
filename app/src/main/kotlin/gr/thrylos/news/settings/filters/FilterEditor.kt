package gr.thrylos.news.settings.filters

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import gr.thrylos.news.model.FilterAction
import gr.thrylos.news.model.FilterField
import gr.thrylos.news.model.FilterMatch
import gr.thrylos.news.model.FilterRule
import java.util.UUID

@Composable
fun FilterEditor(onSave: (FilterRule) -> Unit) {
    var field by remember { mutableStateOf(FilterField.TITLE) }
    var match by remember { mutableStateOf(FilterMatch.CONTAINS) }
    var value by remember { mutableStateOf("") }

    Column(Modifier.fillMaxWidth().padding(20.dp)) {
        Text("Πεδίο")
        Row(Modifier.padding(top = 8.dp, bottom = 16.dp)) {
            listOf(FilterField.TITLE, FilterField.AUTHOR, FilterField.SOURCE, FilterField.BODY).forEach {
                FilterChip(
                    selected = field == it,
                    onClick = { field = it },
                    label = { Text(it.name) },
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
        }

        Text("Κανόνας")
        Row(Modifier.padding(top = 8.dp, bottom = 16.dp)) {
            FilterMatch.entries.forEach {
                FilterChip(
                    selected = match == it,
                    onClick = { match = it },
                    label = { Text(it.name) },
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
        }

        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            label = { Text("Τιμή (π.χ. στοίχημα)") },
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = {
                if (value.isNotBlank()) {
                    onSave(FilterRule(id = UUID.randomUUID().toString(), field = field, match = match, value = value, action = FilterAction.HIDE))
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Text("Προσθήκη κανόνα")
        }
    }
}
