package gr.thrylos.news.settings.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gr.thrylos.news.data.prefs.AppPreferences
import gr.thrylos.news.data.sync.SyncScheduler
import gr.thrylos.news.data.widget.WidgetUpdater
import gr.thrylos.news.model.NotificationPrefs
import gr.thrylos.news.model.RefreshInterval
import gr.thrylos.news.model.SyncPrefs
import gr.thrylos.news.model.WidgetPrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncSettingsViewModel @Inject constructor(
    private val preferences: AppPreferences,
    private val scheduler: SyncScheduler,
    private val widgetUpdater: WidgetUpdater,
) : ViewModel() {

    val syncPrefs: StateFlow<SyncPrefs> = preferences.syncPrefs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyncPrefs())

    val notificationPrefs: StateFlow<NotificationPrefs> = preferences.notificationPrefs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotificationPrefs())

    val widgetPrefs: StateFlow<WidgetPrefs> = preferences.widgetPrefs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WidgetPrefs())

    fun updateSyncPrefs(update: (SyncPrefs) -> SyncPrefs) {
        viewModelScope.launch {
            val next = update(syncPrefs.value)
            preferences.updateSyncPrefs { next }
            scheduler.applySchedule(next.refreshInterval, next.syncOnlyOnWifi)
        }
    }

    fun updateNotificationPrefs(update: (NotificationPrefs) -> NotificationPrefs) {
        viewModelScope.launch { preferences.updateNotificationPrefs(update) }
    }

    fun updateWidgetPrefs(update: (WidgetPrefs) -> WidgetPrefs) {
        viewModelScope.launch {
            preferences.updateWidgetPrefs(update)
            widgetUpdater.requestUpdate()
        }
    }
}
