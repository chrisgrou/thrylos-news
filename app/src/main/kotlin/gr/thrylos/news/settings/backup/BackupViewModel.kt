package gr.thrylos.news.settings.backup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import gr.thrylos.news.data.backup.BackupManager
import gr.thrylos.news.data.backup.RestoreResult
import gr.thrylos.news.data.repo.ArticleRepository
import gr.thrylos.news.data.repo.FilterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupManager: BackupManager,
    private val articleRepository: ArticleRepository,
    private val filterRepository: FilterRepository,
) : ViewModel() {

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    private val _bundledFiltersActive = MutableStateFlow(false)
    val bundledFiltersActive: StateFlow<Boolean> = _bundledFiltersActive.asStateFlow()

    init {
        viewModelScope.launch { _bundledFiltersActive.value = filterRepository.hasBundled() }
    }

    /** Bulk-imports (checked) or bulk-removes (unchecked) the filter rules shipped in
     *  assets/filters/bundled_filters.json — a curated starting set the user can pull
     *  in or discard as a group, without hand-recreating each rule. */
    fun setBundledFiltersActive(active: Boolean) {
        viewModelScope.launch {
            if (active) {
                val json = context.assets.open("filters/bundled_filters.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
                val count = filterRepository.importBundled(json)
                _status.value = if (count > 0) "Εισήχθησαν $count προτεινόμενα φίλτρα." else "Δεν υπάρχουν ακόμα προτεινόμενα φίλτρα σε αυτή την έκδοση."
            } else {
                val count = filterRepository.removeBundled()
                _status.value = "Αφαιρέθηκαν $count προτεινόμενα φίλτρα."
            }
            _bundledFiltersActive.value = filterRepository.hasBundled()
        }
    }

    fun export(write: suspend (String) -> Unit) {
        viewModelScope.launch {
            write(backupManager.export())
            _status.value = "Το αντίγραφο ασφαλείας εξήχθη."
        }
    }

    fun exportOpml(write: suspend (String) -> Unit) {
        viewModelScope.launch {
            write(backupManager.exportOpml())
            _status.value = "Το OPML εξήχθη."
        }
    }

    fun import(content: String) {
        viewModelScope.launch {
            _status.value = when (val result = backupManager.import(content)) {
                is RestoreResult.Success -> "Έγινε επαναφορά: ${result.sourcesImported} πηγές, ${result.filtersImported} φίλτρα, ${result.bookmarksImported} bookmarks."
                is RestoreResult.Failure -> "Σφάλμα: ${result.message}"
            }
        }
    }

    /** Deletes every non-bookmarked article so the next sync re-discovers and
     *  re-extracts everything from scratch — the only way to fix already-synced
     *  articles' data (e.g. a wrong published time from a since-fixed parsing bug)
     *  without waiting for their natural offline-retention expiry. */
    fun clearArticleHistory() {
        viewModelScope.launch {
            articleRepository.clearHistory()
            _status.value = "Το ιστορικό άρθρων εκκαθαρίστηκε."
        }
    }
}
