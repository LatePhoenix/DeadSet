package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SetType
import com.example.data.setlist.SetlistAnalytics
import com.example.ui.theme.*

/**
 * Live setlist tracker (step 4.9): log songs as they're played, with the catalog's best guesses
 * for what comes next. Tapping a guess adds it; [hits] counts how many logged songs were guessed.
 */
@Composable
fun LiveTrackerCard(
    showId: String,
    lastSong: String?,
    predictionsFor: (lastSong: String?, setType: SetType) -> List<String>,
    allSongNames: List<String>,
    onAddSong: (name: String, setType: SetType) -> Unit,
    modifier: Modifier = Modifier
) {
    var setType by rememberSaveable(showId) { mutableStateOf(SetType.SET_1) }
    var input by remember { mutableStateOf("") }
    var hits by rememberSaveable(showId) { mutableIntStateOf(0) }
    var logged by rememberSaveable(showId) { mutableIntStateOf(0) }
    val predictions = remember(lastSong, setType, predictionsFor) { predictionsFor(lastSong, setType) }
    val knownKeys = remember(allSongNames) { allSongNames.map(SetlistAnalytics::canonicalKey).toSet() }
    val suggestions = remember(input, allSongNames) {
        val q = input.trim().lowercase()
        if (q.length < 2) emptyList() else allSongNames.filter { it.lowercase().contains(q) }.take(5)
    }

    fun add(name: String) {
        if (name.isBlank()) return
        val guessed = predictions.any { SetlistAnalytics.canonicalKey(it) == SetlistAnalytics.canonicalKey(name) }
        if (guessed) hits++
        logged++
        onAddSong(name.trim(), setType)
        input = ""
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StealieCardDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, StealieGreen.copy(alpha = 0.6f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_tracker_card")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Track it live", color = StealieGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                if (logged > 0) {
                    Text("Predicted $hits of $logged", color = StealieGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (s in listOf(SetType.SET_1, SetType.SET_2, SetType.ENCORE)) {
                    FilterChip(
                        selected = setType == s,
                        onClick = { setType = s },
                        label = { Text(s.shortLabel, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StealieGreen.copy(alpha = 0.2f),
                            selectedLabelColor = StealieGreen,
                            labelColor = TextSecondaryDark
                        )
                    )
                }
            }
            Text(
                text = if (lastSong == null) "Likely openers:" else "After $lastSong, likely next:",
                color = TextSecondaryDark,
                fontSize = 12.sp
            )
            if (predictions.isEmpty()) {
                Text("No pattern in your catalog yet; add what they play.", color = TextTertiaryDark, fontSize = 12.sp)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                for (p in predictions) {
                    Button(
                        onClick = { add(p) },
                        colors = ButtonDefaults.buttonColors(containerColor = StealieGreen.copy(alpha = 0.25f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("prediction_$p")
                    ) { Text("+ $p", color = TextPrimaryDark, fontSize = 12.sp) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Something else? Type the song", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StealieGreen,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("live_song_input")
                )
                IconButton(
                    onClick = { add(input) },
                    enabled = input.isNotBlank() && SetlistAnalytics.canonicalKey(input) in knownKeys
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add played song", tint = StealieGreen)
                }
            }
            if (input.isNotBlank() && SetlistAnalytics.canonicalKey(input) !in knownKeys) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    for (s in suggestions) {
                        AssistChip(onClick = { add(s) }, label = { Text(s, fontSize = 11.sp) })
                    }
                    if (input.trim().length >= 2) {
                        AssistChip(
                            onClick = { add(input) },
                            label = { Text("Add \"${input.trim()}\" as a new song", fontSize = 11.sp) }
                        )
                    }
                }
            }
        }
    }
}
