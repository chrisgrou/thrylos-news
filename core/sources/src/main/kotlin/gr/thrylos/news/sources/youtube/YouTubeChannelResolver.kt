package gr.thrylos.news.sources.youtube

import gr.thrylos.news.sources.discovery.secureDocumentBuilder
import gr.thrylos.news.sources.http.HttpFetcher
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayInputStream

data class YouTubeChannelInfo(val channelId: String, val name: String)

private val CHANNEL_ID_IN_PATH = Regex("/channel/(UC[\\w-]{22})")
private val CHANNEL_ID_ONLY = Regex("^UC[\\w-]{22}$")

/** A public, unchanging key baked into every YouTube web page's own JavaScript — an
 *  API version tag, not a secret. Long-established open-source tools (NewPipe,
 *  yt-dlp, Invidious) use the same one for the same reason: it's what the innertube
 *  API these clients call is versioned/gated by, not an access credential. */
private const val INNERTUBE_API_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
private const val DEFAULT_RESOLVE_ENDPOINT = "https://www.youtube.com/youtubei/v1/navigation/resolve_url?key=$INNERTUBE_API_KEY"

/**
 * Resolves any way a user might refer to a YouTube channel — an `@handle`, a legacy
 * `/c/` or `/user/` vanity URL, a full channel/video URL, or the raw channel id
 * itself — down to its channel id and real display name.
 *
 * Two earlier versions of this tried reading the channel's own HTML page (the
 * `<link rel="canonical">`/`<meta property="og:title">` tags search engines read,
 * then a fallback into the page's embedded `ytInitialData` JSON). Both failed
 * repeatedly and unpredictably against real, live channels — a plain HTTP GET of a
 * youtube.com page goes through YouTube's bot-detection and consent machinery on
 * the way, and can come back as a GDPR consent wall, a generic logged-out homepage
 * shell, or other variants that were never modeled, each requiring another guess to
 * even name from the outside.
 *
 * This uses two JSON/XML API calls instead, neither of which renders a page at all:
 *
 * 1. `youtubei/v1/navigation/resolve_url` — YouTube's own internal "resolve any URL
 *    to a browse id" endpoint, used by its web client itself to turn a handle into
 *    a channel id. A plain JSON POST/response, not a page.
 * 2. The channel's own video feed (`feeds/videos.xml?channel_id=...`) — already
 *    relied on elsewhere in this app for actual video discovery, so proven to work
 *    without hitting any of the above. Its top-level `<title>` (not an entry's) is
 *    the channel's real display name.
 */
class YouTubeChannelResolver(
    private val http: HttpFetcher = HttpFetcher(),
    private val resolveEndpoint: String = DEFAULT_RESOLVE_ENDPOINT,
    private val feedUrl: (channelId: String) -> String = { "https://www.youtube.com/feeds/videos.xml?channel_id=$it" },
) {

    fun resolve(input: String): YouTubeChannelInfo {
        val trimmed = input.trim()
        if (trimmed.isBlank()) error("Δώσε έναν σύνδεσμο ή το @handle του καναλιού.")

        val channelId = directChannelId(trimmed) ?: resolveChannelId(trimmed)
        val name = channelName(channelId)
            ?: error("Βρέθηκε το κανάλι ($channelId), αλλά όχι το όνομά του — δοκίμασε ξανά ή γράψε το χειροκίνητα.")
        return YouTubeChannelInfo(channelId, name)
    }

    /** Cheap, offline cases: a bare channel id, or a URL that already names one —
     *  no need to call the resolve API at all. */
    private fun directChannelId(input: String): String? =
        if (CHANNEL_ID_ONLY.matches(input)) input else CHANNEL_ID_IN_PATH.find(input)?.groupValues?.get(1)

    private fun resolveChannelId(input: String): String {
        val url = channelUrl(input)
        val requestBody = """{"context":{"client":{"clientName":"WEB","clientVersion":"2.20210721.00.00"}},"url":${jsonQuote(url)}}"""
        val response = runCatching { http.postJson(resolveEndpoint, requestBody) }
            .getOrElse { error("Δεν ήταν δυνατή η σύνδεση στο YouTube (${it.message?.take(120)}).") }
        return extractBrowseId(response)
            ?: error("Δεν βρέθηκε κανάλι YouTube σε '$input'. Απάντηση API: ${response.take(200).ifBlank { "(κενή)" }}")
    }

    private fun channelName(channelId: String): String? {
        val xml = runCatching { http.fetchText(feedUrl(channelId)) }.getOrNull() ?: return null
        return extractFeedTitle(xml)
    }

    /** Normalizes anything a user might paste — a bare handle, `@handle`, a legacy
     *  `/c/`/`/user/` vanity URL, a full channel/video URL, or a raw channel id —
     *  into the URL handed to the resolve API. Exposed for testing; not meant to be
     *  called directly otherwise. */
    internal fun channelUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isBlank()) error("Δώσε έναν σύνδεσμο ή το @handle του καναλιού.")
        return when {
            CHANNEL_ID_ONLY.matches(trimmed) -> "https://www.youtube.com/channel/$trimmed"
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            else -> "https://www.youtube.com/${if (trimmed.startsWith("@")) trimmed else "@$trimmed"}"
        }
    }
}

private fun jsonQuote(value: String): String = JsonPrimitive(value).toString()

/** Pulls `endpoint.browseEndpoint.browseId` out of a resolve_url response — the
 *  field that response exists to carry. Exposed (and kept separate from the network
 *  call) so it's testable against hand-written sample payloads without a live
 *  network. */
internal fun extractBrowseId(json: String): String? =
    runCatching { Json.parseToJsonElement(json).jsonObject }.getOrNull()
        ?.get("endpoint")?.jsonObject
        ?.get("browseEndpoint")?.jsonObject
        ?.get("browseId")?.jsonPrimitive?.contentOrNull

/** The channel feed's own `<title>` — the first one in the document, which by Atom's
 *  structure (the feed's own metadata always precedes its `<entry>`s) is the feed's,
 *  never an individual video's. Exposed for testing without a live network. */
internal fun extractFeedTitle(xml: String): String? {
    val doc = runCatching { secureDocumentBuilder().parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8))) }
        .getOrNull() ?: return null
    return doc.getElementsByTagName("title").item(0)?.textContent?.trim()?.ifBlank { null }
}
