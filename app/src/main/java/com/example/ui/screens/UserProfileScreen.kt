package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ShowEntity
import com.example.data.model.DeadEra
import com.example.data.repository.OverallStats
import com.example.ui.UserProfileData
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun UserProfileScreen(
    allShows: List<ShowEntity>,
    attendedShows: List<ShowEntity>,
    stats: OverallStats?,
    profileData: UserProfileData,
    onUpdateProfile: (UserProfileData) -> Unit,
    onShowClick: (String) -> Unit,
    onToggleAttendance: (ShowEntity) -> Unit,
    onExploreCatalogClick: () -> Unit,
    onAboutCreditsClick: () -> Unit = {},
    onBackupClick: () -> Unit = {},
    onRestoreClick: () -> Unit = {},
    songsByShow: Map<String, List<com.example.data.model.SongEntry>> = emptyMap(),
    modifier: Modifier = Modifier
) {
    var showEditProfileDialog by remember { mutableStateOf(false) }

    // Aggregated attendance metrics
    val totalAttendedCount = attendedShows.size
    val totalCatalogCount = allShows.size.coerceAtLeast(1)
    val totalLiveSongsHeard = remember(attendedShows, songsByShow) {
        attendedShows.sumOf { songsByShow[it.id].orEmpty().size }
    }
    val uniqueLiveSongsCount = remember(attendedShows, songsByShow) {
        attendedShows.flatMap { songsByShow[it.id].orEmpty() }.map { it.name }.distinct().size
    }
    val totalLiveSeguesCount = remember(attendedShows, songsByShow) {
        attendedShows.sumOf { show ->
            songsByShow[show.id].orEmpty().count { it.isConnected }
        }
    }
    val uniqueCitiesVisited = remember(attendedShows, songsByShow) {
        attendedShows.map { it.city }.distinct()
    }
    val uniqueVenuesVisited = remember(attendedShows, songsByShow) {
        attendedShows.map { it.venue }.distinct()
    }
    val terrapinFlyerAttended = remember(attendedShows, songsByShow) {
        attendedShows.count { it.bandName.contains("Terrapin Flyer", ignoreCase = true) }
    }

    val tourRank = remember(totalAttendedCount, totalCatalogCount) {
        userProfileTourRank(totalAttendedCount, totalCatalogCount)
    }

    if (showEditProfileDialog) {
        EditDeadheadProfileDialog(
            initialData = profileData,
            onDismiss = { showEditProfileDialog = false },
            onSave = { updated ->
                onUpdateProfile(updated)
                showEditProfileDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            UserProfileScreenTopBar(onEditProfile = { showEditProfileDialog = true })
        },
        containerColor = StealieMidnight,
        modifier = modifier.testTag("user_profile_screen")
    ) { innerPadding ->
        UserProfileScreenList(
            innerPadding = innerPadding,
            profileData = profileData,
            tourRank = tourRank,
            totalAttendedCount = totalAttendedCount,
            totalCatalogCount = totalCatalogCount,
            totalLiveSongsHeard = totalLiveSongsHeard,
            uniqueLiveSongsCount = uniqueLiveSongsCount,
            totalLiveSeguesCount = totalLiveSeguesCount,
            uniqueCitiesVisited = uniqueCitiesVisited,
            uniqueVenuesVisited = uniqueVenuesVisited,
            terrapinFlyerAttended = terrapinFlyerAttended,
            allShows = allShows,
            attendedShows = attendedShows,
            onShowClick = onShowClick,
            onToggleAttendance = onToggleAttendance,
            onExploreCatalogClick = onExploreCatalogClick,
            onBackupClick = onBackupClick,
            onRestoreClick = onRestoreClick,
            onAboutCreditsClick = onAboutCreditsClick
        )
    }
}

