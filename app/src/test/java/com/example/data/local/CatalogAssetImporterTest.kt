package com.example.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.example.TestSeeds
import com.example.data.repository.ShowRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CatalogAssetImporterTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var prefs: UserPrefsStore
    private lateinit var importer: CatalogAssetImporter

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = AppDatabase.createInMemoryDatabase(context)
        prefs = UserPrefsStore(context)
        prefs.setImportedCatalogVersion(0)
        importer = CatalogAssetImporter(context, db.showDao(), db.songDao(), prefs)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `import upserts catalog and preserves user data and edited setlists`() = runBlocking {
        val seed = TestSeeds.seed("1977-05-08-cornell")
        val repository = ShowRepository(db.showDao(), db.songDao())
        repository.ensureSongCatalog()
        db.showDao().insertShow(
            seed.show.copy(isAttended = true, userRating = 4f, userReview = "mine")
        )
        val edited = seed.songs.toMutableList().also {
            it[0] = it[0].copy(transitionOut = it[0].transitionOut.next())
        }
        repository.saveShow(seed.show.copy(isAttended = true, userRating = 4f, userReview = "mine"), edited)

        val fixture = buildFixtureCatalog(
            catalogShowId = "gdshowsdb-uuid-cornell",
            date = "1977-05-08",
            venue = "Barton Hall",
            extraShowId = "gdshowsdb-uuid-other",
            extraDate = "1978-01-01"
        )

        val result = importer.importIfNeeded(fixture)
        assertTrue(result.imported)
        assertEquals(2, result.showCount)
        assertEquals(CatalogAssetImporter.BUNDLED_CATALOG_VERSION, prefs.importedCatalogVersion())

        val attended = db.showDao().getShowById(seed.id).first()!!
        assertTrue(attended.isAttended)
        assertEquals(4f, attended.userRating)
        assertEquals("mine", attended.userReview)
        assertEquals(edited.first().transitionOut, repository.getSetlist(seed.id).first().transitionOut)

        assertTrue(db.showDao().getShowById("gdshowsdb-uuid-cornell").first() != null)
        assertTrue(db.showDao().getShowById("gdshowsdb-uuid-other").first() != null)
        // New catalog show with no prior setlist gets performances from the asset.
        assertTrue(db.showDao().getSongsForShowOnce("gdshowsdb-uuid-other").isNotEmpty())
        // Catalog UUID for Cornell has asset setlist because seed id is a different PK.
        assertTrue(db.showDao().getSongsForShowOnce("gdshowsdb-uuid-cornell").isNotEmpty())

        val second = importer.importIfNeeded(fixture)
        assertFalse(second.imported)
        assertEquals("already_imported", second.skipped)
    }

    @Test
    fun `import skips performances for custom shows that already exist`() = runBlocking {
        val custom = TestSeeds.shows.first().copy(
            id = "gdshowsdb-uuid-other",
            isCustomShow = true,
            isAttended = true,
            userReview = "custom"
        )
        db.showDao().insertShow(custom)
        // Custom show with an existing (empty) setlist marker: insert one perf so skip set triggers.
        ShowRepository(db.showDao(), db.songDao()).let { repo ->
            repo.ensureSongCatalog()
            repo.saveShow(custom, listOf(com.example.data.model.SongEntry(name = "Bertha")))
        }

        val fixture = buildFixtureCatalog(
            catalogShowId = "gdshowsdb-uuid-cornell",
            date = "1977-05-08",
            venue = "Barton Hall",
            extraShowId = "gdshowsdb-uuid-other",
            extraDate = "1978-01-01"
        )
        importer.importIfNeeded(fixture)

        assertEquals(listOf("Bertha"), ShowRepository(db.showDao(), db.songDao()).getSetlist(custom.id).map { it.name })
        assertEquals("custom", db.showDao().getShowById(custom.id).first()!!.userReview)
    }

    /** Tiny Room-shaped catalog.db for tests (two shows, one song, two performances). */
    private fun buildFixtureCatalog(
        catalogShowId: String,
        date: String,
        venue: String,
        extraShowId: String,
        extraDate: String
    ): File {
        val file = File(context.cacheDir, "fixture-catalog-${System.nanoTime()}.db")
        if (file.exists()) file.delete()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL(
                """
                CREATE TABLE songs (
                  id TEXT NOT NULL PRIMARY KEY,
                  canonicalName TEXT NOT NULL,
                  writers TEXT,
                  debutDate TEXT
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE song_aliases (
                  aliasKey TEXT NOT NULL PRIMARY KEY,
                  songId TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE shows (
                  id TEXT NOT NULL PRIMARY KEY,
                  date TEXT NOT NULL,
                  venue TEXT NOT NULL,
                  city TEXT NOT NULL,
                  stateOrCountry TEXT NOT NULL,
                  eraId TEXT NOT NULL,
                  tourName TEXT NOT NULL,
                  archiveIdentifier TEXT NOT NULL,
                  tapeQuality TEXT NOT NULL,
                  isCustomShow INTEGER NOT NULL,
                  tags TEXT NOT NULL,
                  historicTrivia TEXT NOT NULL,
                  bandName TEXT NOT NULL,
                  venueAddress TEXT NOT NULL,
                  recreatesShowId TEXT,
                  lastUpdatedTimestamp INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE song_performances (
                  performanceId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                  showId TEXT NOT NULL,
                  songId TEXT NOT NULL,
                  songName TEXT NOT NULL,
                  setType TEXT NOT NULL,
                  trackOrder INTEGER NOT NULL,
                  transitionOut TEXT NOT NULL,
                  durationMinutes INTEGER,
                  notes TEXT,
                  isJamHighlight INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("INSERT INTO songs (id, canonicalName) VALUES ('bertha', 'Bertha')")
            db.execSQL("INSERT INTO song_aliases (aliasKey, songId) VALUES ('bertha', 'bertha')")
            fun insertShow(id: String, showDate: String, showVenue: String) {
                db.execSQL(
                    """
                    INSERT INTO shows (
                      id, date, venue, city, stateOrCountry, eraId, tourName, archiveIdentifier,
                      tapeQuality, isCustomShow, tags, historicTrivia, bandName, venueAddress,
                      recreatesShowId, lastUpdatedTimestamp
                    ) VALUES (
                      '$id', '$showDate', '$showVenue', 'Ithaca', 'NY', 'spring_77', '', '',
                      '', 0, '', '', 'Grateful Dead', '', NULL, 1
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO song_performances (
                      showId, songId, songName, setType, trackOrder, transitionOut, isJamHighlight
                    ) VALUES ('$id', 'bertha', 'Bertha', 'SET_1', 0, 'NONE', 0)
                    """.trimIndent()
                )
            }
            insertShow(catalogShowId, date, venue)
            insertShow(extraShowId, extraDate, "Other Venue")
        }
        return file
    }
}
