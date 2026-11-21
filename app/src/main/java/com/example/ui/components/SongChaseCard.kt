package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.setlist.ChaseStatus
import com.example.data.setlist.SetlistAnalytics
import com.example.ui.theme.*

/**
 * Song chase list (step 4.7): songs you want to hear live, with progress and where you first
 * heard each one (any band).
 */
@Composable
fun SongChaseCard(
    statuses: List<ChaseStatus>,
    allSongNames: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onOpenShow: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf("") }
    val chasedNames = statuses.map { it.songName.lowercase() }.toSet()
    val suggestions = remember(input, allSongNames, chasedNames) {
        val q = input.trim().lowercase()
        if (q.length < 2) emptyList()
        else allSongNames.filter { it.lowercase().contains(q) && it.lowercase() !in chasedNames }.take(6)
    }
    val heard = statuses.count { it.heard }
    // "+" only adds a typed title that is already a known song (aliases included); otherwise the
    // user picks a suggestion or explicitly adds a new song, so partial typing never creates junk.
    val knownKeys = remember(allSongNames) { allSongNames.map(SetlistAnalytics::canonicalKey).toSet() }
    val typedIsKnown = input.isNotBlank() && SetlistAnalytics.canonicalKey(input) in knownKeys

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StealieCardDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, StealieGold.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("song_chase_card")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Song Chase", color = StealieGold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
                text = if (statuses.isEmpty()) "Add songs you want to hear live. Any band counts."
                else "Heard $heard of ${statuses.size} songs you're chasing",
                color = TextSecondaryDark,
                fontSize = 12.sp
            )
            if (statuses.isNotEmpty()) {
                LinearProgressIndicator(
                    progress = { heard.toFloat() / statuses.size },
                    color = StealieGold,
                    trackColor = StealieMidnight,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            for (status in statuses) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        imageVector = if (status.heard) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = if (status.heard) "Heard" else "Not heard yet",
                        tint = if (status.heard) StealieGreen else TextTertiaryDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(status.songName, color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        val first = status.firstHeardAt
                        if (first != null) {
                            TextButton(onClick = { onOpenShow(first.id) }, contentPadding = PaddingValues(0.dp)) {
                                Text(
                                    "First heard ${first.date} · ${first.bandName} · ${first.venue}",
                                    color = StealieCyan,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                    IconButton(onClick = { onRemove(status.songId) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Stop chasing ${status.songName}", tint = TextTertiaryDark)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Add a song to chase", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StealieGold,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chase_input")
                )
                IconButton(
                    onClick = { onAdd(input); input = "" },
                    enabled = typedIsKnown,
                    modifier = Modifier.testTag("chase_add_button")
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Add song",
                        tint = if (typedIsKnown) StealieGold else TextTertiaryDark
                    )
                }
            }
            if (input.isNotBlank() && !typedIsKnown) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    for (s in suggestions) {
                        AssistChip(
                            onClick = { onAdd(s); input = "" },
                            label = { Text(s, fontSize = 11.sp) },
                            colors = AssistChipDefaults.assistChipColors(labelColor = StealieCyan)
                        )
                    }
                    if (input.trim().length >= 2) {
                        AssistChip(
                            onClick = { onAdd(input); input = "" },
                            label = { Text("Add \"${input.trim()}\" as a new song", fontSize = 11.sp) },
                            colors = AssistChipDefaults.assistChipColors(labelColor = TextSecondaryDark),
                            modifier = Modifier.testTag("chase_add_new_song")
                        )
                    }
                }
            }
        }
    }
}
