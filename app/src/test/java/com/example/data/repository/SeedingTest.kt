package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.TestSeeds
import com.example.data.model.SetType
import com.example.data.model.SongEntry
import com.example.data.model.TransitionType
import com.example.data.seed.CuratedSeedMerger
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
        // Missing curated GD shows are not resurrected by the repair path.
        // Non-GD Terrapin Flyer is always inserted (R1).
        assertTrue(dao.getShowById("2024-05-18-terrapin-flyer-martyrs").first() != null)
        assertEquals(4, dao.getShowsCount())
    }

    @Test
    fun `catalog UUID rows get curated enrichment without duplicate seed ids`() = runBlocking {
        val cornell = TestSeeds.seed("1977-05-08-cornell")
        val catalogId = "gdshowsdb-uuid-cornell"
        dao.insertShow(
            cornell.show.copy(
                id = catalogId,
                venue = "Barton Hall",
                archiveIdentifier = "",
                historicTrivia = "",
                tags = "",
                isAttended = true,
                userRating = 4f,
                userReview = "I was there"
            )
        )
        // Stale seed-id duplicate from an older restore path.
        dao.insertShow(cornell.show)

        repository.checkAndSeedDatabase()

        val dates = dao.getAllShowsOnce().filter {
            it.date == "1977-05-08" && CuratedSeedMerger.isGratefulDead(it)
        }
        assertEquals(1, dates.size)
        assertEquals(catalogId, dates.single().id)
        val enriched = dao.getShowById(catalogId).first()!!
        assertEquals(cornell.show.archiveIdentifier, enriched.archiveIdentifier)
        assertEquals(cornell.show.historicTrivia, enriched.historicTrivia)
        assertTrue(enriched.isAttended)
        assertEquals(4f, enriched.userRating)
        assertEquals("I was there", enriched.userReview)
        assertEquals(cornell.songs.size, dao.getSongsForShow(catalogId).first().size)
        assertTrue(dao.getShowById("2024-05-18-terrapin-flyer-martyrs").first() != null)
    }

    @Test
    fun `restore merges onto catalog instead of inserting a second Cornell`() = runBlocking {
        val cornell = TestSeeds.seed("1977-05-08-cornell")
        val catalogId = "gdshowsdb-uuid-cornell"
        dao.insertShow(
            cornell.show.copy(
                id = catalogId,
                venue = "Barton Hall",
                archiveIdentifier = "",
                historicTrivia = ""
            )
        )

        repository.restoreCuratedOfflineCatalog()

        val cornellRows = dao.getAllShowsOnce().filter { it.date == "1977-05-08" }
        assertEquals(1, cornellRows.size)
        assertEquals(catalogId, cornellRows.single().id)
        assertEquals(
            cornell.show.historicTrivia,
            dao.getShowById(catalogId).first()!!.historicTrivia
        )
        assertTrue(dao.getShowById("2024-05-18-terrapin-flyer-martyrs").first() != null)
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
            assertEquals(s.id, listOf("", "", ""), listOf(s.userReview, s.attendedWith, s.favoriteSong))
            assertEquals(s.id, 0f, s.userRating)
        }
        // Every seed has a fingerprint, so existing installs get cleaned up too.
        assertEquals(
            TestSeeds.seeds.map { it.id }.toSet(),
            com.example.data.seed.CuratedShows.legacySeedPersonalData.map { it.showId }.toSet()
        )
    }

    @Test
    fun `cleanup keeps attendance the user set on a show whose review was invented`() = runBlocking {
        val veneta = TestSeeds.seed("1972-08-27-veneta").show
        val invented = CuratedShows.legacySeedPersonalData.first { it.showId == veneta.id }
        // Old build's invented review/rating/favorite, but the user marked it attended themselves.
        dao.insertShow(
            veneta.copy(
                isAttended = true, userRating = invented.rating, favoriteSong = invented.favoriteSong,
                userReview = invented.inventedReview
            )
        )

        repository.checkAndSeedDatabase()

        val after = dao.getShowById(veneta.id).first()!!
        assertTrue("user-set attendance must survive", after.isAttended)
        assertEquals(listOf(0f, "", ""), listOf(after.userRating, after.userReview, after.favoriteSong))
    }

    @Test
    fun `cleanup leaves a show alone if the user changed any invented field`() = runBlocking {
        val veneta = TestSeeds.seed("1972-08-27-veneta").show
        val invented = CuratedShows.legacySeedPersonalData.first { it.showId == veneta.id }
        // Same invented review, but the user re-rated it: treat it as theirs.
        val rerated = veneta.copy(
            userRating = 3f, favoriteSong = invented.favoriteSong,
            userReview = invented.inventedReview
        )
        dao.insertShow(rerated)

        repository.checkAndSeedDatabase()

        val after = dao.getShowById(veneta.id).first()!!
        assertEquals(3f, after.userRating)
        assertEquals(rerated.userReview, after.userReview)
    }

    @Test
    fun `existing installs lose invented attendance, but never the user's own edits`() = runBlocking {
        val base = TestSeeds.seed("1977-05-08-cornell").show
        // As an old build stored it: attended, with the invented review and companions.
        val invented = base.copy(
            isAttended = true, userRating = 5f, attendedWith = "College tape traders", favoriteSong = "Morning Dew",
            userReview = "The Barton Hall holy grail. The Scarlet -> Fire transition is sublime cosmic perfection, and Jerry's final crescendo on Morning Dew gave everyone in the room chills. Transcendent."
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
    fun `an appended note on an invented review is left alone`() = runBlocking {
        val base = TestSeeds.seed("1977-05-08-cornell").show
        val appended = base.copy(
            isAttended = true, userRating = 5f, attendedWith = "College tape traders", favoriteSong = "Morning Dew",
            userReview = CuratedShows.legacySeedPersonalData.first { it.showId == base.id }.inventedReview +
                "\n\nP.S. I really was there."
        )
        dao.insertShow(appended)
        repository.checkAndSeedDatabase()
        val after = dao.getShowById(base.id).first()!!
        assertTrue(after.isAttended)
        assertEquals(appended.userReview, after.userReview)
        assertEquals(5f, after.userRating)
    }

    @Test
    fun `updating a show's details never cascades away its setlist`() = runBlocking {
        val show = TestSeeds.shows.first().copy(id = "upsert-show", isCustomShow = true)
        repository.saveShow(show, listOf(SongEntry(name = "Bertha"), SongEntry(name = "Deal")))
        // A REPLACE insert would delete the show row first and CASCADE-delete these rows.
        dao.insertShow(show.copy(venue = "Renamed Venue"))
        assertEquals(listOf("Bertha", "Deal"), repository.getSetlist(show.id).map { it.name })
        assertEquals("Renamed Venue", dao.getShowById(show.id).first()!!.venue)
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
