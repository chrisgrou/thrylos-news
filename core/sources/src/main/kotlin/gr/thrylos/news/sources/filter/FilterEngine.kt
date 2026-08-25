package gr.thrylos.news.sources.filter

import gr.thrylos.news.model.Article
import gr.thrylos.news.model.ContentBlock
import gr.thrylos.news.model.FilterAction
import gr.thrylos.news.model.FilterCombinator
import gr.thrylos.news.model.FilterCondition
import gr.thrylos.news.model.FilterField
import gr.thrylos.news.model.FilterMatch
import gr.thrylos.news.model.FilterRule
import java.text.Normalizer

object FilterEngine {

    fun bodyText(article: Article): String = article.content.joinToString(" ") { block ->
        when (block) {
            is ContentBlock.Paragraph -> block.text
            is ContentBlock.Heading -> block.text
            is ContentBlock.Quote -> block.text
            is ContentBlock.ListBlock -> block.items.joinToString(" ")
            is ContentBlock.Image -> block.caption.orEmpty()
            is ContentBlock.Video -> block.caption.orEmpty()
        }
    }

    fun matches(rule: FilterRule, article: Article): Boolean {
        if (!rule.enabled) return false
        if (rule.scopeSourceId != null && rule.scopeSourceId != article.sourceId) return false
        if (rule.conditions.isEmpty()) return false

        val results = rule.conditions.map { condition -> matchesCondition(condition, article) }
        return combine(rule.combinator, results)
    }

    private fun matchesCondition(condition: FilterCondition, article: Article): Boolean = when (condition.field) {
        FilterField.TITLE -> matchValue(article.title, condition)
        FilterField.BODY -> matchValue(bodyText(article), condition)
        FilterField.AUTHOR -> matchValue(article.author.orEmpty(), condition)
        FilterField.URL -> matchValue(article.url, condition)
        FilterField.SOURCE -> matchValue(article.sourceName, condition)
        // "Οπουδήποτε": true if any field matches — except NOT_CONTAINS, where the
        // useful meaning is "the value appears in none of them", i.e. every field's
        // (already-negated) per-field result must hold, not just one.
        FilterField.ANYWHERE -> {
            val perField = listOf(article.title, bodyText(article), article.author.orEmpty(), article.url, article.sourceName)
                .map { matchValue(it, condition) }
            if (condition.match == FilterMatch.NOT_CONTAINS) perField.all { it } else perField.any { it }
        }
    }

    /**
     * Pre-fetch check used during sync to skip downloading an article body entirely
     * when a title/source/url rule already hides it. Only used as an optimization —
     * a rule that can't be fully evaluated yet (any condition needs AUTHOR/BODY) is
     * simply not applied early; [isHidden] after extraction is the source of truth.
     */
    fun matchesStub(rule: FilterRule, title: String, sourceId: String, sourceName: String, url: String): Boolean {
        if (!rule.enabled) return false
        if (rule.scopeSourceId != null && rule.scopeSourceId != sourceId) return false
        if (rule.conditions.isEmpty()) return false
        if (rule.conditions.any { it.field == FilterField.AUTHOR || it.field == FilterField.BODY || it.field == FilterField.ANYWHERE }) return false

        val results = rule.conditions.map { condition ->
            val haystack = when (condition.field) {
                FilterField.TITLE -> title
                FilterField.SOURCE -> sourceName
                FilterField.URL -> url
                FilterField.AUTHOR, FilterField.BODY, FilterField.ANYWHERE -> return false
            }
            matchValue(haystack, condition)
        }
        return combine(rule.combinator, results)
    }

    fun isHiddenStub(title: String, sourceId: String, sourceName: String, url: String, rules: List<FilterRule>): Boolean =
        rules.any { it.action == FilterAction.HIDE && matchesStub(it, title, sourceId, sourceName, url) }

    private fun combine(combinator: FilterCombinator, results: List<Boolean>): Boolean = when (combinator) {
        FilterCombinator.AND -> results.all { it }
        FilterCombinator.OR -> results.any { it }
    }

    /** Scraped Greek text can arrive as precomposed or decomposed Unicode (e.g. "ά" as
     *  one codepoint vs. "α" + a combining accent) depending on the source site, which
     *  makes String.contains/equals silently fail even though the text looks identical
     *  on screen. Normalizing both sides to NFC before comparing avoids that. */
    private fun nfc(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFC)

    private fun matchValue(haystack: String, condition: FilterCondition): Boolean {
        val normalizedHaystack = nfc(haystack)
        val normalizedValue = nfc(condition.value)
        return when (condition.match) {
            FilterMatch.CONTAINS -> {
                val h = if (condition.caseSensitive) normalizedHaystack else normalizedHaystack.lowercase()
                val v = if (condition.caseSensitive) normalizedValue else normalizedValue.lowercase()
                h.contains(v)
            }
            FilterMatch.NOT_CONTAINS -> {
                val h = if (condition.caseSensitive) normalizedHaystack else normalizedHaystack.lowercase()
                val v = if (condition.caseSensitive) normalizedValue else normalizedValue.lowercase()
                !h.contains(v)
            }
            FilterMatch.EXACT -> {
                if (condition.caseSensitive) normalizedHaystack == normalizedValue else normalizedHaystack.equals(normalizedValue, ignoreCase = true)
            }
            FilterMatch.REGEX -> runCatching {
                val options = if (condition.caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
                // (?U) makes \w/\d/\s Unicode-aware so patterns work against Greek text too.
                Regex("(?U)" + normalizedValue, options).containsMatchIn(normalizedHaystack)
            }.getOrDefault(false)
        }
    }

    fun isHidden(article: Article, rules: List<FilterRule>): Boolean =
        rules.any { it.action == FilterAction.HIDE && matches(it, article) }

    /**
     * The real visibility check: an article is hidden by an explicit HIDE rule, or —
     * when at least one SHOW_ONLY ("Εμφάνιση") rule is enabled — by not matching any
     * of them. HIDE always wins over SHOW_ONLY if both would otherwise apply.
     */
    fun isVisible(article: Article, rules: List<FilterRule>): Boolean {
        if (isHidden(article, rules)) return false
        val showOnlyRules = rules.filter { it.action == FilterAction.SHOW_ONLY && it.enabled }
        if (showOnlyRules.isNotEmpty() && showOnlyRules.none { matches(it, article) }) return false
        return true
    }

    fun isImportant(article: Article, rules: List<FilterRule>): Boolean =
        rules.any { it.action == FilterAction.IMPORTANT && matches(it, article) }

    fun isHighlighted(article: Article, rules: List<FilterRule>): Boolean =
        rules.any { it.action == FilterAction.HIGHLIGHT && matches(it, article) }

    /** How many of [articles] this single rule would currently hide — used for the "→ κρύβει N άρθρα" preview in Settings. */
    fun countMatches(rule: FilterRule, articles: List<Article>): Int =
        articles.count { matches(rule, it) }
}
