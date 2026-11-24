package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupDocument
import com.example.data.backup.BackupManager
import com.example.data.backup.BackupParseResult
import com.example.data.backup.BackupPreview
import com.example.data.backup.PendingRestore
import com.example.data.local.UserPrefsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Profile laminate fields + feedback + backup/restore (6.2 / 6.4 / H3). */
class ProfileViewModel(
    private val userPrefs: UserPrefsStore,
    private val backupManager: BackupManager,
    private val ui: MutableStateFlow<UiState>,
    private val onHomeCityChanged: (String) -> Unit,
    private val onBackupImported: () -> Unit = {}
) : ViewModel() {

    private val _pendingRestore = MutableStateFlow<PendingRestore?>(null)
    /** Parsed backup waiting for confirm/cancel — not saved-instance state (H3). */
    val pendingRestore: StateFlow<PendingRestore?> = _pendingRestore.asStateFlow()

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

    /**
     * After a successful parse, holds the document + preview for the confirm dialog.
     * Clears any previous pending restore on error.
     */
    fun offerPendingRestore(json: String): BackupParseResult {
        return when (val parsed = backupManager.parse(json)) {
            is BackupParseResult.Ok -> {
                _pendingRestore.value = PendingRestore(
                    document = parsed.document,
                    preview = backupManager.preview(parsed.document)
                )
                parsed
            }
            is BackupParseResult.Err -> {
                _pendingRestore.value = null
                parsed
            }
        }
    }

    fun clearPendingRestore() {
        _pendingRestore.value = null
    }

    fun confirmRestore(doc: BackupDocument) {
        viewModelScope.launch { confirmRestoreAwait(doc) }
    }

    /** Confirm the pending restore (clears pending first) or no-op if none. */
    fun confirmPendingRestore() {
        val pending = _pendingRestore.value ?: return
        _pendingRestore.value = null
        confirmRestore(pending.document)
    }

    /** Suspend entry for tests (H2 alerts wiring) and the ViewModel launch path. */
    suspend fun confirmRestoreAwait(doc: BackupDocument) {
        val result = backupManager.importDocument(doc)
        applyImportedPrefsToUi()
        onBackupImported()
        setFeedbackMessage(result.feedbackMessage())
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
