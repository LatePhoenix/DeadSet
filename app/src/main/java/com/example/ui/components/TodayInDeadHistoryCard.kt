package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.ui.util.ConcertIntentHelper
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ShowEntity
import com.example.data.model.DeadEra
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TodayInDeadHistoryCard(
    shows: List<ShowEntity>,
    onOpenShow: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (shows.isEmpty()) return

    // Today's Month & Day (MM-DD)
    val todayFormatter = remember { SimpleDateFormat("MM-dd", Locale.US) }
    val todayDisplayFormatter = remember { SimpleDateFormat("MMMM d", Locale.US) }
    val todayDate = remember { Date() }
    val todayMonthDay = remember { todayFormatter.format(todayDate) }
    val todayDisplay = remember { todayDisplayFormatter.format(todayDate) }

    // Date Presets to allow quick switching
    val presetDates = remember {
        listOf(
            "Today" to todayMonthDay,
            "Oct 31" to "10-31",
            "May 8" to "05-08",
            "Aug 27" to "08-27",
            "Feb 27" to "02-27",
            "Jul 4" to "07-04",
            "Sep 3" to "09-03"
        )
    }

    var selectedDateKey by remember { mutableStateOf(todayMonthDay) }

    // Find the show matching the selected month-day, or fallback to the closest show in that month
    val matchedShow = remember(selectedDateKey, shows) {
        shows.firstOrNull { it.date.endsWith("-$selectedDateKey") }
            ?: shows.firstOrNull {
                val month = selectedDateKey.substringBefore("-")
                it.date.contains("-$month-")
            }
            ?: shows.first()
    }

    val era = DeadEra.fromId(matchedShow.eraId)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StealieCardDark),
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            Brush.linearGradient(listOf(StealieGold, StealieCrimson, StealieBlue))
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("today_in_dead_history_card")
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Header: Title with Calendar Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(StealieGold.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            tint = StealieGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "TODAY IN DEAD HISTORY",
                            color = StealieGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Notable Concert Archive",
                            color = TextSecondaryDark,
                            fontSize = 11.sp
                        )
                    }
                }

                EraBadge(era = era)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Date Switcher Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for ((label, dateKey) in presetDates) {
                    val isSelected = selectedDateKey == dateKey
                    Surface(
                        color = if (isSelected) StealieGold.copy(alpha = 0.25f) else StealieMidnight,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) StealieGold else BorderSubtle
                        ),
                        modifier = Modifier.clickable { selectedDateKey = dateKey }
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) StealieGold else TextSecondaryDark,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Show Date and Venue
            Text(
                text = "${matchedShow.date} • ${matchedShow.venue}",
                color = TextPrimaryDark,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${matchedShow.city}, ${matchedShow.stateOrCountry} • ${matchedShow.tourName}",
                color = StealieCyan,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp)
            )

            // Historic Trivia Callout
            if (matchedShow.historicTrivia.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = StealieMidnight.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StealiePurple.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = StealieGold,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "HISTORIC TRIVIA & LORE",
                                color = StealieGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            )
                            Text(
                                text = matchedShow.historicTrivia,
                                color = TextPrimaryDark,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            // Key Transition / Highlight Badges
            if (matchedShow.tags.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val tags = matchedShow.tags.split(",").take(3)
                    for (t in tags) {
                        Surface(
                            color = StealieMidnight,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = null,
                                    tint = StealieCrimsonLight,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = t.trim(),
                                    color = TextSecondaryDark,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            val context = LocalContext.current

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Maps, Calendar, and Explore Setlist
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Open in Maps
                FilledTonalIconButton(
                    onClick = { ConcertIntentHelper.openVenueInMaps(context, matchedShow) },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = StealieMidnight,
                        contentColor = StealieCyan
                    ),
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("today_history_map_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Map,
                        contentDescription = "Directions in Maps",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Add to Calendar
                FilledTonalIconButton(
                    onClick = { ConcertIntentHelper.addConcertToCalendar(context, matchedShow) },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = StealieMidnight,
                        contentColor = StealieGold
                    ),
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("today_history_calendar_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = "Add to Calendar",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Explore Full Setlist
                FilledTonalButton(
                    onClick = { onOpenShow(matchedShow.id) },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = StealieGold,
                        contentColor = StealieMidnight
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("today_in_history_open_button")
                ) {
                    Text(
                        text = "Explore Setlist",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}
