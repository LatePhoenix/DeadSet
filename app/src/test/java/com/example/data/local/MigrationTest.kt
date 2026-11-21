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
}
