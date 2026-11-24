package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeadEra
import com.example.data.model.EraStat
import com.example.data.model.SongEraFrequency
import com.example.data.model.SongStat
import com.example.data.repository.OverallStats
import com.example.ui.components.StealieEmblem
import com.example.ui.theme.*

@Composable
fun StatsScreen(
    stats: OverallStats?,
    onSongClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showAttendedOnly by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StealieMidnight)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StealieEmblem(size = 38.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Tour Analytics",
                            color = TextPrimaryDark,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Era Song Bar Charts & Setlist Frequencies",
                            color = StealieGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                HorizontalDivider(
                    color = BorderSubtle,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        },
        containerColor = StealieMidnight,
        modifier = modifier
    ) { innerPadding ->
        if (stats == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = StealieGold)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 80.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Key Metrics Header
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SummaryMetricBox(
                            value = "${stats.totalShowsCount}",
                            label = "Shows",
                            sub = "${stats.attendedShowsCount} attended",
                            color = StealieGold,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryMetricBox(
                            value = "${stats.totalUniqueSongs}",
                            label = "Songs",
                            sub = "${stats.totalSongPerformancesIndexed} plays",
                            color = StealieCyan,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryMetricBox(
                            value = "${stats.totalTransitionsLogged}",
                            label = "Segues",
                            sub = "➔ Transitions",
                            color = StealieCrimsonLight,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // BAR CHART VISUALIZATION: SONG PERFORMANCE FREQUENCY ACROSS GRATEFUL DEAD ERAS
                item {
                    EraSongFrequencyBarChartCard(
                        eraStats = stats.eraBreakdown,
                        songEraFrequencies = stats.songEraFrequencies,
                        onSongClick = onSongClick
                    )
                }

                // ERA DISTRIBUTION SECTION WITH CUSTOM CANVAS DONUT CHART
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = StealieCardDark),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("era_distribution_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.PieChart,
                                    contentDescription = null,
                                    tint = StealieGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Era Show Distribution",
                                    color = TextPrimaryDark,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Proportion of shows across the Grateful Dead's 30-year journey",
                                color = TextSecondaryDark,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Custom Canvas Donut Chart
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                EraDonutChart(
                                    eraStats = stats.eraBreakdown,
                                    modifier = Modifier.size(170.dp)
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${stats.totalShowsCount}",
                                        color = TextPrimaryDark,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "SHOWS",
                                        color = TextTertiaryDark,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Era Legend List
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (stat in stats.eraBreakdown) {
                                    EraDistributionRow(stat = stat)
                                }
                            }
                        }
                    }
                }

                // SONG FREQUENCIES LEADERBOARD
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = StealieCardDark),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("song_frequencies_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.MusicNote,
                                        contentDescription = null,
                                        tint = StealieCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Song Frequency Leaderboard",
                                        color = TextPrimaryDark,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Toggle between Catalog and Attended
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(StealieMidnight)
                                        .padding(2.dp)
                                ) {
                                    Surface(
                                        color = if (!showAttendedOnly) StealieCyan.copy(alpha = 0.2f) else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.clickable { showAttendedOnly = false }
                                    ) {
                                        Text(
                                            text = "Catalog",
                                            color = if (!showAttendedOnly) StealieCyan else TextTertiaryDark,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Surface(
                                        color = if (showAttendedOnly) StealieGold.copy(alpha = 0.2f) else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.clickable { showAttendedOnly = true }
                                    ) {
                                        Text(
                                            text = "Attended",
                                            color = if (showAttendedOnly) StealieGold else TextTertiaryDark,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = if (showAttendedOnly)
                                    "Songs you've seen performed live most frequently"
                                else
                                    "Most frequently performed classics across catalog setlists",
                                color = TextSecondaryDark,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            val songList = if (showAttendedOnly) stats.topSongsAttended else stats.topSongsOverall
                            val maxCount = (songList.firstOrNull()?.playCount ?: 1).coerceAtLeast(1)

                            if (songList.isEmpty()) {
                                Text(
                                    text = "No attended song data available yet. Mark shows as attended to see your live song stats!",
                                    color = TextTertiaryDark,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    songList.take(10).forEachIndexed { index, song ->
                                        SongFrequencyBar(
                                            rank = index + 1,
                                            song = song,
                                            maxCount = maxCount,
                                            accentColor = if (showAttendedOnly) StealieGold else StealieCyan,
                                            onClick = { onSongClick?.invoke(song.songName) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // TOP SET OPENERS & ENCORES
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = StealieCardDark),
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Top Set 1 Openers",
                                    color = StealieGold,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                for (opener in stats.topOpeners.take(4)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = opener.songName,
                                            color = TextPrimaryDark,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Text(
                                            text = "${opener.playCount}x",
                                            color = StealieGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = StealieCardDark),
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Top Encores",
                                    color = StealieCrimsonLight,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                for (encore in stats.topEncores.take(4)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = encore.songName,
                                            color = TextPrimaryDark,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Text(
                                            text = "${encore.playCount}x",
                                            color = StealieCrimsonLight,
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
        }
    }
}

