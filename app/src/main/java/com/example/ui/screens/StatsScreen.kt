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

/**
 * Interactive Bar Chart Card visualizing song performance frequency across Grateful Dead eras.
 * Users can toggle between "All Repertoire (Total)" and any top performed song in the catalog,
 * and tap individual era bars to inspect that era's top performed songs.
 */
@Composable
private fun EraSongFrequencyBarChartCard(
    eraStats: List<EraStat>,
    songEraFrequencies: List<SongEraFrequency>,
    onSongClick: ((String) -> Unit)? = null
) {
    // null means "All Songs (Total Era Song Performances)", otherwise a specific songName
    var selectedSongName by remember { mutableStateOf<String?>(null) }
    var selectedEra by remember { mutableStateOf<DeadEra?>(eraStats.maxByOrNull { it.songPerformanceCount }?.era) }

    val selectedSongData = remember(selectedSongName, songEraFrequencies) {
        songEraFrequencies.firstOrNull { it.songName == selectedSongName }
    }

    // Build the bar values per era
    val barItems = remember(eraStats, selectedSongData) {
        eraStats.map { stat ->
            val count = if (selectedSongData != null) {
                selectedSongData.eraCounts[stat.era] ?: 0
            } else {
                stat.songPerformanceCount
            }
            Triple(stat.era, count, stat)
        }
    }

    val maxBarValue = remember(barItems) {
        (barItems.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
    }

    val totalDisplayedPerformances = remember(barItems) {
        barItems.sumOf { it.second }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StealieCardDark),
        border = BorderStroke(1.dp, StealieGold.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("era_song_bar_chart_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.BarChart,
                        contentDescription = null,
                        tint = StealieGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Song Frequency by Era",
                            color = TextPrimaryDark,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (selectedSongName == null)
                                "Total song performances logged across Grateful Dead eras"
                            else
                                "\"$selectedSongName\" performance frequency across eras",
                            color = TextSecondaryDark,
                            fontSize = 11.sp
                        )
                    }
                }

                Surface(
                    color = StealieGold.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, StealieGold.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "$totalDisplayedPerformances plays",
                        color = StealieGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Horizontal Song Filter Chips ("All Repertoire" + Top Songs)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .testTag("bar_chart_song_filter_row")
            ) {
                val isAllSelected = selectedSongName == null
                Surface(
                    color = if (isAllSelected) StealieGold.copy(alpha = 0.22f) else StealieMidnight,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isAllSelected) StealieGold else BorderSubtle
                    ),
                    modifier = Modifier
                        .clickable { selectedSongName = null }
                        .testTag("bar_filter_all_songs")
                ) {
                    Text(
                        text = "⚡ All Repertoire",
                        color = if (isAllSelected) StealieGold else TextSecondaryDark,
                        fontSize = 11.sp,
                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                    )
                }

                songEraFrequencies.take(10).forEachIndexed { index, songFreq ->
                    val isSelected = selectedSongName == songFreq.songName
                    Surface(
                        color = if (isSelected) StealieCyan.copy(alpha = 0.22f) else StealieMidnight,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) StealieCyan else BorderSubtle
                        ),
                        modifier = Modifier
                            .clickable {
                                selectedSongName = if (isSelected) null else songFreq.songName
                            }
                            .testTag("bar_filter_song_$index")
                    ) {
                        Text(
                            text = "${songFreq.songName} (${songFreq.totalPlays})",
                            color = if (isSelected) StealieCyan else TextSecondaryDark,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Custom Vertical Bar Chart with Y-Axis scale & interactive Era Columns
            EraFrequencyVerticalBarChart(
                barItems = barItems,
                maxValue = maxBarValue,
                selectedEra = selectedEra,
                onSelectEra = { era -> selectedEra = era },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("era_frequency_bar_chart_canvas")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Era Breakdown Callout
            val activeEraStat = eraStats.firstOrNull { it.era == selectedEra } ?: eraStats.firstOrNull()
            if (activeEraStat != null) {
                Surface(
                    color = StealieMidnight.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, activeEraStat.era.color.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("selected_era_bar_detail")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(activeEraStat.era.color)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${activeEraStat.era.title} (${activeEraStat.era.years})",
                                    color = TextPrimaryDark,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${activeEraStat.songPerformanceCount} total song plays",
                                color = activeEraStat.era.color,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (selectedSongData != null) {
                            val playsInEra = selectedSongData.eraCounts[activeEraStat.era] ?: 0
                            val sharePct = if (selectedSongData.totalPlays > 0) {
                                ((playsInEra.toFloat() / selectedSongData.totalPlays.toFloat()) * 100f).toInt()
                            } else 0
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "\"${selectedSongData.songName}\" in this era: $playsInEra plays ($sharePct% of song's total)",
                                    color = TextSecondaryDark,
                                    fontSize = 11.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                if (onSongClick != null) {
                                    Text(
                                        text = "View Shows ➔",
                                        color = StealieCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable { onSongClick(selectedSongData.songName) }
                                            .padding(start = 8.dp)
                                    )
                                }
                            }
                        } else if (activeEraStat.topSongsInEra.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Most Performed Songs in ${activeEraStat.era.title}:",
                                color = TextTertiaryDark,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                activeEraStat.topSongsInEra.take(4).forEach { song ->
                                    Surface(
                                        color = activeEraStat.era.color.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.clickable {
                                            selectedSongName = song.songName
                                        }
                                    ) {
                                        Text(
                                            text = "${song.songName} (${song.playCount}x)",
                                            color = activeEraStat.era.color,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

@Composable
private fun EraFrequencyVerticalBarChart(
    barItems: List<Triple<DeadEra, Int, EraStat>>,
    maxValue: Int,
    selectedEra: DeadEra?,
    onSelectEra: (DeadEra) -> Unit,
    modifier: Modifier = Modifier
) {
    val safeMax = maxValue.coerceAtLeast(1)
    val midValue = (safeMax / 2).coerceAtLeast(1)

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .height(195.dp)
        ) {
            // Y-Axis Labels Column
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(top = 18.dp, bottom = 26.dp, end = 6.dp)
                    .width(26.dp)
            ) {
                Text(
                    text = "$safeMax",
                    color = TextTertiaryDark,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$midValue",
                    color = TextTertiaryDark,
                    fontSize = 9.sp
                )
                Text(
                    text = "0",
                    color = TextTertiaryDark,
                    fontSize = 9.sp
                )
            }

            // Bars + Gridlines Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Background Grid Lines Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 22.dp, bottom = 26.dp)
                ) {
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    val gridColor = BorderSubtle.copy(alpha = 0.65f)

                    // Top line
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )
                    // Middle line
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, size.height / 2f),
                        end = Offset(size.width, size.height / 2f),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )
                    // Baseline
                    drawLine(
                        color = StealieGold.copy(alpha = 0.45f),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }

                // Interactive Era Bar Columns
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxSize()
                ) {
                    barItems.forEach { (era, count, _) ->
                        val targetFraction = (count.toFloat() / safeMax.toFloat()).coerceIn(0f, 1f)
                        val animatedFraction by animateFloatAsState(
                            targetValue = targetFraction,
                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                            label = "eraBarHeight_${era.id}"
                        )
                        val isSelected = selectedEra == era

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectEra(era) }
                                .padding(horizontal = 3.dp)
                                .testTag("era_bar_column_${era.id}")
                        ) {
                            // Play count badge above bar
                            Text(
                                text = "$count",
                                color = if (isSelected) era.color else TextPrimaryDark,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Bar Drawing Area
                            Box(
                                contentAlignment = Alignment.BottomCenter,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(26.dp)
                                ) {
                                    val minBarPx = if (count > 0) 8.dp.toPx() else 3.dp.toPx()
                                    val barHeight = (size.height * animatedFraction).coerceAtLeast(minBarPx)
                                    val topY = size.height - barHeight

                                    // Subtle track behind bar
                                    drawRoundRect(
                                        color = StealieMidnight.copy(alpha = 0.65f),
                                        topLeft = Offset(0f, 0f),
                                        size = Size(size.width, size.height),
                                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                    )

                                    // Vertical gradient bar with era signature color
                                    val barBrush = Brush.verticalGradient(
                                        colors = listOf(
                                            era.color,
                                            era.color.copy(alpha = if (isSelected) 0.85f else 0.55f)
                                        ),
                                        startY = topY,
                                        endY = size.height
                                    )

                                    drawRoundRect(
                                        brush = barBrush,
                                        topLeft = Offset(0f, topY),
                                        size = Size(size.width, barHeight),
                                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                    )

                                    if (isSelected) {
                                        drawRoundRect(
                                            color = StealieGold,
                                            topLeft = Offset(0f, topY),
                                            size = Size(size.width, barHeight),
                                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                                            style = Stroke(width = 1.5.dp.toPx())
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // X-Axis Era Short Label
                            val shortYears = era.years
                                .replace("19", "'")
                            Text(
                                text = shortYears,
                                color = if (isSelected) era.color else TextSecondaryDark,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.height(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricBox(
    value: String,
    label: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StealieCardDark,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = value,
                color = color,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                color = TextPrimaryDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = sub,
                color = TextTertiaryDark,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun EraDonutChart(
    eraStats: List<EraStat>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 24.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val arcSize = Size(diameter, diameter)
        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

        var currentStartAngle = -90f

        val totalPercentage = eraStats.sumOf { it.percentage.toDouble() }.toFloat().coerceAtLeast(1f)

        for (stat in eraStats) {
            val sweepAngle = (stat.percentage / totalPercentage) * 360f
            if (sweepAngle > 0.5f) {
                drawArc(
                    color = stat.era.color,
                    startAngle = currentStartAngle,
                    sweepAngle = sweepAngle - 2f, // subtle gap
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            currentStartAngle += sweepAngle
        }
    }
}

@Composable
private fun EraDistributionRow(stat: EraStat) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(stat.era.color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = stat.era.title,
                    color = TextPrimaryDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stat.era.years,
                    color = TextTertiaryDark,
                    fontSize = 10.sp
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${stat.showCount} shows • ${stat.songPerformanceCount} songs",
                color = TextSecondaryDark,
                fontSize = 11.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            Surface(
                color = stat.era.color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "${stat.percentage.toInt()}%",
                    color = stat.era.color,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun SongFrequencyBar(
    rank: Int,
    song: SongStat,
    maxCount: Int,
    accentColor: Color,
    onClick: () -> Unit
) {
    val progress = (song.playCount.toFloat() / maxCount.toFloat()).coerceIn(0.05f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "#$rank",
                    color = TextTertiaryDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(26.dp)
                )
                Text(
                    text = song.songName,
                    color = TextPrimaryDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = "${song.playCount} plays",
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Progress Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(StealieMidnight)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(accentColor)
            )
        }
    }
}
