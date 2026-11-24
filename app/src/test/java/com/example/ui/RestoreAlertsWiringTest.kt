package com.example.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupManager
import com.example.data.backup.BackupParseResult
import com.example.data.local.AppDatabase
import com.example.data.local.CatalogAssetImporter
import com.example.data.local.ShowCatalogEntity
import com.example.data.local.UserPrefsStore
import com.example.data.local.UserShowDataEntity
import com.example.data.repository.ConcertRepository
import com.example.data.seed.TriviaUserProgress
import com.example.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * H2: after restore with alerts on, [ConcertsViewModel.setAlertsEnabled] must run
 * (schedules worker + updates Live UI state).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RestoreAlertsWiringTest {

    private lateinit var app: Application
    private lateinit var db: AppDatabase
    private lateinit var prefs: UserPrefsStore
    private lateinit var concertsVm: ConcertsViewModel
    private lateinit var profileVm: ProfileViewModel
    private var alertsCalls = 0

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        AppContainer.resetForTests()
        app = ApplicationProvider.getApplicationContext()
        db = AppDatabase.createInMemoryDatabase(app)
        prefs = UserPrefsStore(app)
        prefs.setImportedCatalogVersion(CatalogAssetImporter.BUNDLED_CATALOG_VERSION)
        prefs.setAlertsEnabled(false)
        val ui = MutableStateFlow(UiState())
        val backupManager = BackupManager(db, prefs, "test")
        val concertRepo = ConcertRepository(db.concertDao())
        concertsVm = ConcertsViewModel(app, concertRepo, prefs)
        profileVm = ProfileViewModel(
            userPrefs = prefs,
            backupManager = backupManager,
            ui = ui,
            onHomeCityChanged = {},
            onBackupImported = {
                alertsCalls++
                concertsVm.setAlertsEnabled(prefs.alertsEnabled())
            }
        )
    }

    @After
    fun tearDown() {
        db.close()
        AppContainer.resetForTests()
        Dispatchers.resetMain()
    }

    @Test
    fun `restore with alerts on reaches concerts ViewModel`() = runBlocking {
        db.showDao().upsertCatalog(
            ShowCatalogEntity(
                id = "gd-1",
                date = "1977-05-08",
                venue = "Barton Hall",
                city = "Ithaca",
                stateOrCountry = "NY",
                eraId = "spring_77",
                tourName = ""
            )
        )
        db.showDao().upsertUserData(UserShowDataEntity("gd-1", isAttended = true))
        prefs.saveTriviaProgress(TriviaUserProgress())
        prefs.setAlertsEnabled(true)
        val json = BackupManager(db, prefs, "test").exportJson()

        // Phone starts with alerts off; backup has alerts on.
        prefs.setAlertsEnabled(false)
        concertsVm.setAlertsEnabled(false)
        alertsCalls = 0

        val doc = (profileVm.parseBackup(json) as BackupParseResult.Ok).document
        profileVm.confirmRestoreAwait(doc)
        assertTrue(alertsCalls >= 1)
        assertTrue(prefs.alertsEnabled())
        assertTrue(concertsVm.concerts.value.alertsEnabled)
    }
}
