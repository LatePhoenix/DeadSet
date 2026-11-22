package com.example.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveConcertJsonTest {

    @Test
    fun `bandsintown parses venue date and ticket offer`() {
        val json = """
            [{
              "id": "42",
              "title": "",
              "datetime": "2099-06-01T20:00:00",
              "description": "Summer run",
              "url": "https://bandsintown.com/e/42",
              "lineup": ["Terrapin Flyer"],
              "venue": {
                "name": "Martyrs'",
                "city": "Chicago",
                "region": "IL",
                "country": "US",
                "street_address": "3855 N Lincoln",
                "latitude": "41.95",
                "longitude": "-87.69"
              },
              "offers": [{"type": "Tickets", "url": "https://tickets.example/42", "status": "available"}]
            }]
        """.trimIndent()

        val events = LiveConcertJson.parseBandsintownEvents(json, "Terrapin Flyer")
        assertEquals(1, events.size)
        val e = events.single()
        assertEquals("bit-42", e.id)
        assertEquals("Terrapin Flyer", e.bandName)
        assertEquals("2099-06-01", e.date)
        assertEquals("Martyrs'", e.venueName)
        assertEquals("Chicago", e.city)
        assertEquals("IL", e.stateCode)
        assertEquals("https://tickets.example/42", e.ticketUrl)
        assertEquals(41.95, e.latitude!!, 0.001)
        assertTrue(e.eventTitle.contains("Chicago"))
    }

    @Test
    fun `ticketmaster parses embedded venue and price range`() {
        val json = """
            {
              "_embedded": {
                "events": [{
                  "id": "tm1",
                  "name": "Dark Star Orchestra",
                  "url": "https://ticketmaster.com/event/tm1",
                  "info": "Night one",
                  "dates": { "start": { "localDate": "2099-07-04", "localTime": "19:30:00" } },
                  "priceRanges": [{ "min": 35.0, "max": 75.0, "currency": "USD" }],
                  "_embedded": {
                    "venues": [{
                      "name": "The Vic",
                      "city": { "name": "Chicago" },
                      "state": { "stateCode": "IL" },
                      "address": { "line1": "3145 N Sheffield" },
                      "location": { "latitude": "41.94", "longitude": "-87.65" }
                    }]
                  }
                }]
              }
            }
        """.trimIndent()

        val events = LiveConcertJson.parseTicketmasterEvents(json, "Dark Star Orchestra")
        assertEquals(1, events.size)
        val e = events.single()
        assertEquals("tm-tm1", e.id)
        assertEquals("Dark Star Orchestra", e.bandName)
        assertEquals("2099-07-04", e.date)
        assertEquals("The Vic", e.venueName)
        assertEquals("Chicago", e.city)
        assertEquals("$35 – $75", e.priceRange)
        assertEquals("https://ticketmaster.com/event/tm1", e.ticketUrl)
        assertEquals("Night one", e.tourNote)
    }

    @Test
    fun `ticketmaster empty embedded yields empty list`() {
        assertTrue(LiveConcertJson.parseTicketmasterEvents("{}", "X").isEmpty())
    }

    @Test
    fun `JSON null url falls back instead of literal null string`() {
        val json = """
            [{
              "id": "9",
              "datetime": "2099-01-01T20:00:00",
              "url": null,
              "lineup": ["Terrapin Flyer"],
              "venue": { "name": "V", "city": "C", "region": "IL", "country": "US" },
              "offers": [{"url": null}]
            }]
        """.trimIndent()
        val e = LiveConcertJson.parseBandsintownEvents(json, "Terrapin Flyer").single()
        assertEquals("https://www.bandsintown.com/e/9", e.ticketUrl)
    }
}
