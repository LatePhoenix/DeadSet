package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.example.data.local.ShowEntity
import com.example.data.model.*
import com.example.data.seed.TriviaGameMode
import com.example.data.seed.TriviaUserProgress
import com.example.di.AppContainer
import com.example.ui.navigation.NavCommand
import com.example.ui.screens.ConcertsUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Bottom-bar tabs after 6.1 (five tabs). */
enum class MainTab {
    SHOWS,
    LIVE,
    SEGUES,
    ALMANAC,
    ME
}

data class UserProfileData(
    val handle: String = "Uncle John's Bandmate",
    val homeCity: String = "Chicago, IL",
    val favoriteJam: String = "Scarlet Begonias ➔ Fire on the Mountain",
    val favoriteEraTitle: String = "Hiatus & Terrapin ('75–'79)"
)

enum class AttendanceFilter {
    ALL,
    ATTENDED_ONLY,
    UNATTENDED_ONLY,
    TERRAPIN_FLYER,
    UPCOMING_SHOWS
}

enum class SortOrder {
    DATE_DESC,
    DATE_ASC,
    RATING_DESC
}

data class UiState(
    val currentTab: MainTab = MainTab.SHOWS,
    val searchQuery: String = "",
    val selectedEra: DeadEra = DeadEra.ALL,
    val attendanceFilter: AttendanceFilter = AttendanceFilter.ALL,
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
    val selectedSegueFilter: String? = null,
    val shows: List<ShowEntity> = emptyList(),
    /** Each show's setlist in performance order, keyed by show id (from `song_performances`). */
    val songsByShow: Map<String, List<SongEntry>> = emptyMap(),
    val filteredShows: List<ShowEntity> = emptyList(),
    val stats: com.example.data.repository.OverallStats? = null,
    /** Each segue family with its play count and date range, computed from the catalog. */
    val familyStats: List<com.example.data.setlist.FamilyStat> = emptyList(),
    val userProfile: UserProfileData = UserProfileData(),
    val triviaProgress: TriviaUserProgress = TriviaUserProgress(),
    /** Segue School spaced-repetition state by card (question) id. */
    val reviewStates: Map<String, com.example.data.review.ReviewState> = emptyMap(),
    val userFeedbackMessage: String? = null,
    /**
     * Almanac article shown over a tab root without leaving NavHost (keeps Trivia quiz
     * `remember` state alive). Cleared on back.
     */
    val almanacOverlayArticleId: String? = null,
    /** One-shot: Almanac tab should land on Articles pane. */
    val preferAlmanacArticles: Boolean = false
)

/**
 * Composition root for feature ViewModels (6.2). Public API matches the pre-split surface so
 * [com.example.MainActivity] stays unchanged; logic lives in the feature VMs.
 */
class DeadSetViewModel(application: Application) : AndroidViewModel(application) {

    private val container = AppContainer.get(application)
    val repository get() = container.showRepository

    private val _uiState = MutableStateFlow(
        UiState(
            triviaProgress = container.userPrefs.loadTriviaProgress(),
            userProfile = container.userPrefs.loadProfile()?.let {
                UserProfileData(
                    handle = it.handle,
                    homeCity = it.homeCity,
                    favoriteJam = it.favoriteJam,
                    favoriteEraTitle = it.favoriteEraTitle
                )
            } ?: UserProfileData()
        )
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val featureStore = ViewModelStore()
    private val featureFactory = FeatureViewModelFactory(application, container, _uiState)

    val navigationVm: NavigationViewModel =
        ViewModelProvider(featureStore, featureFactory)[NavigationViewModel::class.java]
    val showsVm: ShowsViewModel =
        ViewModelProvider(featureStore, featureFactory)[ShowsViewModel::class.java]
    val concertsVm: ConcertsViewModel =
        ViewModelProvider(featureStore, featureFactory)[ConcertsViewModel::class.java]
    val chaseVm: ChaseViewModel =
        ViewModelProvider(featureStore, featureFactory)[ChaseViewModel::class.java]
    val triviaVm: TriviaViewModel =
        ViewModelProvider(featureStore, featureFactory)[TriviaViewModel::class.java]
    val profileVm: ProfileViewModel =
        ViewModelProvider(featureStore, featureFactory)[ProfileViewModel::class.java]

    val concerts: StateFlow<ConcertsUiState> get() = concertsVm.concerts
    val chaseSongs get() = chaseVm.chaseSongs
    val songNames get() = chaseVm.songNames
    val navigation: Flow<NavCommand> get() = navigationVm.navigation

    init {
        featureFactory.onHomeCityChanged = { city -> concertsVm.locateHome(city) }
        // H2: restored alerts must schedule/cancel the worker and update the Live switch.
        featureFactory.onBackupImported = {
            concertsVm.setAlertsEnabled(container.userPrefs.alertsEnabled())
        }
        concertsVm.locateHome(_uiState.value.userProfile.homeCity)
    }

    override fun onCleared() {
        featureStore.clear()
        super.onCleared()
    }

    fun loadSongNames() = chaseVm.loadSongNames()
    fun addChaseSong(name: String) = chaseVm.addChaseSong(name)
    fun removeChaseSong(songId: String) = chaseVm.removeChaseSong(songId)
    fun appendSongToShow(showId: String, name: String, setType: SetType) =
        chaseVm.appendSongToShow(showId, name, setType)
    fun setRecreation(showId: String, originalId: String?) = showsVm.setRecreation(showId, originalId)
    fun predictNext(showId: String, lastSong: String?, setType: SetType): List<String> =
        chaseVm.predictNext(showId, lastSong, setType)

    fun refreshConcerts() = concertsVm.refreshConcerts()
    fun addBand(name: String) = concertsVm.addBand(name)
    fun setBandFollowed(bandId: String, followed: Boolean) = concertsVm.setBandFollowed(bandId, followed)
    fun removeBand(bandId: String) = concertsVm.removeBand(bandId)
    fun setAlertsEnabled(enabled: Boolean) = concertsVm.setAlertsEnabled(enabled)

    fun songsFor(show: ShowEntity): List<SongEntry> = showsVm.songsFor(show)

    fun setTab(tab: MainTab) = navigationVm.setTab(tab)
    fun openShowDetail(showIdOrDate: String) = navigationVm.openShowDetail(showIdOrDate)
    fun openAddShow() = navigationVm.openAddShow()
    fun openEditShow(showId: String) = navigationVm.openEditShow(showId)
    fun openAboutCredits() = navigationVm.openAboutCredits()
    fun openAlmanac() = navigationVm.openAlmanac()
    fun openAlmanacArticle(articleId: String, asOverlay: Boolean = true) =
        navigationVm.openAlmanacArticle(articleId, asOverlay)
    fun clearAlmanacArticlesPrefer() = navigationVm.clearAlmanacArticlesPrefer()
    fun clearAlmanacOverlay() = navigationVm.clearAlmanacOverlay()
    fun navigateBack(): Boolean = navigationVm.navigateBack()

    fun setSearchQuery(query: String) = showsVm.setSearchQuery(query)
    fun setEra(era: DeadEra) = showsVm.setEra(era)
    fun setAttendanceFilter(filter: AttendanceFilter) = showsVm.setAttendanceFilter(filter)
    fun setSortOrder(order: SortOrder) = showsVm.setSortOrder(order)
    fun filterBySegue(segueTitle: String) = showsVm.filterBySegue(segueTitle)
    fun clearSegueFilter() = showsVm.clearSegueFilter()
    fun toggleAttendance(show: ShowEntity) = showsVm.toggleAttendance(show)
    fun saveReviewAndNotes(
        showId: String,
        reviewText: String,
        rating: Float,
        attendedWith: String,
        favoriteSong: String
    ) = showsVm.saveReviewAndNotes(showId, reviewText, rating, attendedWith, favoriteSong)
    fun toggleSongTransition(show: ShowEntity, songIndex: Int) =
        showsVm.toggleSongTransition(show, songIndex)
    fun deleteShow(show: ShowEntity) = showsVm.deleteShow(show)
    fun saveCustomShow(show: ShowEntity, songs: List<SongEntry>) = showsVm.saveCustomShow(show, songs)
    fun restoreOfflineCatalog() = showsVm.restoreOfflineCatalog()

    fun updateUserProfile(profile: UserProfileData) = profileVm.updateUserProfile(profile)
    suspend fun exportBackupJson(): String = profileVm.exportBackupJson()
    fun parseBackup(json: String) = profileVm.parseBackup(json)
    fun previewBackup(doc: com.example.data.backup.BackupDocument) = profileVm.previewBackup(doc)
    val pendingRestore = profileVm.pendingRestore
    fun offerPendingRestore(json: String) = profileVm.offerPendingRestore(json)
    fun clearPendingRestore() = profileVm.clearPendingRestore()
    fun confirmPendingRestore() = profileVm.confirmPendingRestore()
    fun confirmRestore(doc: com.example.data.backup.BackupDocument) = profileVm.confirmRestore(doc)
    fun setBackupFeedback(message: String) = profileVm.setFeedbackMessage(message)
    fun recordTriviaQuizCompletion(
        answeredCount: Int,
        correctCount: Int,
        pointsEarned: Int,
        maxStreakInRound: Int,
        mode: TriviaGameMode
    ) = triviaVm.recordTriviaQuizCompletion(
        answeredCount, correctCount, pointsEarned, maxStreakInRound, mode
    )
    fun recordCardAnswer(cardId: String, correct: Boolean) = triviaVm.recordCardAnswer(cardId, correct)
    fun clearFeedbackMessage() = profileVm.clearFeedbackMessage()
}

/**
 * Builds feature ViewModels that share one [UiState] flow. Used by [DeadSetViewModel] and tests.
 * [NavigationViewModel] is cached so [ShowsViewModel] uses the same instance as the store.
 */
class FeatureViewModelFactory(
    private val app: Application,
    private val container: AppContainer,
    private val ui: MutableStateFlow<UiState>
) : ViewModelProvider.Factory {
    private var navigationVm: NavigationViewModel? = null

    /** Set after [ConcertsViewModel] exists so profile home-city edits geocode. */
    var onHomeCityChanged: (String) -> Unit = {}

    /** Set after [ConcertsViewModel] exists so restore reapplies alerts (H2). */
    var onBackupImported: () -> Unit = {}

    private fun navigation(): NavigationViewModel =
        navigationVm ?: NavigationViewModel(ui).also { navigationVm = it }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(NavigationViewModel::class.java) -> navigation() as T
            modelClass.isAssignableFrom(ShowsViewModel::class.java) -> ShowsViewModel(
                app = app,
                database = container.database,
                repository = container.showRepository,
                userPrefs = container.userPrefs,
                ui = ui,
                navigation = navigation()
            ) as T
            modelClass.isAssignableFrom(ConcertsViewModel::class.java) -> ConcertsViewModel(
                app = app,
                concertRepository = container.concertRepository,
                userPrefs = container.userPrefs
            ) as T
            modelClass.isAssignableFrom(ChaseViewModel::class.java) -> ChaseViewModel(
                database = container.database,
                repository = container.showRepository,
                showsState = { ui.value }
            ) as T
            modelClass.isAssignableFrom(TriviaViewModel::class.java) -> TriviaViewModel(
                database = container.database,
                userPrefs = container.userPrefs,
                ui = ui
            ) as T
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> ProfileViewModel(
                userPrefs = container.userPrefs,
                backupManager = container.backupManager,
                ui = ui,
                onHomeCityChanged = { city -> onHomeCityChanged(city) },
                onBackupImported = { onBackupImported() }
            ) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
