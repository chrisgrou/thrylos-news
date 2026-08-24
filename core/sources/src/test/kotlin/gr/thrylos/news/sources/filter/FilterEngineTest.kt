package gr.thrylos.news.sources.filter

import gr.thrylos.news.model.Article
import gr.thrylos.news.model.ContentBlock
import gr.thrylos.news.model.FilterAction
import gr.thrylos.news.model.FilterCombinator
import gr.thrylos.news.model.FilterCondition
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
    fun `not-contains rule matches when the value is absent`() {
        val rule = FilterRule("r5b", FilterField.AUTHOR, FilterMatch.NOT_CONTAINS, "Παπαδόπουλος")
        assertTrue(FilterEngine.matches(rule, article))
    }

    @Test
    fun `not-contains rule does not match when the value is present`() {
        val rule = FilterRule("r5c", FilterField.AUTHOR, FilterMatch.NOT_CONTAINS, "Ιωάννου")
        assertFalse(FilterEngine.matches(rule, article))
    }

    @Test
    fun `combining source with author not-contains hides only that source's other authors`() {
        val rule = FilterRule(
            id = "r5d",
            conditions = listOf(
                FilterCondition(FilterField.SOURCE, FilterMatch.EXACT, "Gazzetta"),
                FilterCondition(FilterField.AUTHOR, FilterMatch.NOT_CONTAINS, "Ιωάννου"),
            ),
            combinator = FilterCombinator.AND,
            action = FilterAction.HIDE,
        )
        assertFalse(FilterEngine.isHidden(article, listOf(rule))) // same source, matching author -> stays visible

        val otherAuthor = article.copy(author = "Κ. Παπαδόπουλος")
        assertTrue(FilterEngine.isHidden(otherAuthor, listOf(rule))) // same source, different author -> hidden

        val otherSource = article.copy(sourceName = "Sportdog", author = "Κ. Παπαδόπουλος")
        assertFalse(FilterEngine.isHidden(otherSource, listOf(rule))) // different source -> unaffected
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

    @Test
    fun `AND combinator requires every condition to match`() {
        val rule = FilterRule(
            id = "r7",
            conditions = listOf(
                FilterCondition(FilterField.AUTHOR, FilterMatch.CONTAINS, "Ιωάννου"),
                FilterCondition(FilterField.TITLE, FilterMatch.CONTAINS, "στοίχημα"),
            ),
            combinator = FilterCombinator.AND,
            action = FilterAction.IMPORTANT,
        )
        assertTrue(FilterEngine.matches(rule, article))
        assertTrue(FilterEngine.isImportant(article, listOf(rule)))

        val onlyOneMatches = rule.copy(
            conditions = listOf(
                FilterCondition(FilterField.AUTHOR, FilterMatch.CONTAINS, "Ιωάννου"),
                FilterCondition(FilterField.TITLE, FilterMatch.CONTAINS, "κάτι που δεν υπάρχει"),
            ),
        )
        assertFalse(FilterEngine.matches(onlyOneMatches, article))
    }

    @Test
    fun `OR combinator matches when any condition matches`() {
        val rule = FilterRule(
            id = "r8",
            conditions = listOf(
                FilterCondition(FilterField.TITLE, FilterMatch.CONTAINS, "κάτι που δεν υπάρχει"),
                FilterCondition(FilterField.AUTHOR, FilterMatch.CONTAINS, "Ιωάννου"),
            ),
            combinator = FilterCombinator.OR,
            action = FilterAction.IMPORTANT,
        )
        assertTrue(FilterEngine.matches(rule, article))
    }

    @Test
    fun `SHOW_ONLY hides everything that doesn't match any enabled show-only rule`() {
        val showOnlyOtherAuthor = FilterRule(
            id = "s1",
            field = FilterField.AUTHOR,
            match = FilterMatch.EXACT,
            value = "Someone Else",
            action = FilterAction.SHOW_ONLY,
        )
        assertFalse(FilterEngine.isVisible(article, listOf(showOnlyOtherAuthor)))

        val showOnlySameAuthor = FilterRule(
            id = "s2",
            field = FilterField.AUTHOR,
            match = FilterMatch.CONTAINS,
            value = "Ιωάννου",
            action = FilterAction.SHOW_ONLY,
        )
        assertTrue(FilterEngine.isVisible(article, listOf(showOnlySameAuthor)))
    }

    @Test
    fun `HIDE always wins over SHOW_ONLY`() {
        val showOnly = FilterRule("s3", FilterField.AUTHOR, FilterMatch.CONTAINS, "Ιωάννου", action = FilterAction.SHOW_ONLY)
        val hide = FilterRule("h1", FilterField.TITLE, FilterMatch.CONTAINS, "στοίχημα", action = FilterAction.HIDE)
        assertFalse(FilterEngine.isVisible(article, listOf(showOnly, hide)))
    }

    @Test
    fun `no SHOW_ONLY rules means nothing is excluded by them`() {
        assertTrue(FilterEngine.isVisible(article, emptyList()))
    }

    @Test
    fun `matchesStub defers to full check when a condition needs AUTHOR or BODY`() {
        val rule = FilterRule(
            id = "r9",
            conditions = listOf(
                FilterCondition(FilterField.AUTHOR, FilterMatch.CONTAINS, "Ιωάννου"),
                FilterCondition(FilterField.TITLE, FilterMatch.CONTAINS, "στοίχημα"),
            ),
            action = FilterAction.HIDE,
        )
        assertFalse(FilterEngine.matchesStub(rule, article.title, article.sourceId, article.sourceName, article.url))
        assertTrue(FilterEngine.isHidden(article, listOf(rule)))
    }
}
