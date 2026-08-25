package gr.thrylos.news.settings.sync

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gr.thrylos.news.model.RefreshInterval

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncSettingsScreen(
    onBack: () -> Unit,
    viewModel: SyncSettingsViewModel = hiltViewModel(),
) {
    val sync by viewModel.syncPrefs.collectAsStateWithLifecycle()
    val notifications by viewModel.notificationPrefs.collectAsStateWithLifecycle()
    var editingQuietStart by remember { mutableStateOf(false) }
    var editingQuietEnd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ανανέωση & Ειδοποιήσεις") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Πίσω") } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            SectionTitle("Διάστημα αυτόματης ανανέωσης")
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                RefreshInterval.entries.forEach { interval ->
                    FilterChip(
                        selected = sync.refreshInterval == interval,
                        onClick = { viewModel.updateSyncPrefs { it.copy(refreshInterval = interval) } },
                        label = { Text(labelFor(interval)) },
                    )
                }
            }
            val refreshMinutes = sync.refreshInterval.minutes
            if (refreshMinutes != null && refreshMinutes < 15) {
                Text(
                    "Διαστήματα κάτω των 15′ χρησιμοποιούν συναγερμό συστήματος αντί για το WorkManager· το Android μπορεί να καθυστερήσει την ανανέωση όταν η συσκευή είναι σε αδράνεια.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            } else {
                androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 8.dp))
            }

            SettingSwitchRow("Επισήμανση νέων άρθρων", sync.highlightNewSinceRefresh) {
                viewModel.updateSyncPrefs { p -> p.copy(highlightNewSinceRefresh = it) }
            }
            SettingSwitchRow("Ανανέωση μόνο σε Wi-Fi", sync.syncOnlyOnWifi) {
                viewModel.updateSyncPrefs { p -> p.copy(syncOnlyOnWifi = it) }
            }
            SettingSwitchRow("Κατέβασμα εικόνων μόνο σε Wi-Fi", sync.downloadImagesOnlyOnWifi) {
                viewModel.updateSyncPrefs { p -> p.copy(downloadImagesOnlyOnWifi = it) }
            }
            SettingSwitchRow("Προφόρτωση εικόνων για offline ανάγνωση", sync.prefetchImagesForOffline) {
                viewModel.updateSyncPrefs { p -> p.copy(prefetchImagesForOffline = it) }
            }

            SectionTitle("Ώρες κοινής ησυχίας")
            SettingSwitchRow("Παύση ανανέωσης & ειδοποιήσεων", sync.quietHoursEnabled) {
                viewModel.updateSyncPrefs { p -> p.copy(quietHoursEnabled = it) }
            }
            if (sync.quietHoursEnabled) {
                Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(selected = false, onClick = { editingQuietStart = true }, label = { Text("Από " + formatMinuteOfDay(sync.quietHoursStartMinute)) })
                    FilterChip(selected = false, onClick = { editingQuietEnd = true }, label = { Text("Έως " + formatMinuteOfDay(sync.quietHoursEndMinute)) })
                }
            }

            SectionTitle("Ειδοποιήσεις")
            SettingSwitchRow("Ενεργές ειδοποιήσεις", notifications.enabled) {
                viewModel.updateNotificationPrefs { p -> p.copy(enabled = it) }
            }
            SettingSwitchRow("Μόνο για σημαντικά άρθρα", notifications.onlyImportant) {
                viewModel.updateNotificationPrefs { p -> p.copy(onlyImportant = it) }
            }
            Text(
                "Ειδοποίηση μόνο για άρθρα που πιάνει κάποιος κανόνας φίλτρου με ενέργεια \"Σημαντικό\" — τα υπόλοιπα συγχρονίζονται κανονικά αλλά χωρίς ειδοποίηση.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            SettingSwitchRow("Ομαδοποίηση σε μία ειδοποίηση", notifications.groupIntoSummary) {
                viewModel.updateNotificationPrefs { p -> p.copy(groupIntoSummary = it) }
            }

            SectionTitle("Offline αποθήκευση")
            Text(
                "Διατήρηση άρθρων για ${sync.offlineRetentionDays} ημέρες, έως ${sync.offlineMaxArticles} άρθρα. Τα bookmarks δεν διαγράφονται ποτέ αυτόματα.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }

    if (editingQuietStart) {
        TimePickerDialog(
            initialMinuteOfDay = sync.quietHoursStartMinute,
            onDismiss = { editingQuietStart = false },
            onConfirm = { minute -> viewModel.updateSyncPrefs { it.copy(quietHoursStartMinute = minute) }; editingQuietStart = false },
        )
    }
    if (editingQuietEnd) {
        TimePickerDialog(
            initialMinuteOfDay = sync.quietHoursEndMinute,
            onDismiss = { editingQuietEnd = false },
            onConfirm = { minute -> viewModel.updateSyncPrefs { it.copy(quietHoursEndMinute = minute) }; editingQuietEnd = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(initialMinuteOfDay: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val state = rememberTimePickerState(
        initialHour = initialMinuteOfDay / 60,
        initialMinute = initialMinuteOfDay % 60,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Άκυρο") } },
        text = { TimePicker(state = state) },
    )
}

private fun formatMinuteOfDay(minute: Int) = "%02d:%02d".format(minute / 60, minute % 60)

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
}

@Composable
private fun SettingSwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, modifier = Modifier.padding(end = 12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun labelFor(interval: RefreshInterval) = when (interval) {
    RefreshInterval.NEVER -> "Ποτέ"
    RefreshInterval.MIN_1 -> "1′"
    RefreshInterval.MIN_5 -> "5′"
    RefreshInterval.MIN_10 -> "10′"
    RefreshInterval.MIN_15 -> "15′"
    RefreshInterval.MIN_30 -> "30′"
    RefreshInterval.HOUR_1 -> "1ω"
    RefreshInterval.HOUR_3 -> "3ω"
    RefreshInterval.HOUR_6 -> "6ω"
    RefreshInterval.HOUR_12 -> "12ω"
    RefreshInterval.DAILY -> "Ημερήσια"
}
