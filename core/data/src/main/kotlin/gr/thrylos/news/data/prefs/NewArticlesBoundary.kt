package gr.thrylos.news.data.prefs

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Fixed-per-process-lifetime boundary for "new since I last opened the app": null
 *  until [initializeOnce] runs (so nothing is highlighted before it's known), then
 *  the timestamp of the previous app open for the rest of this process's lifetime. */
@Singleton
class NewArticlesBoundary @Inject constructor(
    private val appPreferences: AppPreferences,
) {
    private val _threshold = MutableStateFlow<Long?>(null)
    val threshold: StateFlow<Long?> = _threshold

    suspend fun initializeOnce() {
        if (_threshold.value != null) return
        _threshold.value = appPreferences.consumeAndAdvanceLastOpenedAt()
    }
}
