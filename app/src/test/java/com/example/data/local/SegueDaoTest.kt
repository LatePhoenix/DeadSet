package com.example.data.local

import androidx.test.core.app.ApplicationProvider
import com.example.data.model.SetType
import com.example.data.model.TransitionType
import com.example.data.songs.songKey
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
class SegueDaoTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = AppDatabase.createInMemoryDatabase(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `adjacent song query finds Scarlet into Fire with SEGUE and skips unrelated`() = runBlocking {
        val scarlet = songKey("Scarlet Begonias")
        val fire = songKey("Fire on the Mountain")
        val estimated = songKey("Estimated Prophet")
        db.songDao().insertSongs(
            listOf(
                SongEntity(scarlet, "Scarlet Begonias"),
                SongEntity(fire, "Fire on the Mountain"),
                SongEntity(estimated, "Estimated Prophet")
            )
        )
        val dao = db.showDao()
        dao.insertShow(
            ShowEntity(
                id = "cornell", date = "1977-05-08", venue = "Barton Hall", city = "Ithaca",
                stateOrCountry = "NY", eraId = "spring_77", tourName = "Spring"
            )
        )
        dao.insertShow(
            ShowEntity(
                id = "other", date = "1978-01-01", venue = "X", city = "Y",
                stateOrCountry = "CA", eraId = "spring_77", tourName = ""
            )
        )
        dao.insertSongPerformances(
            listOf(
                SongPerformanceEntity(
                    showId = "cornell", songId = scarlet, songName = "Scarlet Begonias",
                    setType = SetType.SET_2.name, trackOrder = 0,
                    transitionOut = TransitionType.SEGUE.name
                ),
                SongPerformanceEntity(
                    showId = "cornell", songId = fire, songName = "Fire on the Mountain",
                    setType = SetType.SET_2.name, trackOrder = 1
                ),
                // Same songs but not adjacent as a segue pair (Estimated between) — still adjacent scarlet→fire? No.
                SongPerformanceEntity(
                    showId = "other", songId = scarlet, songName = "Scarlet Begonias",
                    setType = SetType.SET_2.name, trackOrder = 0,
                    transitionOut = TransitionType.NONE.name
                ),
                SongPerformanceEntity(
                    showId = "other", songId = estimated, songName = "Estimated Prophet",
                    setType = SetType.SET_2.name, trackOrder = 1
                ),
                SongPerformanceEntity(
                    showId = "other", songId = fire, songName = "Fire on the Mountain",
                    setType = SetType.SET_2.name, trackOrder = 2
                )
            )
        )

        val withSegue = dao.showIdsWithAdjacentSongs(scarlet, fire, requireSegue = true)
        assertEquals(listOf("cornell"), withSegue)

        val anyAdjacent = dao.showIdsWithAdjacentSongs(scarlet, fire, requireSegue = false)
        assertEquals(listOf("cornell"), anyAdjacent)

        val scarletEstimated = dao.showIdsWithAdjacentSongs(scarlet, estimated, requireSegue = false)
        assertEquals(listOf("other"), scarletEstimated)
        assertTrue(dao.showIdsWithAdjacentSongs(scarlet, estimated, requireSegue = true).isEmpty())
    }
}
