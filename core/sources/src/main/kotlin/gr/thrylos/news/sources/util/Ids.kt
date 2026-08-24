package gr.thrylos.news.sources.util

import java.security.MessageDigest

object Ids {
    /** Stable, deterministic article id derived from its canonical URL — same article, same id, every sync. */
    fun forArticle(canonicalUrl: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(canonicalUrl.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(24)
    }
}
