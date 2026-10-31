package com.example.data.repository

import com.example.data.local.Converters
import com.example.data.local.ShowDao
import com.example.data.local.ShowEntity
import com.example.data.local.ShowWithSongs
import com.example.data.local.SongPerformanceEntity
import com.example.data.model.*
import com.example.data.seed.CuratedShows
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class OverallStats(
    val totalShowsCount: Int,
    val attendedShowsCount: Int,
    val totalUniqueSongs: Int,
    val totalSongPerformancesIndexed: Int,
    val totalTransitionsLogged: Int,
    val distinctCitiesCount: Int,
    val distinctVenuesCount: Int,
    val eraBreakdown: List<EraStat>,
    val topSongsOverall: List<SongStat>,
    val topSongsAttended: List<SongStat>,
    val songEraFrequencies: List<SongEraFrequency> = emptyList(),
    val topTransitions: List<SegueStat>,
    val topOpeners: List<SongStat>,
    val topEncores: List<SongStat>
)

class ShowRepository(private val showDao: ShowDao) {

    private val converters = Converters()

    val allShows: Flow<List<ShowEntity>> = showDao.getAllShows()
    val allShowsWithSongs: Flow<List<ShowWithSongs>> = showDao.getAllShowsWithSongs()
    val attendedShows: Flow<List<ShowEntity>> = showDao.getAttendedShows()
    val distinctCities: Flow<List<String>> = showDao.getDistinctCities()
    val distinctVenues: Flow<List<String>> = showDao.getDistinctVenues()

    fun getShowById(id: String): Flow<ShowEntity?> = showDao.getShowById(id)

    fun getShowWithSongsById(id: String): Flow<ShowWithSongs?> = showDao.getShowWithSongsById(id)

    fun searchShowsOffline(query: String): Flow<List<ShowEntity>> = showDao.searchShowsOffline(query)

    fun getShowsByCity(city: String): Flow<List<ShowEntity>> = showDao.getShowsByCity(city)

    fun getShowsByVenue(venue: String): Flow<List<ShowEntity>> = showDao.getShowsByVenue(venue)

    fun getShowsPerformingSong(songName: String): Flow<List<ShowEntity>> =
        showDao.getShowsPerformingSong(songName)

    suspend fun checkAndSeedDatabase() = withContext(Dispatchers.IO) {
        val count = showDao.getShowsCount()
        val curatedShows = CuratedShows.getInitialShows()
        if (count == 0) {
            showDao.insertShows(curatedShows)
            for (show in curatedShows) {
                syncSongPerformancesForShow(show.id, parseSongs(show))
            }
        } else {
            val perfCount = showDao.getSongPerformancesCount()
            if (perfCount == 0) {
                for (show in curatedShows) {
                    syncSongPerformancesForShow(show.id, parseSongs(show))
                }
            }
        }
    }

    suspend fun restoreCuratedOfflineCatalog() = withContext(Dispatchers.IO) {
        val curatedShows = CuratedShows.getInitialShows()
        showDao.insertShows(curatedShows)
        for (show in curatedShows) {
            syncSongPerformancesForShow(show.id, parseSongs(show))
        }
    }

    suspend fun saveShow(show: ShowEntity) = withContext(Dispatchers.IO) {
        showDao.insertShow(show)
        syncSongPerformancesForShow(show.id, parseSongs(show))
    }

    suspend fun deleteShow(show: ShowEntity) = withContext(Dispatchers.IO) {
        showDao.deleteShow(show)
    }

    suspend fun toggleAttendance(id: String, isAttended: Boolean) = withContext(Dispatchers.IO) {
        showDao.updateAttendance(id, isAttended)
    }

    suspend fun saveUserReview(
        id: String,
        review: String,
        rating: Float,
        attendedWith: String,
        favoriteSong: String
    ) = withContext(Dispatchers.IO) {
        showDao.updateReview(
            id = id,
            review = review,
            rating = rating,
            attendedWith = attendedWith,
            favoriteSong = favoriteSong
        )
    }

    suspend fun toggleSongTransition(show: ShowEntity, songIndex: Int) = withContext(Dispatchers.IO) {
        val songs = converters.toSongList(show.setlistJson).toMutableList()
        if (songIndex in songs.indices) {
            val current = songs[songIndex]
            songs[songIndex] = current.copy(isSegue = !current.isSegue)
            val updatedJson = converters.fromSongList(songs)
            showDao.updateSetlist(show.id, updatedJson)
            syncSongPerformancesForShow(show.id, songs)
        }
    }

    private suspend fun syncSongPerformancesForShow(showId: String, songs: List<SongEntry>) {
        showDao.deleteSongPerformancesForShow(showId)
        val entities = songs.mapIndexed { index, entry ->
            SongPerformanceEntity.fromSongEntry(showId, index, entry)
        }
        if (entities.isNotEmpty()) {
            showDao.insertSongPerformances(entities)
        }
    }

    fun parseSongs(show: ShowEntity): List<SongEntry> {
        return converters.toSongList(show.setlistJson)
    }

    fun serializeSongs(songs: List<SongEntry>): String {
        return converters.fromSongList(songs)
    }

    fun computeOverallStats(shows: List<ShowEntity>): OverallStats {
        val attendedList = shows.filter { it.isAttended }
        val songPlayMap = mutableMapOf<String, Int>()
        val attendedSongMap = mutableMapOf<String, Int>()
        val segueMap = mutableMapOf<String, Int>()
        val openerMap = mutableMapOf<String, Int>()
        val encoreMap = mutableMapOf<String, Int>()
        val eraCountMap = mutableMapOf<String, Int>()
        val eraSongTotalMap = mutableMapOf<String, Int>()
        val eraSongPlayMap = mutableMapOf<String, MutableMap<String, Int>>()
        val songByEraMap = mutableMapOf<String, MutableMap<DeadEra, Int>>()
        var totalPerformancesCount = 0

        for (show in shows) {
            eraCountMap[show.eraId] = (eraCountMap[show.eraId] ?: 0) + 1
            val resolvedEra = DeadEra.fromId(show.eraId)
            val songs = parseSongs(show)
            totalPerformancesCount += songs.size
            eraSongTotalMap[show.eraId] = (eraSongTotalMap[show.eraId] ?: 0) + songs.size

            val firstSong = songs.firstOrNull { it.setType == SetType.SET_1 }
            if (firstSong != null) {
                openerMap[firstSong.name] = (openerMap[firstSong.name] ?: 0) + 1
            }

            val encoreSong = songs.firstOrNull { it.setType == SetType.ENCORE }
            if (encoreSong != null) {
                encoreMap[encoreSong.name] = (encoreMap[encoreSong.name] ?: 0) + 1
            }

            val songsInThisEra = eraSongPlayMap.getOrPut(show.eraId) { mutableMapOf() }

            for (i in songs.indices) {
                val song = songs[i]
                songPlayMap[song.name] = (songPlayMap[song.name] ?: 0) + 1
                songsInThisEra[song.name] = (songsInThisEra[song.name] ?: 0) + 1

                if (resolvedEra != DeadEra.ALL) {
                    val eraCountsForSong = songByEraMap.getOrPut(song.name) { mutableMapOf() }
                    eraCountsForSong[resolvedEra] = (eraCountsForSong[resolvedEra] ?: 0) + 1
                }

                if (show.isAttended) {
                    attendedSongMap[song.name] = (attendedSongMap[song.name] ?: 0) + 1
                }

                if (song.isSegue && i + 1 < songs.size) {
                    val nextSong = songs[i + 1]
                    val pair = "${song.name} ➔ ${nextSong.name}"
                    segueMap[pair] = (segueMap[pair] ?: 0) + 1
                }
            }
        }

        val totalShows = shows.size.coerceAtLeast(1)
        val validEras = DeadEra.entries.filter { it != DeadEra.ALL }
        val eraBreakdown = validEras.map { era ->
            val count = eraCountMap[era.id] ?: 0
            val pct = (count.toFloat() / totalShows.toFloat()) * 100f
            val songTotal = eraSongTotalMap[era.id] ?: 0
            val topEraSongs = (eraSongPlayMap[era.id] ?: emptyMap())
                .entries
                .sortedByDescending { it.value }
                .take(5)
                .map { SongStat(it.key, it.value) }
            EraStat(
                era = era,
                showCount = count,
                percentage = pct,
                songPerformanceCount = songTotal,
                topSongsInEra = topEraSongs
            )
        }

        val topSongsOverall = songPlayMap.entries
            .sortedByDescending { it.value }
            .take(15)
            .map { SongStat(it.key, it.value, attendedSongMap[it.key] ?: 0) }

        val topSongsAttended = attendedSongMap.entries
            .sortedByDescending { it.value }
            .take(15)
            .map { SongStat(it.key, it.value, it.value) }

        val songEraFrequencies = songPlayMap.entries
            .sortedByDescending { it.value }
            .take(15)
            .map { (songName, totalPlays) ->
                val rawMap = songByEraMap[songName] ?: emptyMap()
                val normalizedEraMap = validEras.associateWith { era -> rawMap[era] ?: 0 }
                SongEraFrequency(
                    songName = songName,
                    totalPlays = totalPlays,
                    eraCounts = normalizedEraMap
                )
            }

        val topTransitions = segueMap.entries
            .sortedByDescending { it.value }
            .take(10)
            .map { SegueStat(it.key, it.value) }

        val topOpeners = openerMap.entries
            .sortedByDescending { it.value }
            .take(6)
            .map { SongStat(it.key, it.value) }

        val topEncores = encoreMap.entries
            .sortedByDescending { it.value }
            .take(6)
            .map { SongStat(it.key, it.value) }

        val totalTransitions = segueMap.values.sum()
        val distinctCitiesCount = shows.map { it.city }.distinct().size
        val distinctVenuesCount = shows.map { it.venue }.distinct().size

        return OverallStats(
            totalShowsCount = shows.size,
            attendedShowsCount = attendedList.size,
            totalUniqueSongs = songPlayMap.size,
            totalSongPerformancesIndexed = totalPerformancesCount,
            totalTransitionsLogged = totalTransitions,
            distinctCitiesCount = distinctCitiesCount,
            distinctVenuesCount = distinctVenuesCount,
            eraBreakdown = eraBreakdown,
            topSongsOverall = topSongsOverall,
            topSongsAttended = topSongsAttended,
            songEraFrequencies = songEraFrequencies,
            topTransitions = topTransitions,
            topOpeners = topOpeners,
            topEncores = topEncores
        )
    }
}
