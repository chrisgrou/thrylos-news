package gr.thrylos.news.widget

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import gr.thrylos.news.data.prefs.AppPreferences
import gr.thrylos.news.data.repo.ArticleRepository
import gr.thrylos.news.data.repo.FilterRepository
import gr.thrylos.news.data.sync.SyncScheduler

/** GlanceAppWidget/ActionCallback instances are created by the Glance runtime, not by
 *  Hilt, so they can't get constructor-injected dependencies — this EntryPoint lets
 *  them reach into the Hilt graph via [dagger.hilt.android.EntryPointAccessors]
 *  instead, using the application Context they're always handed. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun articleRepository(): ArticleRepository
    fun filterRepository(): FilterRepository
    fun appPreferences(): AppPreferences
    fun syncScheduler(): SyncScheduler
}
