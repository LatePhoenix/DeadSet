package com.example.ui.compose

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.example.ui.UserProfileData
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.UserProfileScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** H4: real [UserProfileScreen] and [StatsScreen] mounts. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProfileStatsComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun pauseAnims() {
        composeRule.pauseInfiniteAnimations()
    }

    @Test
    fun `profile edit save updates callback`() {
        var saved: UserProfileData? = null
        composeRule.setContent {
            UserProfileScreen(
                allShows = emptyList(),
                attendedShows = emptyList(),
                stats = null,
                profileData = sampleProfile("Old"),
                onUpdateProfile = { saved = it },
                onShowClick = {},
                onToggleAttendance = {},
                onExploreCatalogClick = {}
            )
        }
        composeRule.advanceUi()
        composeRule.clickTag("edit_profile_button")
        composeRule.onNodeWithTag("profile_handle_input").performTextReplacement("Bobby")
        composeRule.advanceUi()
        composeRule.clickTag("save_profile_button")
        assertEquals("Bobby", saved?.handle)
    }

    @Test
    fun `profile backup and restore buttons fire callbacks`() {
        var backup = false
        var restore = false
        composeRule.setContent {
            UserProfileScreen(
                allShows = emptyList(),
                attendedShows = emptyList(),
                stats = null,
                profileData = sampleProfile(),
                onUpdateProfile = {},
                onShowClick = {},
                onToggleAttendance = {},
                onExploreCatalogClick = {},
                onBackupClick = { backup = true },
                onRestoreClick = { restore = true }
            )
        }
        composeRule.advanceUi()
        composeRule.scrollListToTag("user_profile_list", "backup_export_button")
        composeRule.clickTag("backup_export_button")
        assertTrue(backup)

        composeRule.scrollListToTag("user_profile_list", "backup_restore_button")
        composeRule.clickTag("backup_restore_button")
        assertTrue(restore)
    }

    @Test
    fun `profile empty attended list shows empty state`() {
        composeRule.setContent {
            UserProfileScreen(
                allShows = listOf(sampleShow()),
                attendedShows = emptyList(),
                stats = null,
                profileData = sampleProfile(),
                onUpdateProfile = {},
                onShowClick = {},
                onToggleAttendance = {},
                onExploreCatalogClick = {}
            )
        }
        composeRule.advanceUi()
        composeRule.scrollListToTag("user_profile_list", "profile_attended_empty")
        composeRule.onNodeWithText("Punch Your First Tour Ticket").assertIsDisplayed()
    }

    @Test
    fun `stats empty state renders title`() {
        composeRule.setContent {
            StatsScreen(stats = null)
        }
        composeRule.advanceUi()
        composeRule.onNodeWithText("Tour Analytics").assertIsDisplayed()
    }

    @Test
    fun `stats with data renders totals and top song`() {
        composeRule.setContent {
            StatsScreen(stats = sampleStats(totalShows = 12, topSong = "Scarlet Begonias", topPlays = 5))
        }
        composeRule.advanceUi(500)
        composeRule.onNodeWithText("Tour Analytics").assertIsDisplayed()
        composeRule.scrollListToTag("stats_list", "stats_metrics_row")
        composeRule.onNodeWithText("12").assertIsDisplayed()
        composeRule.onNodeWithText("3 attended", substring = true).assertIsDisplayed()
        composeRule.scrollListToTag("stats_list", "song_frequencies_card")
        composeRule.onNodeWithText("Scarlet Begonias").assertIsDisplayed()
    }
}

