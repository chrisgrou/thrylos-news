package gr.thrylos.news.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "filter_rules")
data class FilterRuleEntity(
    @PrimaryKey val id: String,
    /** JSON-encoded List<FilterCondition>. */
    val conditionsJson: String,
    val combinator: String,
    val action: String,
    val scopeSourceId: String?,
    val enabled: Boolean,
)
