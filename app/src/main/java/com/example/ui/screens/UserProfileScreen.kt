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

    val (tourRankTitle, tourRankSubtitle, nextMilestoneTarget) = remember(totalAttendedCount) {
        when {
            totalAttendedCount == 0 -> Triple(
                "Shakedown Stroller",
                "Tap any show below to punch your first ticket!",
                1
            )
            totalAttendedCount in 1..2 -> Triple(
                "Miracle Ticket Holder ⚡",
                "On the bus and catching the groove",
                3
            )
            totalAttendedCount in 3..5 -> Triple(
                "Taper Section Regular 📼",
                "Trading Maxell XL-IIs on the lot",
                6
            )
            totalAttendedCount in 6..9 -> Triple(
                "Further Bus Rider 🚌",
                "Seasoned traveler across eras & venues",
                10
            )
            else -> Triple(
                "Cosmic Charlie • Tour Legend 🌹",
                "Keeper of the vault & Deadhead historian",
                totalCatalogCount
            )
        }
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StealieMidnight)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StealieEmblem(size = 38.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Deadhead Profile",
                                    color = TextPrimaryDark,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.semantics { heading() }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                BerthaRose(size = 18.dp)
                            }
                            Text(
                                text = "Backstage Laminate & Tour Counter",
                                color = StealieGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = { showEditProfileDialog = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = StealieGold.copy(alpha = 0.15f),
                            contentColor = StealieGold
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("edit_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit Profile",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Customize",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                DancingBearsMarchRow()

                HorizontalDivider(
                    color = BorderSubtle,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        containerColor = StealieMidnight,
        modifier = modifier.testTag("user_profile_screen")
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 84.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("user_profile_list")
        ) {
            // 1. VINTAGE BACKSTAGE ALL-ACCESS LAMINATE PASS CARD
            item {
                BackstageLaminateCard(
                    profileData = profileData,
                    rankTitle = tourRankTitle,
                    rankSubtitle = tourRankSubtitle,
                    attendedCount = totalAttendedCount
                )
            }

            // 1B. GLOBAL THEME PROVIDER & MATERIAL 3 DYNAMIC COLOR CONTROL CARD
            item {
                StealieThemeControlCard()
            }

            // 2. VISUAL COUNTER SUMMARY (ODOMETER + RADIAL RING + METRIC TILES)
            item {
                VisualAttendanceCounterSummaryCard(
                    attendedShowsCount = totalAttendedCount,
                    totalCatalogCount = totalCatalogCount,
                    nextMilestoneTarget = nextMilestoneTarget,
                    totalLiveSongsHeard = totalLiveSongsHeard,
                    uniqueLiveSongsCount = uniqueLiveSongsCount,
                    totalLiveSeguesCount = totalLiveSeguesCount,
                    citiesCount = uniqueCitiesVisited.size,
                    venuesCount = uniqueVenuesVisited.size,
                    terrapinFlyerCount = terrapinFlyerAttended
                )
            }

            // 3. DEAD ERA TOUR PASSPORT STAMPS
            item {
                EraPassportStampsCard(
                    allShows = allShows,
                    attendedShows = attendedShows
                )
            }

            // 4. VISITED CITIES & VENUES RIBBON
            if (uniqueCitiesVisited.isNotEmpty()) {
                item {
                    VisitedLocationsCard(
                        cities = uniqueCitiesVisited,
                        venues = uniqueVenuesVisited
                    )
                }
            }

            // 5. QUICK ATTENDANCE CHECKLIST & ATTENDED SHOWS LOG
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MY ATTENDED CONCERT LOG (${attendedShows.size})",
                            color = StealieGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        BerthaRose(size = 14.dp)
                    }
                    TextButton(
                        onClick = onExploreCatalogClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Browse Full Vault →",
                            color = StealieCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (attendedShows.isEmpty()) {
                item {
                    Surface(
                        color = StealieCardDark,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_attended_empty")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(20.dp)
                        ) {
                            StealieEmblem(size = 48.dp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Punch Your First Tour Ticket",
                                color = TextPrimaryDark,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap any show below to toggle attendance and watch your Deadhead Tour Odometer climb!",
                                color = TextSecondaryDark,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(attendedShows, key = { "profile_attended_${it.id}" }) { show ->
                    ShowCard(
                        show = show,
                        onClick = { onShowClick(show.id) }
                    )
                }
            }

            // 6. QUICK PUNCH-TICKET TOGGLES FOR UNATTENDED SHOWS
            val unattendedSample = allShows.filter { !it.isAttended }.take(5)
            if (unattendedSample.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = StealieCardDark),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text(
                                        text = "QUICK TICKET PUNCH • ADD TO COUNTER",
                                        color = StealieCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                    Text(
                                        text = "Tap '+ I Was There' to log attendance into your counter",
                                        color = TextSecondaryDark,
                                        fontSize = 11.sp
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.ConfirmationNumber,
                                    contentDescription = null,
                                    tint = StealieGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            for (show in unattendedSample) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onShowClick(show.id) }
                                    ) {
                                        Text(
                                            text = "${show.date} • ${show.venue}",
                                            color = TextPrimaryDark,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${show.city}, ${show.stateOrCountry} (${show.bandName})",
                                            color = TextSecondaryDark,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    FilledTonalButton(
                                        onClick = { onToggleAttendance(show) },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = StealieGold.copy(alpha = 0.16f),
                                            contentColor = StealieGold
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("quick_attend_${show.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Bolt,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "+ I Was There",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onBackupClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("backup_export_button"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StealieGold),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StealieGold.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Upload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Back up my data",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            item {
                OutlinedButton(
                    onClick = onRestoreClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("backup_restore_button"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StealieGold),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StealieGold.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Restore from backup",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            item {
                OutlinedButton(
                    onClick = onAboutCreditsClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("about_credits_button"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StealieGold),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StealieGold.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "About & Credits",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

