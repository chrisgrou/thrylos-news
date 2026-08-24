package gr.thrylos.news.sources.extract

/**
 * Selectors removed from every article regardless of what the plugin itself
 * specifies. Because articles are rendered as native Compose content built
 * from [gr.thrylos.news.model.ContentBlock]s rather than as raw HTML/WebView,
 * this list is really just a second line of defense — the primary defense is
 * that any tag/element we don't explicitly recognize during extraction is
 * dropped anyway (see [HtmlToBlocks]).
 */
object AdBlockList {
    val selectors: List<String> = listOf(
        ".ad", ".ads", ".advert", ".advertisement", ".adsbygoogle",
        "[id^=div-gpt-ad]", "[id*=google_ads]", "[class*=taboola]", "[class*=outbrain]",
        "[class*=glomex]", ".newsletter", ".newsletter-signup", ".social-share", ".share-buttons",
        ".related", ".related-posts", ".read-more", ".read-also", ".you-may-also-like",
        "iframe", "script", "style", "noscript", "ins", "form",
    )
}
