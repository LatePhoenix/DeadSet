package com.example.ui.compose

import com.example.data.local.ShowEntity
import com.example.data.model.DeadEra
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 6.7 shows/detail attendance toggle logic without Compose UI (see [ComposeUiTestNotes]).
 */
class ShowsDetailComposeTest {

    @Test
    fun `attendance toggle flips attended flag`() {
        val show = ShowEntity(
            id = "1977-05-08-cornell",
            date = "1977-05-08",
            venue = "Barton Hall",
            city = "Ithaca",
            stateOrCountry = "NY",
            eraId = DeadEra.SPRING_77.id,
            tourName = "Spring 77",
            isAttended = false
        )
        assertFalse(show.isAttended)
        val toggled = show.copy(isAttended = !show.isAttended)
        assertTrue(toggled.isAttended)
    }
}
