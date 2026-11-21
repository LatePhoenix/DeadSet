package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.TestSeeds
import com.example.data.model.SetType
import com.example.data.model.SongEntry
import com.example.data.model.TransitionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SeedingTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ShowRepository
    private val dao get() = db.showDao()

    @Before
    fun setUp() {
        db = AppDatabase.createInMemoryDatabase(ApplicationProvider.getApplicationContext<Context>())
        repository = ShowRepository(dao, db.songDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `curated shows missing their setlists get the seed setlist back, without a foreign key crash`() = runBlocking {
        // Partial catalog (some curated shows absent) plus a custom show, and no performance rows.
        val present = TestSeeds.seeds.take(2)
        val custom = TestSeeds.seeds.last().show.copy(id = "my-custom-show", isCustomShow = true)
        present.forEach { dao.insertShow(it.show) }
        dao.insertShow(custom)
        assertEquals(0, dao.getSongPerformancesCount())

        repository.checkAndSeedDatabase()

        for (seed in present) {
            assertEquals(seed.id, seed.songs.size, dao.getSongsForShow(seed.id).first().size)
        }
        // A custom show has no seed setlist to restore, so it is left alone.
        assertEquals(0, dao.getSongsForShow(custom.id).first().size)
        // Missing curated shows are not resurrected by the repair path.
        assertEquals(3, dao.getShowsCount())
    }

    @Test
    fun `repair and restore keep the user's stored setlist edits`() = runBlocking {
        repository.checkAndSeedDatabase()
        val cornellId = "1977-05-08-cornell"
        val edited = repository.getSetlist(cornellId).first().transitionOut.next()
        repository.toggleSongTransition(cornellId, 0)
        assertEquals(edited, repository.getSetlist(cornellId).first().transitionOut)

        // Re-running seeding and restore must not overwrite the edit with the seed setlist.
        repository.checkAndSeedDatabase()
        assertEquals(edited, repository.getSetlist(cornellId).first().transitionOut)
        repository.restoreCuratedOfflineCatalog()
        assertEquals(edited, repository.getSetlist(cornellId).first().transitionOut)
    }

    @Test
    fun `saving a show normalizes titles and registers new songs`() = runBlocking {
        repository.checkAndSeedDatabase()
        val custom = TestSeeds.shows.first().copy(id = "custom-normalize", isCustomShow = true)
        val songs = listOf(
            SongEntry(name = "St Stephen", transitionOut = TransitionType.SEGUE),
            SongEntry(name = "Samson & Delilah"),
            SongEntry(name = "Brand New Tune")
        )

        repository.saveShow(custom, songs)

        val expected = listOf("Saint Stephen", "Samson and Delilah", "Brand New Tune")
        assertEquals(expected, repository.getSetlist(custom.id).map { it.name })
        val rows = dao.getSongsForShow(custom.id).first()
        assertEquals(listOf("saint stephen", "samson and delilah", "brand new tune"), rows.map { it.songId })
        assertEquals("Brand New Tune", db.songDao().getSong("brand new tune")?.canonicalName)
        assertEquals("SEGUE", rows.first().transitionOut)
    }

    @Test
    fun `editing a show replaces its setlist instead of appending`() = runBlocking {
        val show = TestSeeds.shows.first().copy(id = "custom-edit", isCustomShow = true)
        repository.saveShow(show, listOf(SongEntry(name = "Bertha"), SongEntry(name = "Deal")))
        repository.saveShow(show.copy(venue = "New Venue"), listOf(SongEntry(name = "Ripple")))

        assertEquals(listOf("Ripple"), repository.getSetlist(show.id).map { it.name })
        assertEquals("New Venue", dao.getShowById(show.id).first()!!.venue)
    }

    @Test
    fun `setlist positions are numbered within each set`() = runBlocking {
        val show = TestSeeds.shows.first().copy(id = "custom-positions", isCustomShow = true)
        repository.saveShow(
            show,
            listOf(
                SongEntry(name = "Bertha", setType = SetType.SET_1),
                SongEntry(name = "Deal", setType = SetType.SET_1),
                SongEntry(name = "Scarlet Begonias", setType = SetType.SET_2),
                SongEntry(name = "Ripple", setType = SetType.ENCORE)
            )
        )
        assertEquals(listOf(1, 2, 1, 1), repository.getSetlist(show.id).map { it.position })
    }

    @Test
    fun `live tracker appends songs at the end of their set`() = runBlocking {
        repository.ensureSongCatalog() // as at app startup, so aliases resolve
        val show = TestSeeds.shows.first().copy(id = "live-show", isCustomShow = true)
        repository.saveShow(show, emptyList())
        repository.appendSong(show.id, "Bertha", SetType.SET_1)
        repository.appendSong(show.id, "Ripple", SetType.ENCORE)
        repository.appendSong(show.id, "Scarlet", SetType.SET_2) // alias -> canonical
        repository.appendSong(show.id, "Deal", SetType.SET_1)
        assertEquals(
            listOf("Bertha", "Deal", "Scarlet Begonias", "Ripple"),
            repository.getSetlist(show.id).map { it.name }
        )
        assertEquals(listOf(1, 2, 1, 1), repository.getSetlist(show.id).map { it.position })
    }

    @Test
    fun `seed shows no longer ship as attended or with invented reviews`() {
        for (seed in TestSeeds.seeds) {
            val s = seed.show
            assertTrue(s.id, !s.isAttended)
            if (seed.id in com.example.data.seed.CuratedShows.legacySeedPersonalData.map { it.showId }) {
                assertEquals(s.id, listOf("", "", ""), listOf(s.userReview, s.attendedWith, s.favoriteSong))
                assertEquals(s.id, 0f, s.userRating)
            }
        }
    }

    @Test
    fun `existing installs lose invented attendance, but never the user's own edits`() = runBlocking {
        val base = TestSeeds.seed("1977-05-08-cornell").show
        // As an old build stored it: attended, with the invented review and companions.
        val invented = base.copy(
            isAttended = true, userRating = 5f, attendedWith = "College tape traders", favoriteSong = "Morning Dew",
            userReview = "The Barton Hall holy grail. The Scarlet -> Fire transition is sublime cosmic perfection."
        )
        dao.insertShow(invented)
        val soldier = TestSeeds.seed("1995-07-09-soldier-field").show
        // The user rewrote this review themselves, so it must be kept.
        val edited = soldier.copy(isAttended = true, userRating = 4f, attendedWith = "Tour family", userReview = "I was really there!")
        dao.insertShow(edited)

        repository.checkAndSeedDatabase()

        val cleared = dao.getShowById(base.id).first()!!
        assertEquals(listOf(false, 0f, "", "", ""), listOf(cleared.isAttended, cleared.userRating, cleared.userReview,
            cleared.attendedWith, cleared.favoriteSong))
        val kept = dao.getShowById(soldier.id).first()!!
        assertTrue(kept.isAttended)
        assertEquals("I was really there!", kept.userReview)
    }

    @Test
    fun `every stored performance links to an existing song`() = runBlocking {
        repository.checkAndSeedDatabase()
        val songIds = db.songDao().getAllSongs().map { it.id }.toSet()
        val perfs = dao.getAllShows().first().flatMap { dao.getSongsForShow(it.id).first() }
        assertTrue(perfs.isNotEmpty())
        assertTrue(perfs.all { it.songId in songIds })
    }

    @Test
    fun `restore re-adds deleted curated shows with their setlists`() = runBlocking {
        repository.checkAndSeedDatabase()
        val veneta = dao.getShowById("1972-08-27-veneta").first()!!
        repository.deleteShow(veneta)
        assertEquals(0, dao.getSongsForShow(veneta.id).first().size) // cascade

        repository.restoreCuratedOfflineCatalog()

        assertTrue(dao.getShowById(veneta.id).first() != null)
        assertEquals(TestSeeds.seed(veneta.id).songs.size, dao.getSongsForShow(veneta.id).first().size)
    }
}
