package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.*
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.components.LiveTrackerCard
import com.example.data.local.isUpcoming

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DeadSetThemeProvider {
                val viewModel: DeadSetViewModel = viewModel()
                DeadSetApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun DeadSetApp(viewModel: DeadSetViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userFeedbackMessage) {
        val msg = uiState.userFeedbackMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearFeedbackMessage()
        }
    }

    val isRootScreen = uiState.screenState is ScreenState.Main

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    Snackbar(
                        containerColor = StealieCardDark,
                        contentColor = TextPrimaryDark,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                    ) {
                        Text(text = data.visuals.message, fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        },
        bottomBar = {
            if (isRootScreen) {
                NavigationBar(
                    containerColor = StealieMidnight,
                    contentColor = TextSecondaryDark,
                    tonalElevation = 4.dp,
                    windowInsets = WindowInsets.navigationBars,
                    modifier = Modifier.testTag("main_bottom_nav")
                ) {
                    val tabs = listOf(
                        Triple(MainTab.SHOWS, "Setlists", Icons.Filled.QueueMusic),
                        Triple(MainTab.LIVE_TICKETS, "Tickets", Icons.Filled.ConfirmationNumber),
                        Triple(MainTab.ATTENDANCE, "My Shows", Icons.Filled.Bolt),
                        Triple(MainTab.TRANSITIONS, "Segues", Icons.Filled.AltRoute),
                        Triple(MainTab.STATS, "Stats", Icons.Filled.BarChart),
                        Triple(MainTab.TRIVIA, "Trivia", Icons.Filled.Quiz),
                        Triple(MainTab.PROFILE, "Profile", Icons.Filled.Person)
                    )

                    for ((tab, title, icon) in tabs) {
                        val isSelected = uiState.currentTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.setTab(tab) },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = title,
                                    modifier = Modifier.size(19.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = title,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = StealieMidnight,
                                selectedTextColor = StealieGold,
                                indicatorColor = StealieGold,
                                unselectedIconColor = TextSecondaryDark,
                                unselectedTextColor = TextSecondaryDark
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        },
        containerColor = StealieMidnight,
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(
                targetState = uiState.screenState,
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    is ScreenState.Main -> {
                        when (uiState.currentTab) {
                            MainTab.SHOWS -> {
                                ShowsListScreen(
                                    shows = uiState.filteredShows,
                                    allCatalogShows = uiState.shows,
                                    songsByShow = uiState.songsByShow,
                                    searchQuery = uiState.searchQuery,
                                    onSearchChange = viewModel::setSearchQuery,
                                    selectedEra = uiState.selectedEra,
                                    onEraSelected = viewModel::setEra,
                                    attendanceFilter = uiState.attendanceFilter,
                                    onAttendanceFilterChanged = viewModel::setAttendanceFilter,
                                    sortOrder = uiState.sortOrder,
                                    onSortOrderChanged = viewModel::setSortOrder,
                                    selectedSegueFilter = uiState.selectedSegueFilter,
                                    onClearSegueFilter = viewModel::clearSegueFilter,
                                    onShowClick = viewModel::openShowDetail,
                                    onAddShowClick = viewModel::openAddShow,
                                    onRestoreOfflineCatalog = viewModel::restoreOfflineCatalog
                                )
                            }
                            MainTab.LIVE_TICKETS -> {
                                val concerts by viewModel.concerts.collectAsStateWithLifecycle()
                                LiveConcertsScreen(
                                    state = concerts,
                                    onRefresh = viewModel::refreshConcerts,
                                    onAddBand = { viewModel.addBand(it) },
                                    onSetFollowed = { id, f -> viewModel.setBandFollowed(id, f) },
                                    onRemoveBand = { viewModel.removeBand(it) },
                                    onSetAlerts = viewModel::setAlertsEnabled
                                )
                            }
                            MainTab.ATTENDANCE -> {
                                val attended = uiState.shows.filter { it.isAttended }
                                val chased by viewModel.chaseSongs.collectAsStateWithLifecycle()
                                val songNames by viewModel.songNames.collectAsStateWithLifecycle()
                                LaunchedEffect(Unit) { viewModel.loadSongNames() }
                                AttendanceScreen(
                                    attendedShows = attended,
                                    stats = uiState.stats,
                                    onShowClick = viewModel::openShowDetail,
                                    onExploreShowsClick = { viewModel.setTab(MainTab.SHOWS) },
                                    songsByShow = uiState.songsByShow,
                                    chaseStatuses = remember(chased, attended, uiState.songsByShow) {
                                        com.example.data.setlist.ChaseProgress.statuses(
                                            chased.map { it.songId to it.songName }, attended
                                        ) { uiState.songsByShow[it.id].orEmpty() }
                                    },
                                    allSongNames = songNames,
                                    onAddChase = viewModel::addChaseSong,
                                    onRemoveChase = { viewModel.removeChaseSong(it) }
                                )
                            }
                            MainTab.TRANSITIONS -> {
                                TransitionsScreen(
                                    stats = uiState.stats,
                                    onFilterBySegue = viewModel::filterBySegue,
                                    families = uiState.familyStats,
                                    shows = uiState.shows,
                                    songsByShow = uiState.songsByShow
                                )
                            }
                            MainTab.STATS -> {
                                StatsScreen(
                                    stats = uiState.stats,
                                    onSongClick = { songName ->
                                        viewModel.setSearchQuery(songName)
                                        viewModel.setTab(MainTab.SHOWS)
                                    }
                                )
                            }
                            MainTab.TRIVIA -> {
                                TriviaScreen(
                                    shows = uiState.shows,
                                    parseSongs = viewModel::songsFor,
                                    triviaProgress = uiState.triviaProgress,
                                    onRecordQuizCompletion = viewModel::recordTriviaQuizCompletion,
                                    onOpenShowDetail = viewModel::openShowDetail,
                                    reviewStates = uiState.reviewStates,
                                    onCardAnswered = viewModel::recordCardAnswer
                                )
                            }
                            MainTab.PROFILE -> {
                                val attended = uiState.shows.filter { it.isAttended }
                                UserProfileScreen(
                                    allShows = uiState.shows,
                                    songsByShow = uiState.songsByShow,
                                    attendedShows = attended,
                                    stats = uiState.stats,
                                    profileData = uiState.userProfile,
                                    onUpdateProfile = viewModel::updateUserProfile,
                                    onShowClick = viewModel::openShowDetail,
                                    onToggleAttendance = viewModel::toggleAttendance,
                                    onExploreCatalogClick = { viewModel.setTab(MainTab.SHOWS) }
                                )
                            }
                        }
                    }

                    is ScreenState.ShowDetail -> {
                        val show = uiState.shows.firstOrNull { it.id == screen.showId }
                        if (show != null) {
                            val songs = uiState.songsByShow[show.id].orEmpty()
                            ShowDetailScreen(
                                show = show,
                                songs = songs,
                                onBack = { viewModel.navigateBack() },
                                onToggleAttendance = viewModel::toggleAttendance,
                                onToggleSongTransition = viewModel::toggleSongTransition,
                                onSaveReviewAndNotes = viewModel::saveReviewAndNotes,
                                onEditShow = viewModel::openEditShow,
                                onDeleteShow = viewModel::deleteShow,
                                liveTracker = if (show.isUpcoming) {
                                    {
                                        val names by viewModel.songNames.collectAsStateWithLifecycle()
                                        LaunchedEffect(Unit) { viewModel.loadSongNames() }
                                        LiveTrackerCard(
                                            showId = show.id,
                                            songs = songs,
                                            predictionsFor = { last, set -> viewModel.predictNext(show.id, last, set) },
                                            allSongNames = names,
                                            onAddSong = { name, set -> viewModel.appendSongToShow(show.id, name, set) }
                                        )
                                    }
                                } else null,
                                recreation = if (!show.bandName.equals("Grateful Dead", ignoreCase = true)) {
                                    {
                                        val original = show.recreatesShowId?.let { id -> uiState.shows.firstOrNull { it.id == id } }
                                        val suggestion = remember(show.id, songs, uiState.shows) {
                                            if (original != null) null
                                            else com.example.data.setlist.RecreationMatcher.bestMatch(
                                                tribute = songs,
                                                candidates = uiState.shows,
                                                songsFor = { uiState.songsByShow[it.id].orEmpty() }
                                            )
                                        }
                                        com.example.ui.components.RecreationCard(
                                            tributeSongs = songs,
                                            original = original,
                                            originalSongs = original?.let { uiState.songsByShow[it.id] }.orEmpty(),
                                            suggestion = suggestion,
                                            onLink = { viewModel.setRecreation(show.id, it) },
                                            onUnlink = { viewModel.setRecreation(show.id, null) },
                                            onOpenShow = viewModel::openShowDetail
                                        )
                                    }
                                } else null
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                CircularProgressIndicator(color = StealieGold)
                            }
                        }
                    }

                    is ScreenState.AddEditShow -> {
                        val existing = screen.showId?.let { id ->
                            uiState.shows.firstOrNull { it.id == id }
                        }
                        AddEditShowScreen(
                            existingShow = existing,
                            existingSongs = existing?.let { viewModel.songsFor(it) }.orEmpty(),
                            bandSuggestions = viewModel.concerts.value.bands.filter { it.isFollowed }
                                .sortedWith(compareBy({ it.id != "terrapin flyer" }, { it.name }))
                                .map { it.name },
                            onBack = { viewModel.navigateBack() },
                            onSave = viewModel::saveCustomShow
                        )
                    }
                }
            }
        }
    }
}
