package com.example.data.repository

import com.example.data.local.ShowDao
import com.example.data.local.ShowEntity
import com.example.data.local.ShowWithSongs
import com.example.data.local.SongAliasEntity
import com.example.data.local.SongDao
import com.example.data.local.SongEntity
import com.example.data.local.SongPerformanceEntity
import com.example.data.local.toCatalog
import com.example.data.model.*
import com.example.data.seed.CuratedSeedMerger
import com.example.data.seed.CuratedShows
import com.example.data.seed.SeedShow
import com.example.data.songs.SongCatalog
import com.example.data.songs.SongResolver
import com.example.data.songs.songKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

class ShowRepository(
    private val showDao: ShowDao,
    private val songDao: SongDao
) {
    /** Serializes setlist read-modify-write so concurrent adds/toggles cannot overwrite each other. */
    private val setlistMutex = Mutex()

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
        ensureSongCatalog()
        showDao.deleteUntouchedSeedShows(CuratedShows.retiredSeedShowIds)
        for (legacy in CuratedShows.legacySeedPersonalData) {
            showDao.clearInventedPersonalData(
                id = legacy.showId,
                inventedRating = legacy.rating,
                inventedFavorite = legacy.favoriteSong,
                inventedCompanions = legacy.attendedWith,
                inventedReview = legacy.inventedReview,
                clearAttendance = legacy.inventedAttendance
            )
        }
        // Empty DB: insert all seeds. Non-empty: merge curated fields onto catalog matches,
        // always ensure non-GD seeds (Terrapin Flyer); do not resurrect unmatched GD seed ids.
        applyCuratedSeeds(insertUnmatchedGd = false)
        insertSeedSetlistsWhereMissing()
    }

    /**
     * Re-adds missing curated shows and merges curated enrichment onto catalog rows matched by
     * date + venue. Existing user edits and non-empty setlists are kept; catalog-only upsert.
     */
    suspend fun restoreCuratedOfflineCatalog() = withContext(Dispatchers.IO) {
        ensureSongCatalog()
        applyCuratedSeeds(insertUnmatchedGd = true)
        insertSeedSetlistsWhereMissing()
    }

    /**
     * Matches GD curated seeds to catalog rows (date + venue), merges archive/trivia/tags onto
     * the catalog id via [ShowDao.upsertCatalog] (never touches [user_show_data]), inserts
     * missing non-GD seeds, and drops untouched duplicate seed-id rows when a catalog match won.
     */
    private suspend fun applyCuratedSeeds(insertUnmatchedGd: Boolean) {
        val seeds = CuratedShows.getInitialShows()
        val existing = showDao.getAllShowsOnce()
        val bindings = CuratedSeedMerger.plan(
            seeds = seeds,
            existing = existing,
            options = CuratedSeedMerger.PlanOptions(
                emptyDatabase = existing.isEmpty(),
                insertUnmatchedGd = insertUnmatchedGd
            )
        )
        val toInsert = bindings.filter { it.insertNew }.map { it.seed.show }
        if (toInsert.isNotEmpty()) {
            showDao.insertShows(toInsert)
        }
        val afterInsert = showDao.getAllShowsOnce().associateBy { it.id }
        for (binding in bindings) {
            if (binding.insertNew) continue
            val target = afterInsert[binding.targetShowId] ?: continue
            val merged = CuratedSeedMerger.mergeCatalogFields(target, binding.seed.show)
            if (merged != target) {
                showDao.upsertCatalog(merged.toCatalog())
            }
        }
        val seedsById = seeds.associateBy { it.id }
        for (seedId in CuratedSeedMerger.duplicateSeedIdsToDrop(bindings, afterInsert.values.toList())) {
            val binding = bindings.firstOrNull { it.seed.id == seedId } ?: continue
            resolveDuplicateSeedRow(
                seedId = seedId,
                catalogId = binding.targetShowId,
                builtIn = seedsById[seedId] ?: binding.seed,
                showsById = afterInsert
            )
        }
    }

    /**
     * When a catalog UUID owns a curated date, move any seed-id setlist edits onto the catalog
     * row, then delete the seed-id duplicate only if it has no claimed user data.
     */
    private suspend fun resolveDuplicateSeedRow(
        seedId: String,
        catalogId: String,
        builtIn: SeedShow,
        showsById: Map<String, ShowEntity>
    ) {
        val seedShow = showsById[seedId] ?: return
        val seedPerfs = showDao.getSongsForShowOnce(seedId)
        val catalogPerfs = showDao.getSongsForShowOnce(catalogId)
        val seedEdited = seedPerfs.isNotEmpty() && setlistDiffersFromBuiltIn(seedPerfs, builtIn.songs)
        if (seedPerfs.isNotEmpty() && (catalogPerfs.isEmpty() || seedEdited)) {
            showDao.replaceSongPerformances(
                catalogId,
                seedPerfs.map { it.copy(performanceId = 0L, showId = catalogId) }
            )
        }
        if (!CuratedSeedMerger.hasClaimedUserData(seedShow)) {
            showDao.deleteShowById(seedId)
        }
    }

    private fun setlistDiffersFromBuiltIn(
        perfs: List<SongPerformanceEntity>,
        builtIn: List<SongEntry>
    ): Boolean {
        if (perfs.size != builtIn.size) return true
        return perfs.zip(builtIn).any { (perf, entry) ->
            perf.songName != entry.name ||
                perf.setType != entry.setType.name ||
                perf.transitionOut != entry.transitionOut.name
        }
    }

    /**
     * Writes the built-in setlist for each curated binding whose target show exists but has no
     * setlist rows. Uses the catalog id when a seed matched a bundled row (R1).
     */
    private suspend fun insertSeedSetlistsWhereMissing() {
        val existing = showDao.getAllShowsOnce()
        val storedIds = existing.map { it.id }.toSet()
        val withSetlist = showDao.getShowIdsWithPerformances().toSet()
        val bindings = CuratedSeedMerger.plan(
            seeds = CuratedShows.getInitialShows(),
            existing = existing,
            options = CuratedSeedMerger.PlanOptions(
                emptyDatabase = false,
                insertUnmatchedGd = false
            )
        )
        val resolver = loadSongResolver()
        for (binding in bindings) {
            val id = binding.targetShowId
            if (id in storedIds && id !in withSetlist) {
                writeSetlist(id, binding.seed.songs, resolver)
            }
        }
    }

    /** Saves a show and its setlist, normalizing song titles (e.g. "St. Stephen" → "Saint Stephen"). */
    suspend fun saveShow(show: ShowEntity, songs: List<SongEntry>) = withContext(Dispatchers.IO) {
        val resolver = loadSongResolver()
        val entities = buildPerformances(show.id, songs, resolver)
        // Songs must exist before performance rows reference them.
        persistNewSongs(resolver)
        // Show + setlist in one transaction, so an interruption can't leave the show without its setlist.
        showDao.saveShowWithSetlist(show, entities)
    }

    /** Inserts catalog songs and aliases; existing rows are left untouched. */
    suspend fun ensureSongCatalog() = withContext(Dispatchers.IO) {
        songDao.insertSongs(
            SongCatalog.songs.map { SongEntity(id = songKey(it.canonicalName), canonicalName = it.canonicalName) }
        )
        songDao.insertAliases(
            SongCatalog.canonicalByKey.map { (aliasKey, canonical) ->
                SongAliasEntity(aliasKey = aliasKey, songId = songKey(canonical))
            }
        )
        applySongStoryFacts()
    }

    /** Writes sourced Almanac Song Story credits into Room (nulls stay null). */
    private suspend fun applySongStoryFacts() {
        for (fact in com.example.data.almanac.SongStoryFacts.all) {
            songDao.updateSongFacts(
                id = songKey(fact.canonicalName),
                writers = fact.writers,
                debutDate = fact.debutDate
            )
        }
    }

    /** Resolves a typed title to its canonical song (creating it if new); null for blank input. */
    suspend fun resolveSong(name: String): SongResolver.Resolved? = withContext(Dispatchers.IO) {
        if (name.isBlank()) return@withContext null
        val resolver = loadSongResolver()
        resolver.resolve(name).also { persistNewSongs(resolver) }
    }

    /** All known song names (catalog + user-entered), for autocomplete. */
    suspend fun allSongNames(): List<String> = withContext(Dispatchers.IO) {
        songDao.getAllSongs().map { it.canonicalName }.sorted()
    }

    private suspend fun loadSongResolver(): SongResolver = SongResolver(
        aliasToSongId = songDao.getAllAliases().associate { it.aliasKey to it.songId },
        songNames = songDao.getAllSongs().associate { it.id to it.canonicalName }
    )

    private suspend fun persistNewSongs(resolver: SongResolver) {
        if (resolver.newSongs.isNotEmpty()) {
            songDao.insertSongs(resolver.newSongs.map { (id, name) -> SongEntity(id, name) })
            resolver.newSongs.clear()
        }
        if (resolver.newAliases.isNotEmpty()) {
            songDao.insertAliases(resolver.newAliases.map { (key, id) -> SongAliasEntity(key, id) })
            resolver.newAliases.clear()
        }
    }

    suspend fun deleteShow(show: ShowEntity) = withContext(Dispatchers.IO) {
        showDao.deleteShow(show)
    }

    /** Links a tribute show to the Grateful Dead show it recreates, or unlinks with null (4.8). */
    suspend fun setRecreation(showId: String, originalId: String?) = withContext(Dispatchers.IO) {
        showDao.setRecreation(showId, originalId)
    }

    suspend fun toggleAttendance(id: String, isAttended: Boolean) = withContext(Dispatchers.IO) {
        showDao.updateAttendanceEnsured(id, isAttended)
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

    /** Cycles the song's transition into the next one: Stop → Segue → Transition → Jam → Stop. */
    suspend fun toggleSongTransition(showId: String, songIndex: Int) = withContext(Dispatchers.IO) {
        setlistMutex.withLock {
            val songs = getSetlist(showId).toMutableList()
            if (songIndex in songs.indices) {
                val current = songs[songIndex]
                songs[songIndex] = current.copy(transitionOut = current.transitionOut.next())
                writeSetlist(showId, songs, loadSongResolver())
            }
        }
    }

    /**
     * Live tracker (step 4.9): adds a just-played song at the end of [setType], before any later
     * set already logged (e.g. an Encore entered early stays last).
     */
    suspend fun appendSong(showId: String, name: String, setType: SetType) = withContext(Dispatchers.IO) {
        setlistMutex.withLock {
            val songs = getSetlist(showId).toMutableList()
            val insertAt = songs.indexOfFirst { it.setType.ordinal > setType.ordinal }.let { if (it < 0) songs.size else it }
            songs.add(insertAt, SongEntry(name = name.trim(), setType = setType))
            writeSetlist(showId, songs, loadSongResolver())
        }
    }

    /** A show's setlist in performance order. */
    suspend fun getSetlist(showId: String): List<SongEntry> = withContext(Dispatchers.IO) {
        ShowWithSongs.orderSongs(showDao.getSongsForShowOnce(showId))
    }

    /** Replaces a show's setlist, resolving each title to its canonical song. */
    private suspend fun writeSetlist(showId: String, songs: List<SongEntry>, resolver: SongResolver) {
        val entities = buildPerformances(showId, songs, resolver)
        // Songs must exist before performance rows reference them.
        persistNewSongs(resolver)
        showDao.replaceSongPerformances(showId, entities)
    }

    private fun buildPerformances(
        showId: String,
        songs: List<SongEntry>,
        resolver: SongResolver
    ): List<SongPerformanceEntity> =
        songs.mapIndexed { index, entry ->
            val resolved = resolver.resolve(entry.name)
            SongPerformanceEntity.fromSongEntry(
                showId = showId,
                index = index,
                entry = entry.copy(name = resolved.canonicalName),
                songId = resolved.songId
            )
        }

    fun computeOverallStats(
        shows: List<ShowEntity>,
        songsFor: (ShowEntity) -> List<SongEntry>
    ): OverallStats {
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
            val songs = songsFor(show)
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

                if (song.isConnected && i + 1 < songs.size) {
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
