package com.example.ui.compose

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.data.local.ShowEntity
import com.example.ui.screens.ShowDetailScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** H4: real [ShowDetailScreen] for catalog and custom shows. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShowsDetailComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun pauseAnims() {
        composeRule.pauseInfiniteAnimations()
    }

    @Test
    fun `catalog detail toggles attendance and has share and back cds`() {
        var toggled: ShowEntity? = null
        val show = sampleShow(attended = false)
        composeRule.setContent {
            ShowDetailScreen(
                show = show,
                songs = listOf(sampleSong()),
                onBack = {},
                onToggleAttendance = { toggled = it },
                onToggleSongTransition = { _, _ -> },
                onSaveReviewAndNotes = { _, _, _, _, _ -> },
                onDeleteShow = { }
            )
        }
        composeRule.advanceUi()
        composeRule.onNodeWithTag("detail_back_button")
            .assertContentDescriptionEquals("Back")
        composeRule.onNodeWithTag("share_setlist_button").assertIsDisplayed()
        assertEquals(
            0,
            composeRule.onAllNodesWithContentDescription("Delete Show").fetchSemanticsNodes().size
        )
        composeRule.onNodeWithTag("toggle_attendance_button").performClick()
        composeRule.advanceUi()
        assertEquals(show.id, toggled?.id)
    }

    @Test
    fun `custom detail shows delete and toggles attendance`() {
        var deleted: ShowEntity? = null
        var toggled = false
        val show = sampleShow(id = "custom-1", custom = true, attended = false)
        composeRule.setContent {
            ShowDetailScreen(
                show = show,
                songs = emptyList(),
                onBack = {},
                onToggleAttendance = { toggled = true },
                onToggleSongTransition = { _, _ -> },
                onSaveReviewAndNotes = { _, _, _, _, _ -> },
                onDeleteShow = { deleted = it }
            )
        }
        composeRule.advanceUi()
        composeRule.onNodeWithContentDescription("Delete Show").assertIsDisplayed()
        composeRule.onNodeWithTag("toggle_attendance_button").performClick()
        composeRule.advanceUi()
        assertTrue(toggled)
        composeRule.onNodeWithContentDescription("Delete Show").performClick()
        composeRule.advanceUi()
        assertEquals("custom-1", deleted?.id)
    }
}
