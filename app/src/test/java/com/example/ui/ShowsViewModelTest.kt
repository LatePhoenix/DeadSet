package com.example.ui

import com.example.data.local.ShowEntity
import com.example.data.model.DeadEra
import com.example.data.model.SongEntry
import com.example.data.model.TransitionType
import org.junit.Assert.assertEquals
import org.junit.Test

class ShowsViewModelTest {

    @Test
    fun `applyFilters matches query venue city and setlist song`() {
        val shows = listOf(
            show("1", "1977-05-08", venue = "Barton Hall", city = "Ithaca"),
            show("2", "1978-01-01", venue = "Winterland", city = "SF")
        )
        val songs = mapOf(
            "1" to listOf(SongEntry("Scarlet Begonias", transitionOut = TransitionType.NONE)),
            "2" to listOf(SongEntry("Truckin'", transitionOut = TransitionType.NONE))
        )
        val filtered = ShowCatalogFilters.apply(
            shows, query = "scarlet", era = DeadEra.ALL,
            attendance = AttendanceFilter.ALL, sort = SortOrder.DATE_DESC,
            segueFilter = null, songsByShow = songs
        )
        assertEquals(listOf("1"), filtered.map { it.id })
    }

    @Test
    fun `applyFilters attendance attended only`() {
        val shows = listOf(
            show("a", "1977-05-08", attended = true),
            show("b", "1978-01-01", attended = false)
        )
        val filtered = ShowCatalogFilters.apply(
            shows, query = "", era = DeadEra.ALL,
            attendance = AttendanceFilter.ATTENDED_ONLY, sort = SortOrder.DATE_ASC,
            segueFilter = null, songsByShow = emptyMap()
        )
        assertEquals(listOf("a"), filtered.map { it.id })
    }

    @Test
    fun `applyFilters segue chain`() {
        val shows = listOf(show("1", "1977-05-08"), show("2", "1978-01-01"))
        val songs = mapOf(
            "1" to listOf(
                SongEntry("Scarlet Begonias", transitionOut = TransitionType.SEGUE),
                SongEntry("Fire on the Mountain", transitionOut = TransitionType.NONE)
            ),
            "2" to listOf(SongEntry("Truckin'", transitionOut = TransitionType.NONE))
        )
        val chain = "Scarlet Begonias ➔ Fire on the Mountain"
        val filtered = ShowCatalogFilters.apply(
            shows, query = "", era = DeadEra.ALL,
            attendance = AttendanceFilter.ALL, sort = SortOrder.DATE_DESC,
            segueFilter = chain, songsByShow = songs
        )
        assertEquals(listOf("1"), filtered.map { it.id })
    }

    private fun show(
        id: String,
        date: String,
        venue: String = "Venue",
        city: String = "City",
        attended: Boolean = false
    ) = ShowEntity(
        id = id,
        date = date,
        venue = venue,
        city = city,
        stateOrCountry = "CA",
        eraId = "",
        tourName = "",
        bandName = "Grateful Dead",
        isAttended = attended
    )
}
