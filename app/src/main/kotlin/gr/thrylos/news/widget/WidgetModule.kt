package gr.thrylos.news.widget

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import gr.thrylos.news.data.widget.WidgetUpdater

@Module
@InstallIn(SingletonComponent::class)
abstract class WidgetModule {
    @Binds
    abstract fun bindWidgetUpdater(impl: GlanceWidgetUpdater): WidgetUpdater
}
