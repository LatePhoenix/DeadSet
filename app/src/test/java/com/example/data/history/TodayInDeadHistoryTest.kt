package com.example.data.history

import com.example.data.local.ShowEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayInDeadHistoryTest {

    @Test
    fun `monthDayFromIso extracts MM-dd`() {
        assertEquals("05-08", monthDayFromIso("1977-05-08"))
        assertEquals("11-23", monthDayFromIso("2026-11-23"))
    }

    @Test
    fun `showsOnMonthDay returns matching catalog rows only`() {
        val shows = listOf(
            show("1977-05-08-cornell", "1977-05-08", "Barton Hall"),
            show("1978-05-08-other", "1978-05-08", "Field House"),
            show("1977-05-09-next", "1977-05-09", "Somewhere")
        )
        val matches = showsOnMonthDay(shows, "05-08")
        assertEquals(listOf("1977-05-08-cornell", "1978-05-08-other"), matches.map { it.id })
    }

    @Test
    fun `showsOnTodayInHistory empty when no month-day match`() {
        val shows = listOf(show("x", "1977-01-01", "Venue"))
        assertTrue(showsOnTodayInHistory(shows, todayIso = "2026-11-23").isEmpty())
    }

    private fun show(id: String, date: String, venue: String) = ShowEntity(
        id = id,
        date = date,
        venue = venue,
        city = "City",
        stateOrCountry = "CA",
        eraId = "",
        tourName = "",
        bandName = "Grateful Dead"
    )
}
