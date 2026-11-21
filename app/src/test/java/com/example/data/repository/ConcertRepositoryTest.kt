package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.Geo
import com.example.data.LatLon
import com.example.data.checkedAgo
import com.example.data.local.AppDatabase
import com.example.data.local.ShowEntity
import com.example.data.remote.LiveConcertApiClient
import com.example.data.remote.LiveConcertEvent
import com.example.ui.screens.buildEditedShow
import com.example.ui.screens.distanceMiles
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConcertRepositoryTest {

    private lateinit var db: AppDatabase
    private var nextResult = LiveConcertApiClient.FetchResult(emptyList(), emptySet(), false)
    private val requestedBands = mutableListOf<List<String>>()
    private lateinit var repo: ConcertRepository

    private fun event(id: String, band: String, date: String, lat: Double? = null, lon: Double? = null) = LiveConcertEvent(
        id = id, bandName = band, eventTitle = "$band live", date = date, displayDate = date,
        venueName = "Venue", city = "Chicago", stateCode = "IL", venueAddress = "", ticketUrl = "https://x.test/$id",
        apiSource = "Bandsintown API", latitude = lat, longitude = lon
    )

    @Before
    fun setUp() {
        db = AppDatabase.createInMemoryDatabase(ApplicationProvider.getApplicationContext<Context>())
        repo = ConcertRepository(
            dao = db.concertDao(),
            fetchEvents = { bands -> requestedBands += bands; nextResult },
            clock = { 1_000L }
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `default bands follow Terrapin Flyer, DSO and JRAD`() = runBlocking {
        repo.ensureDefaultBands()
        repo.ensureDefaultBands() // idempotent
        val bands = repo.bands.first()
        assertEquals(6, bands.size)
        assertEquals(setOf("Terrapin Flyer", "Dark Star Orchestra", "Joe Russo's Almost Dead"),
            bands.filter { it.isFollowed }.map { it.name }.toSet())
    }

    @Test
    fun `refresh asks only for followed bands and caches with check time`() = runBlocking {
        repo.ensureDefaultBands()
        repo.setFollowed("dark star orchestra", false)
        repo.setFollowed("joe russo's almost dead", false)
        nextResult = LiveConcertApiClient.FetchResult(
            listOf(event("e1", "Terrapin Flyer", "2099-01-01")), setOf("Bandsintown"), false
        )

        val outcome = repo.refresh()

        assertEquals(listOf(listOf("Terrapin Flyer")), requestedBands)
        assertTrue(outcome.anySourceReached)
        val cached = repo.upcomingEvents(today = "2026-11-21").first()
        assertEquals(listOf("e1"), cached.map { it.first.id })
        assertEquals(1_000L, cached.single().second)
    }

    @Test
    fun `a refresh where no source answers keeps the existing cache`() = runBlocking {
        repo.ensureDefaultBands()
        nextResult = LiveConcertApiClient.FetchResult(listOf(event("e1", "Terrapin Flyer", "2099-01-01")), setOf("Bandsintown"), false)
        repo.refresh()

        nextResult = LiveConcertApiClient.FetchResult(emptyList(), emptySet(), false)
        val outcome = repo.refresh()

        assertFalse(outcome.anySourceReached)
        assertEquals(listOf("e1"), repo.upcomingEvents(today = "2026-11-21").first().map { it.first.id })
    }

    @Test
    fun `a successful refresh replaces cancelled dates and past dates disappear`() = runBlocking {
        repo.ensureDefaultBands()
        nextResult = LiveConcertApiClient.FetchResult(
            listOf(event("old", "Terrapin Flyer", "2099-01-01"), event("cancelled", "Terrapin Flyer", "2099-02-01")),
            setOf("Bandsintown"), false
        )
        repo.refresh()
        nextResult = LiveConcertApiClient.FetchResult(listOf(event("old", "Terrapin Flyer", "2099-01-01")), setOf("Bandsintown"), false)
        repo.refresh()
        assertEquals(listOf("old"), repo.upcomingEvents(today = "2026-11-21").first().map { it.first.id })
        assertTrue(repo.upcomingEvents(today = "2100-01-01").first().isEmpty())
    }

    @Test
    fun `users can add and remove their own bands but not built-in ones`() = runBlocking {
        repo.ensureDefaultBands()
        repo.addBand("  Grateful   Shred ")
        assertTrue(repo.bands.first().any { it.name == "Grateful Shred" && it.isFollowed && !it.isBuiltIn })
        repo.removeBand("grateful shred")
        repo.removeBand("terrapin flyer")
        val names = repo.bands.first().map { it.name }
        assertFalse("Grateful Shred" in names)
        assertTrue("Terrapin Flyer" in names)
    }

    @Test
    fun `distance and check-time labels`() {
        val chicago = LatLon(41.8781, -87.6298)
        val milwaukee = LatLon(43.0389, -87.9065)
        assertEquals(81.0, Geo.milesBetween(chicago, milwaukee), 3.0)
        assertEquals(0.0, Geo.milesBetween(chicago, chicago), 0.001)
        assertNull(distanceMiles(chicago, event("x", "B", "2099-01-01")))
        assertNull(distanceMiles(null, event("x", "B", "2099-01-01", 43.0, -87.9)))
        assertEquals("checked just now", checkedAgo(0, 30_000))
        assertEquals("checked 5 min ago", checkedAgo(0, 5 * 60_000L))
        assertEquals("checked 3 h ago", checkedAgo(0, 3 * 3_600_000L))
        assertEquals("checked 2 d ago", checkedAgo(0, 2 * 86_400_000L))
    }

    @Test
    fun `editing a show keeps fields the form does not show`() {
        val existing = ShowEntity(
            id = "1977-05-08-cornell", date = "1977-05-08", venue = "Barton Hall", city = "Ithaca",
            stateOrCountry = "NY", eraId = "spring_77", tourName = "Spring 1977", isAttended = true,
            userReview = "Unreal", attendedWith = "Pals", favoriteSong = "Morning Dew",
            archiveIdentifier = "gd77-05-08", historicTrivia = "Lore", tags = "Cornell", isCustomShow = false
        )
        val edited = buildEditedShow(existing, { "unused" }, " 1977-05-08 ", "Barton Hall, Cornell", "Ithaca", "NY",
            "spring_77", "Spring Tour", "SBD", "Grateful Dead")
        assertEquals("Barton Hall, Cornell", edited.venue)
        assertEquals("Spring Tour", edited.tourName)
        assertEquals(existing.copy(venue = "Barton Hall, Cornell", tourName = "Spring Tour", tapeQuality = "SBD",
            lastUpdatedTimestamp = edited.lastUpdatedTimestamp), edited)
    }

    @Test
    fun `a new show is custom and attended only if it already happened`() {
        val past = buildEditedShow(null, { "custom-1" }, "2024-05-18", "Martyrs'", "Chicago", "IL",
            "all", "", "", "Terrapin Flyer", today = "2026-11-21")
        assertTrue(past.isCustomShow)
        assertTrue(past.isAttended)
        assertEquals("Terrapin Flyer", past.bandName)
        assertEquals("custom-1", past.id)

        val upcoming = buildEditedShow(null, { "custom-2" }, "2026-12-31", "The Vic", "Chicago", "IL",
            "all", "", "", "Terrapin Flyer", today = "2026-11-21")
        assertFalse(upcoming.isAttended)
    }
}
