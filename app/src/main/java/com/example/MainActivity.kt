package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.RestoreConfirmDialog
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.almanac.AlmanacLinkIndex
import com.example.data.almanac.AlmanacRepository
import com.example.data.almanac.SongStoryComposer
import com.example.data.backup.BackupParseResult
import com.example.data.backup.readBoundedUtf8
import com.example.data.local.isUpcoming
import com.example.data.setlist.ChaseProgress
import com.example.data.todayIsoDate
import com.example.ui.DeadSetViewModel
import com.example.ui.MainTab
import com.example.ui.components.LiveTrackerCard
import com.example.ui.components.RecreationCard
import com.example.ui.navigation.DeadSetRoutes
import com.example.ui.navigation.NavCommand
import com.example.ui.screens.AddEditShowScreen
import com.example.ui.screens.AboutCreditsScreen
import com.example.ui.screens.AlmanacArticleScreen
import com.example.ui.screens.AlmanacTabScreen
import com.example.ui.screens.LiveConcertsScreen
import com.example.ui.screens.MeTabScreen
import com.example.ui.screens.ShowDetailScreen
import com.example.ui.screens.ShowsListScreen
import com.example.ui.screens.TransitionsScreen
import com.example.ui.theme.StealieCardDark
import com.example.ui.theme.StealieGold
import com.example.ui.theme.StealieMidnight
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.DeadSetThemeProvider
import com.example.widget.TodayInDeadHistoryWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // H3: refresh home-screen widget so "today" is current even if the 24h period hasn't elapsed.
        TodayInDeadHistoryWidget.refreshAll(this)
        enableEdgeToEdge()
        setContent {
            DeadSetThemeProvider {
                val viewModel: DeadSetViewModel = viewModel()
                DeadSetApp(viewModel = viewModel)
            }
        }
    }
}

private data class TabSpec(
    val tab: MainTab,
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun DeadSetApp(viewModel: DeadSetViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingRestore by viewModel.pendingRestore.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val createBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val json = viewModel.exportBackupJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("Could not open backup file for writing")
                }
                viewModel.setBackupFeedback("Backup saved.")
            } catch (e: Exception) {
                viewModel.setBackupFeedback("Backup failed: ${e.message ?: "unknown error"}")
            }
        }
    }
    val openBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openInputStream(uri)
                        ?: error("Could not read backup file")
                    stream.use { readBoundedUtf8(it) }
                }
                if (json == null) {
                    viewModel.setBackupFeedback("Backup file is too large (max 5 MB).")
                    return@launch
                }
                when (val parsed = viewModel.offerPendingRestore(json)) {
                    is BackupParseResult.Ok -> Unit
                    is BackupParseResult.Err -> viewModel.setBackupFeedback(parsed.message)
                }
            } catch (e: Exception) {
                viewModel.setBackupFeedback("Restore failed: ${e.message ?: "unknown error"}")
            }
        }
    }
    val almanacLinks = remember {
        AlmanacLinkIndex(AlmanacRepository(context).loadArticles())
    }
    val articles = remember {
        AlmanacRepository(context).loadArticles()
    }

    pendingRestore?.let { pending ->
        RestoreConfirmDialog(
            pending = pending,
            onConfirm = { viewModel.confirmPendingRestore() },
            onDismiss = { viewModel.clearPendingRestore() }
        )
    }
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val onTabRoot = currentRoute in DeadSetRoutes.tabRoutes

    LaunchedEffect(uiState.userFeedbackMessage) {
        val msg = uiState.userFeedbackMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Short)
            viewModel.clearFeedbackMessage()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.navigation.collect { cmd ->
            when (cmd) {
                is NavCommand.ToShow -> {
                    viewModel.clearAlmanacOverlay()
                    navController.navigate(DeadSetRoutes.showDetail(cmd.showId)) {
                        launchSingleTop = true
                    }
                }
                is NavCommand.ToEditShow -> {
                    viewModel.clearAlmanacOverlay()
                    navController.navigate(DeadSetRoutes.editShow(cmd.showId)) {
                        launchSingleTop = true
                    }
                }
                NavCommand.ToAbout -> {
                    viewModel.clearAlmanacOverlay()
                    navController.navigate(DeadSetRoutes.ABOUT) { launchSingleTop = true }
                }
                is NavCommand.ToAlmanacArticle -> {
                    viewModel.clearAlmanacOverlay()
                    navController.navigate(DeadSetRoutes.almanacArticle(cmd.articleId)) {
                        launchSingleTop = true
                    }
                }
                NavCommand.ToAlmanacArticles -> {
                    viewModel.setTab(MainTab.ALMANAC)
                    navController.navigate(DeadSetRoutes.ALMANAC) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                NavCommand.ToShowsTab -> {
                    viewModel.setTab(MainTab.SHOWS)
                    navController.navigate(DeadSetRoutes.SHOWS) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                is NavCommand.AfterSaveShow -> {
                    // Leave Add/Edit, then open detail so Back does not return to the editor.
                    navController.popBackStack()
                    navController.navigate(DeadSetRoutes.showDetail(cmd.showId)) {
                        launchSingleTop = true
                    }
                }
                NavCommand.Back -> {
                    if (uiState.almanacOverlayArticleId != null) {
                        viewModel.clearAlmanacOverlay()
                    } else {
                        navController.popBackStack()
                    }
                }
            }
        }
    }

    // Only for stacked detail routes — overlay Back is handled on the overlay itself.
    BackHandler(enabled = uiState.almanacOverlayArticleId == null && !onTabRoot) {
        navController.popBackStack()
    }

    val tabs = listOf(
        TabSpec(MainTab.SHOWS, DeadSetRoutes.SHOWS, "Shows", Icons.AutoMirrored.Filled.QueueMusic),
        TabSpec(MainTab.LIVE, DeadSetRoutes.LIVE, "Live", Icons.Filled.ConfirmationNumber),
        TabSpec(MainTab.SEGUES, DeadSetRoutes.SEGUES, "Segues", Icons.AutoMirrored.Filled.AltRoute),
        TabSpec(MainTab.ALMANAC, DeadSetRoutes.ALMANAC, "Almanac", Icons.Filled.AutoStories),
        TabSpec(MainTab.ME, DeadSetRoutes.ME, "Me", Icons.Filled.Person)
    )

    val showBottomBar = onTabRoot && uiState.almanacOverlayArticleId == null

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
            if (showBottomBar) {
                NavigationBar(
                    containerColor = StealieMidnight,
                    contentColor = TextSecondaryDark,
                    tonalElevation = 4.dp,
                    windowInsets = WindowInsets.navigationBars,
                    modifier = Modifier.testTag("main_bottom_nav")
                ) {
                    for (spec in tabs) {
                        val selected = currentRoute == spec.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                viewModel.setTab(spec.tab)
                                navController.navigate(spec.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = spec.icon,
                                    contentDescription = spec.label,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = spec.label,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = StealieMidnight,
                                selectedTextColor = StealieGold,
                                indicatorColor = StealieGold,
                                unselectedIconColor = TextSecondaryDark,
                                unselectedTextColor = TextSecondaryDark
                            ),
                            modifier = Modifier.testTag("nav_tab_${spec.tab.name.lowercase()}")
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
            NavHost(
                navController = navController,
                startDestination = DeadSetRoutes.SHOWS,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(DeadSetRoutes.SHOWS) {
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
                        onRestoreOfflineCatalog = viewModel::restoreOfflineCatalog,
                        spotlightArticleId = { show ->
                            almanacLinks.articleForShowId(show.id)
                                ?: almanacLinks.articleForIsoDate(show.date)
                        },
                        onOpenAlmanacArticle = { id -> viewModel.openAlmanacArticle(id, asOverlay = true) }
                    )
                }
                composable(DeadSetRoutes.LIVE) {
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
                composable(DeadSetRoutes.SEGUES) {
                    TransitionsScreen(
                        stats = uiState.stats,
                        onFilterBySegue = viewModel::filterBySegue,
                        families = uiState.familyStats,
                        shows = uiState.shows,
                        songsByShow = uiState.songsByShow
                    )
                }
                composable(DeadSetRoutes.ALMANAC) {
                    AlmanacTabScreen(
                        articles = articles,
                        shows = uiState.shows,
                        parseSongs = viewModel::songsFor,
                        triviaProgress = uiState.triviaProgress,
                        onRecordQuizCompletion = viewModel::recordTriviaQuizCompletion,
                        onOpenShowDetail = viewModel::openShowDetail,
                        reviewStates = uiState.reviewStates,
                        onCardAnswered = viewModel::recordCardAnswer,
                        onOpenAlmanacArticle = { id -> viewModel.openAlmanacArticle(id, asOverlay = true) },
                        preferArticles = uiState.preferAlmanacArticles,
                        onArticlesShown = viewModel::clearAlmanacArticlesPrefer
                    )
                }
                composable(DeadSetRoutes.ME) {
                    val attended = uiState.shows.filter { it.isAttended }
                    val chased by viewModel.chaseSongs.collectAsStateWithLifecycle()
                    val songNames by viewModel.songNames.collectAsStateWithLifecycle()
                    LaunchedEffect(Unit) { viewModel.loadSongNames() }
                    MeTabScreen(
                        attendedShows = attended,
                        allShows = uiState.shows,
                        songsByShow = uiState.songsByShow,
                        stats = uiState.stats,
                        profileData = uiState.userProfile,
                        chaseStatuses = remember(chased, attended, uiState.songsByShow) {
                            ChaseProgress.statuses(
                                chased.map { it.songId to it.songName },
                                attended
                            ) { uiState.songsByShow[it.id].orEmpty() }
                        },
                        allSongNames = songNames,
                        onShowClick = viewModel::openShowDetail,
                        onExploreShowsClick = {
                            viewModel.setTab(MainTab.SHOWS)
                            navController.navigate(DeadSetRoutes.SHOWS) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onSongSearch = { songName ->
                            viewModel.setSearchQuery(songName)
                            viewModel.setTab(MainTab.SHOWS)
                            navController.navigate(DeadSetRoutes.SHOWS) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onAddChase = viewModel::addChaseSong,
                        onRemoveChase = { viewModel.removeChaseSong(it) },
                        onUpdateProfile = viewModel::updateUserProfile,
                        onToggleAttendance = viewModel::toggleAttendance,
                        onAboutCreditsClick = viewModel::openAboutCredits,
                        onBackupClick = {
                            createBackupLauncher.launch("deadset-backup-${todayIsoDate()}.json")
                        },
                        onRestoreClick = {
                            openBackupLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                        }
                    )
                }
                composable(
                    route = DeadSetRoutes.SHOW_DETAIL,
                    arguments = listOf(navArgument("showId") { type = NavType.StringType })
                ) { entry ->
                    val rawId = entry.arguments?.getString("showId").orEmpty()
                    val showId = DeadSetRoutes.decode(rawId)
                    val show = uiState.shows.firstOrNull { it.id == showId }
                    if (show != null) {
                        val songs = uiState.songsByShow[show.id].orEmpty()
                        ShowDetailScreen(
                            show = show,
                            songs = songs,
                            onBack = { navController.popBackStack() },
                            onToggleAttendance = viewModel::toggleAttendance,
                            onToggleSongTransition = viewModel::toggleSongTransition,
                            onSaveReviewAndNotes = viewModel::saveReviewAndNotes,
                            onEditShow = viewModel::openEditShow,
                            onDeleteShow = viewModel::deleteShow,
                            songStoryArticleId = almanacLinks::articleForSong,
                            onOpenAlmanacArticle = { id ->
                                viewModel.openAlmanacArticle(id, asOverlay = false)
                            },
                            liveTracker = if (show.isUpcoming) {
                                {
                                    val names by viewModel.songNames.collectAsStateWithLifecycle()
                                    LaunchedEffect(Unit) { viewModel.loadSongNames() }
                                    LiveTrackerCard(
                                        showId = show.id,
                                        songs = songs,
                                        predictionsFor = { last, set ->
                                            viewModel.predictNext(show.id, last, set)
                                        },
                                        allSongNames = names,
                                        onAddSong = { name, set ->
                                            viewModel.appendSongToShow(show.id, name, set)
                                        }
                                    )
                                }
                            } else null,
                            recreation = if (!show.bandName.equals("Grateful Dead", ignoreCase = true)) {
                                {
                                    val original = show.recreatesShowId?.let { id ->
                                        uiState.shows.firstOrNull { it.id == id }
                                    }
                                    val suggestion = remember(show.id, songs, uiState.shows) {
                                        if (original != null) null
                                        else com.example.data.setlist.RecreationMatcher.bestMatch(
                                            tribute = songs,
                                            candidates = uiState.shows,
                                            songsFor = { uiState.songsByShow[it.id].orEmpty() }
                                        )
                                    }
                                    RecreationCard(
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
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = StealieGold)
                        }
                    }
                }
                composable(
                    route = DeadSetRoutes.EDIT_SHOW,
                    arguments = listOf(
                        navArgument("showId") {
                            type = NavType.StringType
                            defaultValue = ""
                            nullable = true
                        }
                    )
                ) { entry ->
                    val raw = entry.arguments?.getString("showId").orEmpty()
                    val showId = raw.takeIf { it.isNotBlank() }?.let { DeadSetRoutes.decode(it) }
                    if (showId == null) {
                        AddEditShowScreen(
                            existingShow = null,
                            existingSongs = emptyList(),
                            bandSuggestions = viewModel.concerts.value.bands.filter { it.isFollowed }
                                .sortedWith(compareBy({ it.id != "terrapin flyer" }, { it.name }))
                                .map { it.name },
                            onBack = { navController.popBackStack() },
                            onSave = viewModel::saveCustomShow
                        )
                    } else {
                        val existing = uiState.shows.firstOrNull { it.id == showId }
                        when {
                            existing != null -> AddEditShowScreen(
                                existingShow = existing,
                                existingSongs = viewModel.songsFor(existing),
                                bandSuggestions = viewModel.concerts.value.bands.filter { it.isFollowed }
                                    .sortedWith(compareBy({ it.id != "terrapin flyer" }, { it.name }))
                                    .map { it.name },
                                onBack = { navController.popBackStack() },
                                onSave = viewModel::saveCustomShow
                            )
                            uiState.shows.isEmpty() -> Box(
                                Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = StealieGold)
                            }
                            else -> Box(
                                Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Show not found",
                                    color = TextSecondaryDark,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }
                composable(DeadSetRoutes.ABOUT) {
                    AboutCreditsScreen(onBack = { navController.popBackStack() })
                }
                composable(
                    route = DeadSetRoutes.ALMANAC_ARTICLE,
                    arguments = listOf(navArgument("articleId") { type = NavType.StringType })
                ) { entry ->
                    val articleId = DeadSetRoutes.decode(entry.arguments?.getString("articleId").orEmpty())
                    val article = remember(articleId) {
                        AlmanacRepository(context).loadArticle(articleId)
                    }
                    if (article != null) {
                        val panels = remember(article, uiState.shows, uiState.songsByShow) {
                            SongStoryComposer.panelsFor(
                                article = article,
                                shows = uiState.shows,
                                songsFor = { show -> uiState.songsByShow[show.id].orEmpty() }
                            )
                        }
                        AlmanacArticleScreen(
                            article = article,
                            onOpenShow = viewModel::openShowDetail,
                            onBack = { navController.popBackStack() },
                            songStoryPanels = panels
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Article not found", color = TextSecondaryDark)
                        }
                    }
                }
            }

            uiState.almanacOverlayArticleId?.let { overlayId ->
                // Takes Back before NavHost / Trivia handlers so quiz state stays alive.
                BackHandler { viewModel.clearAlmanacOverlay() }
                val article = remember(overlayId) {
                    AlmanacRepository(context).loadArticle(overlayId)
                }
                if (article != null) {
                    val panels = remember(article, uiState.shows, uiState.songsByShow) {
                        SongStoryComposer.panelsFor(
                            article = article,
                            shows = uiState.shows,
                            songsFor = { show -> uiState.songsByShow[show.id].orEmpty() }
                        )
                    }
                    AlmanacArticleScreen(
                        article = article,
                        onOpenShow = { showId ->
                            viewModel.clearAlmanacOverlay()
                            viewModel.openShowDetail(showId)
                        },
                        onBack = { viewModel.clearAlmanacOverlay() },
                        songStoryPanels = panels
                    )
                }
            }
        }
    }
}
