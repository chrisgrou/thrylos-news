package gr.thrylos.news.sources.matches

import gr.thrylos.news.model.MatchStatus
import gr.thrylos.news.sources.http.HttpFetcher
import gr.thrylos.news.sources.testutil.Fixtures
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Verifies parsing against a real response from Sofascore's team-events API
 *  (api.sofascore.com/api/v1/team/3245/events/next/0 — Olympiacos FC). */
class SofascoreMatchesFetcherTest {

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

    @Test
    fun `parses real Sofascore fixtures response`() {
        server.enqueue(MockResponse().setBody(Fixtures.read("sofascore-olympiacos-events-next.json")))
        val fetcher = SofascoreMatchesFetcher(HttpFetcher(), baseUrl = server.url("/").toString().trimEnd('/'))

        val matches = fetcher.fetchUpcoming("3245")

        assertEquals(30, matches.size)
        val first = matches.first()
        assertEquals("football", first.sport)
        assertEquals("Stoiximan Super League", first.competition)
        assertEquals("Asteras Aktor", first.homeTeam)
        assertEquals("Olympiacos FC", first.awayTeam)
        assertEquals("M", first.gender)
        assertTrue(first.homeTeamLogoUrl.contains("6342"), "unexpected home logo url: ${first.homeTeamLogoUrl}")
        assertTrue(first.awayTeamLogoUrl.contains("3245"), "unexpected away logo url: ${first.awayTeamLogoUrl}")
        assertEquals(MatchStatus.NOT_STARTED, first.status)
        assertEquals(1788112800_000L, first.kickoffAt)
        assertTrue(first.matchUrl.contains("16559910"), "unexpected match url: ${first.matchUrl}")
        assertEquals("3245", first.teamId)
        assertTrue(!first.isHome, "Olympiacos FC (the queried team) is the away side in this fixture")
        // sorted soonest-first
        assertTrue(matches.zipWithNext().all { (a, b) -> a.kickoffAt <= b.kickoffAt })
    }
}
