package gr.thrylos.news.sources.filter

import gr.thrylos.news.model.Article
import gr.thrylos.news.model.ContentBlock
import gr.thrylos.news.model.FilterAction
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
        }
    }

    fun matches(rule: FilterRule, article: Article): Boolean {
        if (!rule.enabled) return false
        if (rule.scopeSourceId != null && rule.scopeSourceId != article.sourceId) return false

        val haystack = when (rule.field) {
            FilterField.TITLE -> article.title
            FilterField.BODY -> bodyText(article)
            FilterField.AUTHOR -> article.author.orEmpty()
            FilterField.URL -> article.url
            FilterField.SOURCE -> article.sourceName
        }
        return matchValue(haystack, rule)
    }

    private fun matchValue(haystack: String, rule: FilterRule): Boolean {
        return when (rule.match) {
            FilterMatch.CONTAINS -> {
                val h = if (rule.caseSensitive) haystack else haystack.lowercase()
                val v = if (rule.caseSensitive) rule.value else rule.value.lowercase()
                h.contains(v)
            }
            FilterMatch.EXACT -> {
                if (rule.caseSensitive) haystack == rule.value else haystack.equals(rule.value, ignoreCase = true)
            }
            FilterMatch.REGEX -> runCatching {
                val options = if (rule.caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
                // (?U) makes \w/\d/\s Unicode-aware so patterns work against Greek text too.
                Regex("(?U)" + rule.value, options).containsMatchIn(haystack)
            }.getOrDefault(false)
        }
    }

    fun isHidden(article: Article, rules: List<FilterRule>): Boolean =
        rules.any { it.action == FilterAction.HIDE && matches(it, article) }

    /** How many of [articles] this single rule would currently hide — used for the "→ κρύβει N άρθρα" preview in Settings. */
    fun countMatches(rule: FilterRule, articles: List<Article>): Int =
        articles.count { matches(rule, it) }
}
