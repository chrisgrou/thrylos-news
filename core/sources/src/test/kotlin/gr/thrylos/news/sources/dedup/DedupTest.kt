package gr.thrylos.news.sources.dedup

import gr.thrylos.news.model.Article
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class DedupTest {

    private fun article(id: String, title: String, publishedAt: Long) = Article(
        id = id, sourceId = "s", sourceName = "S", url = "https://x/$id",
        title = title, publishedAt = publishedAt, fetchedAt = publishedAt, content = emptyList(),
    )

    @Test
    fun `near-identical titles within window are grouped`() {
        val a = article("a", "Ολυμπιακός: νίκη στο ΣΕΦ για την πρόκριση", 1_000_000)
        val b = article("b", "Ολυμπιακός: η νίκη στο ΣΕΦ φέρνει την πρόκριση", 1_050_000)
        val groups = Dedup.groupDuplicates(listOf(a, b))
        assertEquals(groups["a"], groups["b"])
    }

    @Test
    fun `unrelated titles are not grouped`() {
        val a = article("a", "Ολυμπιακός: νίκη στο ΣΕΦ για την πρόκριση", 1_000_000)
        val b = article("b", "Εισιτήρια: πότε ανοίγει η προπώληση", 1_050_000)
        val groups = Dedup.groupDuplicates(listOf(a, b))
        assertNotEquals(groups["a"], groups["b"])
    }

    @Test
    fun `similar titles outside the time window are not grouped`() {
        val a = article("a", "Ολυμπιακός: νίκη στο ΣΕΦ για την πρόκριση", 0)
        val b = article("b", "Ολυμπιακός: η νίκη στο ΣΕΦ φέρνει την πρόκριση", 48 * 60 * 60 * 1000L)
        val groups = Dedup.groupDuplicates(listOf(a, b))
        assertNotEquals(groups["a"], groups["b"])
    }
}
