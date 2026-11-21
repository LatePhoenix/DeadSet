package com.example.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.local.AppDatabase.Companion.applyMigrationPolicy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Guards user data across schema changes. When you add Migration(N, N+1):
 *  - bump AppDatabase.VERSION and commit schemas/.../(N+1).json
 *  - add it to AppDatabase.ALL_MIGRATIONS
 *  - add a test here that seeds version N with raw SQL and asserts the data after migrating.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {

    private val dbName = "migration-test"
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun `every version from the baseline migrates to current and validates`() {
        for (start in AppDatabase.BASELINE_VERSION..AppDatabase.VERSION) {
            context.deleteDatabase(dbName)
            helper.createDatabase(dbName, start).close()
            // validateDroppedTables = true: fails if a migration leaves or drops tables wrongly.
            helper.runMigrationsAndValidate(dbName, AppDatabase.VERSION, true, *AppDatabase.ALL_MIGRATIONS)
                .close()
        }
    }

    @Test
    fun `user data written at the baseline version survives opening with the real config`() = runBlocking {
        helper.createDatabase(dbName, AppDatabase.BASELINE_VERSION).apply {
            execSQL(
                """
                INSERT INTO shows (id, date, venue, city, stateOrCountry, eraId, tourName, setlistJson,
                    isAttended, userRating, userReview, attendedWith, favoriteSong, archiveIdentifier,
                    tapeQuality, isCustomShow, tags, historicTrivia, bandName, isUpcoming, venueAddress,
                    lastUpdatedTimestamp)
                VALUES ('my-show', '2024-05-18', 'Martyrs''', 'Chicago', 'IL', 'spring_77', 'Spring',
                    '[{"name":"Althea"}]', 1, 4.5, 'Great night', 'Friends', 'Althea', '', '', 1, '', '',
                    'Terrapin Flyer', 0, '', 0)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO song_performances (showId, songName, setType, trackOrder, isSegue, isJamHighlight)
                VALUES ('my-show', 'Althea', 'SET_1', 0, 1, 0)
                """.trimIndent()
            )
            close()
        }

        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .applyMigrationPolicy()
            .allowMainThreadQueries()
            .build()
        try {
            val stored = db.showDao().getShowWithSongsById("my-show").first()
            assertNotNull("User-entered show must survive", stored)
            assertTrue(stored!!.show.isAttended)
            assertEquals("Great night", stored.show.userReview)
            assertEquals(4.5f, stored.show.userRating)
            assertEquals(listOf("Althea"), stored.orderedSongs.map { it.name })
        } finally {
            db.close()
        }
    }

    @Test
    fun `pre-baseline databases are rebuilt instead of crashing`() = runBlocking {
        // Version 2 predates schema export; simulate an unknown old layout.
        val file = context.getDatabasePath(dbName).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use {
            it.execSQL("CREATE TABLE shows (id TEXT PRIMARY KEY, legacyColumn TEXT)")
            it.version = 2
        }

        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .applyMigrationPolicy()
            .allowMainThreadQueries()
            .build()
        try {
            assertEquals(0, db.showDao().getShowsCount())
        } finally {
            db.close()
        }
    }

    @Test
    fun `3 to 4 normalizes song titles, links songIds, and keeps user data`() {
        val setlistJson = """[{"name":"St. Stephen","setType":"SET_2","isSegue":true},""" +
            """{"name":"Lovelight","setType":"SET_2"},{"name":"  My Own   Jam ","setType":"ENCORE"}]"""
        helper.createDatabase(dbName, 3).apply {
            execSQL(
                """
                INSERT INTO shows (id, date, venue, city, stateOrCountry, eraId, tourName, setlistJson,
                    isAttended, userRating, userReview, attendedWith, favoriteSong, archiveIdentifier,
                    tapeQuality, isCustomShow, tags, historicTrivia, bandName, isUpcoming, venueAddress,
                    lastUpdatedTimestamp)
                VALUES ('my-show', '1969-02-27', 'Fillmore West', 'San Francisco', 'CA', 'primal_60s', 'Primal',
                    ?, 1, 5.0, 'Unreal', 'Pals', 'St. Stephen', '', '', 1, '', '', 'Grateful Dead', 0, '', 42)
                """.trimIndent(),
                arrayOf<Any?>(setlistJson)
            )
            val rows = listOf(
                arrayOf<Any?>(10, "St. Stephen", "SET_2", 0, 1, 6, "William Tell bridge", 1),
                arrayOf<Any?>(11, "Lovelight", "SET_2", 1, 0, null, null, 0),
                arrayOf<Any?>(12, "  My Own   Jam ", "ENCORE", 2, 0, null, null, 0)
            )
            for (r in rows) {
                execSQL(
                    "INSERT INTO song_performances (performanceId, showId, songName, setType, trackOrder, " +
                        "isSegue, durationMinutes, notes, isJamHighlight) VALUES (?, 'my-show', ?, ?, ?, ?, ?, ?, ?)",
                    r
                )
            }
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 4, true, MIGRATION_3_4)

        db.query(
            "SELECT performanceId, songId, songName, isSegue, durationMinutes, notes, isJamHighlight " +
                "FROM song_performances ORDER BY trackOrder"
        ).use { c ->
            val got = buildList {
                while (c.moveToNext()) {
                    add(
                        listOf(
                            c.getLong(0), c.getString(1), c.getString(2), c.getInt(3),
                            if (c.isNull(4)) null else c.getInt(4),
                            if (c.isNull(5)) null else c.getString(5), c.getInt(6)
                        )
                    )
                }
            }
            assertEquals(
                listOf(
                    listOf(10L, "saint stephen", "Saint Stephen", 1, 6, "William Tell bridge", 1),
                    listOf(11L, "turn on your love light", "Turn On Your Love Light", 0, null, null, 0),
                    listOf(12L, "my own jam", "My Own Jam", 0, null, null, 0)
                ),
                got
            )
        }

        db.query("SELECT canonicalName FROM songs WHERE id = 'my own jam'").use { c ->
            assertTrue("User-entered song should become a songs row", c.moveToFirst())
            assertEquals("My Own Jam", c.getString(0))
        }
        db.query("SELECT songId FROM song_aliases WHERE aliasKey = 'st stephen'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("saint stephen", c.getString(0))
        }

        db.query(
            "SELECT setlistJson, isAttended, userRating, userReview, attendedWith, favoriteSong, " +
                "isCustomShow, lastUpdatedTimestamp FROM shows WHERE id = 'my-show'"
        ).use { c ->
            assertTrue(c.moveToFirst())
            val songs = Converters().toSongList(c.getString(0))
            assertEquals(listOf("Saint Stephen", "Turn On Your Love Light", "My Own Jam"), songs.map { it.name })
            assertEquals(listOf(true, false, false), songs.map { it.isSegue })
            assertEquals(1, c.getInt(1))
            assertEquals(5.0f, c.getFloat(2))
            assertEquals("Unreal", c.getString(3))
            assertEquals("Pals", c.getString(4))
            assertEquals("St. Stephen", c.getString(5)) // free-text field, intentionally untouched
            assertEquals(1, c.getInt(6))
            assertEquals(42L, c.getLong(7))
        }
        db.query("PRAGMA foreign_key_check").use { c -> assertEquals("FK violations", 0, c.count) }
        db.close()
    }
}
