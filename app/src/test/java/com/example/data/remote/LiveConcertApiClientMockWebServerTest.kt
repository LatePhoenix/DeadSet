package com.example.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * 6.7: MockWebServer coverage for Bandsintown + Ticketmaster proxy paths.
 * setlist.fm client is not built yet (D3) — covered here when that lands.
 */
class LiveConcertApiClientMockWebServerTest {

    private lateinit var server: MockWebServer
    private lateinit var http: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        http = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun client(
        bandsintown: Boolean = true,
        ticketmaster: Boolean = false
    ): LiveConcertApiClient {
        val base = server.url("/").toString()
        return LiveConcertApiClient(
            http = http,
            bandsintownBaseUrl = base,
            bandsintownAppIdOverride = if (bandsintown) "test-app" else "",
            ticketmasterAccessOverride = if (ticketmaster) {
                TicketmasterAccess(
                    mode = TicketmasterAccess.Mode.PROXY,
                    baseUrl = base,
                    apiKey = "",
                    clientToken = "tok"
                )
            } else {
                TicketmasterAccess(TicketmasterAccess.Mode.OFF, "", "", "")
            }
        )
    }

    @Test
    fun `bandsintown success returns upcoming event`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                [{
                  "id":"42","title":"","datetime":"2099-06-01T20:00:00","url":"https://x",
                  "venue":{"name":"Martyrs'","city":"Chicago","region":"IL","country":"US",
                    "street_address":"3855 N Lincoln","latitude":"41.95","longitude":"-87.69"},
                  "offers":[{"type":"Tickets","url":"https://t","status":"available"}]
                }]
                """.trimIndent()
            )
        )
        val result = client(bandsintown = true, ticketmaster = false)
            .fetchEventsForBands(listOf("Terrapin Flyer"))
        assertTrue(result.sourcesReached.contains("Bandsintown"))
        assertEquals(1, result.events.size)
        assertEquals("Chicago", result.events.single().city)
    }

    @Test
    fun `bandsintown empty list is a reached empty source`() = runBlocking {
        server.enqueue(MockResponse().setBody("[]"))
        val result = client().fetchEventsForBands(listOf("Terrapin Flyer"))
        assertTrue(result.sourcesReached.contains("Bandsintown"))
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun `http error does not mark source reached`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("oops"))
        val result = client().fetchEventsForBands(listOf("Terrapin Flyer"))
        assertFalse(result.sourcesReached.contains("Bandsintown"))
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun `malformed json does not mark source reached`() = runBlocking {
        server.enqueue(MockResponse().setBody("{not-json"))
        val result = client().fetchEventsForBands(listOf("Terrapin Flyer"))
        assertFalse(result.sourcesReached.contains("Bandsintown"))
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun `ticketmaster proxy success parses events`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {"_embedded":{"events":[{
                  "id":"tm1","name":"Dark Star Orchestra","url":"https://tm",
                  "dates":{"start":{"localDate":"2099-07-04","localTime":"19:30:00"}},
                  "_embedded":{"venues":[{
                    "name":"The Vic","city":{"name":"Chicago"},"state":{"stateCode":"IL"},
                    "address":{"line1":"3145 N Sheffield"},
                    "location":{"latitude":"41.94","longitude":"-87.65"}
                  }]}
                }]}}
                """.trimIndent()
            )
        )
        val result = client(bandsintown = false, ticketmaster = true)
            .fetchEventsForBands(listOf("Dark Star Orchestra"))
        assertTrue(result.sourcesReached.contains("Ticketmaster"))
        assertEquals(1, result.events.size)
        assertEquals("The Vic", result.events.single().venueName)
    }
}
