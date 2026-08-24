package gr.thrylos.news.model

import kotlinx.serialization.Serializable

/**
 * A single piece of extracted article content, normalized to a common shape
 * regardless of which source it came from. The reader UI renders these blocks
 * directly instead of any source-specific HTML/CSS, which is what makes every
 * article look the same and keeps ads/widgets out (anything that doesn't map
 * to one of these cases is simply dropped during extraction).
 */
@Serializable
sealed class ContentBlock {
    @Serializable
    data class Heading(val text: String, val level: Int = 2) : ContentBlock()

    @Serializable
    data class Paragraph(val text: String) : ContentBlock()

    @Serializable
    data class Image(val url: String, val caption: String? = null) : ContentBlock()

    @Serializable
    data class Quote(val text: String, val attribution: String? = null) : ContentBlock()

    @Serializable
    data class ListBlock(val items: List<String>, val ordered: Boolean = false) : ContentBlock()
}
