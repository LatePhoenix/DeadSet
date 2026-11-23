package com.example.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.CatalogAssetImporter
import com.example.data.local.ShowEntity
import com.example.data.local.UserPrefsStore
import com.example.data.model.DeadEra
import com.example.data.model.SongEntry
import com.example.data.repository.OverallStats
import com.example.data.repository.ShowRepository
import com.example.data.setlist.FamilyStat
import com.example.data.setlist.SetlistAnalytics
import com.example.ui.navigation.NavCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Catalog list, filters, show CRUD, attendance / reviews (6.2). */
class ShowsViewModel(
    private val app: Application,
    private val database: AppDatabase,
    private val repository: ShowRepository,
    private val userPrefs: UserPrefsStore,
    private val ui: MutableStateFlow<UiState>,
    private val navigation: NavigationViewModel
) : ViewModel() {

    init {
        viewModelScope.launch {
            CatalogAssetImporter(
                context = app,
                database = database,
                prefs = userPrefs
            ).importIfNeeded()
            repository.checkAndSeedDatabase()
        }

        viewModelScope.launch {
            repository.allShowsWithSongs.collect { rows ->
                val current = ui.value
                val snapshot = withContext(Dispatchers.Default) {
                    val allShowsList = rows.map { it.show }
                    val songsByShow = rows.associate { it.show.id to it.orderedSongs }
                    val songsFor = { show: ShowEntity -> songsByShow[show.id].orEmpty() }
                    CatalogSnapshot(
                        shows = allShowsList,
                        songsByShow = songsByShow,
                        filtered = ShowCatalogFilters.apply(
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
                ui.update { latest ->
                    val filtersUnchanged = latest.searchQuery == current.searchQuery &&
                        latest.selectedEra == current.selectedEra &&
                        latest.attendanceFilter == current.attendanceFilter &&
                        latest.sortOrder == current.sortOrder &&
                        latest.selectedSegueFilter == current.selectedSegueFilter
                    latest.copy(
                        shows = snapshot.shows,
                        songsByShow = snapshot.songsByShow,
                        filteredShows = if (filtersUnchanged) snapshot.filtered else ShowCatalogFilters.apply(
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

    fun songsFor(show: ShowEntity): List<SongEntry> = ui.value.songsByShow[show.id].orEmpty()

    fun setSearchQuery(query: String) {
        ui.update { current ->
            val updated = current.copy(searchQuery = query)
            updated.copy(
                filteredShows = ShowCatalogFilters.apply(
                    current.shows,
                    query,
                    current.selectedEra,
                    current.attendanceFilter,
                    current.sortOrder,
                    current.selectedSegueFilter,
                    current.songsByShow
                )
            )
        }
    }

    fun setEra(era: DeadEra) {
        ui.update { current ->
            val updated = current.copy(selectedEra = era)
            updated.copy(
                filteredShows = ShowCatalogFilters.apply(
                    current.shows,
                    current.searchQuery,
                    era,
                    current.attendanceFilter,
                    current.sortOrder,
                    current.selectedSegueFilter,
                    current.songsByShow
                )
            )
        }
    }

    fun setAttendanceFilter(filter: AttendanceFilter) {
        ui.update { current ->
            val updated = current.copy(attendanceFilter = filter)
            updated.copy(
                filteredShows = ShowCatalogFilters.apply(
                    current.shows,
                    current.searchQuery,
                    current.selectedEra,
                    filter,
                    current.sortOrder,
                    current.selectedSegueFilter,
                    current.songsByShow
                )
            )
        }
    }

    fun setSortOrder(order: SortOrder) {
        ui.update { current ->
            val updated = current.copy(sortOrder = order)
            updated.copy(
                filteredShows = ShowCatalogFilters.apply(
                    current.shows,
                    current.searchQuery,
                    current.selectedEra,
                    current.attendanceFilter,
                    order,
                    current.selectedSegueFilter,
                    current.songsByShow
                )
            )
        }
    }

    fun filterBySegue(segueTitle: String) {
        ui.update { current ->
            val newFilter = if (current.selectedSegueFilter == segueTitle) null else segueTitle
            current.copy(
                selectedSegueFilter = newFilter,
                currentTab = MainTab.SHOWS,
                filteredShows = ShowCatalogFilters.apply(
                    current.shows,
                    current.searchQuery,
                    current.selectedEra,
                    current.attendanceFilter,
                    current.sortOrder,
                    newFilter,
                    current.songsByShow
                )
            )
        }
        navigation.send(NavCommand.ToShowsTab)
    }

    fun clearSegueFilter() {
        ui.update { current ->
            current.copy(
                selectedSegueFilter = null,
                filteredShows = ShowCatalogFilters.apply(
                    current.shows,
                    current.searchQuery,
                    current.selectedEra,
                    current.attendanceFilter,
                    current.sortOrder,
                    null,
                    current.songsByShow
                )
            )
        }
    }

    fun toggleAttendance(show: ShowEntity) {
        viewModelScope.launch {
            val newAttended = !show.isAttended
            repository.toggleAttendance(show.id, newAttended)
            val msg = if (newAttended) "Marked as attended! ⚡ Golden Stealie added" else "Removed from attended list"
            ui.update { it.copy(userFeedbackMessage = msg) }
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
            ui.update { it.copy(userFeedbackMessage = "Your review and memories saved! 🌹") }
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
            navigation.send(NavCommand.Back)
            ui.update { it.copy(userFeedbackMessage = "Show deleted from catalog") }
        }
    }

    fun saveCustomShow(show: ShowEntity, songs: List<SongEntry>) {
        viewModelScope.launch {
            repository.saveShow(show, songs)
            navigation.send(NavCommand.AfterSaveShow(show.id))
            ui.update {
                it.copy(userFeedbackMessage = "Show saved to your offline Room catalog! 🎸")
            }
        }
    }

    fun restoreOfflineCatalog() {
        viewModelScope.launch {
            repository.restoreCuratedOfflineCatalog()
            ui.update {
                it.copy(userFeedbackMessage = "Offline Room database synced & verified! ⚡")
            }
        }
    }

    fun setRecreation(showId: String, originalId: String?) {
        viewModelScope.launch { repository.setRecreation(showId, originalId) }
    }

    private data class CatalogSnapshot(
        val shows: List<ShowEntity>,
        val songsByShow: Map<String, List<SongEntry>>,
        val filtered: List<ShowEntity>,
        val stats: OverallStats,
        val families: List<FamilyStat>
    )
}
