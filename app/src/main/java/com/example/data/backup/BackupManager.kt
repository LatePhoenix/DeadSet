package com.example.data.backup

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.BandEntity
import com.example.data.local.ChaseSongEntity
import com.example.data.local.ReviewCardEntity
import com.example.data.local.ShowCatalogEntity
import com.example.data.local.SongEntity
import com.example.data.local.SongPerformanceEntity
import com.example.data.local.UserPrefsStore
import com.example.data.local.UserShowDataEntity
import com.example.data.seed.TriviaUserProgress
import com.example.data.todayIsoDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * User-data backup/restore (6.4). Plain class for unit tests; no UI or ContentResolver.
 * JSON via org.json only.
 */
class BackupManager(
    private val database: AppDatabase,
    private val prefs: UserPrefsStore,
    private val appVersion: String,
    private val today: () -> String = { todayIsoDate() }
) {
    private val showDao get() = database.showDao()
    private val songDao get() = database.songDao()
    private val chaseDao get() = database.chaseDao()
    private val reviewDao get() = database.reviewDao()
    private val concertDao get() = database.concertDao()

    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        val userRows = showDao.getAllUserShowData()
        val customCatalog = showDao.getCustomCatalogShows()
        val customShows = customCatalog.map { catalog ->
            BackupCustomShow(
                catalog = catalog,
                performances = showDao.getSongsForShowOnce(catalog.id).map { it.copy(performanceId = 0L) }
            )
        }
        val chase = chaseDao.getAllOnce()
        val reviews = reviewDao.getAllOnce()
        val bands = concertDao.getAllBands().filter { it.isFollowed || !it.isBuiltIn }
        val profile = prefs.loadProfile()
        val trivia = prefs.loadTriviaProgress()
        val doc = BackupDocument(
            version = BACKUP_VERSION,
            exportedAt = today(),
            appVersion = appVersion,
            userShowData = userRows,
            customShows = customShows,
            chaseSongs = chase,
            reviewCards = reviews,
            bands = bands,
            prefs = BackupPrefs(
                profile = profile,
                triviaProgress = trivia,
                alertsEnabled = prefs.alertsEnabled()
            )
        )
        encode(doc).toString()
    }

    fun parse(json: String): BackupParseResult {
        val root = try {
            JSONObject(json)
        } catch (_: Exception) {
            return BackupParseResult.Err("Backup file is not valid JSON.")
        }
        if (root.optString("format") != BACKUP_FORMAT) {
            return BackupParseResult.Err("Not a DeadSet backup (missing format \"$BACKUP_FORMAT\").")
        }
        if (!root.has("version") || root.isNull("version")) {
            return BackupParseResult.Err("Backup is missing a version.")
        }
        val version = root.optInt("version", -1)
        if (version < 1) {
            return BackupParseResult.Err("Backup version is invalid.")
        }
        if (version > BACKUP_VERSION) {
            return BackupParseResult.Err(
                "This backup is version $version; this app supports up to $BACKUP_VERSION. Update DeadSet and try again."
            )
        }
        val required = listOf(
            "exportedAt", "appVersion", "userShowData", "customShows",
            "chaseSongs", "reviewCards", "bands", "prefs"
        )
        for (key in required) {
            if (!root.has(key) || root.isNull(key)) {
                return BackupParseResult.Err("Backup is missing required field \"$key\".")
            }
        }
        return try {
            BackupParseResult.Ok(decode(root))
        } catch (e: Exception) {
            BackupParseResult.Err("Backup could not be read: ${e.message ?: "invalid fields"}")
        }
    }

    fun preview(doc: BackupDocument): BackupPreview = BackupPreview(
        attendedCount = doc.userShowData.count { it.isAttended },
        userShowDataCount = doc.userShowData.size,
        customShowCount = doc.customShows.size,
        chaseSongCount = doc.chaseSongs.size,
        reviewCardCount = doc.reviewCards.size,
        bandCount = doc.bands.size,
        hasProfile = doc.prefs.profile != null,
        alertsEnabled = doc.prefs.alertsEnabled
    )

    /**
     * Upserts validated backup data in one Room transaction. Never deletes existing rows.
     * Skips [UserShowDataEntity] whose show id is not in the catalog, and custom shows whose
     * id already exists as a non-custom catalog show (H2). Prefs are written after the
     * transaction commits. Trivia progress and review cards are merged with local values.
     */
    suspend fun importDocument(doc: BackupDocument): BackupImportResult = withContext(Dispatchers.IO) {
        var skippedUnknown = 0
        var skippedCollisions = 0
        var userUpserts = 0
        var customUpserts = 0
        var chaseUpserts = 0
        var reviewFromBackup = 0
        var reviewKeptLocal = 0
        var bandUpserts = 0
        val localTriviaBefore = prefs.loadTriviaProgress()
        val backupAlerts = doc.prefs.alertsEnabled
        val backupProfile = doc.prefs.profile
        val backupTrivia = doc.prefs.triviaProgress

        database.withTransaction {
            for (custom in doc.customShows) {
                if (showDao.isNonCustomCatalogShow(custom.catalog.id)) {
                    skippedCollisions++
                    continue
                }
                showDao.upsertCatalog(custom.catalog.copy(isCustomShow = true))
                ensureSongsForPerformances(custom.performances)
                showDao.replaceSongPerformances(
                    custom.catalog.id,
                    custom.performances.map { it.copy(performanceId = 0L, showId = custom.catalog.id) }
                )
                customUpserts++
            }

            val knownIds = showDao.getAllShowIds().toHashSet()
            for (row in doc.userShowData) {
                if (row.showId !in knownIds) {
                    skippedUnknown++
                    continue
                }
                // Matching show reviews/ratings: backup wins (H2).
                showDao.upsertUserData(row)
                userUpserts++
            }

            for (song in doc.chaseSongs) {
                chaseDao.upsert(song)
                chaseUpserts++
            }
            for (card in doc.reviewCards) {
                val local = reviewDao.get(card.cardId)
                val chosen = preferReviewCard(local, card)
                reviewDao.upsert(chosen)
                if (local != null && chosen == local) {
                    reviewKeptLocal++
                } else {
                    reviewFromBackup++
                }
            }
            if (doc.bands.isNotEmpty()) {
                concertDao.upsertBands(doc.bands)
                bandUpserts = doc.bands.size
            }
        }

        // Prefs after Room commits (H2): merge trivia; profile + alerts still backup-wins.
        backupProfile?.let { prefs.saveProfile(it) }
        prefs.saveTriviaProgress(mergeTriviaProgress(localTriviaBefore, backupTrivia))
        prefs.setAlertsEnabled(backupAlerts)

        BackupImportResult(
            userShowDataUpserted = userUpserts,
            customShowsUpserted = customUpserts,
            chaseSongsUpserted = chaseUpserts,
            reviewCardsUpserted = reviewFromBackup,
            bandsUpserted = bandUpserts,
            skippedUnknownShowIds = skippedUnknown,
            skippedCatalogCollisions = skippedCollisions,
            reviewCardsKeptLocal = reviewKeptLocal
        )
    }

    private suspend fun ensureSongsForPerformances(perfs: List<SongPerformanceEntity>) {
        if (perfs.isEmpty()) return
        songDao.insertSongs(
            perfs.map { SongEntity(id = it.songId, canonicalName = it.songName) }.distinctBy { it.id }
        )
    }

    private fun encode(doc: BackupDocument): JSONObject = JSONObject().apply {
        put("format", BACKUP_FORMAT)
        put("version", doc.version)
        put("exportedAt", doc.exportedAt)
        put("appVersion", doc.appVersion)
        put("userShowData", JSONArray().also { arr ->
            doc.userShowData.forEach { arr.put(encodeUserShow(it)) }
        })
        put("customShows", JSONArray().also { arr ->
            doc.customShows.forEach { arr.put(encodeCustomShow(it)) }
        })
        put("chaseSongs", JSONArray().also { arr ->
            doc.chaseSongs.forEach { arr.put(encodeChase(it)) }
        })
        put("reviewCards", JSONArray().also { arr ->
            doc.reviewCards.forEach { arr.put(encodeReview(it)) }
        })
        put("bands", JSONArray().also { arr ->
            doc.bands.forEach { arr.put(encodeBand(it)) }
        })
        put("prefs", encodePrefs(doc.prefs))
    }

    private fun decode(root: JSONObject): BackupDocument {
        val prefsObj = root.getJSONObject("prefs")
        return BackupDocument(
            version = root.getInt("version"),
            exportedAt = root.getString("exportedAt"),
            appVersion = root.getString("appVersion"),
            userShowData = root.getJSONArray("userShowData").mapObjects { decodeUserShow(it) },
            customShows = root.getJSONArray("customShows").mapObjects { decodeCustomShow(it) },
            chaseSongs = root.getJSONArray("chaseSongs").mapObjects { decodeChase(it) },
            reviewCards = root.getJSONArray("reviewCards").mapObjects { decodeReview(it) },
            bands = root.getJSONArray("bands").mapObjects { decodeBand(it) },
            prefs = decodePrefs(prefsObj)
        )
    }

    private fun encodeUserShow(u: UserShowDataEntity) = JSONObject().apply {
        put("showId", u.showId)
        put("isAttended", u.isAttended)
        put("userRating", u.userRating.toDouble())
        put("userReview", u.userReview)
        put("attendedWith", u.attendedWith)
        put("favoriteSong", u.favoriteSong)
    }

    private fun decodeUserShow(o: JSONObject) = UserShowDataEntity(
        showId = o.getString("showId"),
        isAttended = o.getBoolean("isAttended"),
        userRating = o.getDouble("userRating").toFloat(),
        userReview = o.getString("userReview"),
        attendedWith = o.getString("attendedWith"),
        favoriteSong = o.getString("favoriteSong")
    )

    private fun encodeCustomShow(c: BackupCustomShow) = JSONObject().apply {
        val s = c.catalog
        put("id", s.id)
        put("date", s.date)
        put("venue", s.venue)
        put("city", s.city)
        put("stateOrCountry", s.stateOrCountry)
        put("eraId", s.eraId)
        put("tourName", s.tourName)
        put("archiveIdentifier", s.archiveIdentifier)
        put("tapeQuality", s.tapeQuality)
        put("isCustomShow", true)
        put("tags", s.tags)
        put("historicTrivia", s.historicTrivia)
        put("bandName", s.bandName)
        put("venueAddress", s.venueAddress)
        if (s.recreatesShowId != null) put("recreatesShowId", s.recreatesShowId) else put("recreatesShowId", JSONObject.NULL)
        put("lastUpdatedTimestamp", s.lastUpdatedTimestamp)
        put("performances", JSONArray().also { arr ->
            c.performances.forEach { arr.put(encodePerformance(it)) }
        })
    }

    private fun decodeCustomShow(o: JSONObject): BackupCustomShow {
        val catalog = ShowCatalogEntity(
            id = o.getString("id"),
            date = o.getString("date"),
            venue = o.getString("venue"),
            city = o.getString("city"),
            stateOrCountry = o.getString("stateOrCountry"),
            eraId = o.getString("eraId"),
            tourName = o.getString("tourName"),
            archiveIdentifier = o.optString("archiveIdentifier", ""),
            tapeQuality = o.optString("tapeQuality", ""),
            isCustomShow = true,
            tags = o.optString("tags", ""),
            historicTrivia = o.optString("historicTrivia", ""),
            bandName = o.optString("bandName", "Grateful Dead"),
            venueAddress = o.optString("venueAddress", ""),
            recreatesShowId = if (o.isNull("recreatesShowId")) null else o.getString("recreatesShowId"),
            lastUpdatedTimestamp = o.optLong("lastUpdatedTimestamp", System.currentTimeMillis())
        )
        val perfs = o.getJSONArray("performances").mapObjects { decodePerformance(it, catalog.id) }
        return BackupCustomShow(catalog, perfs)
    }

    private fun encodePerformance(p: SongPerformanceEntity) = JSONObject().apply {
        put("songId", p.songId)
        put("songName", p.songName)
        put("setType", p.setType)
        put("trackOrder", p.trackOrder)
        put("transitionOut", p.transitionOut)
        if (p.durationMinutes != null) put("durationMinutes", p.durationMinutes) else put("durationMinutes", JSONObject.NULL)
        if (p.notes != null) put("notes", p.notes) else put("notes", JSONObject.NULL)
        put("isJamHighlight", p.isJamHighlight)
    }

    private fun decodePerformance(o: JSONObject, showId: String) = SongPerformanceEntity(
        performanceId = 0L,
        showId = showId,
        songId = o.getString("songId"),
        songName = o.getString("songName"),
        setType = o.getString("setType"),
        trackOrder = o.getInt("trackOrder"),
        transitionOut = o.optString("transitionOut", "NONE"),
        durationMinutes = if (o.isNull("durationMinutes")) null else o.getInt("durationMinutes"),
        notes = if (o.isNull("notes")) null else o.getString("notes"),
        isJamHighlight = o.optBoolean("isJamHighlight", false)
    )

    private fun encodeChase(c: ChaseSongEntity) = JSONObject().apply {
        put("songId", c.songId)
        put("songName", c.songName)
        put("addedDate", c.addedDate)
    }

    private fun decodeChase(o: JSONObject) = ChaseSongEntity(
        songId = o.getString("songId"),
        songName = o.getString("songName"),
        addedDate = o.getString("addedDate")
    )

    private fun encodeReview(r: ReviewCardEntity) = JSONObject().apply {
        put("cardId", r.cardId)
        put("box", r.box)
        put("lastReviewedDate", r.lastReviewedDate)
        put("timesSeen", r.timesSeen)
        put("timesCorrect", r.timesCorrect)
    }

    private fun decodeReview(o: JSONObject) = ReviewCardEntity(
        cardId = o.getString("cardId"),
        box = o.getInt("box"),
        lastReviewedDate = o.getString("lastReviewedDate"),
        timesSeen = o.getInt("timesSeen"),
        timesCorrect = o.getInt("timesCorrect")
    )

    private fun encodeBand(b: BandEntity) = JSONObject().apply {
        put("id", b.id)
        put("name", b.name)
        put("isFollowed", b.isFollowed)
        put("isBuiltIn", b.isBuiltIn)
    }

    private fun decodeBand(o: JSONObject) = BandEntity(
        id = o.getString("id"),
        name = o.getString("name"),
        isFollowed = o.getBoolean("isFollowed"),
        isBuiltIn = o.optBoolean("isBuiltIn", false)
    )

    private fun encodePrefs(p: BackupPrefs) = JSONObject().apply {
        if (p.profile != null) {
            put("profile", JSONObject().apply {
                put("handle", p.profile.handle)
                put("homeCity", p.profile.homeCity)
                put("favoriteJam", p.profile.favoriteJam)
                put("favoriteEraTitle", p.profile.favoriteEraTitle)
            })
        } else {
            put("profile", JSONObject.NULL)
        }
        put("triviaProgress", JSONObject().apply {
            val t = p.triviaProgress
            put("totalAnswered", t.totalAnswered)
            put("totalCorrect", t.totalCorrect)
            put("currentStreak", t.currentStreak)
            put("bestStreak", t.bestStreak)
            put("totalPoints", t.totalPoints)
            put("quizzesCompleted", t.quizzesCompleted)
            put("dailyCompletedDate", t.dailyCompletedDate)
            put("unlockedBadgeIds", JSONArray(t.unlockedBadgeIds.toList()))
        })
        put("alertsEnabled", p.alertsEnabled)
    }

    private fun decodePrefs(o: JSONObject): BackupPrefs {
        val profile = if (o.isNull("profile")) {
            null
        } else {
            val p = o.getJSONObject("profile")
            UserPrefsStore.StoredProfile(
                handle = p.getString("handle"),
                homeCity = p.getString("homeCity"),
                favoriteJam = p.getString("favoriteJam"),
                favoriteEraTitle = p.getString("favoriteEraTitle")
            )
        }
        val t = o.getJSONObject("triviaProgress")
        val badges = mutableSetOf<String>()
        val badgeArr = t.getJSONArray("unlockedBadgeIds")
        for (i in 0 until badgeArr.length()) badges.add(badgeArr.getString(i))
        return BackupPrefs(
            profile = profile,
            triviaProgress = TriviaUserProgress(
                totalAnswered = t.getInt("totalAnswered"),
                totalCorrect = t.getInt("totalCorrect"),
                currentStreak = t.getInt("currentStreak"),
                bestStreak = t.getInt("bestStreak"),
                totalPoints = t.getInt("totalPoints"),
                quizzesCompleted = t.getInt("quizzesCompleted"),
                dailyCompletedDate = t.optString("dailyCompletedDate", ""),
                unlockedBadgeIds = badges
            ),
            alertsEnabled = o.getBoolean("alertsEnabled")
        )
    }

    private fun <T> JSONArray.mapObjects(map: (JSONObject) -> T): List<T> {
        val out = ArrayList<T>(length())
        for (i in 0 until length()) out.add(map(getJSONObject(i)))
        return out
    }
}
