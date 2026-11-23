package com.example.ui

import androidx.lifecycle.ViewModel
import com.example.data.local.UserPrefsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Profile laminate fields + feedback (6.2). */
class ProfileViewModel(
    private val userPrefs: UserPrefsStore,
    private val ui: MutableStateFlow<UiState>,
    private val onHomeCityChanged: (String) -> Unit
) : ViewModel() {

    fun updateUserProfile(profile: UserProfileData) {
        if (profile.homeCity != ui.value.userProfile.homeCity) onHomeCityChanged(profile.homeCity)
        userPrefs.saveProfile(
            UserPrefsStore.StoredProfile(
                handle = profile.handle,
                homeCity = profile.homeCity,
                favoriteJam = profile.favoriteJam,
                favoriteEraTitle = profile.favoriteEraTitle
            )
        )
        ui.update {
            it.copy(
                userProfile = profile,
                userFeedbackMessage = "Backstage Laminate Pass updated! 🌹"
            )
        }
    }

    fun clearFeedbackMessage() {
        ui.update { it.copy(userFeedbackMessage = null) }
    }
}
