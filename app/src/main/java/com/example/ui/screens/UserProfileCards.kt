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


// Extracted for 6.3 screen breakup.

@Composable
internal fun BackstageLaminateCard(
    profileData: UserProfileData,
    rankTitle: String,
    rankSubtitle: String,
    attendedCount: Int
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = StealieCardDark),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            brush = Brush.linearGradient(
                listOf(StealieCrimson, StealieGold, StealieBlue)
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("backstage_laminate_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            StealieCrimson.copy(alpha = 0.22f),
                            StealieCardDark,
                            StealieBlue.copy(alpha = 0.18f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            // Lanyard Punch Slot at top center of Backstage Pass
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(StealieMidnight)
                        .border(1.dp, StealieGold.copy(alpha = 0.5f), RoundedCornerShape(50))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pass Header Strip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = StealieCrimson.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StealieCrimsonLight)
                ) {
                    Text(
                        text = "ALL ACCESS • BACKSTAGE LAMINATE",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = "PASS #GD-${1965 + attendedCount}",
                    color = StealieGold,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Avatar + Handle + Rank
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    StealieEmblem(size = 68.dp)
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(StealieMidnight)
                            .border(1.dp, StealieGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        BerthaRose(size = 16.dp)
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profileData.handle,
                        color = TextPrimaryDark,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = rankTitle,
                        color = StealieGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = rankSubtitle,
                        color = TextSecondaryDark,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Profile Metadata Chips (Home Lot, Favorite Jam, Tape Deck)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ProfileTagPill(
                    label = "HOME LOT",
                    value = profileData.homeCity,
                    accent = StealieCyan,
                    modifier = Modifier.weight(1f)
                )
                ProfileTagPill(
                    label = "SIGNATURE SEGUE",
                    value = profileData.favoriteJam,
                    accent = StealieCrimsonLight,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                ProfileTagPill(
                    label = "FAVORITE ERA",
                    value = profileData.favoriteEraTitle,
                    accent = StealieGold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                TerrapinTurtleBadge()
            }
        }
    }
}

@Composable
internal fun ProfileTagPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StealieMidnight.copy(alpha = 0.75f),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(
                text = label,
                color = accent,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Text(
                text = value,
                color = TextPrimaryDark,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

/**
 * Visual Counter Summary Card featuring a vintage Tour Bus Odometer,
 * Radial Stealie Progress Ring, and 4 Deadhead Metric Counters.
 */
@Composable
internal fun VisualAttendanceCounterSummaryCard(
    attendedShowsCount: Int,
    totalCatalogCount: Int,
    nextMilestoneTarget: Int,
    totalLiveSongsHeard: Int,
    uniqueLiveSongsCount: Int,
    totalLiveSeguesCount: Int,
    citiesCount: Int,
    venuesCount: Int,
    terrapinFlyerCount: Int
) {
    val progressFraction = (attendedShowsCount.toFloat() / totalCatalogCount.coerceAtLeast(1).toFloat())
        .coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "AttendanceRingProgress"
    )

    // Format 3-digit odometer string e.g. "005"
    val odometerDigits = String.format("%03d", attendedShowsCount.coerceAtMost(999)).toList()

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = StealieCardDark),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, StealieGold.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("visual_counter_summary_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = StealieGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TOUR ATTENDANCE COUNTER SUMMARY",
                        color = StealieGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.9.sp
                    )
                }

                Surface(
                    color = StealieMidnight,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StealieCyan.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "${(progressFraction * 100).toInt()}% of Vault Seen",
                        color = StealieCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Center Row: Mechanical Odometer + Stealie Radial Progress Gauge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Vintage Mechanical 3-Digit Odometer Display
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1.2f)
                ) {
                    Text(
                        text = "TOTAL SHOWS ATTENDED",
                        color = TextSecondaryDark,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.testTag("attendance_odometer_digits")
                    ) {
                        for (digit in odometerDigits) {
                            Surface(
                                color = StealieMidnight,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.5.dp,
                                    Brush.verticalGradient(listOf(StealieGold, StealieCrimsonLight))
                                ),
                                modifier = Modifier.size(width = 44.dp, height = 58.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    // Subtle horizontal flip-card divider line
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(BorderSubtle.copy(alpha = 0.6f))
                                    )
                                    Text(
                                        text = digit.toString(),
                                        color = StealieGold,
                                        fontSize = 30.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Next Rank Target: $nextMilestoneTarget Shows",
                        color = StealieCrimsonLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Radial Steal Your Face Progress Ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(112.dp)
                        .padding(4.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 10.dp.toPx()
                        val diameter = size.minDimension - strokeWidth
                        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                        // Background Track
                        drawArc(
                            color = Color(0xFF1E293B),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(diameter, diameter),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // Foreground Stealie Gradient Arc
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(StealieCrimson, StealieGold, StealieCyan, StealieCrimson)
                            ),
                            startAngle = -90f,
                            sweepAngle = (animatedProgress * 360f).coerceAtLeast(12f),
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(diameter, diameter),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$attendedShowsCount/$totalCatalogCount",
                            color = TextPrimaryDark,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "SHOWS",
                            color = StealieGold,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4-Tile Visual Counter Grid
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                CounterSummaryTile(
                    count = "$totalLiveSongsHeard",
                    subtitle = "$uniqueLiveSongsCount Unique",
                    label = "Live Songs Heard",
                    accentColor = StealieCyan,
                    modifier = Modifier.weight(1f)
                )
                CounterSummaryTile(
                    count = "$totalLiveSeguesCount",
                    subtitle = "-> · > · ~>",
                    label = "Transitions Witnessed",
                    accentColor = StealieCrimsonLight,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                CounterSummaryTile(
                    count = "$citiesCount",
                    subtitle = "$venuesCount Venues",
                    label = "Tour Cities Visited",
                    accentColor = StealiePurple,
                    modifier = Modifier.weight(1f)
                )
                CounterSummaryTile(
                    count = "$terrapinFlyerCount",
                    subtitle = "Local Tribute",
                    label = "Terrapin Flyer Shows",
                    accentColor = StealieGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
internal fun CounterSummaryTile(
    count: String,
    subtitle: String,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StealieMidnight,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = count,
                    color = accentColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = subtitle,
                    color = TextSecondaryDark,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = TextPrimaryDark,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
internal fun EraPassportStampsCard(
    allShows: List<ShowEntity>,
    attendedShows: List<ShowEntity>
) {
    val eras = DeadEra.entries.filter { it != DeadEra.ALL }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StealieCardDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DancingBear(color = BearOrange, size = 24.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "DEAD ERA TOUR PASSPORT",
                        color = StealieGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Your attended concerts broken down by Grateful Dead era",
                        color = TextSecondaryDark,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            for (era in eras) {
                val totalInEra = allShows.count { it.eraId.equals(era.id, ignoreCase = true) }
                val attendedInEra = attendedShows.count { it.eraId.equals(era.id, ignoreCase = true) }
                val fraction = if (totalInEra > 0) attendedInEra.toFloat() / totalInEra.toFloat() else 0f

                Column(modifier = Modifier.padding(vertical = 5.dp)) {
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
                                    .background(era.color)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${era.title} (${era.years})",
                                color = TextPrimaryDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = "$attendedInEra / $totalInEra attended",
                            color = if (attendedInEra > 0) era.color else TextTertiaryDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(StealieMidnight)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(era.color)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun VisitedLocationsCard(
    cities: List<String>,
    venues: List<String>
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StealieCardDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, StealieCyan.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "TOUR STOPS & LEGENDARY HALLS VISITED",
                color = StealieCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "📍 Cities: ${cities.joinToString(" • ")}",
                color = TextPrimaryDark,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "🏛️ Venues: ${venues.joinToString(" • ")}",
                color = TextSecondaryDark,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
internal fun EditDeadheadProfileDialog(
    initialData: UserProfileData,
    onDismiss: () -> Unit,
    onSave: (UserProfileData) -> Unit
) {
    var handle by remember { mutableStateOf(initialData.handle) }
    var homeCity by remember { mutableStateOf(initialData.homeCity) }
    var favoriteJam by remember { mutableStateOf(initialData.favoriteJam) }
    var favoriteEraTitle by remember { mutableStateOf(initialData.favoriteEraTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StealieCardDark,
        titleContentColor = TextPrimaryDark,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StealieEmblem(size = 28.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Customize Backstage Pass",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = handle,
                    onValueChange = { handle = it },
                    label = { Text("Deadhead Handle / Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StealieGold,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_handle_input")
                )

                OutlinedTextField(
                    value = homeCity,
                    onValueChange = { homeCity = it },
                    label = { Text("Home Tour City") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StealieCyan,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_city_input")
                )

                OutlinedTextField(
                    value = favoriteJam,
                    onValueChange = { favoriteJam = it },
                    label = { Text("Signature Segue / Jam") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StealieCrimsonLight,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_jam_input")
                )

                OutlinedTextField(
                    value = favoriteEraTitle,
                    onValueChange = { favoriteEraTitle = it },
                    label = { Text("Favorite Grateful Dead Era") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StealieGold,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        UserProfileData(
                            handle = handle.ifBlank { "Captain Trips" },
                            homeCity = homeCity.ifBlank { "Chicago, IL" },
                            favoriteJam = favoriteJam.ifBlank { "Scarlet ➔ Fire" },
                            favoriteEraTitle = favoriteEraTitle.ifBlank { "1977 Spring Tour Peak" }
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = StealieGold),
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Text(
                    text = "Save Pass",
                    color = StealieMidnight,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondaryDark)
            }
        }
    )
}
