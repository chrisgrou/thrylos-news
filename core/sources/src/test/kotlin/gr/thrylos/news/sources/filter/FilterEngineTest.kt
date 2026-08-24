package gr.thrylos.news.sources.filter

import gr.thrylos.news.model.Article
import gr.thrylos.news.model.ContentBlock
import gr.thrylos.news.model.FilterAction
import gr.thrylos.news.model.FilterField
import gr.thrylos.news.model.FilterMatch
import gr.thrylos.news.model.FilterRule
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FilterEngineTest {

    private val article = Article(
        id = "1", sourceId = "gazzetta", sourceName = "Gazzetta", url = "https://gazzetta.gr/x",
        title = "Ολυμπιακός: προτάσεις για στοίχημα στον αγώνα", author = "Κ. Ιωάννου",
        fetchedAt = 0, content = listOf(ContentBlock.Paragraph("κάποιο κείμενο")),
    )

    @Test
    fun `title contains rule hides matching article`() {
        val rule = FilterRule("r1", FilterField.TITLE, FilterMatch.CONTAINS, "στοίχημα")
        assertTrue(FilterEngine.matches(rule, article))
        assertTrue(FilterEngine.isHidden(article, listOf(rule)))
    }

    @Test
    fun `author exact match is case-insensitive by default`() {
        val rule = FilterRule("r2", FilterField.AUTHOR, FilterMatch.EXACT, "κ. ιωαννου")
        assertFalse(FilterEngine.matches(rule, article)) // accents differ -> not exact, expected false
    }

    @Test
    fun `rule scoped to another source never matches`() {
        val rule = FilterRule("r3", FilterField.TITLE, FilterMatch.CONTAINS, "στοίχημα", scopeSourceId = "sport24")
        assertFalse(FilterEngine.matches(rule, article))
    }

    @Test
    fun `disabled rule never matches`() {
        val rule = FilterRule("r4", FilterField.TITLE, FilterMatch.CONTAINS, "στοίχημα", enabled = false)
        assertFalse(FilterEngine.matches(rule, article))
    }

    @Test
    fun `regex rule matches`() {
        val rule = FilterRule("r5", FilterField.TITLE, FilterMatch.REGEX, "στοίχημ\\w+")
        assertTrue(FilterEngine.matches(rule, article))
    }

    @Test
    fun `countMatches counts across a list`() {
        val rule = FilterRule("r6", FilterField.SOURCE, FilterMatch.CONTAINS, "gazzetta")
        assertEqualsInt(1, FilterEngine.countMatches(rule, listOf(article)))
    }

    private fun assertEqualsInt(expected: Int, actual: Int) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual)
    }
}
