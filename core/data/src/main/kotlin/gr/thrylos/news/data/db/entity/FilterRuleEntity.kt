package gr.thrylos.news.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "filter_rules")
data class FilterRuleEntity(
    @PrimaryKey val id: String,
    val field: String,
    val match: String,
    val value: String,
    val caseSensitive: Boolean,
    val action: String,
    val scopeSourceId: String?,
    val enabled: Boolean,
)
