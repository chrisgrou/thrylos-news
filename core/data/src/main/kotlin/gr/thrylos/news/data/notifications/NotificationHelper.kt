package gr.thrylos.news.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
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
    fun notifyNewArticles(articles: List<Article>, groupIntoSummary: Boolean) {
        if (articles.isEmpty()) return
        val manager = NotificationManagerCompat.from(context)
        // The user may not have granted POST_NOTIFICATIONS (API 33+) or may have
        // disabled notifications for the app entirely — both are covered by this check.
        if (!manager.areNotificationsEnabled()) return

        articles.take(10).forEach { article ->
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(article.title)
                .setContentText(article.sourceName)
                .setAutoCancel(true)
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
}
