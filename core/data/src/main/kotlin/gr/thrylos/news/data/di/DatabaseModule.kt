package gr.thrylos.news.data.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import gr.thrylos.news.data.db.AppDatabase
import gr.thrylos.news.data.db.dao.ArticleDao
import gr.thrylos.news.data.db.dao.FilterRuleDao
import gr.thrylos.news.data.db.dao.SourceDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "thrylos-news.db").build()

    @Provides
    fun provideArticleDao(db: AppDatabase): ArticleDao = db.articleDao()

    @Provides
    fun provideSourceDao(db: AppDatabase): SourceDao = db.sourceDao()

    @Provides
    fun provideFilterRuleDao(db: AppDatabase): FilterRuleDao = db.filterRuleDao()
}
