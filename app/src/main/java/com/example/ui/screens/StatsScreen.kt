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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
        topBar = { StatsScreenTopBar() },
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
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("stats_list")
            ) {
                // Key Metrics Header
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("stats_metrics_row")
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

                item {
                    EraShowDistributionCard(
                        stats = stats,
                        eraBreakdown = stats.eraBreakdown
                    )
                }

                item {
                    SongFrequencyLeaderboardCard(
                        stats = stats,
                        showAttendedOnly = showAttendedOnly,
                        onShowAttendedOnlyChange = { showAttendedOnly = it },
                        onSongClick = onSongClick
                    )
                }

                item {
                    TopOpenersEncoresRow(stats = stats)
                }
            }
        }
    }
}

