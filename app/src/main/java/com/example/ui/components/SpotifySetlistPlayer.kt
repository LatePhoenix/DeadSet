package com.example.ui.components

import android.media.MediaPlayer
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SongEntry
import com.example.data.remote.SpotifyApiClient
import com.example.data.remote.SpotifyTrackInfo
import com.example.ui.theme.*
import kotlinx.coroutines.delay

val SpotifyBrandGreen = Color(0xFF1DB954)

/**
 * Compact Spotify Play / Pause button for each song row in the Setlist view.
 */
@Composable
fun SetlistSpotifyPlayButton(
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isCurrentSong) SpotifyBrandGreen.copy(alpha = 0.22f) else StealieMidnight,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isCurrentSong) SpotifyBrandGreen else SpotifyBrandGreen.copy(alpha = 0.45f)
        ),
        modifier = modifier
            .height(32.dp)
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = if (isCurrentSong && isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isCurrentSong && isPlaying) "Pause Spotify track" else "Play on Spotify",
                tint = SpotifyBrandGreen,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = if (isCurrentSong && isPlaying) "Playing" else "Spotify",
                color = SpotifyBrandGreen,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

/**
 * Interactive Spotify Setlist Player Deck displayed in the Setlist view when a song is selected.
 * Resolves track metadata via [SpotifyApiClient], streams 30s preview audio via [MediaPlayer] when
 * available from the Spotify Web API, supports seamless `➔` segue auto-advance across the setlist,
 * and launches native `spotify:track:...` URIs directly in the Spotify app.
 */
@Composable
fun SpotifySetlistPlayerCard(
    showDate: String,
    tourName: String,
    bandName: String,
    songs: List<SongEntry>,
    currentSongIndex: Int,
    isPlaying: Boolean,
    spotifyClient: SpotifyApiClient,
    onTogglePlayPause: () -> Unit,
    onSelectSongIndex: (Int) -> Unit,
    onClosePlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentSong = songs.getOrNull(currentSongIndex) ?: return
    val nextSong = songs.getOrNull(currentSongIndex + 1)

    var trackInfo by remember(currentSong.name, showDate) {
        mutableStateOf(
            spotifyClient.resolveCatalogTrack(
                songName = currentSong.name,
                showDate = showDate,
                tourName = tourName,
                bandName = bandName
            )
        )
    }
    var isResolvingApi by remember(currentSong.name, showDate) { mutableStateOf(false) }
    var playbackFraction by remember(currentSong.name) { mutableFloatStateOf(0f) }

    // Query Spotify Web API v1 when song changes
    LaunchedEffect(currentSong.name, showDate) {
        isResolvingApi = true
        trackInfo = spotifyClient.resolveTrackForSetlistSong(
            songName = currentSong.name,
            showDate = showDate,
            tourName = tourName,
            bandName = bandName
        )
        isResolvingApi = false
    }

    // Optional MediaPlayer for Spotify 30s preview_url stream when provided by the Web API
    DisposableEffect(trackInfo.previewUrl, isPlaying) {
        var mediaPlayer: MediaPlayer? = null
        val preview = trackInfo.previewUrl
        if (!preview.isNullOrBlank() && isPlaying) {
            try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(preview)
                    setOnPreparedListener { it.start() }
                    prepareAsync()
                }
            } catch (_: Exception) {
                mediaPlayer?.release()
                mediaPlayer = null
            }
        }
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (_: Exception) {
            }
        }
    }

    // Smooth setlist playback progress & automatic seamless segue (`➔`) transition
    LaunchedEffect(isPlaying, currentSongIndex) {
        while (isPlaying) {
            delay(250L)
            val nextVal = playbackFraction + 0.008f
            if (nextVal >= 1f) {
                if (currentSongIndex + 1 < songs.size) {
                    onSelectSongIndex(currentSongIndex + 1)
                } else {
                    playbackFraction = 0f
                    onTogglePlayPause()
                }
            } else {
                playbackFraction = nextVal
            }
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StealieCardDark),
        border = BorderStroke(1.5.dp, SpotifyBrandGreen.copy(alpha = 0.65f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("spotify_setlist_player_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SpotifyBrandGreen.copy(alpha = 0.14f),
                            StealieCardDark
                        )
                    )
                )
                .padding(14.dp)
        ) {
            // Top Header: Spotify API Status Badge + Close button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(SpotifyBrandGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isResolvingApi) "Querying Spotify Web API..." else trackInfo.source.badgeLabel,
                        color = SpotifyBrandGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Track ${currentSongIndex + 1}/${songs.size} • ${currentSong.setType.shortLabel}",
                        color = TextSecondaryDark,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close Spotify Player",
                        tint = TextTertiaryDark,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onClosePlayer() }
                            .testTag("close_spotify_player_button")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Track Info & Animated Equalizer Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = trackInfo.title,
                            color = TextPrimaryDark,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (currentSong.isSegue && nextSong != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = StealieCrimsonLight.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "➔ ${nextSong.name}",
                                    color = StealieCrimsonLight,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${trackInfo.artistName} • ${trackInfo.albumName}",
                        color = TextSecondaryDark,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = trackInfo.spotifyUri,
                        color = TextTertiaryDark,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                SpotifyAnimatedEqualizer(isPlaying = isPlaying)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive Scrubber / Progress Bar
            LinearProgressIndicator(
                progress = { playbackFraction },
                color = SpotifyBrandGreen,
                trackColor = StealieMidnight,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Transport Controls + Open in Spotify App CTA
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Prev / Play-Pause / Next Transport
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (currentSongIndex > 0) {
                                onSelectSongIndex(currentSongIndex - 1)
                            } else {
                                playbackFraction = 0f
                            }
                        },
                        enabled = currentSongIndex > 0,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(StealieMidnight)
                            .testTag("spotify_prev_track_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = "Previous Setlist Track",
                            tint = if (currentSongIndex > 0) TextPrimaryDark else TextTertiaryDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SpotifyBrandGreen)
                            .testTag("spotify_player_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = StealieMidnight,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (currentSongIndex + 1 < songs.size) {
                                onSelectSongIndex(currentSongIndex + 1)
                            }
                        },
                        enabled = currentSongIndex + 1 < songs.size,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(StealieMidnight)
                            .testTag("spotify_next_track_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Next Setlist Track",
                            tint = if (currentSongIndex + 1 < songs.size) TextPrimaryDark else TextTertiaryDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Direct Spotify URI Launch Button
                Button(
                    onClick = {
                        SpotifyApiClient.openTrackInSpotifyAppOrWeb(context, trackInfo)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpotifyBrandGreen.copy(alpha = 0.18f),
                        contentColor = SpotifyBrandGreen
                    ),
                    border = BorderStroke(1.dp, SpotifyBrandGreen),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("open_in_spotify_app_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Open in Spotify",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SpotifyAnimatedEqualizer(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "spotifyEq")
    val barFactors = listOf(0.4f, 0.85f, 0.55f, 0.95f, 0.65f)

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .height(24.dp)
            .padding(horizontal = 4.dp)
    ) {
        barFactors.forEachIndexed { idx, base ->
            val anim by infiniteTransition.animateFloat(
                initialValue = 0.25f,
                targetValue = base,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 320 + idx * 90,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "eqBar_$idx"
            )
            val fraction = if (isPlaying) anim else 0.2f
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(fraction)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SpotifyBrandGreen)
            )
        }
    }
}
