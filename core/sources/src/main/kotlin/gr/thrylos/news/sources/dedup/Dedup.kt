package gr.thrylos.news.sources.dedup

import gr.thrylos.news.model.Article
import java.text.Normalizer
import kotlin.math.abs

/**
 * Groups articles that are almost certainly covering the same story across
 * different sites, so the feed can show one card ("+2 πηγές") instead of three
 * near-identical headlines.
 */
object Dedup {
    private val STOPWORDS = setOf(
        "και", "το", "η", "ο", "οι", "τα", "του", "της", "των", "στο", "στη",
        "στον", "στην", "με", "για", "από", "σε", "είναι", "θα", "να", "που",
        "ένα", "μια", "ένας", "τον", "την",
    )

    fun normalizeTokens(title: String): Set<String> {
        val stripped = Normalizer.normalize(title.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
        return Regex("[a-zα-ω0-9]+").findAll(stripped)
            .map { it.value }
            .filter { it.length > 2 && it !in STOPWORDS }
            .toSet()
    }

    fun jaccard(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val intersection = a.intersect(b).size.toDouble()
        val union = a.union(b).size.toDouble()
        return intersection / union
    }

    /** Maps each article id to the id of the group it belongs to (the earliest article in the group). */
    fun groupDuplicates(
        articles: List<Article>,
        windowMs: Long = 24 * 60 * 60 * 1000L,
        threshold: Double = 0.8,
    ): Map<String, String> {
        val sorted = articles.sortedBy { it.publishedAt ?: it.fetchedAt }
        val tokensById = sorted.associate { it.id to normalizeTokens(it.title) }
        val groupOf = mutableMapOf<String, String>()

        for (article in sorted) {
            if (groupOf.containsKey(article.id)) continue
            groupOf[article.id] = article.id
            val time = article.publishedAt ?: article.fetchedAt
            val myTokens = tokensById.getValue(article.id)

            for (other in sorted) {
                if (other.id == article.id || groupOf.containsKey(other.id)) continue
                val otherTime = other.publishedAt ?: other.fetchedAt
                if (abs(otherTime - time) > windowMs) continue
                if (jaccard(myTokens, tokensById.getValue(other.id)) >= threshold) {
                    groupOf[other.id] = article.id
                }
            }
        }
        return groupOf
    }
}
