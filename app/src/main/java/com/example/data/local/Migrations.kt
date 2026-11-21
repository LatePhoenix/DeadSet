package com.example.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.songs.SongCatalog
import com.example.data.songs.SongResolver
import com.example.data.songs.songKey

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

        // Normalize titles inside each show's setlistJson.
        val converters = Converters()
        val showUpdates = mutableListOf<Pair<String, String>>()
        db.query("SELECT id, setlistJson FROM shows").use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0)
                val json = c.getString(1)
                val songs = converters.toSongList(json)
                val normalized = songs.map { it.copy(name = resolver.resolve(it.name).canonicalName) }
                if (normalized != songs) showUpdates += id to converters.fromSongList(normalized)
            }
        }
        for ((id, json) in showUpdates) {
            db.execSQL("UPDATE shows SET setlistJson = ? WHERE id = ?", arrayOf<Any?>(json, id))
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
