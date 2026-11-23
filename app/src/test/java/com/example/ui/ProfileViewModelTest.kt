package com.example.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.UserPrefsStore
import com.example.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProfileViewModelTest {

    private lateinit var prefs: UserPrefsStore
    private lateinit var ui: MutableStateFlow<UiState>
    private var homeCalls = 0

    @Before
    fun setUp() {
        AppContainer.resetForTests()
        val app = ApplicationProvider.getApplicationContext<Application>()
        prefs = UserPrefsStore(app)
        ui = MutableStateFlow(UiState(userProfile = UserProfileData(homeCity = "Chicago, IL")))
        homeCalls = 0
    }

    @After
    fun tearDown() {
        AppContainer.resetForTests()
    }

    @Test
    fun `updateUserProfile persists and notifies home city change`() {
        val vm = ProfileViewModel(prefs, ui) { homeCalls++ }
        val next = UserProfileData(
            handle = "Bobby",
            homeCity = "San Francisco, CA",
            favoriteJam = "China ➔ Rider",
            favoriteEraTitle = "Europe '72"
        )
        vm.updateUserProfile(next)
        assertEquals(1, homeCalls)
        assertEquals("Bobby", ui.value.userProfile.handle)
        assertEquals("San Francisco, CA", prefs.loadProfile()!!.homeCity)
        assertEquals("Backstage Laminate Pass updated! 🌹", ui.value.userFeedbackMessage)
    }
}
