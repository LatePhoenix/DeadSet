package com.example.data.alerts

import com.example.data.local.ShowEntity
import com.example.data.remote.LiveConcertEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConcertAlertRulesTest {

    private fun event(id: String, date: String, band: String = "Terrapin Flyer") = LiveConcertEvent(
        id = id, bandName = band, eventTitle = "", date = date, displayDate = date, venueName = "The Vic",
        city = "Chicago", stateCode = "IL", venueAddress = "", ticketUrl = ""
    )

    private fun show(id: String, date: String) = ShowEntity(
        id = id, date = date, venue = "Martyrs'", city = "Chicago", stateOrCountry = "IL",
        eraId = "all", tourName = "", bandName = "Terrapin Flyer"
    )

    @Test
    fun `only dates not seen before are new, and the first sync never alerts`() {
        val after = listOf(event("b", "2099-02-01"), event("a", "2099-01-01"), event("c", "2099-01-15"))
        assertEquals(listOf("c", "b"), ConcertAlertRules.newEvents(setOf("a"), after, firstSync = false).map { it.id })
        assertTrue(ConcertAlertRules.newEvents(emptySet(), after, firstSync = true).isEmpty())
    }

    @Test
    fun `tonight reminder fires once per day for shows dated today`() {
        val shows = listOf(show("today", "2026-12-31"), show("later", "2027-01-01"))
        assertEquals(listOf("today"), ConcertAlertRules.tonightShows(shows, "2026-12-31", lastReminderDate = "").map { it.id })
        assertTrue(ConcertAlertRules.tonightShows(shows, "2026-12-31", lastReminderDate = "2026-12-31").isEmpty())
    }

    @Test
    fun `notification text`() {
        val one = listOf(event("a", "2099-01-01"))
        assertEquals("New date: Terrapin Flyer", ConcertAlertRules.newDatesTitle(one))
        assertEquals("Terrapin Flyer · 2099-01-01 · The Vic, Chicago", ConcertAlertRules.newDatesBody(one))
        val five = (1..5).map { event("e$it", "2099-01-0$it") }
        assertEquals("5 new dates from bands you follow", ConcertAlertRules.newDatesTitle(five))
        assertTrue(ConcertAlertRules.newDatesBody(five).endsWith("+2 more"))
        assertEquals("Tonight: Terrapin Flyer", ConcertAlertRules.tonightTitle(listOf(show("x", "2026-12-31"))))
    }
}
