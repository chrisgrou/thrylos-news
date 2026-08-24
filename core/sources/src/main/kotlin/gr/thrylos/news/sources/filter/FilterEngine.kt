package gr.thrylos.news.sources.filter

import gr.thrylos.news.model.Article
import gr.thrylos.news.model.ContentBlock
import gr.thrylos.news.model.FilterAction
import gr.thrylos.news.model.FilterCombinator
import gr.thrylos.news.model.FilterCondition
import gr.thrylos.news.model.FilterField
import gr.thrylos.news.model.FilterMatch
import gr.thrylos.news.model.FilterRule

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

        val results = rule.conditions.map { condition ->
            val haystack = when (condition.field) {
                FilterField.TITLE -> article.title
                FilterField.BODY -> bodyText(article)
                FilterField.AUTHOR -> article.author.orEmpty()
                FilterField.URL -> article.url
                FilterField.SOURCE -> article.sourceName
            }
            matchValue(haystack, condition)
        }
        return combine(rule.combinator, results)
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
        if (rule.conditions.any { it.field == FilterField.AUTHOR || it.field == FilterField.BODY }) return false

        val results = rule.conditions.map { condition ->
            val haystack = when (condition.field) {
                FilterField.TITLE -> title
                FilterField.SOURCE -> sourceName
                FilterField.URL -> url
                FilterField.AUTHOR, FilterField.BODY -> return false
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

    private fun matchValue(haystack: String, condition: FilterCondition): Boolean {
        return when (condition.match) {
            FilterMatch.CONTAINS -> {
                val h = if (condition.caseSensitive) haystack else haystack.lowercase()
                val v = if (condition.caseSensitive) condition.value else condition.value.lowercase()
                h.contains(v)
            }
            FilterMatch.EXACT -> {
                if (condition.caseSensitive) haystack == condition.value else haystack.equals(condition.value, ignoreCase = true)
            }
            FilterMatch.REGEX -> runCatching {
                val options = if (condition.caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
                // (?U) makes \w/\d/\s Unicode-aware so patterns work against Greek text too.
                Regex("(?U)" + condition.value, options).containsMatchIn(haystack)
            }.getOrDefault(false)
        }
    }

    fun isHidden(article: Article, rules: List<FilterRule>): Boolean =
        rules.any { it.action == FilterAction.HIDE && matches(it, article) }

    fun isImportant(article: Article, rules: List<FilterRule>): Boolean =
        rules.any { it.action == FilterAction.IMPORTANT && matches(it, article) }

    fun isHighlighted(article: Article, rules: List<FilterRule>): Boolean =
        rules.any { it.action == FilterAction.HIGHLIGHT && matches(it, article) }

    /** How many of [articles] this single rule would currently hide — used for the "→ κρύβει N άρθρα" preview in Settings. */
    fun countMatches(rule: FilterRule, articles: List<Article>): Int =
        articles.count { matches(rule, it) }
}
