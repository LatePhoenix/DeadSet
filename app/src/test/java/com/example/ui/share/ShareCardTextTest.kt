package com.example.ui.share

import com.example.data.local.ShowEntity
import com.example.data.model.SongEntry
import com.example.data.model.TransitionType
import com.example.widget.TodayInDeadHistoryWidget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareCardTextTest {

    @Test
    fun `setlist card credits gdshowsdb and lists songs from data`() {
        val show = ShowEntity(
            id = "1977-05-08-cornell",
            date = "1977-05-08",
            venue = "Barton Hall",
            city = "Ithaca",
            stateOrCountry = "NY",
            eraId = "spring_77",
            tourName = "",
            bandName = "Grateful Dead"
        )
        val songs = listOf(
            SongEntry("Scarlet Begonias", transitionOut = TransitionType.SEGUE),
            SongEntry("Fire on the Mountain", transitionOut = TransitionType.NONE)
        )
        val lines = buildSetlistShareLines(show, songs)
        assertEquals("DeadSet", lines.first())
        assertTrue(lines.any { it.contains("Barton Hall") })
        assertTrue(lines.any { it.startsWith("Scarlet Begonias") && it.endsWith("->") })
        assertEquals(SETLIST_CREDIT, lines.last())
    }

    @Test
    fun `trivia card uses only supplied scores`() {
        val lines = buildTriviaShareLines(
            modeTitle = "Classic Tour",
            correctCount = 4,
            answeredCount = 5,
            sessionPoints = 120,
            maxStreak = 3,
            rankTitle = "Miracle Ticket Holder"
        )
        assertTrue(lines.any { it.contains("4 / 5 correct") })
        assertTrue(lines.any { it.contains("+120 points") })
        assertTrue(lines.any { it.contains("Miracle Ticket Holder") })
    }

    @Test
    fun `widget body says so when no shows`() {
        val lines = TodayInDeadHistoryWidget.widgetBodyLines("11-23", emptyList())
        assertEquals(listOf("No shows on 11-23 in the catalog."), lines)
    }
}
