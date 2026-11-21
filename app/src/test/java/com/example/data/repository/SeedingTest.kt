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
        repository = ShowRepository(dao)
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
        val firstSongWasSegue = repository.parseSongs(cornell).first().isSegue
        repository.toggleSongTransition(cornell, 0)

        // Repair path: wipe performance rows, re-run seeding.
        dao.getAllShows().first().forEach { dao.deleteSongPerformancesForShow(it.id) }
        repository.checkAndSeedDatabase()
        assertEquals(!firstSongWasSegue, dao.getSongsForShow(cornell.id).first().first().isSegue)

        // Restore path must not overwrite the edit with the original seed setlist either.
        repository.restoreCuratedOfflineCatalog()
        assertEquals(!firstSongWasSegue, dao.getSongsForShow(cornell.id).first().first().isSegue)
        val storedJson = repository.parseSongs(dao.getShowById(cornell.id).first()!!)
        assertEquals(!firstSongWasSegue, storedJson.first().isSegue)
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
