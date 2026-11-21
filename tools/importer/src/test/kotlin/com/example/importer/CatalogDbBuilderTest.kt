package com.example.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.sql.DriverManager

class CatalogDbBuilderTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `eraIdForDate matches DeadEra bands`() {
        assertEquals("spring_77", CatalogDbBuilder.eraIdForDate("1977-05-08"))
        assertEquals("primal_60s", CatalogDbBuilder.eraIdForDate("1969-12-31"))
        assertEquals("90s_era", CatalogDbBuilder.eraIdForDate("1995-07-09"))
    }

    @Test
    fun `builds sqlite file with room_master_table and cornell setlist`() {
        val schema = findSchemaFile()
        val out = File(tmp.root, "catalog.db")
        val shows = GdshowsYamlParser.parseYearFile(CORNELL)
        val result = CatalogDbBuilder.build(schema, shows, out)
        assertTrue(out.isFile)
        assertEquals(1, result.showCount)
        assertEquals(6, result.performanceCount)

        DriverManager.getConnection("jdbc:sqlite:${out.absolutePath}").use { conn ->
            conn.createStatement().use { st ->
                st.executeQuery("SELECT identity_hash FROM room_master_table WHERE id = 42").use { rs ->
                    assertTrue(rs.next())
                    assertEquals("161312bafd2f1c05c328fcb8a20165a9", rs.getString(1))
                }
                st.executeQuery("SELECT COUNT(*) FROM user_show_data").use { rs ->
                    assertTrue(rs.next())
                    assertEquals(0, rs.getInt(1))
                }
                st.executeQuery(
                    "SELECT songName, transitionOut FROM song_performances " +
                        "WHERE showId = '40cb4e0a-7c88-4f44-bf87-2596c174b699' ORDER BY trackOrder"
                ).use { rs ->
                    assertTrue(rs.next())
                    assertEquals("New Minglewood Blues", rs.getString(1)) // catalog canonical
                    assertEquals("NONE", rs.getString(2))
                }
                st.executeQuery("PRAGMA user_version").use { rs ->
                    assertTrue(rs.next())
                    assertEquals(11, rs.getInt(1))
                }
            }
        }
    }

    private fun findSchemaFile(): File {
        val candidates = listOf(
            File("../../app/schemas/com.example.data.local.AppDatabase/11.json"),
            File("app/schemas/com.example.data.local.AppDatabase/11.json"),
            File("../app/schemas/com.example.data.local.AppDatabase/11.json")
        )
        // Resolve from repo root when tests run from tools/importer project dir.
        val fromUserDir = File(System.getProperty("user.dir"))
        val rooted = listOf(
            File(fromUserDir, "app/schemas/com.example.data.local.AppDatabase/11.json"),
            File(fromUserDir, "../../app/schemas/com.example.data.local.AppDatabase/11.json"),
            File(fromUserDir.parentFile?.parentFile, "app/schemas/com.example.data.local.AppDatabase/11.json")
        )
        return (candidates + rooted).firstOrNull { it.isFile }
            ?: error("11.json not found (cwd=${fromUserDir.absolutePath})")
    }

    companion object {
        private val CORNELL = """
            1977/05/08:
              :uuid: 40cb4e0a-7c88-4f44-bf87-2596c174b699
              :venue: Barton Hall, Cornell University
              :city: Ithaca
              :state: NY
              :country: US
              :sets:
              - :uuid: b87cfc08-cf5d-4352-8a49-dc59052f54d2
                :songs:
                - :uuid: fb8957db-6ab7-4509-9e0b-93956ed117d0
                  :name: Minglewood Blues
                  :segued: false
                - :uuid: ae3913d2-7fa6-4e67-ba3e-34802bb6acbd
                  :name: Lazy Lightning
                  :segued: true
                - :uuid: 193b52ce-7abf-4923-bc62-6b1de725ec7d
                  :name: Supplication
                  :segued: false
              - :uuid: 9feb1c81-00dd-437a-9498-6e0a35d631a8
                :songs:
                - :uuid: c171f74a-ba46-409e-931d-6c6b38e23e9d
                  :name: Scarlet Begonias
                  :segued: true
                - :uuid: 70ff0939-572d-4e71-a606-f004577385b3
                  :name: Fire On The Mountain
                  :segued: false
              - :uuid: ac54d13e-b4a1-4118-ab04-8c7ddc29f9db
                :songs:
                - :uuid: 2244c5b5-76a1-4e28-bbac-3752e94ba0f3
                  :name: One More Saturday Night
                  :segued: false
        """.trimIndent() + "\n"
    }
}
