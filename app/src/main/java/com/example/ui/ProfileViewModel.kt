package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupDocument
import com.example.data.backup.BackupManager
import com.example.data.backup.BackupParseResult
import com.example.data.backup.BackupPreview
import com.example.data.local.UserPrefsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Profile laminate fields + feedback + backup/restore (6.2 / 6.4). */
class ProfileViewModel(
    private val userPrefs: UserPrefsStore,
    private val backupManager: BackupManager,
    private val ui: MutableStateFlow<UiState>,
    private val onHomeCityChanged: (String) -> Unit,
    private val onBackupImported: () -> Unit = {}
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

    fun setFeedbackMessage(message: String) {
        ui.update { it.copy(userFeedbackMessage = message) }
    }

    suspend fun exportBackupJson(): String = backupManager.exportJson()

    fun parseBackup(json: String): BackupParseResult = backupManager.parse(json)

    fun previewBackup(doc: BackupDocument): BackupPreview = backupManager.preview(doc)

    fun confirmRestore(doc: BackupDocument) {
        viewModelScope.launch {
            val result = backupManager.importDocument(doc)
            applyImportedPrefsToUi()
            onBackupImported()
            setFeedbackMessage(result.feedbackMessage())
        }
    }

    private fun applyImportedPrefsToUi() {
        val profile = userPrefs.loadProfile()?.let {
            UserProfileData(
                handle = it.handle,
                homeCity = it.homeCity,
                favoriteJam = it.favoriteJam,
                favoriteEraTitle = it.favoriteEraTitle
            )
        } ?: ui.value.userProfile
        if (profile.homeCity != ui.value.userProfile.homeCity) onHomeCityChanged(profile.homeCity)
        ui.update {
            it.copy(
                userProfile = profile,
                triviaProgress = userPrefs.loadTriviaProgress()
            )
        }
    }
}
