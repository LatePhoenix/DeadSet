package com.example.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * One-time (per [BUNDLED_CATALOG_VERSION]) merge of the bundled `catalog.db` into an existing
 * Room database (R2). Fresh installs still use [RoomDatabase.Builder.createFromAsset] and are
 * marked already-imported via [AppDatabase.freshInstallPrepackagedCallback] (G1).
 *
 * - Upserts `shows` / `songs` / `song_aliases` by primary key.
 * - Inserts `song_performances` only for shows that have none yet (skips custom shows and any
 *   show that already has a setlist, so user edits survive).
 * - Never reads or writes `user_show_data`.
 * - All write phases run in one Room transaction (F5) so a crash mid-import cannot leave a
 *   half-applied setlist that then blocks repair.
 */
class CatalogAssetImporter(
    private val context: Context,
    private val database: AppDatabase,
    private val prefs: UserPrefsStore,
    /** Test-only hook invoked after shows upsert, still inside the transaction. */
    private val afterShowsUpsert: (() -> Unit)? = null
) {
    private val showDao get() = database.showDao()
    private val songDao get() = database.songDao()

    data class Result(
        val imported: Boolean,
        val showCount: Int = 0,
        val songCount: Int = 0,
        val performanceCount: Int = 0,
        val skipped: String? = null
    )

    suspend fun importIfNeeded(catalogFile: File? = null): Result = withContext(Dispatchers.IO) {
        if (prefs.importedCatalogVersion() >= BUNDLED_CATALOG_VERSION) {
            return@withContext Result(imported = false, skipped = "already_imported")
        }
        val file = catalogFile ?: copyAssetToCache()
            ?: return@withContext Result(imported = false, skipped = "asset_missing")
        try {
            val result = importFromFile(file)
            prefs.setImportedCatalogVersion(BUNDLED_CATALOG_VERSION)
            result
        } catch (e: Exception) {
            Log.e(TAG, "Catalog asset import failed", e)
            Result(imported = false, skipped = "error:${e.javaClass.simpleName}")
        } finally {
            if (catalogFile == null) file.delete()
        }
    }

    private fun copyAssetToCache(): File? {
        return try {
            val out = File(context.cacheDir, "catalog_import.db")
            context.assets.open(AppDatabase.CATALOG_ASSET).use { input ->
                out.outputStream().use { input.copyTo(it) }
            }
            out
        } catch (_: IOException) {
            null
        }
    }

    private suspend fun importFromFile(file: File): Result {
        val db = SQLiteDatabase.openDatabase(
            file.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY
        )
        db.use { bundled ->
            val songs = readSongs(bundled)
            val aliases = readAliases(bundled)
            val shows = readShows(bundled)
            val performances = readPerformances(bundled)

            return database.withTransaction {
                if (songs.isNotEmpty()) songDao.insertSongs(songs)
                if (aliases.isNotEmpty()) songDao.insertAliases(aliases)
                if (shows.isNotEmpty()) {
                    shows.chunked(UPSERT_CHUNK).forEach { showDao.upsertCatalogShows(it) }
                }
                afterShowsUpsert?.invoke()

                val skipShowIds = (
                    showDao.getShowIdsWithPerformances() + showDao.getCustomShowIds()
                ).toHashSet()
                val toInsert = performances.filter { it.showId !in skipShowIds }
                if (toInsert.isNotEmpty()) {
                    toInsert.chunked(UPSERT_CHUNK).forEach { chunk ->
                        showDao.insertSongPerformances(chunk)
                    }
                }
                Result(
                    imported = true,
                    showCount = shows.size,
                    songCount = songs.size,
                    performanceCount = toInsert.size
                )
            }
        }
    }

    private fun readSongs(db: SQLiteDatabase): List<SongEntity> {
        val out = ArrayList<SongEntity>()
        db.rawQuery("SELECT id, canonicalName FROM songs", null).use { c ->
            val id = c.getColumnIndexOrThrow("id")
            val name = c.getColumnIndexOrThrow("canonicalName")
            while (c.moveToNext()) {
                out.add(SongEntity(id = c.getString(id), canonicalName = c.getString(name)))
            }
        }
        return out
    }

    private fun readAliases(db: SQLiteDatabase): List<SongAliasEntity> {
        val out = ArrayList<SongAliasEntity>()
        db.rawQuery("SELECT aliasKey, songId FROM song_aliases", null).use { c ->
            val key = c.getColumnIndexOrThrow("aliasKey")
            val songId = c.getColumnIndexOrThrow("songId")
            while (c.moveToNext()) {
                out.add(SongAliasEntity(aliasKey = c.getString(key), songId = c.getString(songId)))
            }
        }
        return out
    }

    private fun readShows(db: SQLiteDatabase): List<ShowCatalogEntity> {
        val out = ArrayList<ShowCatalogEntity>()
        db.rawQuery(
            """
            SELECT id, date, venue, city, stateOrCountry, eraId, tourName, archiveIdentifier,
                   tapeQuality, isCustomShow, tags, historicTrivia, bandName, venueAddress,
                   recreatesShowId, lastUpdatedTimestamp
            FROM shows
            """.trimIndent(),
            null
        ).use { c ->
            fun col(name: String) = c.getColumnIndexOrThrow(name)
            val id = col("id")
            val date = col("date")
            val venue = col("venue")
            val city = col("city")
            val state = col("stateOrCountry")
            val era = col("eraId")
            val tour = col("tourName")
            val archive = col("archiveIdentifier")
            val tape = col("tapeQuality")
            val custom = col("isCustomShow")
            val tags = col("tags")
            val trivia = col("historicTrivia")
            val band = col("bandName")
            val address = col("venueAddress")
            val recreates = col("recreatesShowId")
            val updated = col("lastUpdatedTimestamp")
            while (c.moveToNext()) {
                out.add(
                    ShowCatalogEntity(
                        id = c.getString(id),
                        date = c.getString(date),
                        venue = c.getString(venue),
                        city = c.getString(city),
                        stateOrCountry = c.getString(state),
                        eraId = c.getString(era),
                        tourName = c.getString(tour).orEmpty(),
                        archiveIdentifier = c.getString(archive).orEmpty(),
                        tapeQuality = c.getString(tape).orEmpty(),
                        isCustomShow = c.getInt(custom) != 0,
                        tags = c.getString(tags).orEmpty(),
                        historicTrivia = c.getString(trivia).orEmpty(),
                        bandName = c.getString(band).orEmpty().ifBlank { "Grateful Dead" },
                        venueAddress = c.getString(address).orEmpty(),
                        recreatesShowId = if (c.isNull(recreates)) null else c.getString(recreates),
                        lastUpdatedTimestamp = c.getLong(updated)
                    )
                )
            }
        }
        return out
    }

    private fun readPerformances(db: SQLiteDatabase): List<SongPerformanceEntity> {
        val out = ArrayList<SongPerformanceEntity>()
        db.rawQuery(
            """
            SELECT showId, songId, songName, setType, trackOrder, transitionOut,
                   durationMinutes, notes, isJamHighlight
            FROM song_performances
            ORDER BY showId, trackOrder
            """.trimIndent(),
            null
        ).use { c ->
            fun col(name: String) = c.getColumnIndexOrThrow(name)
            val showId = col("showId")
            val songId = col("songId")
            val songName = col("songName")
            val setType = col("setType")
            val trackOrder = col("trackOrder")
            val transition = col("transitionOut")
            val duration = col("durationMinutes")
            val notes = col("notes")
            val jam = col("isJamHighlight")
            while (c.moveToNext()) {
                out.add(
                    SongPerformanceEntity(
                        showId = c.getString(showId),
                        songId = c.getString(songId),
                        songName = c.getString(songName),
                        setType = c.getString(setType),
                        trackOrder = c.getInt(trackOrder),
                        transitionOut = c.getString(transition),
                        durationMinutes = if (c.isNull(duration)) null else c.getInt(duration),
                        notes = if (c.isNull(notes)) null else c.getString(notes),
                        isJamHighlight = c.getInt(jam) != 0
                    )
                )
            }
        }
        return out
    }

    companion object {
        private const val TAG = "CatalogAssetImporter"
        private const val UPSERT_CHUNK = 500

        /**
         * Bump when shipping a newer `catalog.db` that existing installs should re-merge.
         * Fresh installs always take the asset via createFromAsset regardless of this value.
         */
        const val BUNDLED_CATALOG_VERSION = 1
    }
}
