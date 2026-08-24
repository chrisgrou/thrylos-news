package gr.thrylos.news.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gr.thrylos.news.data.prefs.AppPreferences
import gr.thrylos.news.model.AppThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppThemeViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
) : ViewModel() {

    val mode: StateFlow<AppThemeMode> = appPreferences.appThemeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppThemeMode.SYSTEM)

    fun setMode(mode: AppThemeMode) {
        viewModelScope.launch { appPreferences.setAppThemeMode(mode) }
    }
}
