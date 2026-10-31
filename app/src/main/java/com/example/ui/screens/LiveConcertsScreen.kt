package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.LiveConcertApiClient
import com.example.data.remote.LiveConcertEvent
import com.example.data.remote.TributeBandFilter
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun LiveConcertsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiClient = remember { LiveConcertApiClient() }

    var searchQuery by remember { mutableStateOf("") }
    var selectedBandFilter by remember { mutableStateOf(TributeBandFilter.ALL) }
    var midwestOnly by remember { mutableStateOf(true) } // Default to Midwest dates so users immediately see Midwest DSO, JRAD & Terrapin Flyer shows
    var isLoading by remember { mutableStateOf(false) }
    var allEvents by remember { mutableStateOf(apiClient.getCuratedUpcomingTourSchedule()) }
    var showApiInfoDialog by remember { mutableStateOf(false) }

    val filteredEvents = remember(allEvents, searchQuery, selectedBandFilter, midwestOnly) {
        apiClient.filterConcertEvents(
            events = allEvents,
            searchQuery = searchQuery,
            bandFilter = selectedBandFilter,
            midwestOnly = midwestOnly
        )
    }

    val totalMidwestCount = remember(allEvents, selectedBandFilter, searchQuery) {
        apiClient.filterConcertEvents(
            events = allEvents,
            searchQuery = searchQuery,
            bandFilter = selectedBandFilter,
            midwestOnly = true
        ).size
    }

    fun triggerLiveSearch() {
        coroutineScope.launch {
            isLoading = true
            val results = apiClient.searchLiveConcerts(
                searchQuery = searchQuery,
                bandFilter = selectedBandFilter,
                midwestOnly = false // Fetch all and filter reactively in UI
            )
            if (results.isNotEmpty()) {
                allEvents = results
            }
            isLoading = false
        }
    }

    LaunchedEffect(selectedBandFilter) {
        triggerLiveSearch()
    }

    if (showApiInfoDialog) {
        AlertDialog(
            onDismissRequest = { showApiInfoDialog = false },
            containerColor = StealieCardDark,
            titleContentColor = TextPrimaryDark,
            textContentColor = TextSecondaryDark,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.ConfirmationNumber,
                        contentDescription = null,
                        tint = StealieGold,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Bandsintown & Ticketmaster API",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Live Tour Search queries the Bandsintown Public Artist Events API (rest.bandsintown.com/artists/{artist}/events) and the Ticketmaster Discovery API v2 (app.ticketmaster.com/discovery/v2/events.json).",
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    Surface(
                        color = StealieMidnight,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "API CONFIGURATION (AI STUDIO SECRETS PANEL)",
                                color = StealieGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "• BANDSINTOWN_APP_ID: ${apiClient.getBandsintownAppId()}",
                                color = StealieCyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "• TICKETMASTER_API_KEY: ${if (apiClient.getTicketmasterApiKey().isNotBlank()) "Configured ✓" else "Optional (set in AI Studio Secrets panel)"}",
                                color = TextPrimaryDark,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showApiInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = StealieGold)
                ) {
                    Text("Got It", color = StealieMidnight, fontWeight = FontWeight.Bold)
                }
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
                        StealieEmblem(size = 40.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Live Concert Search",
                                    color = TextPrimaryDark,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                BerthaRose(size = 18.dp)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { showApiInfoDialog = true }
                                    .testTag("concert_api_status_badge")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ConfirmationNumber,
                                    contentDescription = "Bandsintown & Ticketmaster API",
                                    tint = StealieGold,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Bandsintown & Ticketmaster API • DSO • JRAD • Terrapin Flyer",
                                    color = StealieGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { triggerLiveSearch() },
                        modifier = Modifier.testTag("refresh_concerts_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = StealieGold,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Refresh Live Tour Dates",
                                tint = StealieCyan
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                DancingBearsMarchRow()

                Spacer(modifier = Modifier.height(10.dp))

                // Band / City / Venue Search Input Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "Search band (DSO, JRAD, Terrapin Flyer) or city...",
                                color = TextTertiaryDark,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search Band or City",
                                tint = StealieGold
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = "Clear search",
                                        tint = TextSecondaryDark
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { triggerLiveSearch() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StealieGold,
                            unfocusedBorderColor = BorderSubtle,
                            focusedContainerColor = StealieCardDark,
                            unfocusedContainerColor = StealieCardDark,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("concert_search_input")
                    )

                    FilledTonalButton(
                        onClick = { triggerLiveSearch() },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = StealieGold,
                            contentColor = StealieMidnight
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                        modifier = Modifier.testTag("concert_search_button")
                    ) {
                        Text(
                            text = "Search",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Band Filter Chips + Midwest Dates Toggle Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    // Midwest Dates Filter Toggle Chip
                    FilterChip(
                        selected = midwestOnly,
                        onClick = { midwestOnly = !midwestOnly },
                        label = {
                            Text(
                                text = if (midwestOnly) {
                                    "🌽 Midwest Dates ($totalMidwestCount)"
                                } else {
                                    "🌎 All Regions (Tap for Midwest)"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (midwestOnly) Icons.Filled.Explore else Icons.Filled.Public,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StealieGreen.copy(alpha = 0.22f),
                            selectedLabelColor = StealieGreen,
                            selectedLeadingIconColor = StealieGreen,
                            containerColor = StealieCardDark,
                            labelColor = TextSecondaryDark,
                            iconColor = TextSecondaryDark
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = midwestOnly,
                            borderColor = BorderSubtle,
                            selectedBorderColor = StealieGreen
                        ),
                        modifier = Modifier.testTag("midwest_filter_chip")
                    )

                    // Band Filter Chips: All, Dark Star Orchestra, JRAD, Terrapin Flyer
                    val bandChips = listOf(
                        Triple(TributeBandFilter.ALL, "All Dead Bands", "band_chip_all"),
                        Triple(TributeBandFilter.DARK_STAR_ORCHESTRA, "⚡ Dark Star Orchestra", "band_chip_dso"),
                        Triple(TributeBandFilter.JRAD, "🐙 JRAD", "band_chip_jrad"),
                        Triple(TributeBandFilter.TERRAPIN_FLYER, "🐢 Terrapin Flyer", "band_chip_terrapin")
                    )

                    for ((filter, label, tag) in bandChips) {
                        val isSelected = selectedBandFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedBandFilter = filter },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StealieGold.copy(alpha = 0.22f),
                                selectedLabelColor = StealieGold,
                                containerColor = StealieCardDark,
                                labelColor = TextSecondaryDark
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = BorderSubtle,
                                selectedBorderColor = StealieGold
                            ),
                            modifier = Modifier.testTag(tag)
                        )
                    }
                }

                HorizontalDivider(
                    color = BorderSubtle,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        containerColor = StealieMidnight,
        modifier = modifier.testTag("live_concerts_screen")
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 88.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Summary Banner
            item {
                Surface(
                    color = StealieCardDark,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            DancingBear(color = BearGreen, size = 34.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (midwestOnly) {
                                        "UPCOMING MIDWEST DEAD DATES (IL • WI • IN • MI • OH • MN • MO)"
                                    } else {
                                        "ALL UPCOMING LIVE CONCERT DATES"
                                    },
                                    color = StealieGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.7.sp
                                )
                                Text(
                                    text = "Showing ${filteredEvents.size} live ${if (filteredEvents.size == 1) "concert" else "concerts"} • Tap any ticket link to book",
                                    color = TextSecondaryDark,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            if (filteredEvents.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StealieCardDark),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            StealieEmblem(size = 56.dp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No matching live dates found",
                                color = TextPrimaryDark,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try toggling off 'Midwest Dates Only' or clearing your band search filter.",
                                color = TextSecondaryDark,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    searchQuery = ""
                                    selectedBandFilter = TributeBandFilter.ALL
                                    midwestOnly = false
                                },
                                border = BorderStroke(1.dp, StealieGold)
                            ) {
                                Text("Show All Upcoming Dates", color = StealieGold, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(filteredEvents, key = { it.id }) { event ->
                    LiveConcertEventCard(
                        event = event,
                        onOpenTickets = { url ->
                            runCatching {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }.onFailure {
                                Toast.makeText(context, "Opening ticket link: $url", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onAddToCalendar = { ev ->
                            launchConcertCalendarIntent(context, ev)
                        },
                        onOpenVenueMap = { ev ->
                            launchConcertVenueMapIntent(context, ev)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveConcertEventCard(
    event: LiveConcertEvent,
    onOpenTickets: (String) -> Unit,
    onAddToCalendar: (LiveConcertEvent) -> Unit,
    onOpenVenueMap: (LiveConcertEvent) -> Unit
) {
    val bandAccent = when {
        event.bandName.contains("Terrapin Flyer", ignoreCase = true) -> StealieGreen
        event.bandName.contains("Dark Star Orchestra", ignoreCase = true) -> StealieCrimsonLight
        event.bandName.contains("Joe Russo", ignoreCase = true) ||
            event.bandName.contains("JRAD", ignoreCase = true) -> StealieCyan
        else -> StealieGold
    }

    val bearColor = when {
        event.bandName.contains("Terrapin Flyer", ignoreCase = true) -> BearGreen
        event.bandName.contains("Dark Star Orchestra", ignoreCase = true) -> BearPink
        event.bandName.contains("Joe Russo", ignoreCase = true) -> BearBlue
        else -> BearYellow
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = StealieCardDark),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, bandAccent.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("live_concert_card_${event.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Band Pill + Midwest Badge + API Source
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = bandAccent.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, bandAccent.copy(alpha = 0.5f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(bandAccent)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = event.bandName,
                                color = bandAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    if (event.isMidwest) {
                        Surface(
                            color = StealieGold.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, StealieGold.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "MIDWEST • ${event.stateCode}",
                                color = StealieGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Text(
                    text = event.apiSource,
                    color = TextTertiaryDark,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Middle Row: Date Badge + Event Title, Venue Name & City
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Calendar Date Badge
                val parts = event.date.split("-")
                val monthLabel = when (parts.getOrNull(1)) {
                    "01" -> "JAN"
                    "02" -> "FEB"
                    "03" -> "MAR"
                    "04" -> "APR"
                    "05" -> "MAY"
                    "06" -> "JUN"
                    "07" -> "JUL"
                    "08" -> "AUG"
                    "09" -> "SEP"
                    "10" -> "OCT"
                    "11" -> "NOV"
                    "12" -> "DEC"
                    else -> "LIVE"
                }
                val dayLabel = parts.getOrNull(2) ?: "01"

                Surface(
                    color = StealieMidnight,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, bandAccent.copy(alpha = 0.5f)),
                    modifier = Modifier.size(width = 56.dp, height = 60.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = monthLabel,
                            color = bandAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = dayLabel,
                            color = TextPrimaryDark,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.displayDate.ifBlank { event.date },
                        color = StealieGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = event.venueName,
                        color = TextPrimaryDark,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = StealieCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${event.city}, ${event.stateCode}",
                            color = StealieCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• ${event.priceRange}",
                            color = TextSecondaryDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                DancingBear(color = bearColor, size = 34.dp)
            }

            if (event.tourNote.isNotBlank()) {
                Text(
                    text = event.tourNote,
                    color = TextSecondaryDark,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            // Visible Ticket URL Box (clickable)
            Surface(
                onClick = { onOpenTickets(event.ticketUrl) },
                color = StealieMidnight,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Link,
                            contentDescription = "Ticket URL",
                            tint = StealieGold,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = event.ticketUrl,
                            color = StealieCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open Ticket URL",
                        tint = StealieGold,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Action Buttons Row: Get Tickets (Primary) + Add to Calendar + Venue Map
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { onOpenTickets(event.ticketUrl) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = bandAccent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("ticket_url_button_${event.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ConfirmationNumber,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Get Tickets",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                OutlinedButton(
                    onClick = { onAddToCalendar(event) },
                    border = BorderStroke(1.dp, StealieGold.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("calendar_event_button_${event.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = "Add to Calendar",
                        tint = StealieGold,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Calendar",
                        color = StealieGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = { onOpenVenueMap(event) },
                    border = BorderStroke(1.dp, StealieCyan.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(0.9f)
                        .testTag("map_event_button_${event.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Map,
                        contentDescription = "Open Venue in Maps",
                        tint = StealieCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Map",
                        color = StealieCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun launchConcertCalendarIntent(context: android.content.Context, event: LiveConcertEvent) {
    runCatching {
        val parts = event.date.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val month = (parts.getOrNull(1)?.toIntOrNull() ?: 11) - 1
        val day = parts.getOrNull(2)?.toIntOrNull() ?: 14

        val startMillis = Calendar.getInstance().apply {
            set(year, month, day, 20, 0, 0)
        }.timeInMillis
        val endMillis = startMillis + 3 * 60 * 60 * 1000L

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            putExtra(CalendarContract.Events.TITLE, "${event.bandName} at ${event.venueName}")
            putExtra(
                CalendarContract.Events.DESCRIPTION,
                "${event.eventTitle}\nTickets: ${event.ticketUrl}\n${event.tourNote}"
            )
            putExtra(
                CalendarContract.Events.EVENT_LOCATION,
                event.venueAddress.ifBlank { "${event.venueName}, ${event.city}, ${event.stateCode}" }
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(
            context,
            "Calendar reminder: ${event.bandName} at ${event.venueName} (${event.date})",
            Toast.LENGTH_SHORT
        ).show()
    }
}

private fun launchConcertVenueMapIntent(context: android.content.Context, event: LiveConcertEvent) {
    runCatching {
        val query = Uri.encode("${event.venueName}, ${event.venueAddress.ifBlank { "${event.city}, ${event.stateCode}" }}")
        val geoUri = Uri.parse("geo:0,0?q=$query")
        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(mapIntent)
    }.onFailure {
        val webUri = Uri.parse(
            "https://www.google.com/maps/search/?api=1&query=${Uri.encode("${event.venueName} ${event.city} ${event.stateCode}")}"
        )
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, webUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }
}
