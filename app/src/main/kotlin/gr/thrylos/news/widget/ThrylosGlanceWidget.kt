package gr.thrylos.news.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.defaultWeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dagger.hilt.android.EntryPointAccessors
import gr.thrylos.news.MainActivity
import gr.thrylos.news.R
import gr.thrylos.news.data.notifications.EXTRA_ARTICLE_ID
import gr.thrylos.news.feed.formatRelativeTime
import gr.thrylos.news.feed.stripSourceSuffix
import gr.thrylos.news.model.Article
import gr.thrylos.news.sources.filter.FilterEngine
import kotlinx.coroutines.flow.first
import kotlin.math.max

private val BrandRed = Color(0xFFC6303E)
private val WidgetBackground = Color(0xFFFFFBF5)
private val TextPrimary = Color(0xFF241812)
private val TextSecondary = Color(0xFF7A6A5E)
private val Divider = Color(0xFFE9E0D5)

private val HEADER_HEIGHT = 32.dp
private val ROW_HEIGHT = 52.dp

/** Home-screen widget listing the newest articles — resizable, one article per row
 *  (title + a single "πηγή · συντάκτης · χρόνος" line), with an in-widget refresh
 *  button. How many rows fit is derived from the widget's current on-screen size
 *  ([SizeMode.Exact]) rather than being scrollable, so resizing the widget directly
 *  controls how many articles show. */
class ThrylosGlanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val articleRepository = entryPoint.articleRepository()
        val filterRepository = entryPoint.filterRepository()
        val appPreferences = entryPoint.appPreferences()

        val widgetPrefs = appPreferences.widgetPrefs.first()
        val filters = filterRepository.observeAll().first()
        val articles = articleRepository.observeAll().first()
            .filter { FilterEngine.isVisible(it, filters) }
            .filter { !widgetPrefs.showOnlyImportant || FilterEngine.isImportant(it, filters) }
            .sortedByDescending { it.publishedAt ?: it.fetchedAt }

        provideContent {
            WidgetContent(articles, widgetPrefs.showOnlyImportant)
        }
    }
}

@Composable
private fun WidgetContent(articles: List<Article>, importantOnly: Boolean) {
    val size = LocalSize.current
    val availableHeight = (size.height - HEADER_HEIGHT).coerceAtLeast(0.dp)
    val itemCount = max(1, (availableHeight.value / ROW_HEIGHT.value).toInt())

    Column(
        modifier = GlanceModifier.fillMaxSize().background(WidgetBackground).padding(12.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().height(HEADER_HEIGHT),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = "Ολυμπιακός",
                style = TextStyle(color = ColorProvider(BrandRed), fontWeight = FontWeight.Bold, fontSize = 15.sp),
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Image(
                provider = ImageProvider(R.drawable.ic_refresh),
                contentDescription = "Ανανέωση",
                colorFilter = androidx.glance.ColorFilter.tint(ColorProvider(BrandRed)),
                modifier = GlanceModifier.size(22.dp).clickable(actionRunCallback<RefreshAction>()),
            )
        }

        if (articles.isEmpty()) {
            Text(
                text = if (importantOnly) "Δεν υπάρχουν σημαντικά άρθρα" else "Δεν υπάρχουν άρθρα ακόμα",
                style = TextStyle(color = ColorProvider(TextSecondary), fontSize = 13.sp),
                modifier = GlanceModifier.padding(top = 8.dp),
            )
        } else {
            val shown = articles.take(itemCount)
            shown.forEachIndexed { index, article ->
                WidgetArticleRow(article)
                if (index != shown.lastIndex) {
                    Spacer(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(Divider))
                }
            }
        }
    }
}

@Composable
private fun WidgetArticleRow(article: Article) {
    val context = androidx.glance.LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
            .clickable(actionStartActivity(openArticleIntent(context, article.id)))
            .padding(vertical = 6.dp),
    ) {
        Text(
            text = article.title,
            maxLines = 2,
            style = TextStyle(color = ColorProvider(TextPrimary), fontWeight = FontWeight.Medium, fontSize = 13.sp),
        )
        val meta = listOfNotNull(
            stripSourceSuffix(article.sourceName),
            article.author?.takeIf { it.isNotBlank() },
            formatRelativeTime(article.publishedAt ?: article.fetchedAt),
        ).joinToString(" · ")
        Text(
            text = meta,
            maxLines = 1,
            style = TextStyle(color = ColorProvider(TextSecondary), fontSize = 11.sp),
        )
    }
}

private fun openArticleIntent(context: Context, articleId: String) =
    Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        putExtra(EXTRA_ARTICLE_ID, articleId)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
