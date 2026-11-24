package com.example.ui.compose

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.backup.BackupManager
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** 6.7: restore dialog confirm/cancel clears pending restore in the ViewModel. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RestoreDialogComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var vm: ProfileViewModel

    @Before
    fun setUp() {
        composeRule.pauseInfiniteAnimations()
        Dispatchers.setMain(Dispatchers.Unconfined)
        AppContainer.resetForTests()
        val app = RuntimeEnvironment.getApplication()
        db = AppDatabase.createInMemoryDatabase(app)
        val prefs = UserPrefsStore(app)
        vm = ProfileViewModel(
            userPrefs = prefs,
            backupManager = BackupManager(db, prefs, "test"),
            ui = MutableStateFlow(UiState(userProfile = UserProfileData())),
            onHomeCityChanged = {}
        )
        val json = runBlocking { BackupManager(db, prefs, "test").exportJson() }
        vm.offerPendingRestore(json)
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
        composeRule.setContent {
            val pending by vm.pendingRestore.collectAsStateWithLifecycle()
            pending?.let { p ->
                AlertDialog(
                    onDismissRequest = { vm.clearPendingRestore() },
                    title = { Text("Restore from backup?") },
                    text = { Text(p.preview.summaryLines()) },
                    confirmButton = {
                        TextButton(
                            onClick = { vm.confirmPendingRestore() },
                            modifier = Modifier.testTag("backup_restore_confirm")
                        ) { Text("Restore") }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { vm.clearPendingRestore() },
                            modifier = Modifier.testTag("backup_restore_cancel")
                        ) { Text("Cancel") }
                    }
                )
            }
        }
        composeRule.onNodeWithText("Restore from backup?").assertIsDisplayed()
        composeRule.onNodeWithTag("backup_restore_cancel").performClick()
        assertNull(vm.pendingRestore.value)
    }

    @Test
    fun `confirm clears pending restore`() {
        composeRule.setContent {
            val pending by vm.pendingRestore.collectAsStateWithLifecycle()
            pending?.let {
                AlertDialog(
                    onDismissRequest = { vm.clearPendingRestore() },
                    title = { Text("Restore from backup?") },
                    text = { Text("ok") },
                    confirmButton = {
                        TextButton(
                            onClick = { vm.confirmPendingRestore() },
                            modifier = Modifier.testTag("backup_restore_confirm")
                        ) { Text("Restore") }
                    },
                    dismissButton = {
                        TextButton(onClick = { vm.clearPendingRestore() }) { Text("Cancel") }
                    }
                )
            }
        }
        composeRule.onNodeWithTag("backup_restore_confirm").performClick()
        assertNull(vm.pendingRestore.value)
    }
}
