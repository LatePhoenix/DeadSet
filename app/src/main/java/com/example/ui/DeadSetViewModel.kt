package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ShowEntity
import com.example.data.model.*
import com.example.data.repository.OverallStats
import com.example.data.repository.ShowRepository
import com.example.data.seed.TriviaGameMode
import com.example.data.seed.TriviaUserProgress
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

enum class MainTab {
    SHOWS,
    LIVE_TICKETS,
    ATTENDANCE,
    TRANSITIONS,
    STATS,
    TRIVIA,
    PROFILE
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

sealed class ScreenState {
    data object Main : ScreenState()
    data class ShowDetail(val showId: String) : ScreenState()
    data class AddEditShow(val showId: String? = null) : ScreenState()
}

data class UiState(
    val currentTab: MainTab = MainTab.SHOWS,
    val screenState: ScreenState = ScreenState.Main,
    val searchQuery: String = "",
    val selectedEra: DeadEra = DeadEra.ALL,
    val attendanceFilter: AttendanceFilter = AttendanceFilter.ALL,
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
    val selectedSegueFilter: String? = null,
    val shows: List<ShowEntity> = emptyList(),
    val filteredShows: List<ShowEntity> = emptyList(),
    val stats: OverallStats? = null,
    val userProfile: UserProfileData = UserProfileData(),
    val triviaProgress: TriviaUserProgress = TriviaUserProgress(),
    val userFeedbackMessage: String? = null
)

class DeadSetViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val repository = ShowRepository(database.showDao())

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.checkAndSeedDatabase()
        }

        viewModelScope.launch {
            repository.allShows.collect { allShowsList ->
                val current = _uiState.value
                val filtered = applyFilters(
                    shows = allShowsList,
                    query = current.searchQuery,
                    era = current.selectedEra,
                    attendance = current.attendanceFilter,
                    sort = current.sortOrder,
                    segueFilter = current.selectedSegueFilter
                )
                val stats = repository.computeOverallStats(allShowsList)
                _uiState.update {
                    it.copy(
                        shows = allShowsList,
                        filteredShows = filtered,
                        stats = stats
                    )
                }
            }
        }
    }

    fun setTab(tab: MainTab) {
        _uiState.update { it.copy(currentTab = tab, screenState = ScreenState.Main) }
    }

    fun openShowDetail(showIdOrDate: String) {
        val matchedShowId = _uiState.value.shows.firstOrNull {
            it.id.equals(showIdOrDate, ignoreCase = true) ||
                it.date.equals(showIdOrDate, ignoreCase = true) ||
                it.id.startsWith(showIdOrDate, ignoreCase = true)
        }?.id ?: showIdOrDate
        _uiState.update { it.copy(screenState = ScreenState.ShowDetail(matchedShowId)) }
    }

    fun openAddShow() {
        _uiState.update { it.copy(screenState = ScreenState.AddEditShow(null)) }
    }

    fun openEditShow(showId: String) {
        _uiState.update { it.copy(screenState = ScreenState.AddEditShow(showId)) }
    }

    fun navigateBack(): Boolean {
        val current = _uiState.value.screenState
        return if (current !is ScreenState.Main) {
            _uiState.update { it.copy(screenState = ScreenState.Main) }
            true
        } else {
            false
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { current ->
            val updated = current.copy(searchQuery = query)
            updated.copy(
                filteredShows = applyFilters(
                    current.shows,
                    query,
                    current.selectedEra,
                    current.attendanceFilter,
                    current.sortOrder,
                    current.selectedSegueFilter
                )
            )
        }
    }

    fun setEra(era: DeadEra) {
        _uiState.update { current ->
            val updated = current.copy(selectedEra = era)
            updated.copy(
                filteredShows = applyFilters(
                    current.shows,
                    current.searchQuery,
                    era,
                    current.attendanceFilter,
                    current.sortOrder,
                    current.selectedSegueFilter
                )
            )
        }
    }

    fun setAttendanceFilter(filter: AttendanceFilter) {
        _uiState.update { current ->
            val updated = current.copy(attendanceFilter = filter)
            updated.copy(
                filteredShows = applyFilters(
                    current.shows,
                    current.searchQuery,
                    current.selectedEra,
                    filter,
                    current.sortOrder,
                    current.selectedSegueFilter
                )
            )
        }
    }

    fun setSortOrder(order: SortOrder) {
        _uiState.update { current ->
            val updated = current.copy(sortOrder = order)
            updated.copy(
                filteredShows = applyFilters(
                    current.shows,
                    current.searchQuery,
                    current.selectedEra,
                    current.attendanceFilter,
                    order,
                    current.selectedSegueFilter
                )
            )
        }
    }

    fun filterByLegendarySegue(segueTitle: String) {
        _uiState.update { current ->
            val newFilter = if (current.selectedSegueFilter == segueTitle) null else segueTitle
            val updated = current.copy(
                selectedSegueFilter = newFilter,
                currentTab = MainTab.SHOWS
            )
            updated.copy(
                filteredShows = applyFilters(
                    current.shows,
                    current.searchQuery,
                    current.selectedEra,
                    current.attendanceFilter,
                    current.sortOrder,
                    newFilter
                )
            )
        }
    }

    fun clearSegueFilter() {
        filterByLegendarySegue("")
    }

    fun toggleAttendance(show: ShowEntity) {
        viewModelScope.launch {
            val newAttended = !show.isAttended
            repository.toggleAttendance(show.id, newAttended)
            val msg = if (newAttended) "Marked as attended! ⚡ Golden Stealie added" else "Removed from attended list"
            _uiState.update { it.copy(userFeedbackMessage = msg) }
        }
    }

    fun saveReviewAndNotes(
        showId: String,
        reviewText: String,
        rating: Float,
        attendedWith: String,
        favoriteSong: String
    ) {
        viewModelScope.launch {
            repository.saveUserReview(
                id = showId,
                review = reviewText.trim(),
                rating = rating,
                attendedWith = attendedWith.trim(),
                favoriteSong = favoriteSong.trim()
            )
            _uiState.update { it.copy(userFeedbackMessage = "Your review and memories saved! 🌹") }
        }
    }

    fun toggleSongTransition(show: ShowEntity, songIndex: Int) {
        viewModelScope.launch {
            repository.toggleSongTransition(show, songIndex)
        }
    }

    fun deleteShow(show: ShowEntity) {
        viewModelScope.launch {
            repository.deleteShow(show)
            _uiState.update {
                it.copy(
                    screenState = ScreenState.Main,
                    userFeedbackMessage = "Show deleted from catalog"
                )
            }
        }
    }

    fun saveCustomShow(show: ShowEntity) {
        viewModelScope.launch {
            repository.saveShow(show)
            _uiState.update {
                it.copy(
                    screenState = ScreenState.ShowDetail(show.id),
                    userFeedbackMessage = "Show saved to your offline Room catalog! 🎸"
                )
            }
        }
    }

    fun restoreOfflineCatalog() {
        viewModelScope.launch {
            repository.restoreCuratedOfflineCatalog()
            _uiState.update {
                it.copy(
                    userFeedbackMessage = "Offline Room database synced & verified! ⚡"
                )
            }
        }
    }

    fun updateUserProfile(profile: UserProfileData) {
        _uiState.update {
            it.copy(
                userProfile = profile,
                userFeedbackMessage = "Backstage Laminate Pass updated! 🌹"
            )
        }
    }

    fun recordTriviaQuizCompletion(
        answeredCount: Int,
        correctCount: Int,
        pointsEarned: Int,
        maxStreakInRound: Int,
        mode: TriviaGameMode
    ) {
        _uiState.update { current ->
            val prev = current.triviaProgress
            val newTotalAnswered = prev.totalAnswered + answeredCount
            val newTotalCorrect = prev.totalCorrect + correctCount
            val newTotalPoints = prev.totalPoints + pointsEarned
            val newBestStreak = maxOf(prev.bestStreak, maxStreakInRound)
            val newQuizzesCompleted = prev.quizzesCompleted + 1
            val todayDayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            val newDailyDay = if (mode == TriviaGameMode.DAILY_DEADHEAD) todayDayOfYear else prev.dailyCompletedDayOfYear

            val newBadges = prev.unlockedBadgeIds.toMutableSet()
            if (answeredCount > 0) {
                newBadges.add("first_miracle")
            }
            if (newBestStreak >= 5) {
                newBadges.add("scarlet_fire_streak")
            }
            if (mode == TriviaGameMode.CLASSIC_TOUR && answeredCount > 0 &&
                (correctCount.toFloat() / answeredCount.toFloat()) >= 0.8f
            ) {
                newBadges.add("cornell_scholar")
            }
            if (newTotalPoints >= 1500) {
                newBadges.add("owsley_soundman")
            }
            if (mode == TriviaGameMode.DAILY_DEADHEAD) {
                newBadges.add("daily_stealie")
            }
            if (mode == TriviaGameMode.TERRAPIN_BLITZ && correctCount >= 6) {
                newBadges.add("terrapin_speedster")
            }

            val updatedProgress = prev.copy(
                totalAnswered = newTotalAnswered,
                totalCorrect = newTotalCorrect,
                currentStreak = maxStreakInRound,
                bestStreak = newBestStreak,
                totalPoints = newTotalPoints,
                quizzesCompleted = newQuizzesCompleted,
                dailyCompletedDayOfYear = newDailyDay,
                unlockedBadgeIds = newBadges
            )

            val newlyUnlockedCount = newBadges.size - prev.unlockedBadgeIds.size
            val feedback = if (newlyUnlockedCount > 0) {
                "⚡ +$pointsEarned Trivia pts & $newlyUnlockedCount new Deadhead Badge unlocked!"
            } else {
                "⚡ +$pointsEarned Deadhead Trivia pts added to your Vault standing!"
            }

            current.copy(
                triviaProgress = updatedProgress,
                userFeedbackMessage = feedback
            )
        }
    }

    fun clearFeedbackMessage() {
        _uiState.update { it.copy(userFeedbackMessage = null) }
    }

    private fun applyFilters(
        shows: List<ShowEntity>,
        query: String,
        era: DeadEra,
        attendance: AttendanceFilter,
        sort: SortOrder,
        segueFilter: String?
    ): List<ShowEntity> {
        val q = query.trim().lowercase()

        return shows.filter { show ->
            // Search query matches venue, city, state, date, tags, or setlist songs
            val matchesQuery = if (q.isEmpty()) {
                true
            } else {
                show.venue.lowercase().contains(q) ||
                show.city.lowercase().contains(q) ||
                show.stateOrCountry.lowercase().contains(q) ||
                show.date.contains(q) ||
                show.tourName.lowercase().contains(q) ||
                show.tags.lowercase().contains(q) ||
                show.setlistJson.lowercase().contains(q) ||
                show.userReview.lowercase().contains(q) ||
                show.bandName.lowercase().contains(q)
            }

            // Era filter
            val matchesEra = (era == DeadEra.ALL) || show.eraId.equals(era.id, ignoreCase = true)

            // Attendance & tribute filter
            val matchesAttendance = when (attendance) {
                AttendanceFilter.ALL -> true
                AttendanceFilter.ATTENDED_ONLY -> show.isAttended
                AttendanceFilter.UNATTENDED_ONLY -> !show.isAttended
                AttendanceFilter.TERRAPIN_FLYER -> show.bandName.contains("Terrapin Flyer", ignoreCase = true)
                AttendanceFilter.UPCOMING_SHOWS -> show.isUpcoming
            }

            // Segue filter
            val matchesSegue = if (segueFilter.isNullOrBlank()) {
                true
            } else {
                val clean = segueFilter.replace("➔", "").replace("->", "").lowercase()
                val parts = clean.split(" ").filter { it.isNotBlank() }
                parts.all { part -> show.setlistJson.lowercase().contains(part) }
            }

            matchesQuery && matchesEra && matchesAttendance && matchesSegue
        }.let { list ->
            when (sort) {
                SortOrder.DATE_DESC -> list.sortedByDescending { it.date }
                SortOrder.DATE_ASC -> list.sortedBy { it.date }
                SortOrder.RATING_DESC -> list.sortedWith(
                    compareByDescending<ShowEntity> { it.userRating }.thenByDescending { it.date }
                )
            }
        }
    }
}
