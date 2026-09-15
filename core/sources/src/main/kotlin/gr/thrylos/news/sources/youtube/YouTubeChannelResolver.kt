package gr.thrylos.news.sources.youtube

import gr.thrylos.news.sources.http.HttpFetcher
import gr.thrylos.news.sources.plugin.HttpConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.Jsoup

data class YouTubeChannelInfo(val channelId: String, val name: String)

private val CHANNEL_ID_IN_PATH = Regex("/channel/(UC[\\w-]{22})")
private val CHANNEL_ID_ONLY = Regex("^UC[\\w-]{22}$")

/** A cookie-less request from an EU IP gets redirected to a "Before you continue to
 *  YouTube" consent interstitial instead of the real page — there's no channel data
 *  on it at all. `CONSENT=YES+<anything>` is the exact cookie clicking "I agree" on
 *  that page would set; sending it up front skips the redirect entirely. Widely used
 *  for this — yt-dlp and various other tools hardcode the same value. */
private val YOUTUBE_HTTP_CONFIG = HttpConfig(headers = mapOf("Cookie" to "CONSENT=YES+1"))

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
 * channel's own page — nothing here needs JavaScript execution or an API key — and
 * tries two independent ways to read the channel out of the response, in order:
 *
 * 1. The `<link rel="canonical">`/`<meta property="og:title">` tags YouTube renders
 *    for search engines and link-preview crawlers — cheap, but not present in every
 *    response variant.
 * 2. `metadata.channelMetadataRenderer` inside the page's own embedded `ytInitialData`
 *    JSON — the same data YouTube's client-side rendering reads, and the
 *    unambiguous "this is the channel this page is about" field (the page mentions
 *    plenty of *other* channel ids too, e.g. featured/recommended ones, so this has
 *    to come from a specific structural path rather than the first id-shaped string
 *    found anywhere in the page).
 */
class YouTubeChannelResolver(private val http: HttpFetcher = HttpFetcher()) {

    fun resolve(input: String): YouTubeChannelInfo {
        val url = channelUrl(input)
        val html = runCatching { http.fetchText(url, YOUTUBE_HTTP_CONFIG) }
            .getOrElse { error("Δεν ήταν δυνατή η σύνδεση στο YouTube (${it.message?.take(120)}).") }

        if (looksLikeConsentWall(html)) {
            error("Το YouTube ζήτησε επιβεβαίωση απορρήτου αντί να δείξει το κανάλι — δοκίμασε ξανά σε λίγο, ή πρόσθεσε το κανάλι χειροκίνητα.")
        }

        return resolveFromTags(html, url) ?: resolveFromInitialData(html)
            ?: error("Δεν βρέθηκε κανάλι YouTube σε '$input' — έλεγξε τον σύνδεσμο/handle.")
    }

    private fun resolveFromTags(html: String, url: String): YouTubeChannelInfo? {
        val doc = Jsoup.parse(html, url)
        val canonical = doc.selectFirst("link[rel=canonical]")?.attr("href").orEmpty()
        val channelId = CHANNEL_ID_IN_PATH.find(canonical)?.groupValues?.get(1)
            ?: doc.selectFirst("meta[itemprop=channelId]")?.attr("content")?.ifBlank { null }
            ?: return null
        val name = doc.selectFirst("meta[property=og:title]")?.attr("content")?.ifBlank { null }
            ?: doc.title().removeSuffix(" - YouTube").trim().ifBlank { null }
            ?: return null
        return YouTubeChannelInfo(channelId, name)
    }

    private fun resolveFromInitialData(html: String): YouTubeChannelInfo? {
        val json = extractJsonObjectAfter(html, "var ytInitialData = ")
            ?: extractJsonObjectAfter(html, "ytInitialData\"] = ")
            ?: return null
        val renderer = runCatching { Json.parseToJsonElement(json).jsonObject }.getOrNull()
            ?.get("metadata")?.jsonObject
            ?.get("channelMetadataRenderer")?.jsonObject
            ?: return null
        val channelId = renderer["externalId"]?.jsonPrimitive?.contentOrNull ?: return null
        val name = renderer["title"]?.jsonPrimitive?.contentOrNull ?: return null
        return YouTubeChannelInfo(channelId, name)
    }

    private fun looksLikeConsentWall(html: String): Boolean {
        val head = html.take(4000)
        return head.contains("consent.youtube.com") || head.contains("Before you continue to YouTube")
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

/** Finds `marker` and returns the balanced `{...}` JSON object immediately following
 *  it — a plain regex can't do this correctly since the object is deeply nested and
 *  contains braces inside string values. Tracks string/escape state so a `{`/`}`
 *  inside a JSON string (e.g. in a video description) doesn't throw off the depth
 *  count. */
internal fun extractJsonObjectAfter(html: String, marker: String): String? {
    val markerIndex = html.indexOf(marker)
    if (markerIndex == -1) return null
    var i = markerIndex + marker.length
    while (i < html.length && html[i] != '{') i++
    if (i >= html.length) return null
    val start = i

    var depth = 0
    var inString = false
    var escaped = false
    while (i < html.length) {
        val c = html[i]
        if (inString) {
            when {
                escaped -> escaped = false
                c == '\\' -> escaped = true
                c == '"' -> inString = false
            }
        } else {
            when (c) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return html.substring(start, i + 1)
                }
            }
        }
        i++
    }
    return null
}
