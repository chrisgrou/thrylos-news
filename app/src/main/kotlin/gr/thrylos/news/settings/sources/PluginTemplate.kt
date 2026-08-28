package gr.thrylos.news.settings.sources

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
