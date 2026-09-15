package gr.thrylos.news.sources.youtube

import gr.thrylos.news.sources.http.HttpFetcher
import org.jsoup.Jsoup

data class YouTubeChannelInfo(val channelId: String, val name: String)

private val CHANNEL_ID_IN_PATH = Regex("/channel/(UC[\\w-]{22})")
private val CHANNEL_ID_ONLY = Regex("^UC[\\w-]{22}$")

/**
 * Resolves any way a user might refer to a YouTube channel — an `@handle`, a legacy
 * `/c/` or `/user/` vanity URL, a full channel/video URL, or the raw channel id
 * itself — down to its channel id and real display name.
 *
 * The channel id is the only thing that actually matters for a
 * [gr.thrylos.news.sources.plugin.SourceKind.YOUTUBE] plugin: its video RSS feed
 * (`youtube.com/feeds/videos.xml?channel_id=...`) accepts nothing else, not the
 * handle a person would normally share. Rather than making the user go dig it out of
 * "Κοινοποίηση καναλιού" themselves, this does a single plain HTTP GET of the
 * channel's own page and reads the same `<link rel="canonical">`/`<meta
 * property="og:title">` tags YouTube renders server-side for search engines and
 * link-preview crawlers — nothing here needs JavaScript execution or an API key.
 */
class YouTubeChannelResolver(private val http: HttpFetcher = HttpFetcher()) {

    fun resolve(input: String): YouTubeChannelInfo {
        val url = channelUrl(input)
        val html = runCatching { http.fetchText(url) }
            .getOrElse { error("Δεν ήταν δυνατή η σύνδεση στο YouTube (${it.message?.take(120)}).") }
        val doc = Jsoup.parse(html, url)

        val canonical = doc.selectFirst("link[rel=canonical]")?.attr("href").orEmpty()
        val channelId = CHANNEL_ID_IN_PATH.find(canonical)?.groupValues?.get(1)
            ?: doc.selectFirst("meta[itemprop=channelId]")?.attr("content")?.ifBlank { null }
            ?: error("Δεν βρέθηκε κανάλι YouTube σε '$input' — έλεγξε τον σύνδεσμο/handle.")

        val name = doc.selectFirst("meta[property=og:title]")?.attr("content")?.ifBlank { null }
            ?: doc.title().removeSuffix(" - YouTube").trim().ifBlank { null }
            ?: error("Βρέθηκε το κανάλι, αλλά όχι το όνομά του — δοκίμασε ξανά ή γράψε το χειροκίνητα.")

        return YouTubeChannelInfo(channelId, name)
    }

    /** Normalizes anything a user might paste — a bare handle, `@handle`, a legacy
     *  `/c/`/`/user/` vanity URL, a full channel/video URL, or a raw channel id — to
     *  the URL that's actually fetched. Exposed for testing without a live network
     *  call; not meant to be called directly otherwise. */
    internal fun channelUrl(input: String): String {
        val trimmed = input.trim()
        return when {
            trimmed.isBlank() -> error("Δώσε έναν σύνδεσμο ή το @handle του καναλιού.")
            CHANNEL_ID_ONLY.matches(trimmed) -> "https://www.youtube.com/channel/$trimmed"
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            else -> "https://www.youtube.com/${if (trimmed.startsWith("@")) trimmed else "@$trimmed"}"
        }
    }
}
