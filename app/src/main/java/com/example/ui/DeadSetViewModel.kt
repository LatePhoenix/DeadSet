package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.UserPrefsStore
import com.example.data.local.ReviewCardEntity
import com.example.data.review.Leitner
import com.example.data.review.ReviewState
import com.example.data.local.ShowEntity
import com.example.data.local.isUpcoming
import com.example.data.setlist.FamilyStat
import com.example.data.setlist.SegueMatcher
import com.example.data.setlist.SetlistAnalytics
import com.example.data.setlist.predictNext
import com.example.data.model.*
import com.example.data.repository.OverallStats
import com.example.data.repository.ShowRepository
import com.example.data.repository.ConcertRepository
import com.example.data.LatLon
import com.example.ui.screens.ConcertsUiState
import com.example.data.seed.TriviaGameMode
import com.example.data.seed.TriviaUserProgress
import com.example.ui.navigation.NavCommand
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

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
    val stats: OverallStats? = null,
    /** Each segue family with its play count and date range, computed from the catalog. */
    val familyStats: List<FamilyStat> = emptyList(),
    val userProfile: UserProfileData = UserProfileData(),
    val triviaProgress: TriviaUserProgress = TriviaUserProgress(),
    /** Segue School spaced-repetition state by card (question) id. */
    val reviewStates: Map<String, ReviewState> = emptyMap(),
    val userFeedbackMessage: String? = null,
    /**
     * Almanac article shown over a tab root without leaving NavHost (keeps Trivia quiz
     * `remember` state alive). Cleared on back.
     */
    val almanacOverlayArticleId: String? = null,
    /** One-shot: Almanac tab should land on Articles pane. */
    val preferAlmanacArticles: Boolean = false
)

/** Everything derived from one catalog emission, computed together off the main thread. */
private data class CatalogSnapshot(
    val shows: List<ShowEntity>,
    val songsByShow: Map<String, List<SongEntry>>,
    val filtered: List<ShowEntity>,
    val stats: OverallStats,
    val families: List<FamilyStat>
)

class DeadSetViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val repository = ShowRepository(database.showDao(), database.songDao())
    private val userPrefs = UserPrefsStore(application)
    private val reviewMutex = Mutex()
    private val homeLookupGeneration = AtomicInteger(0)

    private val _uiState = MutableStateFlow(
        UiState(
            triviaProgress = userPrefs.loadTriviaProgress(),
            userProfile = userPrefs.loadProfile()?.let {
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

    private val reviewDao = database.reviewDao()

    // --- Song chase list (step 4.7) ---
    private val chaseDao = database.chaseDao()
    val chaseSongs: StateFlow<List<com.example.data.local.ChaseSongEntity>> =
        chaseDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _songNames = MutableStateFlow<List<String>>(emptyList())
    /** Every known song title, for chase-list autocomplete. */
    val songNames: StateFlow<List<String>> = _songNames.asStateFlow()

    fun loadSongNames() {
        viewModelScope.launch { _songNames.value = repository.allSongNames() }
    }

    fun addChaseSong(name: String) {
        viewModelScope.launch {
            val song = repository.resolveSong(name) ?: return@launch
            chaseDao.add(
                com.example.data.local.ChaseSongEntity(song.songId, song.canonicalName, com.example.data.todayIsoDate())
            )
        }
    }

    fun removeChaseSong(songId: String) = viewModelScope.launch { chaseDao.remove(songId) }

    /** Live tracker (step 4.9): log a just-played song. */
    fun appendSongToShow(showId: String, name: String, setType: SetType) {
        viewModelScope.launch {
            repository.appendSong(showId, name, setType)
            loadSongNames()
        }
    }

    /** Links (or with null, unlinks) a tribute show to the GD show it recreates (step 4.8). */
    fun setRecreation(showId: String, originalId: String?) {
        viewModelScope.launch { repository.setRecreation(showId, originalId) }
    }

    /** Best guesses for the next song, from the whole catalog (step 4.9). */
    fun predictNext(showId: String, lastSong: String?, setType: SetType): List<String> {
        val state = _uiState.value
        return SetlistAnalytics.predictNext(
            lastSong, setType, state.shows, { state.songsByShow[it.id].orEmpty() },
            alreadyPlayed = state.songsByShow[showId].orEmpty().map { it.name }
        )
    }

    // --- Tickets tab (Phase 4): followed bands + cached upcoming dates ---
    private val concertRepository = ConcertRepository(database.concertDao())
    private val _concerts = MutableStateFlow(ConcertsUiState())
    val concerts: StateFlow<ConcertsUiState> = _concerts.asStateFlow()

    fun refreshConcerts() {
        if (_concerts.value.isRefreshing) return
        viewModelScope.launch {
            _concerts.update { it.copy(isRefreshing = true) }
            val outcome = runCatching { concertRepository.refresh() }.getOrNull()
            _concerts.update { it.copy(isRefreshing = false, lastOutcome = outcome ?: it.lastOutcome) }
        }
    }

    fun addBand(name: String) = viewModelScope.launch { concertRepository.addBand(name) }

    fun setBandFollowed(bandId: String, followed: Boolean) =
        viewModelScope.launch { concertRepository.setFollowed(bandId, followed) }

    fun removeBand(bandId: String) = viewModelScope.launch { concertRepository.removeBand(bandId) }

    /** Turns background date checks + notifications on or off (step 4.6). */
    fun setAlertsEnabled(enabled: Boolean) {
        userPrefs.setAlertsEnabled(enabled)
        val ctx = getApplication<Application>()
        if (enabled) com.example.notifications.ConcertAlertsWorker.schedule(ctx)
        else com.example.notifications.ConcertAlertsWorker.cancel(ctx)
        _concerts.update { it.copy(alertsEnabled = enabled) }
    }

    /** Geocodes the profile's home city for "near me" (no network key needed; may be unavailable). */
    private fun locateHome(homeCity: String) {
        val generation = homeLookupGeneration.incrementAndGet()
        viewModelScope.launch {
            val home = withContext(Dispatchers.IO) {
                runCatching {
                    @Suppress("DEPRECATION")
                    android.location.Geocoder(getApplication())
                        .getFromLocationName(homeCity, 1)
                        ?.firstOrNull()
                        ?.let { LatLon(it.latitude, it.longitude) }
                }.getOrNull()
            }
            if (generation != homeLookupGeneration.get()) return@launch
            _concerts.update { it.copy(home = home, homeLabel = homeCity.substringBefore(",").trim()) }
        }
    }

    init {
        viewModelScope.launch {
            reviewDao.observeAll().collect { cards ->
                _uiState.update { s -> s.copy(reviewStates = cards.associate { it.cardId to it.toState() }) }
            }
        }

        viewModelScope.launch {
            repository.checkAndSeedDatabase()
        }

        viewModelScope.launch { concertRepository.ensureDefaultBands() }
        viewModelScope.launch {
            concertRepository.bands.collect { bands -> _concerts.update { it.copy(bands = bands) } }
        }
        viewModelScope.launch {
            concertRepository.upcomingEvents().collect { events -> _concerts.update { it.copy(events = events) } }
        }
        locateHome(_uiState.value.userProfile.homeCity)
        if (userPrefs.alertsEnabled()) setAlertsEnabled(true) // re-schedules (KEEP) and syncs UI state

        viewModelScope.launch {
            repository.allShowsWithSongs.collect { rows ->
                val current = _uiState.value
                // Stats and filtering walk every setlist; keep that off the main thread.
                val snapshot = withContext(Dispatchers.Default) {
                    val allShowsList = rows.map { it.show }
                    val songsByShow = rows.associate { it.show.id to it.orderedSongs }
                    val songsFor = { show: ShowEntity -> songsByShow[show.id].orEmpty() }
                    CatalogSnapshot(
                        shows = allShowsList,
                        songsByShow = songsByShow,
                        filtered = applyFilters(
                            shows = allShowsList,
                            query = current.searchQuery,
                            era = current.selectedEra,
                            attendance = current.attendanceFilter,
                            sort = current.sortOrder,
                            segueFilter = current.selectedSegueFilter,
                            songsByShow = songsByShow
                        ),
                        stats = repository.computeOverallStats(allShowsList, songsFor),
                        families = SetlistAnalytics.familyStats(allShowsList, songsFor)
                    )
                }
                _uiState.update { latest ->
                    // A filter setter may have run while we were off the main thread; if so, the
                    // precomputed list is for old filters, so re-filter with the latest ones.
                    val filtersUnchanged = latest.searchQuery == current.searchQuery &&
                        latest.selectedEra == current.selectedEra &&
                        latest.attendanceFilter == current.attendanceFilter &&
                        latest.sortOrder == current.sortOrder &&
                        latest.selectedSegueFilter == current.selectedSegueFilter
                    latest.copy(
                        shows = snapshot.shows,
                        songsByShow = snapshot.songsByShow,
                        filteredShows = if (filtersUnchanged) snapshot.filtered else applyFilters(
                            shows = snapshot.shows,
                            query = latest.searchQuery,
                            era = latest.selectedEra,
                            attendance = latest.attendanceFilter,
                            sort = latest.sortOrder,
                            segueFilter = latest.selectedSegueFilter,
                            songsByShow = snapshot.songsByShow
                        ),
                        stats = snapshot.stats,
                        familyStats = snapshot.families
                    )
                }
            }
        }
    }

    /** A show's setlist in performance order (empty if it has none yet). */
    fun songsFor(show: ShowEntity): List<SongEntry> = _uiState.value.songsByShow[show.id].orEmpty()

    private val navCommands = Channel<NavCommand>(Channel.BUFFERED)
    val navigation: Flow<NavCommand> = navCommands.receiveAsFlow()

    fun setTab(tab: MainTab) {
        _uiState.update {
            it.copy(
                currentTab = tab,
                almanacOverlayArticleId = null,
                preferAlmanacArticles = tab == MainTab.ALMANAC && it.preferAlmanacArticles
            )
        }
    }

    fun openShowDetail(showIdOrDate: String) {
        val matchedShowId = _uiState.value.shows.firstOrNull {
            it.id.equals(showIdOrDate, ignoreCase = true) ||
                it.date.equals(showIdOrDate, ignoreCase = true) ||
                it.id.startsWith(showIdOrDate, ignoreCase = true)
        }?.id ?: showIdOrDate
        navCommands.trySend(NavCommand.ToShow(matchedShowId))
    }

    fun openAddShow() {
        navCommands.trySend(NavCommand.ToEditShow(null))
    }

    fun openEditShow(showId: String) {
        navCommands.trySend(NavCommand.ToEditShow(showId))
    }

    fun openAboutCredits() {
        navCommands.trySend(NavCommand.ToAbout)
    }

    fun openAlmanac() {
        _uiState.update {
            it.copy(currentTab = MainTab.ALMANAC, preferAlmanacArticles = true, almanacOverlayArticleId = null)
        }
        navCommands.trySend(NavCommand.ToAlmanacArticles)
    }

    /**
     * @param asOverlay when true (tab root / quiz), keep the current destination composed.
     */
    fun openAlmanacArticle(articleId: String, asOverlay: Boolean = true) {
        if (asOverlay) {
            _uiState.update { it.copy(almanacOverlayArticleId = articleId) }
        } else {
            navCommands.trySend(NavCommand.ToAlmanacArticle(articleId))
        }
    }

    fun clearAlmanacArticlesPrefer() {
        _uiState.update { it.copy(preferAlmanacArticles = false) }
    }

    fun clearAlmanacOverlay() {
        _uiState.update { it.copy(almanacOverlayArticleId = null) }
    }

    fun navigateBack(): Boolean {
        if (_uiState.value.almanacOverlayArticleId != null) {
            clearAlmanacOverlay()
            return true
        }
        navCommands.trySend(NavCommand.Back)
        return true
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

    fun filterBySegue(segueTitle: String) {
        _uiState.update { current ->
            val newFilter = if (current.selectedSegueFilter == segueTitle) null else segueTitle
            current.copy(
                selectedSegueFilter = newFilter,
                currentTab = MainTab.SHOWS,
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
        navCommands.trySend(NavCommand.ToShowsTab)
    }

    /** Clears the segue filter without changing the current tab. */
    fun clearSegueFilter() {
        _uiState.update { current ->
            current.copy(
                selectedSegueFilter = null,
                filteredShows = applyFilters(
                    current.shows,
                    current.searchQuery,
                    current.selectedEra,
                    current.attendanceFilter,
                    current.sortOrder,
                    null
                )
            )
        }
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
            repository.toggleSongTransition(show.id, songIndex)
        }
    }

    fun deleteShow(show: ShowEntity) {
        viewModelScope.launch {
            repository.deleteShow(show)
            navCommands.trySend(NavCommand.Back)
            _uiState.update {
                it.copy(userFeedbackMessage = "Show deleted from catalog")
            }
        }
    }

    fun saveCustomShow(show: ShowEntity, songs: List<SongEntry>) {
        viewModelScope.launch {
            repository.saveShow(show, songs)
            navCommands.trySend(NavCommand.AfterSaveShow(show.id))
            _uiState.update {
                it.copy(userFeedbackMessage = "Show saved to your offline Room catalog! 🎸")
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
        if (profile.homeCity != _uiState.value.userProfile.homeCity) locateHome(profile.homeCity)
        userPrefs.saveProfile(
            UserPrefsStore.StoredProfile(
                handle = profile.handle,
                homeCity = profile.homeCity,
                favoriteJam = profile.favoriteJam,
                favoriteEraTitle = profile.favoriteEraTitle
            )
        )
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
            val newDailyDate = if (mode == TriviaGameMode.DAILY_DEADHEAD) {
                com.example.data.todayIsoDate()
            } else {
                prev.dailyCompletedDate
            }

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
                dailyCompletedDate = newDailyDate,
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
        // Persist outside update{}, whose lambda may re-run under contention.
        userPrefs.saveTriviaProgress(_uiState.value.triviaProgress)
    }

    /** Records a Segue School answer and moves the card between Leitner boxes. */
    fun recordCardAnswer(cardId: String, correct: Boolean) {
        viewModelScope.launch {
            reviewMutex.withLock {
                val previous = reviewDao.get(cardId)?.toState()
                val next = Leitner.afterAnswer(previous, correct, com.example.data.todayIsoDate())
                reviewDao.upsert(ReviewCardEntity.from(cardId, next))
            }
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
        segueFilter: String?,
        songsByShow: Map<String, List<SongEntry>> = _uiState.value.songsByShow
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
                songsByShow[show.id].orEmpty().any { it.name.lowercase().contains(q) } ||
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
                SegueMatcher.matches(songsByShow[show.id].orEmpty(), segueFilter)
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
