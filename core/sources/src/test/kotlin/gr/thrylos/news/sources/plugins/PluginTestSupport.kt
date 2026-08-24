package gr.thrylos.news.sources.plugins

import gr.thrylos.news.sources.plugin.PluginParseResult
import gr.thrylos.news.sources.plugin.PluginParser
import gr.thrylos.news.sources.plugin.SourcePlugin
import java.io.File

/** Loads a plugin the app actually ships (from app/src/main/assets/plugins),
 * so plugin-verification tests exercise the real file and can't drift from it. */
object PluginTestSupport {
    fun shippedPlugin(fileName: String): SourcePlugin {
        val file = File("../../app/src/main/assets/plugins/$fileName")
        check(file.exists()) { "Δεν βρέθηκε το bundled plugin: ${file.absolutePath}" }
        return when (val result = PluginParser.parse(file.readText())) {
            is PluginParseResult.Success -> result.plugin
            is PluginParseResult.Failure -> throw AssertionError("Άκυρο plugin '$fileName': ${result.errors}")
        }
    }
}
