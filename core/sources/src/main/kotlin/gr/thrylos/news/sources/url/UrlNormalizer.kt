package gr.thrylos.news.sources.url

import gr.thrylos.news.sources.plugin.UrlRules
import java.net.URI

object UrlNormalizer {

    /** Resolves [href] against [baseUrl] if it's relative, otherwise returns it unchanged. */
    fun resolve(baseUrl: String, href: String): String =
        try {
            URI(baseUrl).resolve(href).toString()
        } catch (e: Exception) {
            href
        }

    /** Strips query parameters listed in [rules], drops the fragment, and lowercases the host. */
    fun canonicalize(url: String, rules: UrlRules): String {
        val uri = try { URI(url) } catch (e: Exception) { return url }
        val host = uri.host?.lowercase() ?: return url

        val remainingQuery = uri.rawQuery
            ?.split("&")
            ?.filter { param ->
                val key = param.substringBefore('=')
                key !in rules.stripQueryParams
            }
            ?.joinToString("&")
            ?.ifBlank { null }

        val path = uri.rawPath?.let { if (it.length > 1 && it.endsWith("/")) it.dropLast(1) else it } ?: ""

        val query = remainingQuery?.let { "?$it" } ?: ""
        val port = if (uri.port != -1) ":${uri.port}" else ""
        return "${uri.scheme}://$host$port$path$query"
    }

    /** True if [url] is allowed to be crawled per [rules] (allow-list wins over host defaults, deny-list wins over allow). */
    fun isAllowed(url: String, rules: UrlRules): Boolean {
        if (rules.deny.any { Regex(it).containsMatchIn(url) }) return false
        if (rules.allow.isEmpty()) return true
        return rules.allow.any { Regex(it).containsMatchIn(url) }
    }
}
