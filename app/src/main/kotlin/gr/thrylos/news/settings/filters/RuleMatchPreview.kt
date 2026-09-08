package gr.thrylos.news.settings.filters

import gr.thrylos.news.data.repo.ArticleRepository
import gr.thrylos.news.model.Article
import gr.thrylos.news.model.FilterRule
import gr.thrylos.news.sources.filter.FilterEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** The articles the rule currently open in the editor matches. */
data class RuleMatches(val rule: FilterRule, val articles: List<Article>)

/**
 * Shared between the rule editor (which shows the count on its toolbar badge) and the
 * screen listing those articles, so opening that list doesn't re-run the match pass the
 * editor just ran.
 *
 * A @Singleton rather than something the editor owns because Navigation Compose disposes
 * a destination's composable as soon as another is pushed on top — the editor's UI is
 * gone the moment its own list opens. Its ViewModel isn't, so that is what calls [clear]
 * (from onCleared), which fires when the editor is actually popped.
 */
@Singleton
class RuleMatchPreview @Inject constructor(
    private val articleRepository: ArticleRepository,
) {
    private val _matches = MutableStateFlow<RuleMatches?>(null)

    /** null before the first pass for the current draft has finished. */
    val matches: StateFlow<RuleMatches?> = _matches.asStateFlow()

    /** Recomputes for [rule], or clears when it's null (an incomplete draft). Suspends;
     *  the caller debounces, so a burst of keystrokes costs one pass. */
    suspend fun update(rule: FilterRule?) {
        if (rule == null) {
            _matches.value = null
            return
        }
        // Bodies are only read when a condition actually inspects them — without a
        // Κείμενο/Οπουδήποτε condition the summaries carry every field the rest match on.
        val needsBody = FilterEngine.rulesNeedBody(listOf(rule))
        val corpus = if (needsBody) articleRepository.getAllWithContentOnce() else articleRepository.getAllSummariesOnce()
        // Bypasses the enabled check FilterEngine.matches applies: the editor should
        // show what a rule catches even while it's switched off.
        val probe = rule.copy(enabled = true)
        val matched = corpus.filter { FilterEngine.matches(probe, it) }
            // Drops each article's body before publishing. For a Κείμενο rule the corpus
            // was loaded with bodies attached, and holding those alive for as long as the
            // editor stays open is exactly the retention the list screens were freed of —
            // nothing downstream renders body text, and the reader reloads by id anyway.
            .map { if (needsBody) it.copy(content = emptyList()) else it }
        _matches.value = RuleMatches(rule, matched)
    }

    fun clear() {
        _matches.value = null
    }
}
