package com.example.ui

import com.example.data.local.ShowEntity
import com.example.ui.navigation.NavCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationViewModelTest {

    @Test
    fun `setTab clears almanac overlay`() {
        val ui = MutableStateFlow(
            UiState(currentTab = MainTab.SHOWS, almanacOverlayArticleId = "scarlet-fire")
        )
        val vm = NavigationViewModel(ui)
        vm.setTab(MainTab.ME)
        assertEquals(MainTab.ME, ui.value.currentTab)
        assertNull(ui.value.almanacOverlayArticleId)
    }

    @Test
    fun `openShowDetail resolves by date`() = runTest {
        val show = ShowEntity(
            id = "cornell",
            date = "1977-05-08",
            venue = "Barton Hall",
            city = "Ithaca",
            stateOrCountry = "NY",
            eraId = "",
            tourName = "",
            bandName = "Grateful Dead"
        )
        val ui = MutableStateFlow(UiState(shows = listOf(show)))
        val vm = NavigationViewModel(ui)
        var cmd: NavCommand? = null
        val job = launch { cmd = vm.navigation.first() }
        vm.openShowDetail("1977-05-08")
        job.join()
        assertEquals(NavCommand.ToShow("cornell"), cmd)
    }

    @Test
    fun `navigateBack clears overlay first`() {
        val ui = MutableStateFlow(UiState(almanacOverlayArticleId = "help-slip-franklins"))
        val vm = NavigationViewModel(ui)
        assertTrue(vm.navigateBack())
        assertNull(ui.value.almanacOverlayArticleId)
    }
}
