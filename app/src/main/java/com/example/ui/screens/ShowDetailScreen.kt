package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ShowEntity
import com.example.data.model.DeadEra
import com.example.data.model.SetType
import com.example.data.model.SongEntry
import com.example.data.remote.SpotifyApiClient
import com.example.ui.util.ConcertIntentHelper
import com.example.ui.components.BerthaRose
import com.example.ui.components.CassetteTapeJCard
import com.example.ui.components.EraBadge
import com.example.ui.components.RatingStars
import com.example.ui.components.SegueIndicatorPill
import com.example.ui.components.SetlistSpotifyPlayButton
import com.example.ui.components.SpotifyBrandGreen
import com.example.ui.components.SpotifySetlistPlayerCard
import com.example.ui.components.StealieEmblem
import com.example.ui.components.TerrapinTurtleBadge
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowDetailScreen(
    show: ShowEntity,
    songs: List<SongEntry>,
    onBack: () -> Unit,
    onToggleAttendance: (ShowEntity) -> Unit,
    onToggleSongTransition: (ShowEntity, Int) -> Unit,
    onSaveReviewAndNotes: (showId: String, review: String, rating: Float, attendedWith: String, favoriteSong: String) -> Unit,
    onEditShow: ((String) -> Unit)? = null,
    onDeleteShow: ((ShowEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val era = DeadEra.fromId(show.eraId)

    // Local state for user review and notes section
    var reviewText by remember(show.id) { mutableStateOf(show.userReview) }
    var ratingValue by remember(show.id) { mutableStateOf(show.userRating) }
    var attendedWithText by remember(show.id) { mutableStateOf(show.attendedWith) }
    var favoriteSongText by remember(show.id) { mutableStateOf(show.favoriteSong) }
    var reviewSavedConfirmation by remember { mutableStateOf(false) }

    // Spotify Setlist Playback State
    val spotifyClient = remember { SpotifyApiClient() }
    var activeSpotifySongIndex by remember(show.id) { mutableStateOf<Int?>(null) }
    var isSpotifyPlaying by remember(show.id) { mutableStateOf(false) }

    val quickTags = listOf(
        "🔥 Scorcher",
        "🎸 Epic Jerry Solo",
        "⚡ Seamless Segues",
        "🥁 Deep Space",
        "🌹 Emotional Dew",
        "🎧 Betty Board SBD",
        "🌤️ Sunshine Daydream"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = show.date,
                        color = StealieGold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimaryDark
                        )
                    }
                },
                actions = {
                    // "I Was There" Attendance Toggle Button
                    FilterChip(
                        selected = show.isAttended,
                        onClick = { onToggleAttendance(show) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = if (show.isAttended) StealieGold else TextSecondaryDark,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                text = if (show.isAttended) "Attended" else "I Was There",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = StealieCardDark,
                            labelColor = TextSecondaryDark,
                            selectedContainerColor = StealieGold.copy(alpha = 0.2f),
                            selectedLabelColor = StealieGold
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = show.isAttended,
                            borderColor = BorderSubtle,
                            selectedBorderColor = StealieGold
                        ),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("toggle_attendance_button")
                    )

                    if (show.isCustomShow && onDeleteShow != null) {
                        IconButton(onClick = { onDeleteShow(show) }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete Show",
                                tint = StealieCrimsonLight
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StealieMidnight)
            )
        },
        containerColor = StealieMidnight,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 40.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Show Header Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = StealieCardDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            EraBadge(era = era)
                            if (show.isAttended) {
                                Surface(
                                    color = StealieGold.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, StealieGold.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Bolt,
                                            contentDescription = null,
                                            tint = StealieGold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "IN YOUR JOURNAL",
                                            color = StealieGold,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = show.venue,
                            color = TextPrimaryDark,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${show.city}, ${show.stateOrCountry} • ${show.tourName}",
                            color = TextSecondaryDark,
                            fontSize = 13.sp
                        )

                        if (show.bandName != "Grateful Dead") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = StealiePurple.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, StealiePurple.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Bolt,
                                        contentDescription = null,
                                        tint = StealiePurple,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${show.bandName.uppercase()} • TRIBUTE BAND CONCERT",
                                        color = StealiePurple,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (show.venueAddress.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Place,
                                    contentDescription = null,
                                    tint = TextSecondaryDark,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = show.venueAddress,
                                    color = TextSecondaryDark,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (show.tapeQuality.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = StealieMidnight.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Headphones,
                                        contentDescription = null,
                                        tint = StealieCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = show.tapeQuality,
                                        color = StealieCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // CALENDAR & MAPS ACTIONS
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Open in Maps
                            Button(
                                onClick = { ConcertIntentHelper.openVenueInMaps(context, show) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StealieMidnight,
                                    contentColor = StealieCyan
                                ),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, StealieCyan.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("detail_maps_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Map,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Directions in Maps",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Add to Device Calendar
                            Button(
                                onClick = { ConcertIntentHelper.addConcertToCalendar(context, show) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StealieMidnight,
                                    contentColor = StealieGold
                                ),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, StealieGold.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("detail_calendar_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Add to Calendar",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Historic Trivia Card
            if (show.historicTrivia.isNotBlank()) {
                item {
                    Surface(
                        color = StealieCardDark,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StealiePurple.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(StealiePurple.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = StealieGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "CONCERT LORE & HISTORIC TRIVIA",
                                    color = StealieGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = show.historicTrivia,
                                    color = TextPrimaryDark,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            // Cassette Tape J-Card & Tribute Badge Art
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (show.bandName.contains("Terrapin Flyer", ignoreCase = true)) {
                        TerrapinTurtleBadge(modifier = Modifier.fillMaxWidth())
                    }
                    CassetteTapeJCard(
                        date = show.date,
                        venue = show.venue,
                        tapeQuality = show.tapeQuality
                    )
                }
            }

            // Setlist Header, Spotify Setlist Launcher & Transition Tip
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "Setlist & Segues",
                                color = TextPrimaryDark,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap Spotify to play tracks or ➔ to tag transitions",
                                color = StealieCrimsonLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (songs.isNotEmpty()) {
                            Surface(
                                color = SpotifyBrandGreen.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SpotifyBrandGreen),
                                modifier = Modifier
                                    .clickable {
                                        if (activeSpotifySongIndex == null) {
                                            activeSpotifySongIndex = 0
                                            isSpotifyPlaying = true
                                        } else {
                                            isSpotifyPlaying = !isSpotifyPlaying
                                        }
                                    }
                                    .testTag("play_full_setlist_spotify_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (activeSpotifySongIndex != null && isSpotifyPlaying)
                                            Icons.Filled.Pause
                                        else
                                            Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        tint = SpotifyBrandGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (activeSpotifySongIndex != null && isSpotifyPlaying)
                                            "Pause Setlist"
                                        else
                                            "Play Setlist",
                                        color = SpotifyBrandGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }

                    val currentPlayIdx = activeSpotifySongIndex
                    if (currentPlayIdx != null && currentPlayIdx in songs.indices) {
                        SpotifySetlistPlayerCard(
                            showDate = show.date,
                            tourName = show.tourName,
                            bandName = show.bandName,
                            songs = songs,
                            currentSongIndex = currentPlayIdx,
                            isPlaying = isSpotifyPlaying,
                            spotifyClient = spotifyClient,
                            onTogglePlayPause = { isSpotifyPlaying = !isSpotifyPlaying },
                            onSelectSongIndex = { newIdx ->
                                activeSpotifySongIndex = newIdx
                                isSpotifyPlaying = true
                            },
                            onClosePlayer = {
                                isSpotifyPlaying = false
                                activeSpotifySongIndex = null
                            }
                        )
                    }
                }
            }

            // Group songs by Set
            val setGroups = songs.groupBy { it.setType }
            for ((setType, setSongs) in setGroups) {
                item {
                    Surface(
                        color = StealieMidnight.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = setType.label.uppercase(),
                            color = StealieGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                itemsIndexed(setSongs) { indexInSet, song ->
                    val globalIndex = songs.indexOf(song)
                    val isLastInSet = indexInSet == setSongs.lastIndex

                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Song Row Card
                        Surface(
                            color = StealieCardDark,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (song.isJamHighlight) StealieGold.copy(alpha = 0.5f) else BorderSubtle
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Song number pill
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(StealieMidnight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${indexInSet + 1}",
                                            color = TextSecondaryDark,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = song.name,
                                                color = TextPrimaryDark,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (song.isJamHighlight) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Filled.Star,
                                                    contentDescription = "Jam Highlight",
                                                    tint = StealieGold,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }

                                        if (!song.notes.isNullOrBlank()) {
                                            Text(
                                                text = song.notes,
                                                color = TextSecondaryDark,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (song.durationMinutes != null) {
                                        Text(
                                            text = "${song.durationMinutes}m",
                                            color = TextTertiaryDark,
                                            fontSize = 11.sp
                                        )
                                    }

                                    // Direct Spotify Track Play Button
                                    SetlistSpotifyPlayButton(
                                        isCurrentSong = activeSpotifySongIndex == globalIndex,
                                        isPlaying = isSpotifyPlaying,
                                        onClick = {
                                            if (activeSpotifySongIndex == globalIndex) {
                                                isSpotifyPlaying = !isSpotifyPlaying
                                            } else {
                                                activeSpotifySongIndex = globalIndex
                                                isSpotifyPlaying = true
                                            }
                                        },
                                        modifier = Modifier.testTag("spotify_play_button_$globalIndex")
                                    )

                                    // Toggleable Segue Pill
                                    SegueIndicatorPill(
                                        isSegue = song.isSegue,
                                        onToggle = {
                                            onToggleSongTransition(show, globalIndex)
                                        },
                                        modifier = Modifier.testTag("segue_pill_$globalIndex")
                                    )
                                }
                            }
                        }

                        // Connecting visual transition line/ribbon if segue is active
                        if (song.isSegue && !isLastInSet) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(start = 26.dp)
                                    .height(22.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .fillMaxHeight()
                                        .background(StealieCrimsonLight)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "➔ seamless transition",
                                    color = StealieCrimsonLight,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (!isLastInSet) {
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }

            // USER REVIEW & NOTES SECTION (Requested feature)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = StealieCardDark),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, StealiePurple.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("user_review_section")
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        // Section Title & Icon
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(StealiePurple.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.EditNote,
                                        contentDescription = null,
                                        tint = StealiePurple,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Performance Review & Notes",
                                            color = TextPrimaryDark,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        BerthaRose(size = 18.dp)
                                    }
                                    Text(
                                        text = "Record your thoughts, memories & tape review",
                                        color = TextSecondaryDark,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Star Rating Selector
                        Text(
                            text = "YOUR SHOW RATING",
                            color = StealieGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        RatingStars(
                            rating = ratingValue,
                            onRatingChanged = {
                                ratingValue = it
                                reviewSavedConfirmation = false
                            },
                            starSize = 26.dp,
                            modifier = Modifier.testTag("review_rating_stars")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Multiline Review & Notes Input
                        Text(
                            text = "PERSONAL THOUGHTS & SHOW MEMORIES",
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = reviewText,
                            onValueChange = {
                                reviewText = it
                                reviewSavedConfirmation = false
                            },
                            placeholder = {
                                Text(
                                    "How was Jerry playing? What was the lot vibe? Notable jams, acoustics, or personal stories...",
                                    color = TextTertiaryDark,
                                    fontSize = 13.sp
                                )
                            },
                            minLines = 4,
                            maxLines = 8,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = StealieMidnight,
                                unfocusedContainerColor = StealieMidnight,
                                focusedBorderColor = StealiePurple,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("user_review_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick deadhead tag suggestions
                        Text(
                            text = "QUICK HIGHLIGHT TAGS",
                            color = TextSecondaryDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val chunked = quickTags.take(3)
                            for (tag in chunked) {
                                Surface(
                                    color = StealieMidnight,
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, StealieCyan.copy(alpha = 0.4f)),
                                    modifier = Modifier.clickable {
                                        reviewText = if (reviewText.isBlank()) tag else "$reviewText\n$tag"
                                    }
                                ) {
                                    Text(
                                        text = tag,
                                        color = StealieCyan,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Optional Details: Favorite Song and Companions
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = favoriteSongText,
                                onValueChange = {
                                    favoriteSongText = it
                                    reviewSavedConfirmation = false
                                },
                                label = { Text("Favorite Song", fontSize = 11.sp) },
                                placeholder = { Text("e.g. Morning Dew", fontSize = 12.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = StealieMidnight,
                                    unfocusedContainerColor = StealieMidnight,
                                    focusedBorderColor = StealieGold,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedTextColor = TextPrimaryDark,
                                    unfocusedTextColor = TextPrimaryDark
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("favorite_song_input")
                            )

                            OutlinedTextField(
                                value = attendedWithText,
                                onValueChange = {
                                    attendedWithText = it
                                    reviewSavedConfirmation = false
                                },
                                label = { Text("Attended With", fontSize = 11.sp) },
                                placeholder = { Text("e.g. Bobby & friends", fontSize = 12.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = StealieMidnight,
                                    unfocusedContainerColor = StealieMidnight,
                                    focusedBorderColor = StealieGold,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedTextColor = TextPrimaryDark,
                                    unfocusedTextColor = TextPrimaryDark
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("attended_with_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Save Review Button
                        Button(
                            onClick = {
                                onSaveReviewAndNotes(
                                    show.id,
                                    reviewText,
                                    ratingValue,
                                    attendedWithText,
                                    favoriteSongText
                                )
                                reviewSavedConfirmation = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StealiePurple
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_review_button")
                        ) {
                            Icon(
                                imageVector = if (reviewSavedConfirmation) Icons.Outlined.CheckCircle else Icons.Filled.Save,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (reviewSavedConfirmation) "Review Saved! 🌹" else "Save Review & Notes",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
