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
        assertTrue("Scarlet Begonias should have seamless transition flag", scarlet?.isSegue == true)

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
    fun `curated shows include Terrapin Flyer tribute band concerts with venue addresses`() {
        val shows = CuratedShows.getInitialShows()
        val terrapinShows = shows.filter { it.bandName == "Terrapin Flyer" }
        assertTrue("Terrapin Flyer shows should be present", terrapinShows.isNotEmpty())

        val upcomingShow = terrapinShows.firstOrNull { it.isUpcoming }
        assertNotNull("Upcoming Terrapin Flyer concert should exist", upcomingShow)
        assertTrue("Venue address should be populated for navigation", upcomingShow?.venueAddress?.isNotBlank() == true)
        assertEquals("Chicago", upcomingShow?.city)
    }

    @Test
    fun `room database schema stores shows and relational song performances for offline browsing`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.createInMemoryDatabase(context)
        val dao = db.showDao()
        val repository = ShowRepository(dao)

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
    fun `spotify api client resolves grateful dead setlist tracks to playable spotify uris`() = runBlocking {
        val spotifyClient = com.example.data.remote.SpotifyApiClient()
        val track = spotifyClient.resolveTrackForSetlistSong(
            songName = "Scarlet Begonias",
            showDate = "1977-05-08",
            tourName = "Spring Tour 1977",
            bandName = "Grateful Dead"
        )
        assertEquals("Scarlet Begonias", track.title)
        assertEquals("Grateful Dead", track.artistName)
        assertTrue("Spotify URI should start with spotify:track:", track.spotifyUri.startsWith("spotify:track:"))
        assertTrue("Web URL should point to open.spotify.com/track/", track.webUrl.startsWith("https://open.spotify.com/track/"))
        assertTrue("Duration should be positive", track.durationMs > 0L)
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
    fun `live concert search filters upcoming Midwest dates for Dark Star Orchestra, JRAD, and Terrapin Flyer with ticket URLs`() {
        val apiClient = com.example.data.remote.LiveConcertApiClient()
        val allSchedule = apiClient.getCuratedUpcomingTourSchedule()
        assertTrue("Tour schedule should not be empty", allSchedule.isNotEmpty())

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
