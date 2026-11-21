package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.seed.CuratedShows
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
    fun `shows without performance rows get them rebuilt without a foreign key crash`() = runBlocking {
        // Partial catalog (some curated shows missing) plus a custom show, and no performance rows.
        val curated = CuratedShows.getInitialShows()
        val present = curated.take(2)
        val custom = curated.last().copy(id = "my-custom-show", isCustomShow = true)
        (present + custom).forEach { dao.insertShow(it) }
        assertEquals(0, dao.getSongPerformancesCount())

        repository.checkAndSeedDatabase() // previously threw SQLiteConstraintException

        for (show in present + custom) {
            val rows = dao.getSongsForShow(show.id).first()
            assertEquals(show.id, repository.parseSongs(show).size, rows.size)
        }
        // Missing curated shows are not resurrected by the repair path.
        assertEquals(3, dao.getShowsCount())
    }

    @Test
    fun `repair and restore keep the user's stored setlist edits`() = runBlocking {
        repository.checkAndSeedDatabase()
        val cornell = dao.getShowById("1977-05-08-cornell").first()!!
        val edited = repository.parseSongs(cornell).first().transitionOut.next()
        repository.toggleSongTransition(cornell, 0)

        // Repair path: wipe performance rows, re-run seeding.
        dao.getAllShows().first().forEach { dao.deleteSongPerformancesForShow(it.id) }
        repository.checkAndSeedDatabase()
        assertEquals(edited.name, dao.getSongsForShow(cornell.id).first().first().transitionOut)

        // Restore path must not overwrite the edit with the original seed setlist either.
        repository.restoreCuratedOfflineCatalog()
        assertEquals(edited.name, dao.getSongsForShow(cornell.id).first().first().transitionOut)
        val storedJson = repository.parseSongs(dao.getShowById(cornell.id).first()!!)
        assertEquals(edited, storedJson.first().transitionOut)
    }

    @Test
    fun `saving a show normalizes titles in both setlist copies and registers new songs`() = runBlocking {
        repository.checkAndSeedDatabase()
        val template = CuratedShows.getInitialShows().first()
        val custom = template.copy(
            id = "custom-normalize",
            isCustomShow = true,
            setlistJson = repository.serializeSongs(
                listOf(
                    com.example.data.model.SongEntry(name = "St Stephen", transitionOut = com.example.data.model.TransitionType.SEGUE),
                    com.example.data.model.SongEntry(name = "Samson & Delilah"),
                    com.example.data.model.SongEntry(name = "Brand New Tune")
                )
            )
        )

        repository.saveShow(custom)

        val expected = listOf("Saint Stephen", "Samson and Delilah", "Brand New Tune")
        val stored = dao.getShowById(custom.id).first()!!
        assertEquals(expected, repository.parseSongs(stored).map { it.name })
        val rows = dao.getSongsForShow(custom.id).first()
        assertEquals(expected, rows.map { it.songName })
        assertEquals(listOf("saint stephen", "samson and delilah", "brand new tune"), rows.map { it.songId })
        assertEquals("Brand New Tune", db.songDao().getSong("brand new tune")?.canonicalName)
        assertEquals("SEGUE", rows.first().transitionOut)
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
    fun `restore re-adds deleted curated shows`() = runBlocking {
        repository.checkAndSeedDatabase()
        val veneta = dao.getShowById("1972-08-27-veneta").first()!!
        repository.deleteShow(veneta)

        repository.restoreCuratedOfflineCatalog()

        assertTrue(dao.getShowById(veneta.id).first() != null)
        assertTrue(dao.getSongsForShow(veneta.id).first().isNotEmpty())
    }
}
