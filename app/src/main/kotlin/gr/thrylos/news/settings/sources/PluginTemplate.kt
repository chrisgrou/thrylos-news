package gr.thrylos.news.settings.sources

import kotlinx.serialization.json.JsonPrimitive

/** Starter JSON shown when adding a brand-new source — filled in with placeholders
 * the user replaces with real selectors, then validates with "Δοκιμή". */
fun newPluginTemplate(): String = """
{
  "schemaVersion": 1,
  "id": "my-source",
  "name": "Το site μου",
  "homepage": "https://example.gr",
  "enabled": true,
  "discovery": {
    "type": "rss",
    "url": "https://example.gr/feed",
    "maxItems": 40
  },
  "article": {
    "title": "h1",
    "author": ".author",
    "date": "time@datetime",
    "leadImage": "figure img@src",
    "content": "div.article-body",
    "remove": [".ad", ".related", ".newsletter"]
  },
  "urlRules": {
    "deny": ["/live/", "/gallery/"],
    "stripQueryParams": ["utm_source", "utm_medium", "utm_campaign"]
  },
  "fallback": "readability"
}
""".trimIndent()

/** Starter JSON for a Facebook page/profile, scraped via mbasic.facebook.com (no
 *  login) instead of the JS-heavy main site. Best-effort: Facebook's markup has no
 *  stable class names to target reliably from here, so these selectors are a
 *  starting guess — expect to adjust them with "Δοκιμή" against the real page.
 *  A post has no real headline, so title and content point at the same text. */
fun newFacebookPluginTemplate(): String = """
{
  "schemaVersion": 1,
  "id": "my-facebook-page",
  "name": "Η σελίδα μου (Facebook)",
  "homepage": "https://www.facebook.com/athlitiki.diaploki.official",
  "enabled": true,
  "kind": "facebook",
  "discovery": {
    "type": "html-list",
    "url": "https://mbasic.facebook.com/athlitiki.diaploki.official",
    "maxItems": 25
  },
  "listSelectors": {
    "item": "div:has(> a[href*=story_fbid])",
    "link": "a[href*=story_fbid]@href",
    "title": "div:has(> a[href*=story_fbid])"
  },
  "article": {
    "title": "#MPhotoContent, #m_story_permalink_view, div[role=article]",
    "content": "#MPhotoContent, #m_story_permalink_view, div[role=article]",
    "date": "abbr@title",
    "dateFormat": "MMMM d, yyyy 'at' h:mm a"
  },
  "urlRules": {
    "stripQueryParams": ["__tn__", "eid", "acontext", "refid", "_ft_"]
  },
  "http": {
    "userAgent": "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36",
    "delayMs": 800
  },
  "fallback": "none"
}
""".trimIndent()

/** Starter JSON for a YouTube channel, given its real name and channel id — both
 *  already resolved (see [AddYouTubeChannelScreen]/[gr.thrylos.news.sources.youtube.YouTubeChannelResolver])
 *  from whatever handle or link the user pasted, so there's nothing left here to
 *  guess or fill in by hand. [id] is derived from [channelId] rather than [name]:
 *  a channel id is always plain ASCII, unlike a name (Greek, emoji, punctuation all
 *  fair game), and the plugin id schema only allows lowercase latin/digits/hyphens.
 *  [name] and [id] are still editable in the JSON field before saving, same as any
 *  other source. */
fun newYouTubePluginTemplate(name: String, channelId: String): String {
    val id = "youtube-" + channelId.lowercase().replace('_', '-')
    val jsonName = JsonPrimitive(name).toString()
    return """
{
  "schemaVersion": 1,
  "id": "$id",
  "name": $jsonName,
  "homepage": "https://www.youtube.com/channel/$channelId",
  "enabled": true,
  "kind": "youtube",
  "discovery": {
    "type": "rss",
    "url": "https://www.youtube.com/feeds/videos.xml?channel_id=$channelId",
    "maxItems": 30
  },
  "article": {
    "title": "unused for kind=youtube — video titles come from the channel feed"
  }
}
""".trimIndent()
}

/** Starter JSON for a YouTube channel when nothing could be auto-resolved — the
 *  channel id has to be found and pasted in by hand (channel → "Σχετικά" →
 *  "Κοινοποίηση καναλιού" → "Αντιγραφή αναγνωριστικού καναλιού"). The escape hatch
 *  from [AddYouTubeChannelScreen] when resolving fails. */
fun newYouTubePluginTemplateManual(): String = """
{
  "schemaVersion": 1,
  "id": "my-youtube-channel",
  "name": "Το κανάλι μου",
  "homepage": "https://www.youtube.com/channel/UC...",
  "enabled": true,
  "kind": "youtube",
  "discovery": {
    "type": "rss",
    "url": "https://www.youtube.com/feeds/videos.xml?channel_id=UC...",
    "maxItems": 30
  },
  "article": {
    "title": "unused for kind=youtube — video titles come from the channel feed"
  }
}
""".trimIndent()
