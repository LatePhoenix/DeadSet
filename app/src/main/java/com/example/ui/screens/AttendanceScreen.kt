package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ShowEntity
import com.example.data.model.SongStat
import com.example.data.repository.OverallStats
import com.example.ui.components.BerthaRose
import com.example.ui.components.DancingBearsMarchRow
import com.example.ui.components.RatingStars
import com.example.ui.components.ShowCard
import com.example.ui.components.StatMetricCard
import com.example.ui.components.StealieEmblem
import com.example.ui.theme.*

@Composable
fun AttendanceScreen(
    attendedShows: List<ShowEntity>,
    stats: OverallStats?,
    onShowClick: (String) -> Unit,
    onExploreShowsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLiveSongsSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StealieMidnight)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
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
                                    text = "My Tour Journal",
                                    color = TextPrimaryDark,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                BerthaRose(size = 18.dp)
                            }
                            Text(
                                text = "Personal Attendance & Memories",
                                color = StealieGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (attendedShows.isNotEmpty()) {
                        FilledTonalButton(
                            onClick = { showLiveSongsSheet = !showLiveSongsSheet },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = StealieCyan.copy(alpha = 0.15f),
                                contentColor = StealieCyan
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("view_live_songs_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showLiveSongsSheet) "Hide Songs" else "My Live Songs",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
        modifier = modifier
    ) { innerPadding ->
        if (attendedShows.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    StealieEmblem(size = 64.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No shows attended yet",
                        color = TextPrimaryDark,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Browse classic shows in the catalog and tap 'I Was There' to log your tour attendance, ratings, and memories!",
                        color = TextSecondaryDark,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onExploreShowsClick,
                        colors = ButtonDefaults.buttonColors(containerColor = StealieGold),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = StealieMidnight
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Explore Classic Shows",
                            color = StealieMidnight,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 80.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Personal Attendance Summary Cards
                item {
                    val ratedShows = attendedShows.filter { it.userRating > 0f }
                    val avgRating = if (ratedShows.isNotEmpty()) {
                        ratedShows.map { it.userRating }.average().toFloat()
                    } else 0f

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StatMetricCard(
                            value = "${attendedShows.size}",
                            label = "Shows Attended",
                            icon = Icons.Filled.Bolt,
                            accentColor = StealieGold,
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricCard(
                            value = "${stats?.topSongsAttended?.size ?: 0}",
                            label = "Live Songs Seen",
                            icon = Icons.Filled.MusicNote,
                            accentColor = StealieCyan,
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricCard(
                            value = if (avgRating > 0f) String.format("%.1f", avgRating) else "—",
                            label = "Avg Rating",
                            icon = Icons.Filled.Star,
                            accentColor = StealiePurple,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // If user toggled "My Live Songs Repertoire"
                if (showLiveSongsSheet && stats?.topSongsAttended?.isNotEmpty() == true) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = StealieCardDark),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StealieCyan.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Your Live Song Repertoire",
                                    color = StealieCyan,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Songs you witnessed performed in person",
                                    color = TextSecondaryDark,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                for (song in stats.topSongsAttended) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = song.songName,
                                            color = TextPrimaryDark,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Surface(
                                            color = StealieMidnight,
                                            shape = RoundedCornerShape(6.dp),
                                            border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle)
                                        ) {
                                            Text(
                                                text = "${song.playCount}x live",
                                                color = StealieGold,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "YOUR ATTENDED SHOWS (${attendedShows.size})",
                        color = StealieGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(start = 2.dp, top = 4.dp)
                    )
                }

                items(attendedShows, key = { it.id }) { show ->
                    ShowCard(
                        show = show,
                        onClick = { onShowClick(show.id) }
                    )
                }
            }
        }
    }
}
