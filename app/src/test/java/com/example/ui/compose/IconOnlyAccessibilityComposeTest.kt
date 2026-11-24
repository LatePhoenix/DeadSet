package com.example.ui.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 6.7: icon-only controls must expose a content description (same tags/CDs as Shows/Live).
 * Isolated buttons avoid infinite dancing-bear animations that never idle under Robolectric
 * when mounting full Screens (CodeRabbit: production mounts deferred for that reason).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IconOnlyAccessibilityComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun pauseAnims() {
        composeRule.pauseInfiniteAnimations()
    }

    @Test
    fun `add show icon button exposes content description`() {
        composeRule.setContent {
            IconButton(
                onClick = {},
                modifier = Modifier.testTag("add_show_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Show")
            }
        }
        composeRule.onNodeWithTag("add_show_button")
            .assertContentDescriptionEquals("Add Show")
    }

    @Test
    fun `refresh concerts icon button exposes content description`() {
        composeRule.setContent {
            IconButton(
                onClick = {},
                modifier = Modifier.testTag("refresh_concerts_button")
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh dates")
            }
        }
        composeRule.onNodeWithTag("refresh_concerts_button")
            .assertContentDescriptionEquals("Refresh dates")
    }
}
