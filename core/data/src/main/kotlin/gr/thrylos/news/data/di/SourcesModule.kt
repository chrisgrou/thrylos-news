package gr.thrylos.news.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import gr.thrylos.news.sources.http.HttpFetcher
import gr.thrylos.news.sources.matches.SofascoreMatchesFetcher
import gr.thrylos.news.sources.sync.SourceSyncCoordinator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SourcesModule {

    @Provides
    @Singleton
    fun provideHttpFetcher(): HttpFetcher = HttpFetcher()

    @Provides
    @Singleton
    fun provideSourceSyncCoordinator(http: HttpFetcher): SourceSyncCoordinator = SourceSyncCoordinator(http)

    @Provides
    @Singleton
    fun provideSofascoreMatchesFetcher(http: HttpFetcher): SofascoreMatchesFetcher = SofascoreMatchesFetcher(http)
}
