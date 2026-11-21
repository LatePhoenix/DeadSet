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
            // Read raw: at v4 the JSON must still carry `isSegue` for the 4→5 migration to convert.
            val songs = org.json.JSONArray(c.getString(0)).let { a -> (0 until a.length()).map { a.getJSONObject(it) } }
            assertEquals(listOf("Saint Stephen", "Turn On Your Love Light", "My Own Jam"), songs.map { it.getString("name") })
            assertEquals(listOf(true, false, false), songs.map { it.optBoolean("isSegue", false) })
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

    @Test
    fun `4 to 5 turns isSegue into transitionOut in both setlist copies`() {
        val json = """[{"name":"Scarlet Begonias","isSegue":true,"notes":"into Fire"},""" +
            """{"name":"Fire on the Mountain","isSegue":false,"isJamHighlight":true}]"""
        helper.createDatabase(dbName, 4).apply {
            execSQL("INSERT INTO songs (id, canonicalName) VALUES ('scarlet begonias', 'Scarlet Begonias')")
            execSQL("INSERT INTO songs (id, canonicalName) VALUES ('fire on the mountain', 'Fire on the Mountain')")
            insertV3Show(this, json)
            execSQL(
                "INSERT INTO song_performances (performanceId, showId, songId, songName, setType, trackOrder, " +
                    "isSegue, durationMinutes, notes, isJamHighlight) VALUES " +
                    "(7, 'my-show', 'scarlet begonias', 'Scarlet Begonias', 'SET_2', 0, 1, 11, 'into Fire', 0), " +
                    "(8, 'my-show', 'fire on the mountain', 'Fire on the Mountain', 'SET_2', 1, 0, NULL, NULL, 1)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 5, true, MIGRATION_4_5)

        db.query(
            "SELECT performanceId, transitionOut, durationMinutes, notes, isJamHighlight " +
                "FROM song_performances ORDER BY trackOrder"
        ).use { c ->
            assertTrue(c.moveToNext())
            assertEquals(listOf<Any?>(7L, "SEGUE", 11, "into Fire", 0),
                listOf(c.getLong(0), c.getString(1), c.getInt(2), c.getString(3), c.getInt(4)))
            assertTrue(c.moveToNext())
            assertEquals(listOf<Any?>(8L, "NONE", true, true, 1),
                listOf(c.getLong(0), c.getString(1), c.isNull(2), c.isNull(3), c.getInt(4)))
        }
        db.query("SELECT setlistJson FROM shows WHERE id = 'my-show'").use { c ->
            assertTrue(c.moveToFirst())
            val songs = org.json.JSONArray(c.getString(0)).let { a -> (0 until a.length()).map { a.getJSONObject(it) } }
            assertEquals(listOf("SEGUE", "NONE"), songs.map { it.getString("transitionOut") })
            assertTrue("isSegue key should be gone", songs.none { it.has("isSegue") })
            // Keys the migration doesn't know about are preserved.
            assertEquals("into Fire", songs[0].getString("notes"))
            assertTrue(songs[1].getBoolean("isJamHighlight"))
        }
        db.close()
    }

    @Test
    fun `a v3 install upgraded all the way keeps its segue tags`() = runBlocking {
        // The chain that a coupled 3→4 migration would have broken: segues must survive 3→4→5.
        val json = """[{"name":"Help on the Way","isSegue":true},{"name":"Slipknot","isSegue":true},""" +
            """{"name":"Franklin's Tower","isSegue":false}]"""
        helper.createDatabase(dbName, 3).apply {
            insertV3Show(this, json)
            execSQL(
                "INSERT INTO song_performances (showId, songName, setType, trackOrder, isSegue, isJamHighlight) VALUES " +
                    "('my-show', 'Help on the Way', 'SET_2', 0, 1, 0), " +
                    "('my-show', 'Slipknot', 'SET_2', 1, 1, 0), " +
                    "('my-show', 'Franklin''s Tower', 'SET_2', 2, 0, 0)"
            )
            close()
        }

        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .applyMigrationPolicy()
            .allowMainThreadQueries()
            .build()
        try {
            val expectedNames = listOf("Help on the Way", "Slipknot!", "Franklin's Tower")
            val expectedTransitions = listOf(
                com.example.data.model.TransitionType.SEGUE,
                com.example.data.model.TransitionType.SEGUE,
                com.example.data.model.TransitionType.NONE
            )
            val stored = db.showDao().getShowWithSongsById("my-show").first()!!
            assertEquals(expectedNames, stored.orderedSongs.map { it.name })
            assertEquals(expectedTransitions, stored.orderedSongs.map { it.transitionOut })
        } finally {
            db.close()
        }
    }

    @Test
    fun `5 to 6 keeps every setlist row and user field when the JSON column goes`() {
        helper.createDatabase(dbName, 5).apply {
            execSQL("INSERT INTO songs (id, canonicalName) VALUES ('scarlet begonias', 'Scarlet Begonias')")
            execSQL("INSERT INTO songs (id, canonicalName) VALUES ('fire on the mountain', 'Fire on the Mountain')")
            execSQL("INSERT INTO songs (id, canonicalName) VALUES ('saint stephen', 'Saint Stephen')")
            execSQL("INSERT INTO song_aliases (aliasKey, songId) VALUES ('saint stephen', 'saint stephen')")
            execSQL("INSERT INTO song_aliases (aliasKey, songId) VALUES ('st stephen', 'saint stephen')")
            // Show with rows (the normal case): rows must survive the shows-table rebuild.
            insertV5Show(this, "with-rows", """[{"name":"Scarlet Begonias","transitionOut":"SEGUE"}]""", attended = true)
            execSQL(
                "INSERT INTO song_performances (performanceId, showId, songId, songName, setType, trackOrder, " +
                    "transitionOut, durationMinutes, notes, isJamHighlight) VALUES " +
                    "(21, 'with-rows', 'scarlet begonias', 'Scarlet Begonias', 'SET_2', 0, 'SEGUE', 11, 'into Fire', 0), " +
                    "(22, 'with-rows', 'fire on the mountain', 'Fire on the Mountain', 'SET_2', 1, 'NONE', NULL, NULL, 1)"
            )
            // Show with JSON only: its setlist must be backfilled before the column is dropped.
            insertV5Show(
                this, "json-only",
                """[{"name":"St. Stephen","setType":"SET_2","transitionOut":"JAM","durationMinutes":7},""" +
                    """{"name":"Mystery Tune","setType":"ENCORE","notes":"first time","isJamHighlight":true}]""",
                attended = false
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 6, true, MIGRATION_5_6)

        db.query(
            "SELECT performanceId, songName, transitionOut, durationMinutes, notes, isJamHighlight " +
                "FROM song_performances WHERE showId = 'with-rows' ORDER BY trackOrder"
        ).use { c ->
            val got = buildList {
                while (c.moveToNext()) add(
                    listOf(c.getLong(0), c.getString(1), c.getString(2),
                        if (c.isNull(3)) null else c.getInt(3), if (c.isNull(4)) null else c.getString(4), c.getInt(5))
                )
            }
            assertEquals(
                listOf(
                    listOf(21L, "Scarlet Begonias", "SEGUE", 11, "into Fire", 0),
                    listOf(22L, "Fire on the Mountain", "NONE", null, null, 1)
                ),
                got
            )
        }
        db.query(
            "SELECT songId, songName, setType, trackOrder, transitionOut, durationMinutes, notes, isJamHighlight " +
                "FROM song_performances WHERE showId = 'json-only' ORDER BY trackOrder"
        ).use { c ->
            val got = buildList {
                while (c.moveToNext()) add(
                    listOf(c.getString(0), c.getString(1), c.getString(2), c.getInt(3), c.getString(4),
                        if (c.isNull(5)) null else c.getInt(5), if (c.isNull(6)) null else c.getString(6), c.getInt(7))
                )
            }
            assertEquals(
                listOf(
                    listOf("saint stephen", "Saint Stephen", "SET_2", 0, "JAM", 7, null, 0),
                    listOf("mystery tune", "Mystery Tune", "ENCORE", 1, "NONE", null, "first time", 1)
                ),
                got
            )
        }
        db.query("SELECT isAttended, userReview, userRating, lastUpdatedTimestamp FROM shows WHERE id = 'with-rows'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(listOf<Any?>(1, "Mine", 5.0f, 99L), listOf(c.getInt(0), c.getString(1), c.getFloat(2), c.getLong(3)))
        }
        db.query("PRAGMA table_info(shows)").use { c ->
            val cols = buildList { while (c.moveToNext()) add(c.getString(1)) }
            assertTrue(cols.toString(), "setlistJson" !in cols && "isUpcoming" !in cols)
        }
        db.query("PRAGMA foreign_key_check").use { c -> assertEquals("FK violations", 0, c.count) }
        db.close()
    }

    @Test
    fun `5 to 6 keeps a segue edit that only reached the JSON`() {
        // v5 wrote setlistJson before the rows; an interrupted edit left the row stale.
        helper.createDatabase(dbName, 5).apply {
            execSQL("INSERT INTO songs (id, canonicalName) VALUES ('scarlet begonias', 'Scarlet Begonias')")
            execSQL("INSERT INTO songs (id, canonicalName) VALUES ('fire on the mountain', 'Fire on the Mountain')")
            insertV5Show(
                this, "stale",
                """[{"name":"Scarlet Begonias","transitionOut":"SEGUE"},{"name":"Fire on the Mountain","transitionOut":"NONE"}]""",
                attended = true
            )
            execSQL(
                "INSERT INTO song_performances (performanceId, showId, songId, songName, setType, trackOrder, " +
                    "transitionOut, durationMinutes, notes, isJamHighlight) VALUES " +
                    "(31, 'stale', 'scarlet begonias', 'Scarlet Begonias', 'SET_2', 0, 'NONE', NULL, NULL, 0), " +
                    "(32, 'stale', 'fire on the mountain', 'Fire on the Mountain', 'SET_2', 1, 'NONE', NULL, NULL, 0)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 6, true, MIGRATION_5_6)

        db.query("SELECT performanceId, transitionOut FROM song_performances WHERE showId = 'stale' ORDER BY trackOrder").use { c ->
            val got = buildList { while (c.moveToNext()) add(c.getLong(0) to c.getString(1)) }
            assertEquals(listOf(31L to "SEGUE", 32L to "NONE"), got)
        }
        db.close()
    }

    @Test
    fun `5 to 6 keeps a JSON-only segue edit even when JSON and rows have different lengths`() {
        helper.createDatabase(dbName, 5).apply {
            for ((id, name) in listOf("bertha" to "Bertha", "scarlet begonias" to "Scarlet Begonias",
                "fire on the mountain" to "Fire on the Mountain")) {
                execSQL("INSERT INTO songs (id, canonicalName) VALUES (?, ?)", arrayOf<Any?>(id, name))
            }
            // JSON (what v5 displayed) has an extra song first; rows drifted and are stale.
            insertV5Show(
                this, "drifted",
                """[{"name":"Bertha","transitionOut":"SEGUE"},{"name":"Scarlet Begonias","transitionOut":"SEGUE"},""" +
                    """{"name":"Fire on the Mountain","transitionOut":"NONE"}]""",
                attended = true
            )
            execSQL(
                "INSERT INTO song_performances (performanceId, showId, songId, songName, setType, trackOrder, " +
                    "transitionOut, durationMinutes, notes, isJamHighlight) VALUES " +
                    "(41, 'drifted', 'scarlet begonias', 'Scarlet Begonias', 'SET_2', 0, 'NONE', NULL, NULL, 0), " +
                    "(42, 'drifted', 'fire on the mountain', 'Fire on the Mountain', 'SET_2', 1, 'NONE', NULL, NULL, 0)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 6, true, MIGRATION_5_6)

        db.query("SELECT performanceId, songName, transitionOut FROM song_performances WHERE showId = 'drifted' ORDER BY trackOrder").use { c ->
            val got = buildList { while (c.moveToNext()) add(Triple(c.getLong(0), c.getString(1), c.getString(2))) }
            // Scarlet's edit is kept on Scarlet's row; Bertha's SEGUE is not copied onto another song.
            assertEquals(
                listOf(Triple(41L, "Scarlet Begonias", "SEGUE"), Triple(42L, "Fire on the Mountain", "NONE")),
                got
            )
        }
        db.close()
    }

    private fun insertV5Show(
        db: androidx.sqlite.db.SupportSQLiteDatabase,
        id: String,
        setlistJson: String,
        attended: Boolean
    ) {
        db.execSQL(
            """
            INSERT INTO shows (id, date, venue, city, stateOrCountry, eraId, tourName, setlistJson,
                isAttended, userRating, userReview, attendedWith, favoriteSong, archiveIdentifier,
                tapeQuality, isCustomShow, tags, historicTrivia, bandName, isUpcoming, venueAddress,
                lastUpdatedTimestamp)
            VALUES (?, '1977-05-08', 'Barton Hall', 'Ithaca', 'NY', 'spring_77', 'Spring',
                ?, ?, 5.0, 'Mine', '', '', '', '', 1, '', '', 'Grateful Dead', 0, '', 99)
            """.trimIndent(),
            arrayOf<Any?>(id, setlistJson, if (attended) 1 else 0)
        )
    }

    private fun insertV3Show(db: androidx.sqlite.db.SupportSQLiteDatabase, setlistJson: String) {
        // The shows table is identical in v3, v4 and v5.
        db.execSQL(
            """
            INSERT INTO shows (id, date, venue, city, stateOrCountry, eraId, tourName, setlistJson,
                isAttended, userRating, userReview, attendedWith, favoriteSong, archiveIdentifier,
                tapeQuality, isCustomShow, tags, historicTrivia, bandName, isUpcoming, venueAddress,
                lastUpdatedTimestamp)
            VALUES ('my-show', '1976-06-09', 'Boston Music Hall', 'Boston', 'MA', 'wall_of_sound', 'Summer',
                ?, 1, 5.0, 'Mine', '', '', '', '', 1, '', '', 'Grateful Dead', 0, '', 0)
            """.trimIndent(),
            arrayOf<Any?>(setlistJson)
        )
    }
}
