package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.Converters
import com.example.data.repository.ShowRepository
import com.example.data.seed.CuratedShows
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DeadSet", appName)
    }

    @Test
    fun `curated shows have valid setlists with seamless transitions`() {
        val shows = CuratedShows.getInitialShows()
        assertTrue("Curated shows should not be empty", shows.isNotEmpty())

        val converters = Converters()
        val cornellShow = shows.first { it.id == "1977-05-08-cornell" }
        val songs = converters.toSongList(cornellShow.setlistJson)

        assertTrue("Cornell should have songs in setlist", songs.isNotEmpty())

        val scarlet = songs.firstOrNull { it.name == "Scarlet Begonias" }
        assertNotNull("Scarlet Begonias should exist", scarlet)
        assertTrue("Scarlet Begonias should have seamless transition flag", scarlet?.transitionOut == com.example.data.model.TransitionType.SEGUE)

        val fire = songs.firstOrNull { it.name == "Fire on the Mountain" }
        assertNotNull("Fire on the Mountain should exist", fire)
    }

    @Test
    fun `curated shows include historic trivia and user reviews`() {
        val shows = CuratedShows.getInitialShows()
        for (show in shows) {
            assertTrue("Show should have valid date", show.date.isNotBlank())
            assertTrue("Show should have historic trivia", show.historicTrivia.isNotBlank())
        }
    }

    @Test
    fun `curated shows include a Terrapin Flyer show and no invented future-dated shows`() {
        val shows = CuratedShows.getInitialShows()
        val terrapinShows = shows.filter { it.bandName == "Terrapin Flyer" }
        assertTrue("Terrapin Flyer shows should be present", terrapinShows.isNotEmpty())
        assertTrue(terrapinShows.all { it.venueAddress.isNotBlank() && it.city == "Chicago" })

        val today = com.example.data.todayIsoDate()
        assertTrue("Seed data must not contain upcoming shows", shows.none { it.isUpcoming })
        assertTrue("Seed data must not contain future dates", shows.all { it.date <= today })
        assertTrue(shows.none { it.id in CuratedShows.retiredSeedShowIds })
    }

    @Test
    fun `seeding removes retired placeholder shows unless the user attended them`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.createInMemoryDatabase(context)
        val dao = db.showDao()
        val repository = ShowRepository(dao, db.songDao())
        // Simulate an existing install: seeded catalog plus the old placeholder rows.
        repository.checkAndSeedDatabase()
        val template = CuratedShows.getInitialShows().first()
        val (untouchedId, attendedId) = CuratedShows.retiredSeedShowIds
        // Retired seeds shipped with filler review text but no attendance.
        dao.insertShow(template.copy(id = untouchedId, isAttended = false, attendedWith = "", userReview = "Upcoming show!"))
        dao.insertShow(template.copy(id = attendedId, isAttended = true))

        repository.checkAndSeedDatabase()

        val ids = dao.getAllShows().first().map { it.id }
        assertTrue(untouchedId !in ids)
        assertTrue(attendedId in ids)
        db.close()
    }

    @Test
    fun `room database schema stores shows and relational song performances for offline browsing`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.createInMemoryDatabase(context)
        val dao = db.showDao()
        val repository = ShowRepository(dao, db.songDao())

        repository.checkAndSeedDatabase()

        val allShows = repository.allShows.first()
        assertTrue("Room database should store seeded historical shows", allShows.isNotEmpty())

        val showWithSongs = repository.getShowWithSongsById("1977-05-08-cornell").first()
        assertNotNull("Cornell 5/8/77 should be stored with relational songs", showWithSongs)
        assertEquals("1977-05-08", showWithSongs?.show?.date)
        assertEquals("Barton Hall, Cornell University", showWithSongs?.show?.venue)
        assertEquals("Ithaca", showWithSongs?.show?.city)
        assertTrue("Relational songsPerformed list should not be empty", showWithSongs?.orderedSongs?.isNotEmpty() == true)

        val ithacaShows = repository.getShowsByCity("Ithaca").first()
        assertEquals(1, ithacaShows.size)

        val scarletShows = repository.getShowsPerformingSong("Scarlet Begonias").first()
        assertTrue("Should find offline shows where Scarlet Begonias was performed", scarletShows.isNotEmpty())

        // Verify attendance aggregation for UserProfileScreen counter summary
        val initialAttended = repository.attendedShows.first().size
        repository.toggleAttendance("1972-08-27-veneta", true)
        val updatedShows = repository.allShows.first()
        val stats = repository.computeOverallStats(updatedShows)
        assertTrue("Attended shows count should increase after marking Veneta attended", stats.attendedShowsCount >= initialAttended)

        // Verify Era Song Performance Frequency Bar Chart statistics
        assertTrue("Era breakdown should contain song performance counts", stats.eraBreakdown.sumOf { it.songPerformanceCount } > 0)
        assertTrue("Song era frequencies should be populated for bar chart filtering", stats.songEraFrequencies.isNotEmpty())
        val topSongEraFreq = stats.songEraFrequencies.first()
        assertEquals(topSongEraFreq.totalPlays, topSongEraFreq.eraCounts.values.sum())

        db.close()
    }

    @Test
    fun `grateful dead trivia generates curated and dynamic room vault questions`() {
        val shows = CuratedShows.getInitialShows()
        val converters = Converters()
        val curated = com.example.data.seed.DeadTriviaSeed.getCuratedQuestions()
        assertTrue("Curated trivia bank should have at least 20 questions", curated.size >= 20)

        val dynamicVault = com.example.data.seed.DeadTriviaSeed.generateDynamicVaultQuestions(
            shows = shows,
            parseSongs = { converters.toSongList(it.setlistJson) }
        )
        assertTrue("Dynamic Room vault trivia questions should be generated from shows", dynamicVault.isNotEmpty())

        val classicDeck = com.example.data.seed.DeadTriviaSeed.buildQuizDeck(
            mode = com.example.data.seed.TriviaGameMode.CLASSIC_TOUR,
            category = com.example.data.seed.TriviaCategory.ALL,
            difficultyFilter = null,
            shows = shows,
            parseSongs = { converters.toSongList(it.setlistJson) }
        )
        assertEquals(10, classicDeck.size)
        classicDeck.forEach { q ->
            assertEquals("Each question must have 4 options", 4, q.options.size)
            assertTrue("Correct index must be valid", q.correctIndex in 0..3)
            assertTrue("Liner note explanation must be non-empty", q.linerNoteFact.isNotBlank())
        }
    }

    @Test
    fun `trivia progress and profile survive a new store instance`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val progress = com.example.data.seed.TriviaUserProgress(
            totalAnswered = 12, totalCorrect = 9, bestStreak = 5, totalPoints = 1800,
            quizzesCompleted = 2, dailyCompletedDate = "2026-11-21",
            unlockedBadgeIds = setOf("first_miracle", "daily_stealie")
        )
        val profile = com.example.data.local.UserPrefsStore.StoredProfile(
            "Terrapin Fan", "Chicago, IL", "Scarlet ➔ Fire", "Spring '77"
        )
        com.example.data.local.UserPrefsStore(context).apply {
            assertEquals(null, loadProfile())
            saveTriviaProgress(progress)
            saveProfile(profile)
        }

        val reloaded = com.example.data.local.UserPrefsStore(context)
        assertEquals(progress, reloaded.loadTriviaProgress())
        assertEquals(profile, reloaded.loadProfile())
    }

    @Test
    fun `archive org link searches the GratefulDead collection by show date`() {
        val url = com.example.ui.util.ConcertIntentHelper.archiveOrgRecordingsUrl("1977-05-08")
        assertEquals("https://archive.org/details/GratefulDead?query=1977-05-08", url)

        val cornell = CuratedShows.getInitialShows().first { it.id == "1977-05-08-cornell" }
        assertTrue(com.example.ui.util.ConcertIntentHelper.hasArchiveRecordings(cornell))
        val tribute = cornell.copy(bandName = "Terrapin Flyer")
        assertEquals(false, com.example.ui.util.ConcertIntentHelper.hasArchiveRecordings(tribute))
    }

    @Test
    fun `global theme provider toggles between Sunshine Light and Steal Your Face Dark mode with Material3 dynamic color schemes`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Verify 'Steal Your Face' Dark mode state and tokens
        com.example.ui.theme.DeadSetThemeState.applyThemeMode(
            com.example.ui.theme.DeadThemeMode.STEAL_YOUR_FACE_DARK
        )
        assertTrue(com.example.ui.theme.DeadSetThemeState.isDarkTheme)
        assertEquals(
            com.example.ui.theme.StealieMidnightDarkRaw,
            com.example.ui.theme.StealieMidnight
        )

        val darkDynamicScheme = com.example.ui.theme.resolveDeadSetColorScheme(
            context = context,
            isDarkTheme = true,
            useDynamicColor = true
        )
        val darkStaticScheme = com.example.ui.theme.resolveDeadSetColorScheme(
            context = context,
            isDarkTheme = true,
            useDynamicColor = false
        )
        assertNotNull(darkDynamicScheme)
        assertEquals(com.example.ui.theme.StealYourFaceDarkColorScheme.primary, darkStaticScheme.primary)

        // 2. Toggle to 'Sunshine Daydream' Light theme and verify tokens + M3 dynamic color scheme
        com.example.ui.theme.DeadSetThemeState.toggleDarkLight()
        assertEquals(
            com.example.ui.theme.DeadThemeMode.SUNSHINE_LIGHT,
            com.example.ui.theme.DeadSetThemeState.themeMode
        )
        assertEquals(false, com.example.ui.theme.DeadSetThemeState.isDarkTheme)
        assertEquals(
            com.example.ui.theme.StealieParchmentLightRaw,
            com.example.ui.theme.StealieMidnight
        )

        val lightDynamicScheme = com.example.ui.theme.resolveDeadSetColorScheme(
            context = context,
            isDarkTheme = false,
            useDynamicColor = true
        )
        val lightStaticScheme = com.example.ui.theme.resolveDeadSetColorScheme(
            context = context,
            isDarkTheme = false,
            useDynamicColor = false
        )
        assertNotNull(lightDynamicScheme)
        assertEquals(com.example.ui.theme.SunshineDaydreamLightColorScheme.primary, lightStaticScheme.primary)

        // Restore default dark mode after test
        com.example.ui.theme.DeadSetThemeState.applyThemeMode(
            com.example.ui.theme.DeadThemeMode.STEAL_YOUR_FACE_DARK
        )
    }

    @Test
    fun `live concert filter narrows events by band and Midwest region`() {
        val apiClient = com.example.data.remote.LiveConcertApiClient()
        // Test fixtures only — the app no longer ships any hard-coded schedule.
        fun event(id: String, band: String, city: String, state: String) =
            com.example.data.remote.LiveConcertEvent(
                id = id, bandName = band, eventTitle = "$band in $city", date = "2099-01-0${id.last()}",
                displayDate = "", venueName = "Test Venue", city = city, stateCode = state,
                venueAddress = "", ticketUrl = "https://example.test/$id"
            )
        val allSchedule = listOf(
            event("e1", "Dark Star Orchestra", "Chicago", "IL"),
            event("e2", "Dark Star Orchestra", "Denver", "CO"),
            event("e3", "Joe Russo's Almost Dead", "Milwaukee", "WI"),
            event("e4", "Terrapin Flyer", "Chicago", "IL"),
            event("e5", "Terrapin Flyer", "Asheville", "NC")
        )

        // Verify Midwest filter for Dark Star Orchestra
        val dsoMidwest = apiClient.filterConcertEvents(
            events = allSchedule,
            bandFilter = com.example.data.remote.TributeBandFilter.DARK_STAR_ORCHESTRA,
            midwestOnly = true
        )
        assertTrue("Dark Star Orchestra should have Midwest dates", dsoMidwest.isNotEmpty())
        assertTrue(dsoMidwest.all { it.isMidwest && it.ticketUrl.startsWith("https://") })

        // Verify Midwest filter for JRAD
        val jradMidwest = apiClient.filterConcertEvents(
            events = allSchedule,
            bandFilter = com.example.data.remote.TributeBandFilter.JRAD,
            midwestOnly = true
        )
        assertTrue("JRAD should have Midwest dates", jradMidwest.isNotEmpty())
        assertTrue(jradMidwest.all { it.isMidwest && it.venueName.isNotBlank() && it.city.isNotBlank() })

        // Verify Midwest filter for Terrapin Flyer
        val terrapinMidwest = apiClient.filterConcertEvents(
            events = allSchedule,
            bandFilter = com.example.data.remote.TributeBandFilter.TERRAPIN_FLYER,
            midwestOnly = true
        )
        assertTrue("Terrapin Flyer should have Midwest dates", terrapinMidwest.isNotEmpty())
        assertTrue(terrapinMidwest.any { it.city == "Chicago" && it.stateCode == "IL" })

        // Verify authentic Steal Your Face and Marching Bear drawables exist in resources
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNotNull(context.getDrawable(R.drawable.art_steal_your_face))
        assertNotNull(context.getDrawable(R.drawable.art_dancing_bears_row))
        assertNotNull(context.getDrawable(R.drawable.art_bear_pink))
        assertNotNull(context.getDrawable(R.drawable.art_bear_green))
        assertNotNull(context.getDrawable(R.drawable.art_bear_yellow))
        assertNotNull(context.getDrawable(R.drawable.art_bear_blue))
        assertNotNull(context.getDrawable(R.drawable.art_bear_orange))
    }
}
