package gr.thrylos.news.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import gr.thrylos.news.data.db.dao.ArticleDao
import gr.thrylos.news.data.db.dao.FilterRuleDao
import gr.thrylos.news.data.db.dao.SourceDao
import gr.thrylos.news.data.db.entity.ArticleEntity
import gr.thrylos.news.data.db.entity.FilterRuleEntity
import gr.thrylos.news.data.db.entity.SourceEntity

@Database(
    entities = [ArticleEntity::class, SourceEntity::class, FilterRuleEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun sourceDao(): SourceDao
    abstract fun filterRuleDao(): FilterRuleDao
}
