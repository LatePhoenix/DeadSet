package com.example.data.backup

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.BandEntity
import com.example.data.local.CatalogAssetImporter
import com.example.data.local.ChaseSongEntity
import com.example.data.local.ReviewCardEntity
import com.example.data.local.ShowCatalogEntity
import com.example.data.local.SongEntity
import com.example.data.local.SongPerformanceEntity
import com.example.data.local.UserPrefsStore
import com.example.data.local.UserShowDataEntity
import com.example.data.seed.TriviaUserProgress
import com.example.di.AppContainer
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupManagerTest {

    private lateinit var app: Application
    private lateinit var db: AppDatabase
    private lateinit var prefs: UserPrefsStore
    private lateinit var manager: BackupManager

    @Before
    fun setUp() {
        AppContainer.resetForTests()
        app = ApplicationProvider.getApplicationContext()
        openFreshDb()
    }

    @After
    fun tearDown() {
        db.close()
        AppContainer.resetForTests()
    }

    private fun openFreshDb() {
        if (::db.isInitialized) db.close()
        db = AppDatabase.createInMemoryDatabase(app)
        prefs = UserPrefsStore(app)
        prefs.setImportedCatalogVersion(CatalogAssetImporter.BUNDLED_CATALOG_VERSION)
        // Reset prefs between cases (SharedPreferences survive in-memory DB recreation).
        prefs.saveTriviaProgress(TriviaUserProgress())
        prefs.setAlertsEnabled(false)
        prefs.saveProfile(UserPrefsStore.StoredProfile("", "", "", ""))
        manager = BackupManager(db, prefs, appVersion = "test-1.0", today = { "2026-11-23" })
    }

    @Test
    fun `export wipe import round trip restores user data`() = runBlocking {
        seedCatalogShow("gd-1")
        seedUserData()
        val json = manager.exportJson()

        openFreshDb()
        seedCatalogShow("gd-1") // catalog only; no user rows

        assertTrue(db.showDao().getAllUserShowData().isEmpty())
        assertTrue(db.chaseDao().getAllOnce().isEmpty())

        val parsed = manager.parse(json) as BackupParseResult.Ok
        val result = manager.importDocument(parsed.document)

        assertEquals(2, result.userShowDataUpserted) // gd-1 + custom-1
        assertEquals(1, result.customShowsUpserted)
        assertEquals(1, result.chaseSongsUpserted)
        assertEquals(1, result.reviewCardsUpserted)
        assertEquals(0, result.skippedUnknownShowIds)

        val user = db.showDao().getAllUserShowData().single { it.showId == "gd-1" }
        assertTrue(user.isAttended)
        assertEquals(4.5f, user.userRating, 0.01f)
        assertEquals("Great night", user.userReview)

        val custom = db.showDao().getCustomCatalogShows().single()
        assertEquals("custom-1", custom.id)
        assertEquals(2, db.showDao().getSongsForShowOnce("custom-1").size)

        assertEquals("Scarlet Begonias", db.chaseDao().getAllOnce().single().songName)
        assertEquals(2, db.reviewDao().getAllOnce().single().box)
        assertEquals("Deadhead", prefs.loadProfile()!!.handle)
        assertEquals(12, prefs.loadTriviaProgress().totalPoints)
        assertTrue(prefs.alertsEnabled())
        assertTrue(db.concertDao().getAllBands().any { it.id == "my band" && it.isFollowed })
    }

    @Test
    fun `import merges into existing data without deleting`() = runBlocking {
        seedCatalogShow("gd-1")
        seedCatalogShow("gd-2")
        seedUserData()
        val json = manager.exportJson()

        openFreshDb()
        seedCatalogShow("gd-1")
        seedCatalogShow("gd-2")
        db.showDao().upsertUserData(
            UserShowDataEntity("gd-2", isAttended = true, userReview = "Keep me")
        )
        db.chaseDao().upsert(ChaseSongEntity("fire", "Fire on the Mountain", "2026-01-01"))

        val parsed = manager.parse(json) as BackupParseResult.Ok
        manager.importDocument(parsed.document)

        val ids = db.showDao().getAllUserShowData().map { it.showId }.toSet()
        assertTrue(ids.contains("gd-1"))
        assertTrue(ids.contains("gd-2"))
        assertEquals("Keep me", db.showDao().getAllUserShowData().first { it.showId == "gd-2" }.userReview)
        assertEquals(2, db.chaseDao().getAllOnce().size)
    }

    @Test
    fun `malformed and wrong format and newer version reject with no writes`() = runBlocking {
        seedCatalogShow("gd-1")
        db.showDao().upsertUserData(UserShowDataEntity("gd-1", isAttended = true))
        val goodExport = manager.exportJson()

        suspend fun assertRejected(json: String) {
            val before = db.showDao().getAllUserShowData()
            assertTrue(manager.parse(json) is BackupParseResult.Err)
            assertEquals(before, db.showDao().getAllUserShowData())
        }

        assertRejected("{not json")
        assertRejected(
            """{"format":"other","version":1,"exportedAt":"x","appVersion":"1",""" +
                """"userShowData":[],"customShows":[],"chaseSongs":[],"reviewCards":[],""" +
                """"bands":[],"prefs":{"profile":null,"triviaProgress":{"totalAnswered":0,""" +
                """"totalCorrect":0,"currentStreak":0,"bestStreak":0,"totalPoints":0,""" +
                """"quizzesCompleted":0,"dailyCompletedDate":"","unlockedBadgeIds":[]},""" +
                """"alertsEnabled":false}}"""
        )
        assertRejected(JSONObject(goodExport).put("version", BACKUP_VERSION + 1).toString())
    }

    @Test
    fun `unknown show ids are skipped on import`() = runBlocking {
        seedCatalogShow("gd-1")
        seedUserData()
        val json = manager.exportJson()

        openFreshDb()
        // No gd-1 catalog row → that user_show_data row is skipped; custom show still lands.

        val parsed = manager.parse(json) as BackupParseResult.Ok
        val result = manager.importDocument(parsed.document)
        assertTrue(result.skippedUnknownShowIds >= 1)
        assertFalse(db.showDao().getAllUserShowData().any { it.showId == "gd-1" })
        assertTrue(db.showDao().getCustomCatalogShows().any { it.id == "custom-1" })
        assertTrue(db.showDao().getAllUserShowData().any { it.showId == "custom-1" })
    }

    private suspend fun seedCatalogShow(id: String) {
        db.showDao().upsertCatalog(
            ShowCatalogEntity(
                id = id,
                date = "1977-05-08",
                venue = "Barton Hall",
                city = "Ithaca",
                stateOrCountry = "NY",
                eraId = "spring_77",
                tourName = "Spring 77",
                isCustomShow = false
            )
        )
    }

    private suspend fun seedUserData() {
        db.songDao().insertSongs(
            listOf(
                SongEntity("scarlet", "Scarlet Begonias"),
                SongEntity("fire", "Fire on the Mountain")
            )
        )
        db.showDao().upsertUserData(
            UserShowDataEntity(
                showId = "gd-1",
                isAttended = true,
                userRating = 4.5f,
                userReview = "Great night",
                attendedWith = "Bobby",
                favoriteSong = "Scarlet Begonias"
            )
        )
        val custom = ShowCatalogEntity(
            id = "custom-1",
            date = "2024-06-01",
            venue = "Martyrs'",
            city = "Chicago",
            stateOrCountry = "IL",
            eraId = "",
            tourName = "",
            isCustomShow = true,
            bandName = "Terrapin Flyer"
        )
        db.showDao().upsertCatalog(custom)
        db.showDao().replaceSongPerformances(
            "custom-1",
            listOf(
                SongPerformanceEntity(
                    showId = "custom-1", songId = "scarlet", songName = "Scarlet Begonias",
                    setType = "SET_1", trackOrder = 0, transitionOut = "SEGUE"
                ),
                SongPerformanceEntity(
                    showId = "custom-1", songId = "fire", songName = "Fire on the Mountain",
                    setType = "SET_1", trackOrder = 1, transitionOut = "NONE"
                )
            )
        )
        db.showDao().upsertUserData(UserShowDataEntity("custom-1", isAttended = true))
        db.chaseDao().upsert(ChaseSongEntity("scarlet", "Scarlet Begonias", "2026-11-01"))
        db.reviewDao().upsert(ReviewCardEntity("card-1", 2, "2026-11-01", 3, 2))
        db.concertDao().upsertBands(
            listOf(BandEntity("my band", "My Band", isFollowed = true, isBuiltIn = false))
        )
        prefs.saveProfile(
            UserPrefsStore.StoredProfile("Deadhead", "Chicago, IL", "Scarlet ➔ Fire", "Spring '77")
        )
        prefs.saveTriviaProgress(TriviaUserProgress(totalPoints = 12, totalAnswered = 4, totalCorrect = 3))
        prefs.setAlertsEnabled(true)
    }
}
