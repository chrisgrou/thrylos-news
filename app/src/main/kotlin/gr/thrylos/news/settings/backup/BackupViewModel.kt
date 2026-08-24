package gr.thrylos.news.settings.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gr.thrylos.news.data.backup.BackupManager
import gr.thrylos.news.data.backup.RestoreResult
import gr.thrylos.news.data.repo.ArticleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager,
    private val articleRepository: ArticleRepository,
) : ViewModel() {

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

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
