package com.example.ui.compose

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupManager
import com.example.data.backup.BackupParseResult
import com.example.data.local.AppDatabase
import com.example.data.local.UserPrefsStore
import com.example.di.AppContainer
import com.example.ui.ProfileViewModel
import com.example.ui.UiState
import com.example.ui.UserProfileData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 6.7 restore confirm/cancel via ProfileViewModel (no Compose — see [ComposeUiTestNotes]).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RestoreDialogComposeTest {

    private lateinit var db: AppDatabase
    private lateinit var vm: ProfileViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        AppContainer.resetForTests()
        val app = ApplicationProvider.getApplicationContext<Application>()
        db = AppDatabase.createInMemoryDatabase(app)
        val prefs = UserPrefsStore(app)
        vm = ProfileViewModel(
            userPrefs = prefs,
            backupManager = BackupManager(db, prefs, "test"),
            ui = MutableStateFlow(UiState(userProfile = UserProfileData())),
            onHomeCityChanged = {}
        )
        val json = runBlocking { BackupManager(db, prefs, "test").exportJson() }
        assertTrue(vm.offerPendingRestore(json) is BackupParseResult.Ok)
        assertNotNull(vm.pendingRestore.value)
    }

    @After
    fun tearDown() {
        db.close()
        AppContainer.resetForTests()
        Dispatchers.resetMain()
    }

    @Test
    fun `cancel clears pending restore`() {
        vm.clearPendingRestore()
        assertNull(vm.pendingRestore.value)
    }

    @Test
    fun `confirm clears pending restore`() {
        vm.confirmPendingRestore()
        assertNull(vm.pendingRestore.value)
    }
}
