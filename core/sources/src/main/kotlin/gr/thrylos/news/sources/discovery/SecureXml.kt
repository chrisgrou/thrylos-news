package gr.thrylos.news.sources.discovery

import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

/**
 * A [DocumentBuilder] that refuses to resolve any external entity or DTD (XXE
 * prevention — blocks SSRF and local file reads via a hostile feed), while still
 * allowing a `<!DOCTYPE>` that only defines internal entities — a common, benign
 * WordPress RSS quirk (e.g. defining `&nbsp;`).
 *
 * This uses an [org.xml.sax.EntityResolver] rather than the SAX feature flags
 * (`external-general-entities` etc.) because Android's bundled XML parser doesn't
 * support setting those features — only the desktop JVM's Xerces does, which made
 * that approach pass local unit tests while failing on-device.
 */
fun secureDocumentBuilder(): DocumentBuilder {
    val builder = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = false
    }.newDocumentBuilder()
    builder.setEntityResolver { _, _ -> InputSource(StringReader("")) }
    return builder
}
