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
                                LiveConcertsScreen()
                            }
                            MainTab.ATTENDANCE -> {
                                val attended = uiState.shows.filter { it.isAttended }
                                AttendanceScreen(
                                    attendedShows = attended,
                                    stats = uiState.stats,
                                    onShowClick = viewModel::openShowDetail,
                                    onExploreShowsClick = { viewModel.setTab(MainTab.SHOWS) }
                                )
                            }
                            MainTab.TRANSITIONS -> {
                                TransitionsScreen(
                                    stats = uiState.stats,
                                    onFilterBySegue = viewModel::filterByLegendarySegue
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
                                    parseSongs = viewModel.repository::parseSongs,
                                    triviaProgress = uiState.triviaProgress,
                                    onRecordQuizCompletion = viewModel::recordTriviaQuizCompletion,
                                    onOpenShowDetail = viewModel::openShowDetail
                                )
                            }
                            MainTab.PROFILE -> {
                                val attended = uiState.shows.filter { it.isAttended }
                                UserProfileScreen(
                                    allShows = uiState.shows,
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
                            val songs = remember(show.setlistJson) {
                                viewModel.repository.parseSongs(show)
                            }
                            ShowDetailScreen(
                                show = show,
                                songs = songs,
                                onBack = { viewModel.navigateBack() },
                                onToggleAttendance = viewModel::toggleAttendance,
                                onToggleSongTransition = viewModel::toggleSongTransition,
                                onSaveReviewAndNotes = viewModel::saveReviewAndNotes,
                                onEditShow = viewModel::openEditShow,
                                onDeleteShow = viewModel::deleteShow
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
                            repository = viewModel.repository,
                            onBack = { viewModel.navigateBack() },
                            onSave = viewModel::saveCustomShow
                        )
                    }
                }
            }
        }
    }
}
