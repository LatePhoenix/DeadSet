package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ShowEntity
import com.example.data.model.DeadEra
import com.example.ui.AttendanceFilter
import com.example.ui.SortOrder
import com.example.ui.components.BerthaRose
import com.example.ui.components.DancingBearsMarchRow
import com.example.ui.components.ShowCard
import com.example.ui.components.StealieEmblem
import com.example.ui.components.TodayInDeadHistoryCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowsListScreen(
    shows: List<ShowEntity>,
    allCatalogShows: List<ShowEntity> = shows,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedEra: DeadEra,
    onEraSelected: (DeadEra) -> Unit,
    attendanceFilter: AttendanceFilter,
    onAttendanceFilterChanged: (AttendanceFilter) -> Unit,
    sortOrder: SortOrder,
    onSortOrderChanged: (SortOrder) -> Unit,
    selectedSegueFilter: String?,
    onClearSegueFilter: () -> Unit,
    onShowClick: (String) -> Unit,
    onAddShowClick: () -> Unit,
    onRestoreOfflineCatalog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showOfflineVaultDialog by remember { mutableStateOf(false) }

    val totalIndexedSongs = remember(allCatalogShows) {
        allCatalogShows.sumOf { it.getSongsPerformed().size }
    }
    val distinctCities = remember(allCatalogShows) {
        allCatalogShows.map { it.city }.distinct().sorted()
    }
    val distinctVenuesCount = remember(allCatalogShows) {
        allCatalogShows.map { it.venue }.distinct().size
    }

    if (showOfflineVaultDialog) {
        AlertDialog(
            onDismissRequest = { showOfflineVaultDialog = false },
            containerColor = StealieCardDark,
            titleContentColor = TextPrimaryDark,
            textContentColor = TextSecondaryDark,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Storage,
                        contentDescription = null,
                        tint = StealieGold,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Offline Room Concert Vault",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "All historical Grateful Dead & tribute concerts are stored locally in an indexed SQLite Room database for 100% offline browsing.",
                        color = TextSecondaryDark,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Surface(
                        color = StealieMidnight,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "ROOM SQLITE SCHEMA & INDEX STATS",
                                color = StealieGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "• Table `shows`: ${allCatalogShows.size} shows (indexed by date, venue, city, eraId)",
                                color = TextPrimaryDark,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "• Table `song_performances`: $totalIndexedSongs songs (FK CASCADE + ShowWithSongs @Relation)",
                                color = TextPrimaryDark,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "• Indexed Coverage: ${distinctCities.size} cities • $distinctVenuesCount iconic venues",
                                color = StealieCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (distinctCities.isNotEmpty()) {
                        Text(
                            text = "QUICK OFFLINE FILTER BY CITY",
                            color = TextSecondaryDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (city in distinctCities) {
                                Surface(
                                    color = StealieMidnight,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, StealieCyan.copy(alpha = 0.5f)),
                                    modifier = Modifier.clickable {
                                        onSearchChange(city)
                                        showOfflineVaultDialog = false
                                    }
                                ) {
                                    Text(
                                        text = "📍 $city",
                                        color = StealieCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRestoreOfflineCatalog()
                        showOfflineVaultDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StealieGold)
                ) {
                    Text(
                        text = "Verify & Sync Vault",
                        color = StealieMidnight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showOfflineVaultDialog = false }) {
                    Text("Close", color = TextSecondaryDark)
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
            ) {
                // Top App Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StealieEmblem(size = 38.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "DeadSet",
                                    color = TextPrimaryDark,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                BerthaRose(size = 18.dp)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { showOfflineVaultDialog = true }
                                    .testTag("offline_db_badge")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.OfflinePin,
                                    contentDescription = "Offline Room Database Ready",
                                    tint = StealieCyan,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Offline Room Vault • ${allCatalogShows.size} Shows • $totalIndexedSongs Songs",
                                    color = StealieCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StealieQuickThemeToggleButton()

                        FilledTonalButton(
                            onClick = onAddShowClick,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = StealieGold.copy(alpha = 0.15f),
                                contentColor = StealieGold
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("add_show_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add Show",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add Show",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = {
                        Text(
                            "Search venue, song, city, year...",
                            color = TextTertiaryDark,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = StealieGold,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Clear",
                                    tint = TextSecondaryDark
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = StealieCardDark,
                        unfocusedContainerColor = StealieCardDark,
                        focusedBorderColor = StealieGold,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("search_shows_input")
                )

                // Segue Filter Active Banner
                if (!selectedSegueFilter.isNullOrBlank()) {
                    Surface(
                        color = StealieCrimson.copy(alpha = 0.18f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StealieCrimsonLight.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = null,
                                    tint = StealieCrimsonLight,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Filtered by Segue: $selectedSegueFilter",
                                    color = StealieCrimsonLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            IconButton(
                                onClick = onClearSegueFilter,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear segue filter",
                                    tint = StealieCrimsonLight,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Era Filter Scroll Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (era in DeadEra.entries) {
                        val isSelected = selectedEra == era
                        val chipBg = if (isSelected) era.color.copy(alpha = 0.25f) else StealieCardDark
                        val border = if (isSelected) era.color else BorderSubtle
                        val textColor = if (isSelected) era.color else TextSecondaryDark

                        FilterChip(
                            selected = isSelected,
                            onClick = { onEraSelected(era) },
                            label = {
                                Text(
                                    text = era.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = chipBg,
                                labelColor = textColor,
                                selectedContainerColor = chipBg,
                                selectedLabelColor = textColor
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = border,
                                selectedBorderColor = border
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("era_chip_${era.id}")
                        )
                    }
                }

                // Attendance & Tribute Filter & Sort Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    // Filter Chips (Scrollable)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AttendanceTabChip(
                            label = "All Shows",
                            isSelected = attendanceFilter == AttendanceFilter.ALL,
                            onClick = { onAttendanceFilterChanged(AttendanceFilter.ALL) }
                        )
                        AttendanceTabChip(
                            label = "Attended",
                            isSelected = attendanceFilter == AttendanceFilter.ATTENDED_ONLY,
                            onClick = { onAttendanceFilterChanged(AttendanceFilter.ATTENDED_ONLY) }
                        )
                        AttendanceTabChip(
                            label = "🐢 Terrapin Flyer",
                            isSelected = attendanceFilter == AttendanceFilter.TERRAPIN_FLYER,
                            onClick = { onAttendanceFilterChanged(AttendanceFilter.TERRAPIN_FLYER) }
                        )
                        AttendanceTabChip(
                            label = "⚡ Upcoming",
                            isSelected = attendanceFilter == AttendanceFilter.UPCOMING_SHOWS,
                            onClick = { onAttendanceFilterChanged(AttendanceFilter.UPCOMING_SHOWS) }
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Sort Order selector
                    TextButton(
                        onClick = {
                            val nextSort = when (sortOrder) {
                                SortOrder.DATE_DESC -> SortOrder.DATE_ASC
                                SortOrder.DATE_ASC -> SortOrder.RATING_DESC
                                SortOrder.RATING_DESC -> SortOrder.DATE_DESC
                            }
                            onSortOrderChanged(nextSort)
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = StealieCyan),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Sort,
                            contentDescription = "Sort",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (sortOrder) {
                                SortOrder.DATE_DESC -> "Newest"
                                SortOrder.DATE_ASC -> "Oldest"
                                SortOrder.RATING_DESC -> "Top Rated"
                            },
                            fontSize = 11.sp
                        )
                    }
                }

                // Marching Dancing Bears Art Banner
                DancingBearsMarchRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )

                HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
            }
        },
        containerColor = StealieMidnight,
        modifier = modifier
    ) { innerPadding ->
        if (shows.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    StealieEmblem(size = 56.dp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No shows found",
                        color = TextPrimaryDark,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Try adjusting your search query, era filter, or add a custom show.",
                        color = TextSecondaryDark,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = onAddShowClick,
                        colors = ButtonDefaults.buttonColors(containerColor = StealieGold),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = StealieMidnight
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add Custom Show",
                            color = StealieMidnight,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 80.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (searchQuery.isBlank() && allCatalogShows.isNotEmpty()) {
                    item {
                        TodayInDeadHistoryCard(
                            shows = allCatalogShows,
                            onOpenShow = onShowClick,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                item {
                    Text(
                        text = "${shows.size} classic shows cataloged",
                        color = TextTertiaryDark,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                    )
                }

                items(shows, key = { it.id }) { show ->
                    ShowCard(
                        show = show,
                        onClick = { onShowClick(show.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AttendanceTabChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) StealieGold.copy(alpha = 0.2f) else Color.Transparent,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) StealieGold else BorderSubtle
        ),
        modifier = Modifier.clickable { onClick() }
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
