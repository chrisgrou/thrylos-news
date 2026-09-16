package gr.thrylos.news.data.db.entity

/**
 * Every column of [ArticleEntity] *except* `contentJson` — the article's full body,
 * which for a typical scraped article is by far the largest thing stored per row and
 * is dead weight for every list screen (feed, bookmarks, source/author profiles): all
 * of those show title, source, image, author and time, and none of them render body
 * text.
 *
 * Room re-runs an observing query — and re-reads every row it selects — after *any*
 * write to the table, including a single article's isRead flag. With `SELECT *` and a
 * few hundred stored articles that meant reading and allocating several megabytes of
 * body JSON per emission, dozens of times in a row while a sync writes, purely to
 * throw it away again. That allocation churn (and the GC it caused) was the real cost
 * behind the sluggish feed and choppy scrolling, not the filtering/sorting downstream.
 *
 * The reader still reads bodies, but one article at a time via `observeById`, which
 * selects the full row.
 */
data class ArticleSummary(
    val id: String,
    val sourceId: String,
    val sourceName: String,
    val url: String,
    val title: String,
    val author: String?,
    val publishedAt: Long?,
    val fetchedAt: Long,
    val leadImageUrl: String?,
    val usedFallbackExtraction: Boolean,
    val isRead: Boolean,
    val isBookmarked: Boolean,
    val dedupGroupId: String?,
)

/** Just the body of one article, for the rare caller that needs body text for a
 *  known, bounded set of ids (a BODY/"Οπουδήποτε" filter rule) rather than for
 *  everything in the table. */
data class ArticleContent(val id: String, val contentJson: String)

/** The per-article state a re-sync must carry forward rather than reset — see
 *  [gr.thrylos.news.data.repo.ArticleRepository.upsertAll]. */
data class ArticleState(val id: String, val isRead: Boolean, val isBookmarked: Boolean, val dedupGroupId: String?)
