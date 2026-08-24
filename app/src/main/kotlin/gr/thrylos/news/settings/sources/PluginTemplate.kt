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
