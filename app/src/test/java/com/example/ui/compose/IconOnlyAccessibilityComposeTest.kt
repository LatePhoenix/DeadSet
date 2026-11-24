package com.example.ui.compose

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.example.data.model.DeadEra
import com.example.ui.AttendanceFilter
import com.example.ui.SortOrder
import com.example.ui.screens.ConcertsUiState
import com.example.ui.screens.LiveConcertsScreen
import com.example.ui.screens.ShowsListScreen
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** H4: icon-only CDs on real [ShowsListScreen] and [LiveConcertsScreen]. */
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
            ShowsListScreen(
                shows = emptyList(),
                searchQuery = "",
                onSearchChange = {},
                selectedEra = DeadEra.ALL,
                onEraSelected = {},
                attendanceFilter = AttendanceFilter.ALL,
                onAttendanceFilterChanged = {},
                sortOrder = SortOrder.DATE_DESC,
                onSortOrderChanged = {},
                selectedSegueFilter = null,
                onClearSegueFilter = {},
                onShowClick = {},
                onAddShowClick = {}
            )
        }
        composeRule.advanceUi()
        composeRule.onNodeWithTag("add_show_button")
            .assertContentDescriptionEquals("Add Show")
    }

    @Test
    fun `refresh concerts icon button exposes content description`() {
        composeRule.setContent {
            LiveConcertsScreen(
                state = ConcertsUiState(),
                onRefresh = {},
                onAddBand = {},
                onSetFollowed = { _, _ -> },
                onRemoveBand = {}
            )
        }
        composeRule.advanceUi(200)
        composeRule.onNodeWithTag("refresh_concerts_button")
            .assertContentDescriptionEquals("Refresh dates")
    }
}
