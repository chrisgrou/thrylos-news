package gr.thrylos.news.settings.sources

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gr.thrylos.news.data.repo.SourceRepository
import gr.thrylos.news.model.Article
import gr.thrylos.news.sources.plugin.PluginParseResult
import gr.thrylos.news.sources.plugin.PluginParser
import gr.thrylos.news.sources.sync.SourceSyncCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class TestState {
    data object Idle : TestState()
    data object Loading : TestState()
    data class Success(val article: Article, val articleCount: Int) : TestState()
    data class Error(val message: String) : TestState()
}

@HiltViewModel
class SourceEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sourceRepository: SourceRepository,
    private val coordinator: SourceSyncCoordinator,
) : ViewModel() {

    val sourceId: String? = savedStateHandle["sourceId"]
    val isNew: Boolean = sourceId.isNullOrBlank()

    private val _jsonText = MutableStateFlow(
        if (savedStateHandle.get<String>("kind") == "facebook") newFacebookPluginTemplate() else newPluginTemplate(),
    )
    val jsonText: StateFlow<String> = _jsonText.asStateFlow()

    private val _saveErrors = MutableStateFlow<List<String>>(emptyList())
    val saveErrors: StateFlow<List<String>> = _saveErrors.asStateFlow()

    private val _testState = MutableStateFlow<TestState>(TestState.Idle)
    val testState: StateFlow<TestState> = _testState.asStateFlow()

    init {
        if (!isNew) {
            viewModelScope.launch {
                sourceRepository.getById(sourceId!!)?.let { _jsonText.value = it.pluginJson }
            }
        }
    }

    fun updateJson(text: String) {
        _jsonText.value = text
        _saveErrors.value = emptyList()
    }

    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            when (val result = sourceRepository.importPlugin(_jsonText.value)) {
                is PluginParseResult.Success -> onSaved()
                is PluginParseResult.Failure -> _saveErrors.value = result.errors
            }
        }
    }

    fun test() {
        viewModelScope.launch {
            _testState.value = TestState.Loading
            _testState.value = withContext(Dispatchers.IO) {
                val parsed = PluginParser.parse(_jsonText.value)
                if (parsed !is PluginParseResult.Success) {
                    return@withContext TestState.Error((parsed as PluginParseResult.Failure).errors.joinToString("\n"))
                }
                runCatching {
                    val plugin = parsed.plugin
                    val stubs = coordinator.discoverNew(plugin, emptySet())
                    if (stubs.isEmpty()) return@runCatching TestState.Error("Το discovery.url δεν επέστρεψε άρθρα.")
                    val article = coordinator.extractArticle(plugin, stubs.first())
                    TestState.Success(article, stubs.size)
                }.getOrElse { e -> TestState.Error(e.message ?: "Άγνωστο σφάλμα δικτύου/εξαγωγής.") }
            }
        }
    }
}
