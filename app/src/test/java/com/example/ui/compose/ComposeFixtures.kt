package com.example.ui.compose

import com.example.data.local.ShowEntity
import com.example.data.model.SongEntry
import com.example.data.model.SongStat
import com.example.data.repository.OverallStats
import com.example.data.seed.TriviaUserProgress
import com.example.ui.UserProfileData

internal fun sampleShow(
    id: String = "gd1977-05-08",
    date: String = "1977-05-08",
    venue: String = "Barton Hall",
    city: String = "Ithaca",
    custom: Boolean = false,
    attended: Boolean = false
) = ShowEntity(
    id = id,
    date = date,
    venue = venue,
    city = city,
    stateOrCountry = "NY",
    eraId = "spring_77",
    tourName = "Spring 1977",
    bandName = "Grateful Dead",
    isCustomShow = custom,
    isAttended = attended
)

internal fun sampleSong(name: String = "Scarlet Begonias") =
    SongEntry(name = name)

internal fun sampleStats(
    totalShows: Int = 12,
    topSong: String = "Scarlet Begonias",
    topPlays: Int = 5
) = OverallStats(
    totalShowsCount = totalShows,
    attendedShowsCount = 3,
    totalUniqueSongs = 8,
    totalSongPerformancesIndexed = 20,
    totalTransitionsLogged = 4,
    distinctCitiesCount = 2,
    distinctVenuesCount = 2,
    eraBreakdown = emptyList(),
    topSongsOverall = listOf(SongStat(songName = topSong, playCount = topPlays)),
    topSongsAttended = listOf(SongStat(songName = topSong, playCount = 2)),
    topTransitions = emptyList(),
    topOpeners = emptyList(),
    topEncores = emptyList()
)

internal fun sampleProfile(handle: String = "OldHandle") = UserProfileData(
    handle = handle,
    homeCity = "Chicago, IL",
    favoriteJam = "Scarlet > Fire",
    favoriteEraTitle = "Spring '77"
)

internal fun emptyTriviaProgress() = TriviaUserProgress()
