package gr.thrylos.news.data.repo

import gr.thrylos.news.data.db.dao.FilterRuleDao
import gr.thrylos.news.data.db.entity.FilterRuleEntity
import gr.thrylos.news.model.FilterAction
import gr.thrylos.news.model.FilterCombinator
import gr.thrylos.news.model.FilterCondition
import gr.thrylos.news.model.FilterRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilterRepository @Inject constructor(
    private val dao: FilterRuleDao,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val conditionsSerializer = ListSerializer(FilterCondition.serializer())

    companion object {
        /** Id prefix given to every rule shipped in assets/filters/bundled_filters.json,
         *  so they can be found/removed as a group without a separate DB column. */
        const val BUNDLED_ID_PREFIX = "bundled-"
    }

    fun observeAll(): Flow<List<FilterRule>> = dao.observeAll().map { list -> list.map(::toDomain) }

    suspend fun getEnabled(): List<FilterRule> = dao.getEnabled().map(::toDomain)

    suspend fun upsert(rule: FilterRule) = dao.upsert(toEntity(rule))

    suspend fun delete(rule: FilterRule) = dao.delete(toEntity(rule))

    /** Whether any bundled ("Προτεινόμενα") filter rule is currently imported. */
    suspend fun hasBundled(): Boolean = dao.countByIdPrefix(BUNDLED_ID_PREFIX) > 0

    /** Imports every rule in [rawJson] (a JSON array of [FilterRule], shipped as an app
     *  asset), upserting each — a rule already present with the same id is updated in
     *  place rather than duplicated. Returns how many were imported. */
    suspend fun importBundled(rawJson: String): Int {
        val rules = runCatching { json.decodeFromString(ListSerializer(FilterRule.serializer()), rawJson) }.getOrDefault(emptyList())
        rules.forEach { upsert(it) }
        return rules.size
    }

    /** Removes every rule whose id carries the bundled-rule prefix — the "toggle off"
     *  half of the bundled-filters switch in Settings → Δεδομένα. Any other rule the
     *  user created themselves is untouched. */
    suspend fun removeBundled(): Int = dao.deleteByIdPrefix(BUNDLED_ID_PREFIX)

    private fun toEntity(rule: FilterRule) = FilterRuleEntity(
        id = rule.id,
        conditionsJson = json.encodeToString(conditionsSerializer, rule.conditions),
        combinator = rule.combinator.name,
        action = rule.action.name,
        scopeSourceId = rule.scopeSourceId,
        enabled = rule.enabled,
    )

    private fun toDomain(entity: FilterRuleEntity) = FilterRule(
        id = entity.id,
        conditions = runCatching { json.decodeFromString(conditionsSerializer, entity.conditionsJson) }.getOrDefault(emptyList()),
        combinator = runCatching { FilterCombinator.valueOf(entity.combinator) }.getOrDefault(FilterCombinator.AND),
        action = FilterAction.valueOf(entity.action),
        scopeSourceId = entity.scopeSourceId,
        enabled = entity.enabled,
    )
}
