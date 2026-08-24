package gr.thrylos.news.sources.plugin

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

sealed class PluginParseResult {
    data class Success(val plugin: SourcePlugin) : PluginParseResult()
    data class Failure(val errors: List<String>) : PluginParseResult()
}

/**
 * Parses and validates a plugin JSON file. Kept separate from [SourcePlugin]
 * itself so that error messages are human-readable — this is the file a user
 * sees when an imported plugin fails, so it needs to say *why*, not just throw.
 */
object PluginParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(rawJson: String): PluginParseResult {
        val plugin = try {
            json.decodeFromString(SourcePlugin.serializer(), rawJson)
        } catch (e: SerializationException) {
            return PluginParseResult.Failure(listOf("Μη έγκυρο JSON: ${e.message}"))
        } catch (e: IllegalArgumentException) {
            return PluginParseResult.Failure(listOf("Μη έγκυρο plugin: ${e.message}"))
        }

        val errors = validate(plugin)
        return if (errors.isEmpty()) PluginParseResult.Success(plugin) else PluginParseResult.Failure(errors)
    }

    private fun validate(plugin: SourcePlugin): List<String> {
        val errors = mutableListOf<String>()

        if (plugin.schemaVersion > PLUGIN_SCHEMA_VERSION) {
            errors += "Το plugin απαιτεί νεότερη έκδοση της εφαρμογής (schemaVersion=${plugin.schemaVersion})."
        }
        if (plugin.id.isBlank()) errors += "Λείπει το πεδίο 'id'."
        if (!plugin.id.matches(Regex("^[a-z0-9][a-z0-9-]*$"))) {
            errors += "Το 'id' πρέπει να είναι λατινικά πεζά, ψηφία και παύλες (π.χ. 'gazzetta-olympiacos')."
        }
        if (plugin.name.isBlank()) errors += "Λείπει το πεδίο 'name'."
        if (plugin.discovery.url.isBlank()) errors += "Λείπει το discovery.url."
        if (plugin.discovery.type == DiscoveryType.HTML_LIST && plugin.listSelectors == null) {
            errors += "Το discovery.type='html-list' απαιτεί 'listSelectors'."
        }
        if (plugin.article.title.isBlank()) errors += "Λείπει το article.title selector."
        if (plugin.article.content.isBlank()) errors += "Λείπει το article.content selector."

        return errors
    }
}
