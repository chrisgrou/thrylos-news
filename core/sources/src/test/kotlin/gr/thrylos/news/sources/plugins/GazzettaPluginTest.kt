package gr.thrylos.news.sources.plugins

import gr.thrylos.news.model.ArticleStub
import gr.thrylos.news.model.ContentBlock
import gr.thrylos.news.sources.discovery.HtmlListDiscovery
import gr.thrylos.news.sources.extract.ArticleExtractor
import gr.thrylos.news.sources.http.HttpFetcher
import gr.thrylos.news.sources.testutil.Fixtures
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

/** Verifies the shipped gazzetta plugin against real page snapshots. gazzetta.gr
 * has no RSS at all, so discovery is html-list against the team page. */
class GazzettaPluginTest {

    private lateinit var server: MockWebServer

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    private fun shippedPlugin() = PluginTestSupport.shippedPlugin("gazzetta.json")

    @Test
    fun `discovers article stubs from the real team page`() {
        server.enqueue(MockResponse().setBody(Fixtures.read("gazzetta-list.html")))
        val plugin = shippedPlugin().let { it.copy(discovery = it.discovery.copy(url = server.url("/teams/olympiakos").toString())) }

        val stubs = HtmlListDiscovery().discover(plugin, HttpFetcher())

        assertEquals(25, stubs.size, "there are 25 article.is-flex.mb-32 teasers on the page, capped by maxItems")
        val target = stubs.firstOrNull { "2563531" in it.url }
        assertTrue(target != null, "expected to find the Mæhle article, got: ${stubs.map { it.url }}")
        assertEquals("Πλησιάζει στο Λιμάνι ο Μέλε, όπως μεταδίδουν στη Δανία", target!!.title)
    }

    @Test
    fun `extracts a real gazzetta article cleanly`() {
        server.enqueue(MockResponse().setBody(Fixtures.read("gazzetta-article.html")))
        val plugin = shippedPlugin()
        val url = server.url("/football/superleague/2563531/mele-olympiakos-plisiazei-sto-limani-opos-metadidoyn-sti-hora-toy").toString()

        val article = ArticleExtractor(HttpFetcher()).extract(plugin, ArticleStub(plugin.id, url, "(stub)"))

        assertFalse(article.usedFallbackExtraction)
        assertEquals("Μέλε - Ολυμπιακός: Πλησιάζει στο Λιμάνι, όπως μεταδίδουν στη χώρα του", article.title)
        assertEquals("Δημήτρης Τομαράς", article.author)
        assertEquals(Instant.parse("2026-08-24T14:28:54Z").toEpochMilli(), article.publishedAt)
        assertTrue(article.leadImageUrl!!.contains("mele1_1.jpg"), "unexpected lead image: ${article.leadImageUrl}")

        val paragraphs = article.content.filterIsInstance<ContentBlock.Paragraph>()
        assertTrue(paragraphs.size >= 2, "expected the article body, got ${paragraphs.size} paragraphs")
        val text = paragraphs.joinToString(" ") { it.text }
        assertTrue(text.contains("Ο Μέλε είναι ένα γνωστό"))
        // "Οι ομάδες που έχει παίξει" is a legitimate h2 heading further down the
        // real body — but the removed .summary-list table-of-contents links to
        // that same heading with identical text (inside a <ul>), so if it weren't
        // stripped the phrase would appear twice across the extracted blocks.
        val allText = article.content.joinToString(" ") { block ->
            when (block) {
                is ContentBlock.Paragraph -> block.text
                is ContentBlock.Heading -> block.text
                is ContentBlock.Quote -> block.text
                is ContentBlock.ListBlock -> block.items.joinToString(" ")
                is ContentBlock.Image -> block.caption.orEmpty()
                is ContentBlock.Video -> block.caption.orEmpty()
            }
        }
        val occurrences = Regex(Regex.escape("Οι ομάδες που έχει παίξει")).findAll(allText).count()
        assertEquals(1, occurrences, "summary-list duplicate not stripped")
    }

    /** gazzetta.gr turns out to have (at least) two distinct article templates: regular
     *  team-page articles use `div.content.is-relative` for the whole body in one
     *  container, but its long-form "specials" features (this fixture, a real
     *  gazzetta.gr/specials/... page) build the body from several independent
     *  Drupal "paragraph" blocks with no shared body-only wrapper — the closest common
     *  ancestor is the whole `div.node--type-special-article`, header/hero/byline
     *  included. Reported as "the article cuts off partway through" — the old
     *  `div.content.is-relative` selector didn't match this template at all, so
     *  extraction silently fell back to Readability, which stopped short of the
     *  later paragraph/image/quote blocks. */
    @Test
    fun `extracts a real gazzetta specials article past the old truncation point`() {
        server.enqueue(MockResponse().setBody(Fixtures.read("gazzetta-specials-article.html")))
        val plugin = shippedPlugin()
        val url = server.url("/specials/2565625/o-gordito-poy-egine-poyerta-i-amfisbitisi-kai-ta-dakrya-eftiaxan-ti-metagrafi").toString()

        val article = ArticleExtractor(HttpFetcher()).extract(plugin, ArticleStub(plugin.id, url, "(stub)"))

        assertFalse(article.usedFallbackExtraction, "div.node--type-special-article should now match directly, no Readability fallback needed")
        assertEquals(
            "Ο «Gordito» που έγινε Πουέρτα: Η αμφισβήτηση και τα δάκρυα έφτιαξαν τη μεταγραφή-ρεκόρ του Ολυμπιακού",
            article.title,
        )
        assertEquals("Νότης Χάλαρης", article.author, "author markup here is <a class=authoring__author>, not <span>")
        assertTrue(article.leadImageUrl!!.contains("puerta_osfp_article.jpg"), "unexpected lead image: ${article.leadImageUrl}")

        val allText = article.content.joinToString(" ") { block ->
            when (block) {
                is ContentBlock.Paragraph -> block.text
                is ContentBlock.Heading -> block.text
                is ContentBlock.Quote -> block.text
                is ContentBlock.ListBlock -> block.items.joinToString(" ")
                is ContentBlock.Image -> block.caption.orEmpty()
                is ContentBlock.Video -> block.caption.orEmpty()
            }
        }
        assertTrue(
            allText.contains("άρχισε τη δική του πορεία"),
            "missing text right at the old truncation point",
        )
        assertTrue(
            allText.contains("Είχε μία σφαίρα στο πόδι για τέσσερα χρόνια"),
            "missing the pull-quote heading from a later paragraph block, past the old cutoff",
        )
        assertFalse(allText.contains("BEST OF"), "leaked the 'BEST OF INTERNET' recommended-content widget")
        assertFalse(allText.contains(article.title), "duplicated the headline into the body")
        assertFalse(allText.contains("Νότης Χάλαρης"), "duplicated the byline into the body")
    }
}
