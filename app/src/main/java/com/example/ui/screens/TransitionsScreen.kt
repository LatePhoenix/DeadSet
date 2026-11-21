package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ShowEntity
import com.example.data.model.SongEntry
import com.example.data.repository.OverallStats
import com.example.data.setlist.FamilyStat
import com.example.data.setlist.SetlistAnalytics
import com.example.data.setlist.SongFlow
import com.example.data.setlist.SongLink
import com.example.ui.components.BerthaRose
import com.example.ui.components.StealieEmblem
import com.example.ui.theme.*

@Composable
fun TransitionsScreen(
    stats: OverallStats?,
    onFilterBySegue: (String) -> Unit,
    modifier: Modifier = Modifier,
    families: List<FamilyStat> = emptyList(),
    shows: List<ShowEntity> = emptyList(),
    songsByShow: Map<String, List<SongEntry>> = emptyMap()
) {
    // Songs to explore: the most played first, so the chips start with useful ones.
    val explorableSongs = remember(stats) { stats?.topSongsOverall?.map { it.songName }.orEmpty() }
    var selectedSong by rememberSaveable { mutableStateOf<String?>(null) }
    val activeSong = selectedSong ?: explorableSongs.firstOrNull()
    val flow = remember(activeSong, shows, songsByShow) {
        activeSong?.let { name -> SetlistAnalytics.songFlow(name, shows) { songsByShow[it.id].orEmpty() } }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StealieMidnight)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StealieEmblem(size = 38.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Segue Hall of Fame",
                                color = TextPrimaryDark,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            BerthaRose(size = 18.dp)
                        }
                        Text(
                            text = "How songs flow into each other",
                            color = StealieCrimsonLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                HorizontalDivider(
                    color = BorderSubtle,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        },
        containerColor = StealieMidnight,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 80.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item { IntroCard() }

            if (stats?.topTransitions?.isNotEmpty() == true) {
                item { SectionLabel("MOST PLAYED TRANSITIONS IN CATALOG") }
                item {
                    TopTransitionsCard(
                        labels = stats.topTransitions.take(5).map { it.transitionLabel to it.occurrences },
                        onClick = onFilterBySegue
                    )
                }
            }

            if (flow != null) {
                item { SectionLabel("SONG FLOW EXPLORER") }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(explorableSongs) { song ->
                            FilterChip(
                                selected = song == activeSong,
                                onClick = { selectedSong = song },
                                label = { Text(song, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = StealieCardDark,
                                    labelColor = TextSecondaryDark,
                                    selectedContainerColor = StealieCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = StealieCyan
                                ),
                                modifier = Modifier.testTag("flow_chip_$song")
                            )
                        }
                    }
                }
                item { SongFlowCard(flow) }
            }

            item { SectionLabel("SEGUE FAMILIES") }
            items(families, key = { it.family.id }) { stat ->
                SegueFamilyCard(stat = stat, onFilterClick = { onFilterBySegue(stat.family.label) })
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = StealieGold,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 2.dp, top = 6.dp)
    )
}

@Composable
private fun IntroCard() {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StealieCardDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, StealieCrimsonLight.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StealieCrimson.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "➔", color = StealieCrimsonLight, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "The Art of the Dead Segue",
                    color = TextPrimaryDark,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "-> segue  ·  > transition  ·  ~> jam. Counts and dates below come from the shows " +
                        "in your catalog. Tap a family to see every show with it.",
                    color = TextSecondaryDark,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun TopTransitionsCard(labels: List<Pair<String, Int>>, onClick: (String) -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StealieCardDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            for ((label, count) in labels) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClick(label) }
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = StealieCrimsonLight,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = label, color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    CountBadge("${count}x")
                }
            }
        }
    }
}

@Composable
private fun CountBadge(text: String) {
    Surface(
        color = StealieMidnight,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle)
    ) {
        Text(
            text = text,
            color = StealieGold,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun SongFlowCard(flow: SongFlow) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StealieCardDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, StealieCyan.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("song_flow_card")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = flow.songName, color = TextPrimaryDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "Played ${flow.timesPlayed}x in your catalog",
                color = TextSecondaryDark,
                fontSize = 12.sp
            )
            FlowColumn(title = "COMES IN FROM", links = flow.into, emptyText = "Always started fresh (no segue in)")
            FlowColumn(title = "GOES INTO", links = flow.outOf, emptyText = "Always ended with a stop (no segue out)")
        }
    }
}

@Composable
private fun FlowColumn(title: String, links: List<SongLink>, emptyText: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = title, color = StealieCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
        if (links.isEmpty()) {
            Text(text = emptyText, color = TextTertiaryDark, fontSize = 12.sp)
        }
        for (link in links.take(5)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = link.songName,
                    color = TextPrimaryDark,
                    fontSize = 13.sp,
                    modifier = Modifier.width(150.dp)
                )
                LinearProgressIndicator(
                    progress = { link.share },
                    color = StealieCyan,
                    trackColor = StealieMidnight,
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${(link.share * 100).toInt()}%",
                    color = TextSecondaryDark,
                    fontSize = 11.sp,
                    modifier = Modifier.width(36.dp)
                )
            }
        }
    }
}

@Composable
private fun SegueFamilyCard(stat: FamilyStat, onFilterClick: () -> Unit) {
    val family = stat.family
    val inCatalog = stat.occurrences > 0
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StealieCardDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = inCatalog) { onFilterClick() }
            .testTag("segue_family_${family.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(text = family.emoji, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = family.nickname, color = TextPrimaryDark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                if (inCatalog) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Show matching shows",
                        tint = StealieCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = family.chain.joinToString(" ➔ "), color = StealieCrimsonLight, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = family.note, color = TextSecondaryDark, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = familyEvidence(stat),
                color = if (inCatalog) StealieCyan else TextTertiaryDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/** "3 shows · 1969–1977" from the catalog, or a plain note when it isn't there yet. */
internal fun familyEvidence(stat: FamilyStat): String = when {
    stat.occurrences == 0 -> "Not in your catalog yet"
    stat.occurrences == 1 -> "1 show in your catalog · ${stat.firstDate}"
    else -> {
        val first = stat.firstDate.orEmpty().take(4)
        val last = stat.lastDate.orEmpty().take(4)
        val span = if (first == last) first else "$first–$last"
        "${stat.occurrences} shows in your catalog · $span"
    }
}
