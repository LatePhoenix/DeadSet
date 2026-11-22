package com.example.importer

import com.example.data.songs.songKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.sql.DriverManager

/**
 * Full-corpus checks against a catalog built from the local gdshowsdb cache.
 * Skips when cache is missing (CI without --fetch).
 */
class CatalogIntegrityTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `built catalog has expected show counts no future dates and cornell setlist`() {
        val moduleDir = File(System.getProperty("user.dir"))
        val cacheDir = File(moduleDir, "cache")
        assumeTrue("gdshowsdb cache required", File(cacheDir, "1977.yaml").isFile)

        val schema = resolveSchemaFile(moduleDir)
        val shows = loadCachedShows(cacheDir)
        val out = File(tmp.root, "catalog.db")
        val result = CatalogDbBuilder.build(schema, shows, out)

        assertTrue("show count", result.showCount in 2300..2400)
        assertEquals(2358, result.showCount)
        assertEquals(39774, result.performanceCount)

        DriverManager.getConnection("jdbc:sqlite:${out.absolutePath}").use { conn ->
            conn.createStatement().use { st ->
                st.executeQuery("SELECT COUNT(*) FROM shows WHERE date > date('now')").use { rs ->
                    assertTrue(rs.next())
                    assertEquals(0, rs.getInt(1))
                }

                // Cornell 1977-05-08 — exact song sequence after SongResolver normalization.
                st.executeQuery(
                    "SELECT id FROM shows WHERE date = '1977-05-08' AND venue LIKE '%Cornell%'"
                ).use { rs ->
                    assertTrue("Cornell show present", rs.next())
                    val showId = rs.getString(1)
                    st.executeQuery(
                        "SELECT songName, transitionOut, setType FROM song_performances " +
                            "WHERE showId = '$showId' ORDER BY trackOrder"
                    ).use { songs ->
                        val rows = mutableListOf<List<String>>()
                        while (songs.next()) {
                            rows += listOf(songs.getString(1), songs.getString(2), songs.getString(3))
                        }
                        assertEquals(20, rows.size)
                        assertEquals(listOf("New Minglewood Blues", "NONE", "SET_1"), rows.first())
                        assertEquals(listOf("One More Saturday Night", "NONE", "ENCORE"), rows.last())
                        // Scarlet -> Fire
                        val scarletIdx = rows.indexOfFirst { it[0] == "Scarlet Begonias" }
                        assertTrue(scarletIdx >= 0)
                        assertEquals("SEGUE", rows[scarletIdx][1])
                        assertEquals("Fire on the Mountain", rows[scarletIdx + 1][0])
                    }

                    // DAO-equivalent: Scarlet segued into Fire includes Cornell.
                    val scarlet = songKey("Scarlet Begonias")
                    val fire = songKey("Fire on the Mountain")
                    st.executeQuery(
                        """
                        SELECT COUNT(*) FROM song_performances a
                        INNER JOIN song_performances b
                          ON a.showId = b.showId AND b.trackOrder = a.trackOrder + 1
                        WHERE a.showId = '$showId'
                          AND a.songId = '$scarlet' AND b.songId = '$fire'
                          AND a.transitionOut = 'SEGUE'
                        """.trimIndent()
                    ).use { rs2 ->
                        assertTrue(rs2.next())
                        assertEquals(1, rs2.getInt(1))
                    }
                }
            }
        }
    }
}
