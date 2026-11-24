package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.data.local.ShowEntity
import com.example.data.model.SongEntry
import com.example.data.repository.OverallStats
import com.example.data.setlist.ChaseStatus
import com.example.ui.UserProfileData
import com.example.ui.theme.StealieGold
import com.example.ui.theme.StealieMidnight
import com.example.ui.theme.TextSecondaryDark

private val ME_PANES = listOf("My Shows", "Stats", "Profile")

/**
 * Me tab (6.1): Attendance + Stats + Profile behind a secondary tab row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeTabScreen(
    attendedShows: List<ShowEntity>,
    allShows: List<ShowEntity>,
    songsByShow: Map<String, List<SongEntry>>,
    stats: OverallStats?,
    profileData: UserProfileData,
    chaseStatuses: List<ChaseStatus>,
    allSongNames: List<String>,
    onShowClick: (String) -> Unit,
    onExploreShowsClick: () -> Unit,
    onSongSearch: (String) -> Unit,
    onAddChase: (String) -> Unit,
    onRemoveChase: (String) -> Unit,
    onUpdateProfile: (UserProfileData) -> Unit,
    onToggleAttendance: (ShowEntity) -> Unit,
    onAboutCreditsClick: () -> Unit,
    onBackupClick: () -> Unit = {},
    onRestoreClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var pane by rememberSaveable { mutableIntStateOf(0) }
    Column(modifier.fillMaxSize()) {
        SecondaryTabRow(
            selectedTabIndex = pane,
            containerColor = StealieMidnight,
            contentColor = StealieGold,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("me_pane_tabs")
        ) {
            ME_PANES.forEachIndexed { index, title ->
                Tab(
                    selected = pane == index,
                    onClick = { pane = index },
                    text = {
                        Text(
                            title,
                            fontSize = 13.sp,
                            fontWeight = if (pane == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (pane == index) StealieGold else TextSecondaryDark
                        )
                    },
                    modifier = Modifier.testTag("me_pane_$index")
                )
            }
        }
        when (pane) {
            0 -> AttendanceScreen(
                attendedShows = attendedShows,
                stats = stats,
                onShowClick = onShowClick,
                onExploreShowsClick = onExploreShowsClick,
                songsByShow = songsByShow,
                chaseStatuses = chaseStatuses,
                allSongNames = allSongNames,
                onAddChase = onAddChase,
                onRemoveChase = onRemoveChase
            )
            1 -> StatsScreen(
                stats = stats,
                onSongClick = onSongSearch
            )
            else -> UserProfileScreen(
                allShows = allShows,
                songsByShow = songsByShow,
                attendedShows = attendedShows,
                stats = stats,
                profileData = profileData,
                onUpdateProfile = onUpdateProfile,
                onShowClick = onShowClick,
                onToggleAttendance = onToggleAttendance,
                onExploreCatalogClick = onExploreShowsClick,
                onAboutCreditsClick = onAboutCreditsClick,
                onBackupClick = onBackupClick,
                onRestoreClick = onRestoreClick
            )
        }
    }
}
