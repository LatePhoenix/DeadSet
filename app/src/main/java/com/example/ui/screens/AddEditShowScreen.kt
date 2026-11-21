package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ShowEntity
import com.example.data.model.DeadEra
import com.example.data.model.SetType
import com.example.data.model.SongEntry
import com.example.data.model.TransitionType
import com.example.ui.components.SegueIndicatorPill
import com.example.ui.theme.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditShowScreen(
    existingShow: ShowEntity?,
    existingSongs: List<SongEntry> = emptyList(),
    /** Followed tribute bands offered as one-tap choices for the Band field. */
    bandSuggestions: List<String> = emptyList(),
    onBack: () -> Unit,
    onSave: (ShowEntity, List<SongEntry>) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var dateText by remember { mutableStateOf(existingShow?.date ?: "1977-05-08") }
    var venueText by remember { mutableStateOf(existingShow?.venue ?: "") }
    var cityText by remember { mutableStateOf(existingShow?.city ?: "") }
    var stateText by remember { mutableStateOf(existingShow?.stateOrCountry ?: "CA") }
    var tourText by remember { mutableStateOf(existingShow?.tourName ?: "Tour Run") }
    var bandText by remember { mutableStateOf(existingShow?.bandName ?: "Grateful Dead") }
    var selectedEra by remember {
        mutableStateOf(
            if (existingShow != null) DeadEra.fromId(existingShow.eraId) else DeadEra.SPRING_77
        )
    }
    var tapeQualityText by remember { mutableStateOf(existingShow?.tapeQuality ?: "Soundboard SBD") }

    // Dynamic songs in setlist
    val songList = remember {
        mutableStateListOf<SongEntry>().apply {
            if (existingShow != null) {
                addAll(existingSongs)
            }
        }
    }

    var newSongName by remember { mutableStateOf("") }
    var currentSetType by remember { mutableStateOf(SetType.SET_1) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (existingShow != null) "Edit Show" else "Add New Show",
                        color = StealieGold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("add_show_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimaryDark
                        )
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // General Details Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = StealieCardDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "SHOW DETAILS",
                            color = StealieGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Date & Venue
                        OutlinedTextField(
                            value = dateText,
                            onValueChange = { dateText = it },
                            label = { Text("Show Date (YYYY-MM-DD)") },
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
                                .fillMaxWidth()
                                .testTag("input_show_date")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = venueText,
                            onValueChange = { venueText = it },
                            label = { Text("Venue Name") },
                            placeholder = { Text("e.g. Barton Hall, Cornell University") },
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
                                .fillMaxWidth()
                                .testTag("input_show_venue")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = cityText,
                                onValueChange = { cityText = it },
                                label = { Text("City") },
                                placeholder = { Text("e.g. Ithaca") },
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
                                    .weight(1.5f)
                                    .testTag("input_show_city")
                            )

                            OutlinedTextField(
                                value = stateText,
                                onValueChange = { stateText = it },
                                label = { Text("State") },
                                placeholder = { Text("NY") },
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
                                    .testTag("input_show_state")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = tourText,
                            onValueChange = { tourText = it },
                            label = { Text("Tour / Run Name") },
                            placeholder = { Text("e.g. Spring Tour 1977") },
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
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Band (step 4.4): log tribute shows yourself, past or upcoming.
                        OutlinedTextField(
                            value = bandText,
                            onValueChange = { bandText = it },
                            label = { Text("Band") },
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
                                .fillMaxWidth()
                                .testTag("band_input")
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            for (name in listOf("Grateful Dead") + bandSuggestions) {
                                FilterChip(
                                    selected = bandText.equals(name, ignoreCase = true),
                                    onClick = { bandText = name },
                                    label = { Text(name, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = StealieMidnight,
                                        labelColor = TextSecondaryDark,
                                        selectedContainerColor = StealieGold.copy(alpha = 0.2f),
                                        selectedLabelColor = StealieGold
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Era Selector Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = StealieCardDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "TOUR ERA",
                            color = StealieGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val availableEras = DeadEra.entries.filter { it != DeadEra.ALL }
                            for (era in availableEras.take(3)) {
                                FilterChip(
                                    selected = selectedEra == era,
                                    onClick = { selectedEra = era },
                                    label = { Text(era.title, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = StealieMidnight,
                                        selectedContainerColor = era.color.copy(alpha = 0.25f),
                                        labelColor = TextSecondaryDark,
                                        selectedLabelColor = era.color
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val availableEras = DeadEra.entries.filter { it != DeadEra.ALL }
                            for (era in availableEras.drop(3)) {
                                FilterChip(
                                    selected = selectedEra == era,
                                    onClick = { selectedEra = era },
                                    label = { Text(era.title, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = StealieMidnight,
                                        selectedContainerColor = era.color.copy(alpha = 0.25f),
                                        labelColor = TextSecondaryDark,
                                        selectedLabelColor = era.color
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Setlist Builder Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = StealieCardDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "BUILD SETLIST & TAG TRANSITIONS",
                            color = StealieGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Add songs to each set and toggle ➔ for seamless transitions",
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Set Type Selector (Set 1, Set 2, Set 3, Encore)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (st in SetType.entries) {
                                val isSelected = currentSetType == st
                                Surface(
                                    color = if (isSelected) StealieCyan.copy(alpha = 0.2f) else StealieMidnight,
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) StealieCyan else BorderSubtle
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { currentSetType = st }
                                ) {
                                    Text(
                                        text = st.shortLabel,
                                        color = if (isSelected) StealieCyan else TextSecondaryDark,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Add Song Input Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = newSongName,
                                onValueChange = { newSongName = it },
                                placeholder = { Text("e.g. Scarlet Begonias", fontSize = 12.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = StealieMidnight,
                                    unfocusedContainerColor = StealieMidnight,
                                    focusedBorderColor = StealieCyan,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedTextColor = TextPrimaryDark,
                                    unfocusedTextColor = TextPrimaryDark
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("new_song_input")
                            )

                            Button(
                                onClick = {
                                    if (newSongName.isNotBlank()) {
                                        val pos = songList.count { it.setType == currentSetType } + 1
                                        songList.add(
                                            SongEntry(
                                                name = newSongName.trim(),
                                                setType = currentSetType,
                                                position = pos,
                                                transitionOut = TransitionType.NONE
                                            )
                                        )
                                        newSongName = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = StealieCyan),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("add_song_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add",
                                    tint = StealieMidnight
                                )
                            }
                        }
                    }
                }
            }

            // Current Songs List
            if (songList.isNotEmpty()) {
                item {
                    Text(
                        text = "ADDED SONGS (${songList.size})",
                        color = StealieGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }

                itemsIndexed(songList) { index, song ->
                    Surface(
                        color = StealieCardDark,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = StealieMidnight,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = song.setType.shortLabel,
                                            color = StealieGold,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = song.name,
                                        color = TextPrimaryDark,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Toggle Segue
                                SegueIndicatorPill(
                                    transition = song.transitionOut,
                                    onToggle = {
                                        songList[index] = song.copy(transitionOut = song.transitionOut.next())
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { songList.removeAt(index) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Remove",
                                        tint = TextTertiaryDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Error display
            if (errorMessage != null) {
                item {
                    Text(
                        text = errorMessage ?: "",
                        color = StealieCrimsonLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Save Show Button
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (venueText.isBlank()) {
                            errorMessage = "Please enter a venue name."
                            return@Button
                        }
                        if (songList.isEmpty()) {
                            errorMessage = "Please add at least one song to the setlist."
                            return@Button
                        }

                        val show = buildEditedShow(
                            existing = existingShow,
                            newId = { "custom-${UUID.randomUUID()}" },
                            date = dateText,
                            venue = venueText,
                            city = cityText,
                            state = stateText,
                            eraId = selectedEra.id,
                            tour = tourText,
                            tapeQuality = tapeQualityText,
                            band = bandText
                        )
                        onSave(show, songList.toList())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StealieGold),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_show_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Save,
                        contentDescription = null,
                        tint = StealieMidnight
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (existingShow != null) "Update Show Setlist" else "Save Show to Catalog",
                        color = StealieMidnight,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * The show to save from the editor's fields. Editing copies [existing] so everything the form
 * doesn't show (historic trivia, archive id, tags, attendance, review, companions, custom flag)
 * survives; previously an edit rebuilt the show and silently wiped those. A brand-new show is
 * custom, and counts as attended only if its date has already passed.
 */
internal fun buildEditedShow(
    existing: ShowEntity?,
    newId: () -> String,
    date: String,
    venue: String,
    city: String,
    state: String,
    eraId: String,
    tour: String,
    tapeQuality: String,
    band: String,
    today: String = com.example.data.todayIsoDate()
): ShowEntity {
    val base = existing ?: ShowEntity(
        id = newId(),
        date = "",
        venue = "",
        city = "",
        stateOrCountry = "",
        eraId = eraId,
        tourName = "",
        isAttended = date.trim() < today,
        isCustomShow = true,
        tags = "Custom Show"
    )
    return base.copy(
        date = date.trim(),
        venue = venue.trim(),
        city = city.trim().ifEmpty { "San Francisco" },
        stateOrCountry = state.trim().ifEmpty { "CA" },
        eraId = eraId,
        tourName = tour.trim().ifEmpty { "Tour Run" },
        tapeQuality = tapeQuality.trim(),
        bandName = band.trim().ifEmpty { "Grateful Dead" },
        lastUpdatedTimestamp = System.currentTimeMillis()
    )
}
