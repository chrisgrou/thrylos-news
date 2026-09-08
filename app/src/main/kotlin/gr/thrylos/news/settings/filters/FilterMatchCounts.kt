package gr.thrylos.news.settings.filters

import gr.thrylos.news.data.repo.ArticleRepository
import gr.thrylos.news.model.Article
import gr.thrylos.news.model.FilterRule
import gr.thrylos.news.sources.filter.FilterEngine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The "→ Ταιριάζει με N άρθρα" numbers on the Φίλτρα screen, computed at most once per
 * meaningful change.
 *
 * Producing them means matching every rule against every stored article, so it is by
 * far the most expensive thing that screen does. It used to hang off an observing query
 * inside the screen's ViewModel, which meant two things: the whole screen waited on it
 * before drawing a single rule, and because a ViewModel dies with its navigation entry,
 * leaving and re-entering the screen threw the result away and recomputed from scratch
 * every time.
 *
 * Being a @Singleton, this outlives the screen, so a return visit reuses the previous
 * answer whenever neither the rules nor the article corpus have actually changed —
 * checked with [ArticleRepository.corpusSignature], two aggregate queries, rather than
 * by reading the articles to find out.
 */
@Singleton
class FilterMatchCounts @Inject constructor(
    private val articleRepository: ArticleRepository,
) {
    private data class Snapshot(
        val rules: List<FilterRule>,
        val corpus: Pair<Int, Long>,
        val counts: Map<String, Int>,
    )

    private val mutex = Mutex()
    private var snapshot: Snapshot? = null

    /** Rule id → how many stored articles it matches. Suspends; call it off the main
     *  thread (it reads from the database and, on a miss, does the whole pass). */
    suspend fun countsFor(rules: List<FilterRule>): Map<String, Int> = mutex.withLock {
        val corpus = articleRepository.corpusSignature()
        snapshot?.let { if (it.rules == rules && it.corpus == corpus) return@withLock it.counts }

        val articles = loadCorpus(FilterEngine.rulesNeedBody(rules))
        val counts = FilterEngine.countMatchesBatch(rules, articles)
        snapshot = Snapshot(rules, corpus, counts)
        counts
    }

    /** The articles one rule matches right now, newest first — what the rule editor
     *  shows so a rule can be judged by what it actually catches rather than by a
     *  number alone. Deliberately not cached: it answers for a rule still being typed,
     *  and holding a whole corpus (bodies included, for a text rule) alive between
     *  visits is exactly the memory cost the list screens were freed of. */
    suspend fun articlesMatching(rule: FilterRule): List<Article> {
        val articles = loadCorpus(FilterEngine.rulesNeedBody(listOf(rule)))
        // Bypasses the enabled check that FilterEngine.matches applies: the editor
        // should show what the rule catches even while it's switched off.
        val probe = rule.copy(enabled = true)
        return articles.filter { FilterEngine.matches(probe, it) }
    }

    /** Only reads bodies when something actually inspects them — without a BODY/
     *  "Οπουδήποτε" condition the summaries carry every field the rest can match on. */
    private suspend fun loadCorpus(needsBody: Boolean): List<Article> =
        if (needsBody) articleRepository.getAllWithContentOnce() else articleRepository.getAllSummariesOnce()
}
