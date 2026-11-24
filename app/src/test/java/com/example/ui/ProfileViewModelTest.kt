package com.example.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupManager
import com.example.data.backup.BackupParseResult
import com.example.data.local.AppDatabase
import com.example.data.local.ShowCatalogEntity
import com.example.data.local.UserPrefsStore
import com.example.data.local.UserShowDataEntity
import com.example.data.seed.TriviaUserProgress
import com.example.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProfileViewModelTest {

    private lateinit var db: AppDatabase
    private lateinit var prefs: UserPrefsStore
    private lateinit var ui: MutableStateFlow<UiState>
    private var homeCalls = 0

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        AppContainer.resetForTests()
        val app = ApplicationProvider.getApplicationContext<Application>()
        db = AppDatabase.createInMemoryDatabase(app)
        prefs = UserPrefsStore(app)
        ui = MutableStateFlow(UiState(userProfile = UserProfileData(homeCity = "Chicago, IL")))
        homeCalls = 0
    }

    @After
    fun tearDown() {
        db.close()
        AppContainer.resetForTests()
        Dispatchers.resetMain()
    }

    @Test
    fun `updateUserProfile persists and notifies home city change`() {
        val vm = ProfileViewModel(
            userPrefs = prefs,
            backupManager = BackupManager(db, prefs, "test"),
            ui = ui,
            onHomeCityChanged = { homeCalls++ }
        )
        val next = UserProfileData(
            handle = "Bobby",
            homeCity = "San Francisco, CA",
            favoriteJam = "China ➔ Rider",
            favoriteEraTitle = "Europe '72"
        )
        vm.updateUserProfile(next)
        assertEquals(1, homeCalls)
        assertEquals("Bobby", ui.value.userProfile.handle)
        assertEquals("San Francisco, CA", prefs.loadProfile()!!.homeCity)
        assertEquals("Backstage Laminate Pass updated! 🌹", ui.value.userFeedbackMessage)
    }

    @Test
    fun `pending restore set on parse and cleared on confirm and cancel`() = runBlocking {
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
        val json = BackupManager(db, prefs, "test").exportJson()

        val vm = ProfileViewModel(
            userPrefs = prefs,
            backupManager = BackupManager(db, prefs, "test"),
            ui = ui,
            onHomeCityChanged = {}
        )
        assertNull(vm.pendingRestore.value)

        val parsed = vm.offerPendingRestore(json)
        assertTrue(parsed is BackupParseResult.Ok)
        assertNotNull(vm.pendingRestore.first { it != null })

        vm.clearPendingRestore()
        assertNull(vm.pendingRestore.value)

        assertTrue(vm.offerPendingRestore(json) is BackupParseResult.Ok)
        val pending = vm.pendingRestore.value!!
        vm.clearPendingRestore()
        // Await import so tearDown doesn't close the DB mid-transaction (H4 flake fix).
        vm.confirmRestoreAwait(pending.document)
        assertNull(vm.pendingRestore.value)
        assertNotNull(ui.value.userFeedbackMessage)
    }
}
