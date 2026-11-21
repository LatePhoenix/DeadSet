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
import com.example.data.Geo
import com.example.data.LatLon
import com.example.data.checkedAgo
import com.example.data.local.BandEntity
import com.example.data.repository.RefreshOutcome
import com.example.data.remote.LiveConcertEvent
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.roundToInt

/** UI state for the Tickets tab, supplied by the ViewModel (Phase 4). */
data class ConcertsUiState(
    val bands: List<BandEntity> = emptyList(),
    /** Cached upcoming events with the epoch millis they were last confirmed. */
    val events: List<Pair<LiveConcertEvent, Long>> = emptyList(),
    val isRefreshing: Boolean = false,
    val lastOutcome: RefreshOutcome? = null,
    /** Home location from the profile's home city, if it could be geocoded. */
    val home: LatLon? = null,
    val homeLabel: String = ""
)

/** Radius choices for "near me", in miles; null means any distance. */
private val RADIUS_CHOICES = listOf<Int?>(null, 100, 250, 500)

@Composable
fun LiveConcertsScreen(
    state: ConcertsUiState,
    onRefresh: () -> Unit,
    onAddBand: (String) -> Unit,
    onSetFollowed: (bandId: String, followed: Boolean) -> Unit,
    onRemoveBand: (bandId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedBand by remember { mutableStateOf<String?>(null) } // null = all followed
    var radiusMiles by remember { mutableStateOf<Int?>(null) }
    var showApiInfoDialog by remember { mutableStateOf(false) }
    var showBandsDialog by remember { mutableStateOf(false) }
    val now = remember(state.events) { System.currentTimeMillis() }

    LaunchedEffect(Unit) { onRefresh() }

    // Terrapin Flyer (the app's home band) first, then alphabetical.
    val followed = state.bands.filter { it.isFollowed }
        .sortedWith(compareBy<BandEntity>({ it.id != "terrapin flyer" }, { it.name }))
    val followedNames = followed.map { it.name.lowercase() }.toSet()
    val visible = remember(state.events, state.home, searchQuery, selectedBand, radiusMiles, followedNames) {
        val q = searchQuery.trim().lowercase()
        state.events.mapNotNull { (event, checkedAt) ->
            val distance = distanceMiles(state.home, event)
            val bandOk = event.bandName.lowercase() in followedNames &&
                (selectedBand == null || event.bandName.equals(selectedBand, ignoreCase = true))
            val radiusOk = radiusMiles == null || (distance != null && distance <= radiusMiles!!)
            val queryOk = q.isEmpty() || listOf(event.bandName, event.eventTitle, event.venueName, event.city, event.stateCode)
                .any { it.lowercase().contains(q) }
            if (bandOk && radiusOk && queryOk) Triple(event, checkedAt, distance) else null
        }.sortedBy { it.first.date }
    }

    if (showApiInfoDialog) {
        ConcertSourcesDialog(outcome = state.lastOutcome, onDismiss = { showApiInfoDialog = false })
    }
    if (showBandsDialog) {
        ManageBandsDialog(
            bands = state.bands,
            onAddBand = onAddBand,
            onSetFollowed = onSetFollowed,
            onRemoveBand = onRemoveBand,
            onDismiss = {
                showBandsDialog = false
                onRefresh()
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
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        StealieEmblem(size = 40.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Upcoming Shows",
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
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = "Where dates come from",
                                    tint = StealieGold,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Following ${followed.size} ${if (followed.size == 1) "band" else "bands"} • sources",
                                    color = StealieGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    IconButton(onClick = onRefresh, modifier = Modifier.testTag("refresh_concerts_button")) {
                        if (state.isRefreshing) {
                            CircularProgressIndicator(color = StealieGold, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        } else {
                            Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Refresh dates", tint = StealieCyan)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text("Filter by band, venue or city", color = TextTertiaryDark, fontSize = 13.sp, maxLines = 1)
                    },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = StealieGold) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear filter", tint = TextSecondaryDark)
                            }
                        }
                    },
                    singleLine = true,
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
                        .fillMaxWidth()
                        .testTag("concert_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    AssistChip(
                        onClick = { showBandsDialog = true },
                        label = { Text("Bands", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(15.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = StealieCardDark,
                            labelColor = StealieCyan,
                            leadingIconContentColor = StealieCyan
                        ),
                        modifier = Modifier.testTag("manage_bands_chip")
                    )
                    ConcertChip(label = "All followed", selected = selectedBand == null, tag = "band_chip_all") {
                        selectedBand = null
                    }
                    for (band in followed) {
                        ConcertChip(
                            label = band.name,
                            selected = selectedBand.equals(band.name, ignoreCase = true),
                            tag = "band_chip_${band.id}"
                        ) { selectedBand = band.name }
                    }
                }

                if (state.home != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Text("Near ${state.homeLabel}:", color = TextSecondaryDark, fontSize = 11.sp)
                        for (r in RADIUS_CHOICES) {
                            ConcertChip(
                                label = r?.let { "≤ $it mi" } ?: "Any distance",
                                selected = radiusMiles == r,
                                tag = "radius_chip_${r ?: "any"}"
                            ) { radiusMiles = r }
                        }
                    }
                }

                HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp, modifier = Modifier.padding(top = 8.dp))
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
            if (visible.isEmpty()) {
                item {
                    EmptyConcertsCard(
                        state = state,
                        hasFilters = searchQuery.isNotBlank() || selectedBand != null || radiusMiles != null,
                        onClearFilters = {
                            searchQuery = ""
                            selectedBand = null
                            radiusMiles = null
                        },
                        onShowSources = { showApiInfoDialog = true }
                    )
                }
            } else {
                items(visible, key = { it.first.id }) { (event, checkedAt, distance) ->
                    val provenance = buildString {
                        append(event.apiSource)
                        append(" · ")
                        append(checkedAgo(checkedAt, now))
                        if (distance != null) append(" · ~${distance.roundToInt()} mi")
                    }
                    LiveConcertEventCard(
                        event = event,
                        provenance = provenance,
                        onOpenTickets = { url ->
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }.onFailure {
                                Toast.makeText(context, "No app can open $url", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onAddToCalendar = { ev -> launchConcertCalendarIntent(context, ev) },
                        onOpenVenueMap = { ev -> launchConcertVenueMapIntent(context, ev) }
                    )
                }
            }
        }
    }
}

/** Distance from [home] to the event's venue, if both are known. */
internal fun distanceMiles(home: LatLon?, event: LiveConcertEvent): Double? {
    val lat = event.latitude ?: return null
    val lon = event.longitude ?: return null
    return home?.let { Geo.milesBetween(it, LatLon(lat, lon)) }
}

@Composable
private fun ConcertChip(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
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
            selected = selected,
            borderColor = BorderSubtle,
            selectedBorderColor = StealieGold
        ),
        modifier = Modifier.testTag(tag)
    )
}

@Composable
private fun EmptyConcertsCard(
    state: ConcertsUiState,
    hasFilters: Boolean,
    onClearFilters: () -> Unit,
    onShowSources: () -> Unit
) {
    val outcome = state.lastOutcome
    val (title, body) = when {
        state.isRefreshing && state.events.isEmpty() -> "Checking for dates…" to "Asking Bandsintown and Ticketmaster."
        hasFilters && state.events.isNotEmpty() -> "No dates match these filters" to
            "Try another band, a wider distance, or clear the filter."
        state.bands.none { it.isFollowed } -> "You're not following any bands" to
            "Tap Bands to follow Terrapin Flyer, DSO, or any band you like."
        outcome != null && !outcome.anySourceReached -> "Couldn't reach any date source" to
            "Bandsintown needs a registered app ID and Ticketmaster needs an API key. Without them, " +
            "or without a connection, no dates can be shown. You can still add a show yourself from the Setlists tab."
        else -> "No upcoming dates found" to
            "None of the bands you follow have dates listed right now. Dates come only from real sources; " +
            "nothing here is made up. You can add a show yourself from the Setlists tab."
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = StealieCardDark),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("concerts_empty_card")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            StealieEmblem(size = 56.dp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = title, color = TextPrimaryDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = body, color = TextSecondaryDark, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hasFilters) {
                    OutlinedButton(onClick = onClearFilters, border = BorderStroke(1.dp, StealieGold)) {
                        Text("Clear filters", color = StealieGold, fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedButton(onClick = onShowSources, border = BorderStroke(1.dp, StealieCyan)) {
                    Text("Sources", color = StealieCyan, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ConcertSourcesDialog(outcome: RefreshOutcome?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StealieCardDark,
        titleContentColor = TextPrimaryDark,
        textContentColor = TextSecondaryDark,
        title = { Text("Where dates come from", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Upcoming dates come only from Bandsintown and, if an API key is set up, Ticketmaster. " +
                        "Nothing is filled in by hand. Each card shows its source and when it was last checked. " +
                        "Dates are kept offline and refreshed when you open this tab.",
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
                val reached = outcome?.sourcesReached.orEmpty()
                Text(
                    text = "Last check: " + when {
                        outcome == null -> "not yet"
                        reached.isEmpty() -> "no source answered"
                        else -> "${reached.sorted().joinToString(" + ")} answered"
                    },
                    color = StealieCyan,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Ticketmaster key: " + if (outcome?.ticketmasterConfigured == true) "set" else "not set",
                    color = TextPrimaryDark,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = StealieGold)) {
                Text("Got it", color = StealieMidnight, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun ManageBandsDialog(
    bands: List<BandEntity>,
    onAddBand: (String) -> Unit,
    onSetFollowed: (String, Boolean) -> Unit,
    onRemoveBand: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newBand by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StealieCardDark,
        titleContentColor = TextPrimaryDark,
        title = { Text("Bands you follow", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (band in bands) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(
                            checked = band.isFollowed,
                            onCheckedChange = { onSetFollowed(band.id, it) },
                            modifier = Modifier.testTag("follow_${band.id}")
                        )
                        Text(band.name, color = TextPrimaryDark, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        if (!band.isBuiltIn) {
                            IconButton(onClick = { onRemoveBand(band.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove ${band.name}", tint = TextTertiaryDark)
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newBand,
                        onValueChange = { newBand = it },
                        placeholder = { Text("Add a band", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_band_input")
                    )
                    IconButton(
                        onClick = {
                            onAddBand(newBand)
                            newBand = ""
                        },
                        enabled = newBand.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add band", tint = StealieGold)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = StealieGold)) {
                Text("Done", color = StealieMidnight, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun LiveConcertEventCard(
    event: LiveConcertEvent,
    provenance: String,
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
                }

                Text(
                    text = provenance,
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
