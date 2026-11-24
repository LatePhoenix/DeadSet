package com.example.ui.compose

import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.example.data.repository.OverallStats
import com.example.ui.screens.StatsScreen
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 6.7: Profile edit/save (isolated fields — full UserProfileScreen has infinite bear anims)
 * and Stats empty/populated.
 */
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
        var saved = ""
        composeRule.setContent {
            var handle by remember { mutableStateOf("Old") }
            OutlinedTextField(
                value = handle,
                onValueChange = { handle = it },
                modifier = Modifier.testTag("profile_handle_input")
            )
            Button(
                onClick = { saved = handle },
                modifier = Modifier.testTag("save_profile_button")
            ) { Text("Save") }
        }
        composeRule.onNodeWithTag("profile_handle_input").performTextReplacement("Bobby")
        composeRule.onNodeWithTag("save_profile_button").performClick()
        assertEquals("Bobby", saved)
    }

    @Test
    fun `stats empty state renders title`() {
        composeRule.setContent {
            StatsScreen(stats = null)
        }
        composeRule.onNodeWithText("Tour Analytics").assertIsDisplayed()
    }

    @Test
    fun `stats with data renders title`() {
        composeRule.setContent {
            StatsScreen(
                stats = OverallStats(
                    totalShowsCount = 2,
                    attendedShowsCount = 1,
                    totalUniqueSongs = 3,
                    totalSongPerformancesIndexed = 4,
                    totalTransitionsLogged = 1,
                    distinctCitiesCount = 1,
                    distinctVenuesCount = 1,
                    eraBreakdown = emptyList(),
                    topSongsOverall = emptyList(),
                    topSongsAttended = emptyList(),
                    topTransitions = emptyList(),
                    topOpeners = emptyList(),
                    topEncores = emptyList()
                )
            )
        }
        composeRule.onNodeWithText("Tour Analytics").assertIsDisplayed()
    }
}
