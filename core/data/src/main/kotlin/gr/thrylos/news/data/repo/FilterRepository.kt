package gr.thrylos.news.data.repo

import gr.thrylos.news.data.db.dao.FilterRuleDao
import gr.thrylos.news.data.db.entity.FilterRuleEntity
import gr.thrylos.news.model.FilterAction
import gr.thrylos.news.model.FilterField
import gr.thrylos.news.model.FilterMatch
import gr.thrylos.news.model.FilterRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilterRepository @Inject constructor(
    private val dao: FilterRuleDao,
) {
    fun observeAll(): Flow<List<FilterRule>> = dao.observeAll().map { list -> list.map(::toDomain) }

    suspend fun getEnabled(): List<FilterRule> = dao.getEnabled().map(::toDomain)

    suspend fun upsert(rule: FilterRule) = dao.upsert(toEntity(rule))

    suspend fun delete(rule: FilterRule) = dao.delete(toEntity(rule))

    private fun toEntity(rule: FilterRule) = FilterRuleEntity(
        id = rule.id,
        field = rule.field.name,
        match = rule.match.name,
        value = rule.value,
        caseSensitive = rule.caseSensitive,
        action = rule.action.name,
        scopeSourceId = rule.scopeSourceId,
        enabled = rule.enabled,
    )

    private fun toDomain(entity: FilterRuleEntity) = FilterRule(
        id = entity.id,
        field = FilterField.valueOf(entity.field),
        match = FilterMatch.valueOf(entity.match),
        value = entity.value,
        caseSensitive = entity.caseSensitive,
        action = FilterAction.valueOf(entity.action),
        scopeSourceId = entity.scopeSourceId,
        enabled = entity.enabled,
    )
}
