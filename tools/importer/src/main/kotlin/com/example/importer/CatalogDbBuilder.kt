package com.example.importer

import com.example.data.songs.SongCatalog
import com.example.data.songs.SongResolver
import com.example.data.songs.songKey
import org.json.JSONObject
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

/**
 * Builds a Room-compatible `catalog.db` from the exported schema JSON + parsed gdshowsdb shows.
 *
 * - Creates tables/indices/views/`room_master_table` from schema (same identity hash as Room).
 * - Populates `songs`, `song_aliases`, `shows`, `song_performances`.
 * - Leaves `user_show_data` empty (user data must never come from the asset).
 * - Other user/runtime tables (review_cards, bands, events, chase_songs) are created empty.
 *
 * Merge policy for existing installs (documented for owner): upsert catalog tables by primary key;
 * never DELETE/UPDATE `user_show_data`. Fresh installs use Room `createFromAsset`.
 */
object CatalogDbBuilder {

    data class BuildResult(
        val showCount: Int,
        val songCount: Int,
        val performanceCount: Int,
        val output: File
    )

    fun build(
        schemaJsonFile: File,
        shows: List<ParsedShow>,
        outputFile: File
    ): BuildResult {
        outputFile.parentFile?.mkdirs()
        if (outputFile.exists()) outputFile.delete()

        val schema = JSONObject(schemaJsonFile.readText()).getJSONObject("database")
        val version = schema.getInt("version")
        val identityHash = schema.getString("identityHash")

        DriverManager.getConnection("jdbc:sqlite:${outputFile.absolutePath}").use { conn ->
            conn.autoCommit = false
            conn.createStatement().use { it.execute("PRAGMA foreign_keys = ON") }
            createSchema(conn, schema)
            val counts = populate(conn, shows)
            conn.createStatement().use { st ->
                st.execute(
                    "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)"
                )
                st.execute(
                    "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, '$identityHash')"
                )
                st.execute("PRAGMA user_version = $version")
            }
            conn.commit()
            return BuildResult(
                showCount = counts.shows,
                songCount = counts.songs,
                performanceCount = counts.performances,
                output = outputFile
            )
        }
    }

    private data class Counts(val shows: Int, val songs: Int, val performances: Int)

    private fun createSchema(conn: Connection, schema: JSONObject) {
        val entities = schema.getJSONArray("entities")
        // Create songs before aliases/performances; shows before user_show_data/performances.
        val byName = (0 until entities.length()).associate { i ->
            val e = entities.getJSONObject(i)
            e.getString("tableName") to e
        }
        val order = listOf(
            "songs", "song_aliases", "shows", "user_show_data", "song_performances",
            "review_cards", "bands", "events", "chase_songs"
        )
        for (name in order) {
            val entity = byName[name] ?: continue
            execCreate(conn, entity.getString("createSql").replace("\${TABLE_NAME}", name))
            val indices = entity.optJSONArray("indices") ?: continue
            for (i in 0 until indices.length()) {
                val idx = indices.getJSONObject(i)
                execCreate(conn, idx.getString("createSql").replace("\${TABLE_NAME}", name))
            }
        }
        val views = schema.optJSONArray("views") ?: return
        for (i in 0 until views.length()) {
            val view = views.getJSONObject(i)
            val viewName = view.getString("viewName")
            execCreate(conn, view.getString("createSql").replace("\${VIEW_NAME}", viewName))
        }
    }

    private fun execCreate(conn: Connection, sql: String) {
        conn.createStatement().use { it.execute(sql) }
    }

    private fun populate(conn: Connection, shows: List<ParsedShow>): Counts {
        val resolver = SongResolver.fromCatalog()
        // Seed catalog aliases first so resolver maps variants during import.
        conn.prepareStatement("INSERT OR IGNORE INTO songs (id, canonicalName) VALUES (?, ?)").use { ps ->
            for (song in SongCatalog.songs) {
                ps.setString(1, songKey(song.canonicalName))
                ps.setString(2, song.canonicalName)
                ps.addBatch()
            }
            ps.executeBatch()
        }
        conn.prepareStatement("INSERT OR IGNORE INTO song_aliases (aliasKey, songId) VALUES (?, ?)").use { ps ->
            for ((aliasKey, canonical) in SongCatalog.canonicalByKey) {
                ps.setString(1, aliasKey)
                ps.setString(2, songKey(canonical))
                ps.addBatch()
            }
            ps.executeBatch()
        }

        val insertShow = conn.prepareStatement(
            "INSERT OR REPLACE INTO shows (id, date, venue, city, stateOrCountry, eraId, tourName, " +
                "archiveIdentifier, tapeQuality, isCustomShow, tags, historicTrivia, bandName, " +
                "venueAddress, recreatesShowId, lastUpdatedTimestamp) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
        )
        val insertPerf = conn.prepareStatement(
            "INSERT INTO song_performances (showId, songId, songName, setType, trackOrder, " +
                "transitionOut, durationMinutes, notes, isJamHighlight) " +
                "VALUES (?,?,?,?,?,?,NULL,NULL,0)"
        )
        val insertSong = conn.prepareStatement(
            "INSERT OR IGNORE INTO songs (id, canonicalName) VALUES (?, ?)"
        )
        val insertAlias = conn.prepareStatement(
            "INSERT OR IGNORE INTO song_aliases (aliasKey, songId) VALUES (?, ?)"
        )

        var performances = 0
        val now = System.currentTimeMillis()
        for (show in shows) {
            insertShow.setString(1, show.uuid)
            insertShow.setString(2, show.dateIso)
            insertShow.setString(3, show.venue)
            insertShow.setString(4, show.city)
            insertShow.setString(5, stateOrCountry(show))
            insertShow.setString(6, eraIdForDate(show.dateIso))
            insertShow.setString(7, "")
            insertShow.setString(8, "")
            insertShow.setString(9, "")
            insertShow.setInt(10, 0)
            insertShow.setString(11, "")
            insertShow.setString(12, "")
            insertShow.setString(13, "Grateful Dead")
            insertShow.setString(14, "")
            insertShow.setObject(15, null)
            insertShow.setLong(16, now)
            insertShow.addBatch()

            val setTypes = setTypeNamesFor(show.sets.size)
            var trackOrder = 0
            show.sets.forEachIndexed { setIndex, set ->
                val setType = setTypes.getOrElse(setIndex) { "SET_1" }
                for (song in set.songs) {
                    val resolved = resolver.resolve(song.name)
                    insertSong.setString(1, resolved.songId)
                    insertSong.setString(2, resolved.canonicalName)
                    insertSong.addBatch()
                    insertPerf.setString(1, show.uuid)
                    insertPerf.setString(2, resolved.songId)
                    insertPerf.setString(3, resolved.canonicalName)
                    insertPerf.setString(4, setType)
                    insertPerf.setInt(5, trackOrder++)
                    insertPerf.setString(6, if (song.segued) "SEGUE" else "NONE")
                    insertPerf.addBatch()
                    performances++
                }
            }
        }
        // Persist any new songs/aliases discovered while resolving titles.
        for ((id, name) in resolver.newSongs) {
            insertSong.setString(1, id)
            insertSong.setString(2, name)
            insertSong.addBatch()
        }
        for ((aliasKey, songId) in resolver.newAliases) {
            insertAlias.setString(1, aliasKey)
            insertAlias.setString(2, songId)
            insertAlias.addBatch()
        }

        insertShow.executeBatch()
        insertSong.executeBatch()
        insertAlias.executeBatch()
        insertPerf.executeBatch()
        insertShow.close()
        insertSong.close()
        insertAlias.close()
        insertPerf.close()

        var songCount = 0
        conn.createStatement().use { st ->
            st.executeQuery("SELECT COUNT(*) FROM songs").use { rs ->
                rs.next()
                songCount = rs.getInt(1)
            }
        }
        return Counts(shows = shows.size, songs = songCount, performances = performances)
    }

    fun eraIdForDate(dateIso: String): String {
        val year = dateIso.take(4).toIntOrNull() ?: return "all"
        return when {
            year <= 1969 -> "primal_60s"
            year <= 1972 -> "early_70s"
            year <= 1976 -> "wall_of_sound"
            year <= 1979 -> "spring_77"
            year <= 1990 -> "brent_80s"
            else -> "90s_era"
        }
    }

    fun stateOrCountry(show: ParsedShow): String = when {
        show.state.isNotBlank() -> show.state
        show.country.isNotBlank() -> show.country
        else -> ""
    }
}
