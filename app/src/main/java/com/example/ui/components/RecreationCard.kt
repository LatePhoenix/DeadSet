package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ShowEntity
import com.example.data.model.SongEntry
import com.example.data.setlist.RecreationMatcher
import com.example.ui.theme.*

/**
 * Tribute ↔ original comparison (step 4.8). When linked, shows the two setlists side by side with
 * a match score; when not linked but the setlist closely follows a catalog show, offers the link.
 */
@Composable
fun RecreationCard(
    tributeSongs: List<SongEntry>,
    original: ShowEntity?,
    originalSongs: List<SongEntry>,
    suggestion: Pair<ShowEntity, Float>?,
    onLink: (String) -> Unit,
    onUnlink: () -> Unit,
    onOpenShow: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (original == null && suggestion == null) return
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StealieCardDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, StealiePurple.copy(alpha = 0.6f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("recreation_card")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (original == null && suggestion != null) {
                val (show, score) = suggestion
                Text("Looks like a recreation", color = StealiePurple, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(
                    "This setlist follows ${(score * 100).toInt()}% of ${show.date} at ${show.venue} in order.",
                    color = TextSecondaryDark,
                    fontSize = 12.sp
                )
                Button(
                    onClick = { onLink(show.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = StealiePurple),
                    modifier = Modifier.testTag("link_recreation_button")
                ) { Text("Link as a recreation of ${show.date}", color = TextPrimaryDark, fontSize = 12.sp) }
                return@Column
            }
            val show = original ?: return@Column
            val comparison = remember(tributeSongs, originalSongs) { RecreationMatcher.compare(tributeSongs, originalSongs) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Recreates ${show.date}",
                    color = StealiePurple,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text("${(comparison.score * 100).toInt()}% match", color = StealieGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = { onOpenShow(show.id) }, contentPadding = PaddingValues(0.dp)) {
                Text("${show.venue}, ${show.city} · open original", color = StealieCyan, fontSize = 12.sp)
            }
            Row {
                Text("Tonight", color = TextTertiaryDark, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Original", color = TextTertiaryDark, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
            for (row in comparison.rows) {
                val color = if (row.matches) TextPrimaryDark else TextTertiaryDark
                Row {
                    Text(row.tribute ?: "—", color = color, fontSize = 12.sp, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(row.original ?: "—", color = color, fontSize = 12.sp, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
            }
            TextButton(onClick = onUnlink, contentPadding = PaddingValues(0.dp)) {
                Text("Unlink", color = TextTertiaryDark, fontSize = 11.sp)
            }
        }
    }
}
