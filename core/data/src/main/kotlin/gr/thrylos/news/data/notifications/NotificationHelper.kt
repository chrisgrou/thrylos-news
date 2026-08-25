package gr.thrylos.news.data.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import gr.thrylos.news.model.Article
import javax.inject.Inject
import javax.inject.Singleton

private const val CHANNEL_ID = "new_articles"
private const val SUMMARY_NOTIFICATION_ID = 1
private const val GROUP_KEY = "gr.thrylos.news.NEW_ARTICLES"

/** Read by :app's MainActivity (via onCreate's intent and onNewIntent) to know which
 *  article to open. Built here via the app's own launch intent (PackageManager) rather
 *  than referencing MainActivity directly, since :core:data can't depend on :app. */
const val EXTRA_ARTICLE_ID = "gr.thrylos.news.EXTRA_ARTICLE_ID"

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Νέα άρθρα",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Ειδοποιήσεις για νέα άρθρα από τις πηγές σου" }
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    /** Posts one notification per article (max a handful) plus, when [groupIntoSummary],
     * a summary notification so they collapse into a single group on the lock screen. */
    @SuppressLint("MissingPermission")
    fun notifyNewArticles(articles: List<Article>, groupIntoSummary: Boolean) {
        if (articles.isEmpty()) return
        val manager = NotificationManagerCompat.from(context)
        // The user may not have granted POST_NOTIFICATIONS (API 33+) or may have
        // disabled notifications for the app entirely — both are covered by this
        // runtime check; lint can't verify it statically, hence the suppression above.
        if (!manager.areNotificationsEnabled()) return

        articles.take(10).forEach { article ->
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(article.title)
                .setContentText(article.sourceName)
                .setAutoCancel(true)
                .setContentIntent(articlePendingIntent(article.id))
                .apply { if (groupIntoSummary) setGroup(GROUP_KEY) }
                .build()
            runCatching { manager.notify(article.id.hashCode(), notification) }
        }

        if (groupIntoSummary && articles.size > 1) {
            val summary = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Thrylos News")
                .setContentText("${articles.size} νέα άρθρα")
                .setGroup(GROUP_KEY)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .build()
            runCatching { manager.notify(SUMMARY_NOTIFICATION_ID, summary) }
        }
    }

    private fun articlePendingIntent(articleId: String): PendingIntent? {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        launchIntent.apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_ARTICLE_ID, articleId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            articleId.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
