package com.example.ui.compose

import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 6.7: attendance toggle (isolated — full ShowDetailScreen / ShowsListScreen pull in
 * infinite dancing-bear animations that never idle under Robolectric).
 */
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
    fun `attendance toggle fires callback`() {
        var attended = false
        composeRule.setContent {
            var selected by remember { mutableStateOf(false) }
            FilterChip(
                selected = selected,
                onClick = {
                    selected = !selected
                    attended = selected
                },
                label = { Text(if (selected) "Attended" else "I Was There") },
                modifier = Modifier.testTag("toggle_attendance_button")
            )
        }
        composeRule.onNodeWithTag("toggle_attendance_button").assertIsDisplayed().performClick()
        assertTrue(attended)
    }
}
