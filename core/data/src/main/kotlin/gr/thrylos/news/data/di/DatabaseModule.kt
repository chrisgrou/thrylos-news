package gr.thrylos.news.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import gr.thrylos.news.data.db.AppDatabase
import gr.thrylos.news.data.db.dao.ArticleDao
import gr.thrylos.news.data.db.dao.FilterRuleDao
import gr.thrylos.news.data.db.dao.SourceDao
import gr.thrylos.news.data.db.dao.UpdateHistoryDao
import javax.inject.Singleton

/** Purely additive (new table, nothing existing touched) — real installs now carry
 *  real user data (bookmarks, filters), so this bump must not fall back to the
 *  destructive wipe used for earlier, pre-release schema changes. */
private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS update_history (versionCode INTEGER NOT NULL PRIMARY KEY, notes TEXT NOT NULL, installedAt INTEGER NOT NULL)",
        )
    }
}

/** Purely additive (two new nullable columns on the existing sources table). */
private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sources ADD COLUMN lastSyncError TEXT")
        db.execSQL("ALTER TABLE sources ADD COLUMN lastSyncAt INTEGER")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "thrylos-news.db")
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
            // Fallback only for schema changes made before any real install existed.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideArticleDao(db: AppDatabase): ArticleDao = db.articleDao()

    @Provides
    fun provideSourceDao(db: AppDatabase): SourceDao = db.sourceDao()

    @Provides
    fun provideFilterRuleDao(db: AppDatabase): FilterRuleDao = db.filterRuleDao()

    @Provides
    fun provideUpdateHistoryDao(db: AppDatabase): UpdateHistoryDao = db.updateHistoryDao()
}
