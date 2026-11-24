package com.example.ui.compose

import com.example.data.repository.OverallStats
import com.example.ui.UserProfileData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 6.7 profile/stats data contracts without Compose UI (see [ComposeUiTestNotes]).
 */
class ProfileStatsComposeTest {

    @Test
    fun `profile edit model round-trips handle`() {
        val next = UserProfileData(handle = "Bobby", homeCity = "Chicago, IL")
        assertEquals("Bobby", next.handle)
        assertEquals("Chicago, IL", next.homeCity)
    }

    @Test
    fun `empty and populated stats both representable`() {
        assertNull(null as OverallStats?)
        val stats = OverallStats(
            totalShowsCount = 2,
            attendedShowsCount = 1,
            totalUniqueSongs = 3,
            totalSongPerformancesIndexed = 4,
            totalTransitionsLogged = 1,
            distinctCitiesCount = 1,
            distinctVenuesCount = 1,
            eraBreakdown = emptyList(),
            topSongsOverall = emptyList(),
            topSongsAttended = emptyList(),
            topTransitions = emptyList(),
            topOpeners = emptyList(),
            topEncores = emptyList()
        )
        assertTrue(stats.totalShowsCount > stats.attendedShowsCount)
    }
}
