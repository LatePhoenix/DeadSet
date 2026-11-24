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

    @Test
    fun `overlap keeps backup show review later card and merged trivia`() = runBlocking {
        seedCatalogShow("gd-1")
        db.showDao().upsertUserData(
            UserShowDataEntity("gd-1", isAttended = true, userRating = 5f, userReview = "Local review")
        )
        db.reviewDao().upsert(ReviewCardEntity("card-1", 3, "2026-11-20", 10, 8))
        prefs.saveTriviaProgress(
            TriviaUserProgress(
                totalPoints = 500,
                totalAnswered = 20,
                unlockedBadgeIds = setOf("local_badge"),
                dailyCompletedDate = "2026-11-21",
                currentStreak = 3
            )
        )

        seedUserData() // backup side: rating 4.5, card dated 2026-11-01, 12 points
        val json = manager.exportJson()

        // Restore onto a DB that still has the "local" values above for overlap checks.
        openFreshDb()
        seedCatalogShow("gd-1")
        db.showDao().upsertUserData(
            UserShowDataEntity("gd-1", isAttended = true, userRating = 5f, userReview = "Local review")
        )
        db.reviewDao().upsert(ReviewCardEntity("card-1", 3, "2026-11-20", 10, 8))
        prefs.saveTriviaProgress(
            TriviaUserProgress(
                totalPoints = 500,
                totalAnswered = 20,
                unlockedBadgeIds = setOf("local_badge"),
                dailyCompletedDate = "2026-11-21",
                currentStreak = 3
            )
        )

        val parsed = manager.parse(json) as BackupParseResult.Ok
        manager.importDocument(parsed.document)

        // Show: backup wins
        val user = db.showDao().getAllUserShowData().first { it.showId == "gd-1" }
        assertEquals("Great night", user.userReview)
        assertEquals(4.5f, user.userRating, 0.01f)

        // Card: local has later lastReviewedDate → local kept
        val card = db.reviewDao().get("card-1")!!
        assertEquals("2026-11-20", card.lastReviewedDate)
        assertEquals(3, card.box)
        assertEquals(10, card.timesSeen)

        // Trivia: max points + union badges
        val trivia = prefs.loadTriviaProgress()
        assertEquals(500, trivia.totalPoints)
        assertEquals(20, trivia.totalAnswered)
        assertTrue(trivia.unlockedBadgeIds.contains("local_badge"))
    }

    @Test
    fun `trivia merge via import keeps higher points and badge union`() = runBlocking {
        seedCatalogShow("gd-1")
        prefs.saveTriviaProgress(
            TriviaUserProgress(
                totalPoints = 900,
                totalAnswered = 10,
                unlockedBadgeIds = setOf("a"),
                dailyCompletedDate = "2026-11-10",
                currentStreak = 1
            )
        )
        seedUserData() // 12 points, no badges in seed — wait seed has 12 points
        // Make backup weaker on points so local must win via merge after export.
        prefs.saveTriviaProgress(
            TriviaUserProgress(
                totalPoints = 50,
                totalAnswered = 2,
                unlockedBadgeIds = setOf("b"),
                dailyCompletedDate = "2026-11-22",
                currentStreak = 9
            )
        )
        val json = manager.exportJson()

        openFreshDb()
        seedCatalogShow("gd-1")
        prefs.saveTriviaProgress(
            TriviaUserProgress(
                totalPoints = 900,
                totalAnswered = 10,
                unlockedBadgeIds = setOf("a"),
                dailyCompletedDate = "2026-11-10",
                currentStreak = 1
            )
        )
        val parsed = manager.parse(json) as BackupParseResult.Ok
        manager.importDocument(parsed.document)
        val trivia = prefs.loadTriviaProgress()
        assertEquals(900, trivia.totalPoints)
        assertEquals(setOf("a", "b"), trivia.unlockedBadgeIds)
        assertEquals("2026-11-22", trivia.dailyCompletedDate)
        assertEquals(9, trivia.currentStreak)
    }

    @Test
    fun `catalog id collision skips custom show and leaves catalog setlist`() = runBlocking {
        seedCatalogShow("gd-1")
        db.songDao().insertSongs(listOf(SongEntity("truckin", "Truckin'")))
        db.showDao().replaceSongPerformances(
            "gd-1",
            listOf(
                SongPerformanceEntity(
                    showId = "gd-1", songId = "truckin", songName = "Truckin'",
                    setType = "SET_1", trackOrder = 0, transitionOut = "NONE"
                )
            )
        )
        // Build a backup whose "custom" show reuses the catalog id.
        seedUserData()
        val json = manager.exportJson()
        val root = JSONObject(json)
        val customs = root.getJSONArray("customShows")
        val forged = customs.getJSONObject(0)
        forged.put("id", "gd-1")
        forged.getJSONArray("performances").getJSONObject(0).put("songName", "FORGED")
        root.put("customShows", org.json.JSONArray().put(forged))

        openFreshDb()
        seedCatalogShow("gd-1")
        db.songDao().insertSongs(listOf(SongEntity("truckin", "Truckin'")))
        db.showDao().replaceSongPerformances(
            "gd-1",
            listOf(
                SongPerformanceEntity(
                    showId = "gd-1", songId = "truckin", songName = "Truckin'",
                    setType = "SET_1", trackOrder = 0, transitionOut = "NONE"
                )
            )
        )

        val parsed = manager.parse(root.toString()) as BackupParseResult.Ok
        val result = manager.importDocument(parsed.document)
        assertEquals(1, result.skippedCatalogCollisions)
        assertEquals(0, result.customShowsUpserted)
        val songs = db.showDao().getSongsForShowOnce("gd-1")
        assertEquals(1, songs.size)
        assertEquals("Truckin'", songs.single().songName)
        assertTrue(db.showDao().isNonCustomCatalogShow("gd-1"))
    }

    @Test
    fun `oversize conceptual reject — parse still requires valid JSON under limit`() {
        // File-size gate lives in MainActivity (byte read). Guard the constant here.
        assertTrue(BACKUP_MAX_BYTES == 5 * 1024 * 1024)
        assertTrue(manager.parse("{") is BackupParseResult.Err)
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
