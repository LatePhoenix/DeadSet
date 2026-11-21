package com.example.data.setlist

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.ChaseSongEntity
import com.example.data.local.ShowEntity
import com.example.data.model.SongEntry
import com.example.data.repository.ShowRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChaseProgressTest {

    private fun show(id: String, date: String, band: String) = ShowEntity(
        id = id, date = date, venue = "Venue $id", city = "Chicago", stateOrCountry = "IL",
        eraId = "all", tourName = "T", isAttended = true, bandName = band
    )

    private val tf2024 = show("tf", "2024-05-18", "Terrapin Flyer")
    private val dso2023 = show("dso", "2023-03-01", "Dark Star Orchestra")
    private val setlists = mapOf(
        "tf" to listOf(SongEntry("Althea"), SongEntry("Saint Stephen"), SongEntry("Deal")),
        "dso" to listOf(SongEntry("St. Stephen"), SongEntry("Morning Dew"))
    )
    private val songsFor = { s: ShowEntity -> setlists[s.id].orEmpty() }

    @Test
    fun `chased songs show where they were first heard, any band, aliases merged`() {
        val statuses = ChaseProgress.statuses(
            listOf("saint stephen" to "Saint Stephen", "dark star" to "Dark Star", "althea" to "Althea"),
            listOf(tf2024, dso2023),
            songsFor
        ).associateBy { it.songName }
        assertEquals("dso", statuses.getValue("Saint Stephen").firstHeardAt?.id) // 2023 beats 2024
        assertEquals("tf", statuses.getValue("Althea").firstHeardAt?.id)
        assertNull(statuses.getValue("Dark Star").firstHeardAt)
        assertFalse(statuses.getValue("Dark Star").heard)
    }

    @Test
    fun `unheard songs are listed first`() {
        val order = ChaseProgress.statuses(
            listOf("althea" to "Althea", "dark star" to "Dark Star"), listOf(tf2024), songsFor
        ).map { it.songName }
        assertEquals(listOf("Dark Star", "Althea"), order)
    }

    @Test
    fun `distinct songs heard is not capped and merges aliases`() {
        assertEquals(4, ChaseProgress.distinctSongsHeard(listOf(tf2024, dso2023), songsFor))
        val many = (1..40).associate { "s$it" to listOf(SongEntry("Song $it")) }
        val shows = (1..40).map { show("s$it", "2020-01-01", "Grateful Dead") }
        assertEquals(40, ChaseProgress.distinctSongsHeard(shows) { many[it.id].orEmpty() })
    }

    @Test
    fun `shows by band counts attended shows per band`() {
        assertEquals(
            listOf("Terrapin Flyer" to 2, "Dark Star Orchestra" to 1),
            ChaseProgress.showsByBand(listOf(tf2024, dso2023, tf2024.copy(id = "tf2")))
        )
    }

    @Test
    fun `chasing a typed title stores its canonical song once`() = runBlocking {
        val db = AppDatabase.createInMemoryDatabase(ApplicationProvider.getApplicationContext<Context>())
        val repo = ShowRepository(db.showDao(), db.songDao())
        repo.ensureSongCatalog()
        for (typed in listOf("st stephen", "Saint Stephen!")) {
            val song = repo.resolveSong(typed)!!
            db.chaseDao().add(ChaseSongEntity(song.songId, song.canonicalName, "2026-11-21"))
        }
        val stored = db.chaseDao().observeAll().first()
        assertEquals(listOf("Saint Stephen"), stored.map { it.songName })
        assertNull(repo.resolveSong("   "))
        assertTrue("Saint Stephen" in repo.allSongNames())
        db.close()
    }
}
