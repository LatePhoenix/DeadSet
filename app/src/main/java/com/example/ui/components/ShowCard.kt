package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ShowEntity
import com.example.data.model.DeadEra
import com.example.ui.theme.*
import com.example.ui.util.ConcertIntentHelper

@Composable
fun ShowCard(
    show: ShowEntity,
    onClick: () -> Unit,
    onOpenMaps: ((ShowEntity) -> Unit)? = null,
    onAddToCalendar: ((ShowEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val era = DeadEra.fromId(show.eraId)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (show.isAttended) StealieCardElevated else StealieCardDark
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (show.isUpcoming) StealieGreen.copy(alpha = 0.6f)
            else if (show.isAttended) StealieGold.copy(alpha = 0.5f)
            else BorderSubtle
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("show_card_${show.id}")
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Top Row: Date, Era / Band Badge & Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = show.date,
                        color = if (show.isUpcoming) StealieGreen else StealieGold,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    if (show.isUpcoming) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = StealieGreen.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StealieGreen.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "UPCOMING",
                                color = StealieGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (show.isAttended) {
                        AttendedBadge()
                    }
                    if (show.bandName != "Grateful Dead") {
                        Surface(
                            color = StealiePurple.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StealiePurple.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = show.bandName.uppercase(),
                                color = StealiePurple,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    } else {
                        EraBadge(era = era)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Venue Name
            Text(
                text = show.venue,
                color = TextPrimaryDark,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Location
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 3.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = TextSecondaryDark,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${show.city}, ${show.stateOrCountry}",
                    color = TextSecondaryDark,
                    fontSize = 13.sp
                )
                if (show.bandName != "Grateful Dead") {
                    Text(
                        text = " • ${show.bandName}",
                        color = StealieCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Key Transition Highlights or Tags if present
            if (show.tags.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val tagList = show.tags.split(",").take(3)
                    for (tag in tagList) {
                        Surface(
                            color = StealieMidnight.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle)
                        ) {
                            Text(
                                text = tag.trim(),
                                color = StealieCyan,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // User Review Preview (if entered)
            if (show.userReview.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = StealieMidnight.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, StealiePurple.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ChatBubbleOutline,
                            contentDescription = "User Review",
                            tint = StealiePurple,
                            modifier = Modifier
                                .size(14.dp)
                                .padding(top = 1.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = show.userReview,
                            color = TextSecondaryDark,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Bottom Actions: Calendar, Maps, and View Setlist
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Calendar and Map Shortcuts
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Map Directions shortcut
                    FilledTonalIconButton(
                        onClick = {
                            if (onOpenMaps != null) onOpenMaps(show)
                            else ConcertIntentHelper.openVenueInMaps(context, show)
                        },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = StealieMidnight,
                            contentColor = StealieCyan
                        ),
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("card_map_button_${show.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Map,
                            contentDescription = "Open in Maps",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Add to Calendar shortcut
                    FilledTonalIconButton(
                        onClick = {
                            if (onAddToCalendar != null) onAddToCalendar(show)
                            else ConcertIntentHelper.addConcertToCalendar(context, show)
                        },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = StealieMidnight,
                            contentColor = StealieGold
                        ),
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("card_calendar_button_${show.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = "Add to Calendar",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (show.userRating > 0f) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = StealieGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = String.format("%.1f", show.userRating),
                                color = StealieGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // View Setlist Link
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onClick() }
                ) {
                    Text(
                        text = if (show.isUpcoming) "Preview Setlist" else "View Setlist",
                        color = StealieCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = StealieCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
