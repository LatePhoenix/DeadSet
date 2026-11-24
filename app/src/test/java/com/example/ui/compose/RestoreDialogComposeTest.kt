package com.example.ui.compose

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.backup.BackupDocument
import com.example.data.backup.BackupPrefs
import com.example.data.backup.BackupPreview
import com.example.data.backup.PendingRestore
import com.example.ui.components.RestoreConfirmDialog
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** H4: real [RestoreConfirmDialog] — wording, counts, Restore/Cancel callbacks. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RestoreDialogComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val pending = PendingRestore(
        document = BackupDocument(
            version = 1,
            exportedAt = "2026-01-01T00:00:00Z",
            appVersion = "test",
            userShowData = emptyList(),
            customShows = emptyList(),
            chaseSongs = emptyList(),
            reviewCards = emptyList(),
            bands = emptyList(),
            prefs = BackupPrefs(
                profile = null,
                triviaProgress = emptyTriviaProgress(),
                alertsEnabled = false
            )
        ),
        preview = BackupPreview(
            attendedCount = 2,
            userShowDataCount = 2,
            customShowCount = 1,
            chaseSongCount = 0,
            reviewCardCount = 3,
            bandCount = 1,
            hasProfile = true,
            alertsEnabled = false
        )
    )

    @Before
    fun pauseAnims() {
        composeRule.pauseInfiniteAnimations()
    }

    @Test
    fun `dialog shows replace wording and preview counts`() {
        composeRule.setContent {
            RestoreConfirmDialog(pending = pending, onConfirm = {}, onDismiss = {})
        }
        composeRule.advanceUi()
        composeRule.onNodeWithText("Restore from backup?").assertIsDisplayed()
        composeRule.onNodeWithText("replace the matching items", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("2 attended shows", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("1 custom show", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("3 review cards", substring = true).assertIsDisplayed()
    }

    @Test
    fun `restore confirm callback fires`() {
        var confirmed = false
        composeRule.setContent {
            RestoreConfirmDialog(
                pending = pending,
                onConfirm = { confirmed = true },
                onDismiss = {}
            )
        }
        composeRule.advanceUi()
        composeRule.onNodeWithTag("backup_restore_confirm").performClick()
        composeRule.advanceUi()
        assertTrue(confirmed)
    }

    @Test
    fun `cancel dismiss callback fires`() {
        var dismissed = false
        composeRule.setContent {
            RestoreConfirmDialog(
                pending = pending,
                onConfirm = {},
                onDismiss = { dismissed = true }
            )
        }
        composeRule.advanceUi()
        composeRule.onNodeWithTag("backup_restore_cancel").performClick()
        composeRule.advanceUi()
        assertTrue(dismissed)
    }
}
