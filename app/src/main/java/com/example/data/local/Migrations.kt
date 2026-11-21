package com.example.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.songs.SongCatalog
import com.example.data.songs.SongResolver
import com.example.data.songs.songKey
import org.json.JSONArray
import org.json.JSONObject

/**
 * Rewrites every `shows.setlistJson` entry in place with [transform].
 *
 * Migrations must not parse setlists through the app's current model classes (`SongEntry` via
 * `Converters`): those classes change over time, so an old migration would silently drop fields
 * that a later migration still needs to read (e.g. `isSegue` before 4→5). Raw org.json keeps
 * every key that [transform] doesn't touch. Unparseable JSON is left as is.
 */
internal fun rewriteSetlistJson(db: SupportSQLiteDatabase, transform: (JSONObject) -> Unit) {
    val updates = mutableListOf<Pair<String, String>>()
    db.query("SELECT id, setlistJson FROM shows").use { c ->
        while (c.moveToNext()) {
            val id = c.getString(0)
            val original = c.getString(1)
            val array = runCatching { JSONArray(original) }.getOrNull() ?: continue
            for (i in 0 until array.length()) {
                array.optJSONObject(i)?.let(transform)
            }
            val rewritten = array.toString()
            if (rewritten != original) updates += id to rewritten
        }
    }
    for ((id, json) in updates) {
        db.execSQL("UPDATE shows SET setlistJson = ? WHERE id = ?", arrayOf<Any?>(json, id))
    }
}

/**
 * 3 → 4: add `songs` + `song_aliases`, normalize every stored song title, and give each
 * `song_performances` row a `songId`.
 *
 * - Catalog songs and aliases are inserted first, so "St. Stephen" in an old setlist resolves to
 *   the same song as "Saint Stephen".
 * - Titles not in the catalog (user-entered songs) become new songs under their cleaned name.
 * - Both `shows.setlistJson` and `song_performances.songName` are rewritten to canonical names so
 *   search and stats agree. All other user data (attendance, reviews, ratings, segue flags) is
 *   copied unchanged.
 * - `song_performances` is rebuilt because SQLite can't add a NOT NULL foreign-key column in place.
 *
 * SQL below is copied from schemas/.../4.json; keep them identical.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `songs` (`id` TEXT NOT NULL, `canonicalName` TEXT NOT NULL, " +
                "PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `song_aliases` (`aliasKey` TEXT NOT NULL, `songId` TEXT NOT NULL, " +
                "PRIMARY KEY(`aliasKey`), FOREIGN KEY(`songId`) REFERENCES `songs`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_aliases_songId` ON `song_aliases` (`songId`)")

        val resolver = SongResolver.fromCatalog()
        insertCatalog(db)

        // Normalize titles inside each show's setlistJson. Only the "name" key is touched.
        rewriteSetlistJson(db) { song ->
            if (song.has("name")) song.put("name", resolver.resolve(song.optString("name")).canonicalName)
        }

        // Rebuild song_performances with songId.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `song_performances_new` (`performanceId` INTEGER PRIMARY KEY " +
                "AUTOINCREMENT NOT NULL, `showId` TEXT NOT NULL, `songId` TEXT NOT NULL, `songName` TEXT NOT NULL, " +
                "`setType` TEXT NOT NULL, `trackOrder` INTEGER NOT NULL, `isSegue` INTEGER NOT NULL, " +
                "`durationMinutes` INTEGER, `notes` TEXT, `isJamHighlight` INTEGER NOT NULL, " +
                "FOREIGN KEY(`showId`) REFERENCES `shows`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`songId`) REFERENCES `songs`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )"
        )
        val rows = mutableListOf<Array<Any?>>()
        db.query(
            "SELECT performanceId, showId, songName, setType, trackOrder, isSegue, durationMinutes, " +
                "notes, isJamHighlight FROM song_performances"
        ).use { c ->
            while (c.moveToNext()) {
                val resolved = resolver.resolve(c.getString(2))
                rows += arrayOf(
                    c.getLong(0),
                    c.getString(1),
                    resolved.songId,
                    resolved.canonicalName,
                    c.getString(3),
                    c.getInt(4),
                    c.getInt(5),
                    if (c.isNull(6)) null else c.getInt(6),
                    if (c.isNull(7)) null else c.getString(7),
                    c.getInt(8)
                )
            }
        }
        // Songs first, so the songId foreign key is satisfied.
        insertResolvedSongs(db, resolver)
        for (row in rows) {
            db.execSQL(
                "INSERT INTO song_performances_new (performanceId, showId, songId, songName, setType, " +
                    "trackOrder, isSegue, durationMinutes, notes, isJamHighlight) VALUES (?,?,?,?,?,?,?,?,?,?)",
                row
            )
        }
        db.execSQL("DROP TABLE song_performances")
        db.execSQL("ALTER TABLE song_performances_new RENAME TO song_performances")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_performances_showId` ON `song_performances` (`showId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_performances_songName` ON `song_performances` (`songName`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_performances_songId` ON `song_performances` (`songId`)")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_song_performances_showId_trackOrder` " +
                "ON `song_performances` (`showId`, `trackOrder`)"
        )
    }

    private fun insertCatalog(db: SupportSQLiteDatabase) {
        for (song in SongCatalog.songs) {
            db.execSQL(
                "INSERT OR IGNORE INTO songs (id, canonicalName) VALUES (?, ?)",
                arrayOf<Any?>(songKey(song.canonicalName), song.canonicalName)
            )
        }
        for ((aliasKey, canonical) in SongCatalog.canonicalByKey) {
            db.execSQL(
                "INSERT OR IGNORE INTO song_aliases (aliasKey, songId) VALUES (?, ?)",
                arrayOf<Any?>(aliasKey, songKey(canonical))
            )
        }
    }

    private fun insertResolvedSongs(db: SupportSQLiteDatabase, resolver: SongResolver) {
        for ((id, name) in resolver.newSongs) {
            db.execSQL("INSERT OR IGNORE INTO songs (id, canonicalName) VALUES (?, ?)", arrayOf<Any?>(id, name))
        }
        for ((aliasKey, songId) in resolver.newAliases) {
            db.execSQL(
                "INSERT OR IGNORE INTO song_aliases (aliasKey, songId) VALUES (?, ?)",
                arrayOf<Any?>(aliasKey, songId)
            )
        }
        resolver.newSongs.clear()
        resolver.newAliases.clear()
    }
}

/**
 * 4 → 5: replace the `isSegue` boolean with `transitionOut` ([com.example.data.model.TransitionType]
 * name) in both `song_performances` and `shows.setlistJson`.
 *
 * Every existing `isSegue = true` becomes `SEGUE`: the app has always described the flag as a
 * seamless `->`. Finer distinctions (`>` transition, `~>` jam) are new and start from `NONE`.
 *
 * SQL below is copied from schemas/.../5.json; keep them identical.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        rewriteSetlistJson(db) { song ->
            if (song.has("isSegue")) {
                val wasSegue = song.optBoolean("isSegue", false)
                song.remove("isSegue")
                if (!song.has("transitionOut")) song.put("transitionOut", if (wasSegue) "SEGUE" else "NONE")
            }
        }

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `song_performances_new` (`performanceId` INTEGER PRIMARY KEY " +
                "AUTOINCREMENT NOT NULL, `showId` TEXT NOT NULL, `songId` TEXT NOT NULL, `songName` TEXT NOT NULL, " +
                "`setType` TEXT NOT NULL, `trackOrder` INTEGER NOT NULL, `transitionOut` TEXT NOT NULL, " +
                "`durationMinutes` INTEGER, `notes` TEXT, `isJamHighlight` INTEGER NOT NULL, " +
                "FOREIGN KEY(`showId`) REFERENCES `shows`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`songId`) REFERENCES `songs`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )"
        )
        db.execSQL(
            "INSERT INTO song_performances_new (performanceId, showId, songId, songName, setType, trackOrder, " +
                "transitionOut, durationMinutes, notes, isJamHighlight) " +
                "SELECT performanceId, showId, songId, songName, setType, trackOrder, " +
                "CASE WHEN isSegue != 0 THEN 'SEGUE' ELSE 'NONE' END, durationMinutes, notes, isJamHighlight " +
                "FROM song_performances"
        )
        db.execSQL("DROP TABLE song_performances")
        db.execSQL("ALTER TABLE song_performances_new RENAME TO song_performances")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_performances_showId` ON `song_performances` (`showId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_performances_songName` ON `song_performances` (`songName`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_performances_songId` ON `song_performances` (`songId`)")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_song_performances_showId_trackOrder` " +
                "ON `song_performances` (`showId`, `trackOrder`)"
        )
    }
}

/**
 * 5 → 6: `song_performances` becomes the only copy of each setlist. Drops `shows.setlistJson`,
 * and drops `shows.isUpcoming` (now computed from the date, see [isUpcoming]).
 *
 * 1. Backfill: any show with setlist JSON but no performance rows gets rows from its JSON first,
 *    so dropping the column can't lose a setlist. Titles resolve through the existing
 *    `song_aliases`, like a normal save.
 * 2. Rebuild `shows` without the two columns. Dropping a parent table with foreign keys enabled
 *    would CASCADE-delete every performance row, so performances are parked in a constraint-free
 *    temp table first and restored after.
 *
 * SQL below is copied from schemas/.../6.json; keep them identical.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        backfillPerformancesFromJson(db)
        reconcileTransitionsFromJson(db)

        db.execSQL("CREATE TABLE `song_performances_backup` AS SELECT * FROM `song_performances`")
        db.execSQL("DROP TABLE `song_performances`")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `shows_new` (`id` TEXT NOT NULL, `date` TEXT NOT NULL, `venue` TEXT NOT NULL, " +
                "`city` TEXT NOT NULL, `stateOrCountry` TEXT NOT NULL, `eraId` TEXT NOT NULL, `tourName` TEXT NOT NULL, " +
                "`isAttended` INTEGER NOT NULL, `userRating` REAL NOT NULL, `userReview` TEXT NOT NULL, " +
                "`attendedWith` TEXT NOT NULL, `favoriteSong` TEXT NOT NULL, `archiveIdentifier` TEXT NOT NULL, " +
                "`tapeQuality` TEXT NOT NULL, `isCustomShow` INTEGER NOT NULL, `tags` TEXT NOT NULL, " +
                "`historicTrivia` TEXT NOT NULL, `bandName` TEXT NOT NULL, `venueAddress` TEXT NOT NULL, " +
                "`lastUpdatedTimestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        val showColumns = "id, date, venue, city, stateOrCountry, eraId, tourName, isAttended, userRating, " +
            "userReview, attendedWith, favoriteSong, archiveIdentifier, tapeQuality, isCustomShow, tags, " +
            "historicTrivia, bandName, venueAddress, lastUpdatedTimestamp"
        db.execSQL("INSERT INTO `shows_new` ($showColumns) SELECT $showColumns FROM `shows`")
        db.execSQL("DROP TABLE `shows`")
        db.execSQL("ALTER TABLE `shows_new` RENAME TO `shows`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shows_date` ON `shows` (`date`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shows_venue` ON `shows` (`venue`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shows_city` ON `shows` (`city`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shows_eraId` ON `shows` (`eraId`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `song_performances` (`performanceId` INTEGER PRIMARY KEY " +
                "AUTOINCREMENT NOT NULL, `showId` TEXT NOT NULL, `songId` TEXT NOT NULL, `songName` TEXT NOT NULL, " +
                "`setType` TEXT NOT NULL, `trackOrder` INTEGER NOT NULL, `transitionOut` TEXT NOT NULL, " +
                "`durationMinutes` INTEGER, `notes` TEXT, `isJamHighlight` INTEGER NOT NULL, " +
                "FOREIGN KEY(`showId`) REFERENCES `shows`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`songId`) REFERENCES `songs`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )"
        )
        val perfColumns = "performanceId, showId, songId, songName, setType, trackOrder, transitionOut, " +
            "durationMinutes, notes, isJamHighlight"
        db.execSQL(
            "INSERT INTO `song_performances` ($perfColumns) SELECT $perfColumns FROM `song_performances_backup`"
        )
        db.execSQL("DROP TABLE `song_performances_backup`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_performances_showId` ON `song_performances` (`showId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_performances_songName` ON `song_performances` (`songName`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_performances_songId` ON `song_performances` (`songId`)")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_song_performances_showId_trackOrder` " +
                "ON `song_performances` (`showId`, `trackOrder`)"
        )
    }

    /**
     * v5 wrote `setlistJson` first and `song_performances` second, so an interrupted edit could
     * leave a row with an older transition than the JSON the app displayed. Before the JSON goes,
     * copy each JSON `transitionOut` onto the matching row (same `trackOrder`) where they differ.
     * Only done when JSON and rows have the same length, so a mismatch can't misalign songs.
     * `performanceId`s are untouched.
     */
    private fun reconcileTransitionsFromJson(db: SupportSQLiteDatabase) {
        val shows = mutableListOf<Pair<String, String>>()
        db.query(
            "SELECT id, setlistJson FROM shows WHERE EXISTS " +
                "(SELECT 1 FROM song_performances sp WHERE sp.showId = shows.id)"
        ).use { c -> while (c.moveToNext()) shows += c.getString(0) to c.getString(1) }
        for ((showId, json) in shows) {
            val array = runCatching { JSONArray(json) }.getOrNull() ?: continue
            val rows = mutableListOf<Pair<Int, String>>() // trackOrder to transitionOut
            db.query(
                "SELECT trackOrder, transitionOut FROM song_performances WHERE showId = ? ORDER BY trackOrder",
                arrayOf<Any?>(showId)
            ).use { c -> while (c.moveToNext()) rows += c.getInt(0) to c.getString(1) }
            if (rows.size != array.length()) continue
            rows.forEachIndexed { i, (trackOrder, rowTransition) ->
                val jsonTransition = array.optJSONObject(i)?.optString("transitionOut", "")?.ifBlank { null }
                    ?: return@forEachIndexed
                if (jsonTransition != rowTransition) {
                    db.execSQL(
                        "UPDATE song_performances SET transitionOut = ? WHERE showId = ? AND trackOrder = ?",
                        arrayOf<Any?>(jsonTransition, showId, trackOrder)
                    )
                }
            }
        }
    }

    private fun backfillPerformancesFromJson(db: SupportSQLiteDatabase) {
        val orphans = mutableListOf<Pair<String, String>>()
        db.query(
            "SELECT id, setlistJson FROM shows WHERE NOT EXISTS " +
                "(SELECT 1 FROM song_performances sp WHERE sp.showId = shows.id)"
        ).use { c -> while (c.moveToNext()) orphans += c.getString(0) to c.getString(1) }
        if (orphans.isEmpty()) return

        val aliases = mutableMapOf<String, String>()
        db.query("SELECT aliasKey, songId FROM song_aliases").use { c ->
            while (c.moveToNext()) aliases[c.getString(0)] = c.getString(1)
        }
        val names = mutableMapOf<String, String>()
        db.query("SELECT id, canonicalName FROM songs").use { c ->
            while (c.moveToNext()) names[c.getString(0)] = c.getString(1)
        }
        val resolver = SongResolver(aliases, names)

        val rows = mutableListOf<Array<Any?>>()
        for ((showId, json) in orphans) {
            val array = runCatching { JSONArray(json) }.getOrNull() ?: continue
            for (i in 0 until array.length()) {
                val song = array.optJSONObject(i) ?: continue
                val rawName = song.optString("name").ifBlank { continue }
                val resolved = resolver.resolve(rawName)
                rows += arrayOf(
                    showId,
                    resolved.songId,
                    resolved.canonicalName,
                    song.optString("setType", "SET_1").ifBlank { "SET_1" },
                    i,
                    song.optString("transitionOut", "NONE").ifBlank { "NONE" },
                    if (song.has("durationMinutes") && !song.isNull("durationMinutes")) song.optInt("durationMinutes") else null,
                    if (song.has("notes") && !song.isNull("notes")) song.optString("notes") else null,
                    if (song.optBoolean("isJamHighlight", false)) 1 else 0
                )
            }
        }
        for ((id, name) in resolver.newSongs) {
            db.execSQL("INSERT OR IGNORE INTO songs (id, canonicalName) VALUES (?, ?)", arrayOf<Any?>(id, name))
        }
        for ((key, id) in resolver.newAliases) {
            db.execSQL("INSERT OR IGNORE INTO song_aliases (aliasKey, songId) VALUES (?, ?)", arrayOf<Any?>(key, id))
        }
        for (row in rows) {
            db.execSQL(
                "INSERT INTO song_performances (showId, songId, songName, setType, trackOrder, transitionOut, " +
                    "durationMinutes, notes, isJamHighlight) VALUES (?,?,?,?,?,?,?,?,?)",
                row
            )
        }
    }
}
