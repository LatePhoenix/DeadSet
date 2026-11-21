package com.example.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GdshowsYamlParserTest {

    @Test
    fun `parses Cornell 1977-05-08 fixture`() {
        val shows = GdshowsYamlParser.parseYearFile(CORNELL_FIXTURE)
        assertEquals(1, shows.size)
        val show = shows.single()
        assertEquals("1977-05-08", show.dateIso)
        assertEquals("40cb4e0a-7c88-4f44-bf87-2596c174b699", show.uuid)
        assertEquals("Barton Hall, Cornell University", show.venue)
        assertEquals("Ithaca", show.city)
        assertEquals("NY", show.state)
        assertEquals("US", show.country)
        assertEquals(3, show.sets.size)
        assertEquals(12, show.sets[0].songs.size)
        assertEquals(7, show.sets[1].songs.size)
        assertEquals(1, show.sets[2].songs.size)
        assertEquals(20, show.songCount)
        assertEquals(5, show.segueCount)

        val set1 = show.sets[0].songs
        assertEquals("Minglewood Blues", set1.first().name)
        assertFalse(set1.first().segued)
        val lazy = set1.first { it.name == "Lazy Lightning" }
        assertTrue(lazy.segued)
        assertEquals("Supplication", set1[set1.indexOf(lazy) + 1].name)

        val set2 = show.sets[1].songs
        assertEquals("Scarlet Begonias", set2[0].name)
        assertTrue(set2[0].segued)
        assertEquals("Fire On The Mountain", set2[1].name)
        assertEquals("One More Saturday Night", show.sets[2].songs.single().name)
    }

    @Test
    fun `parses same-day multi-show date suffix`() {
        val yaml = """
            1966/03/03/0:
              :uuid: aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa
              :venue: First Show
              :city: X
              :state: CA
              :country: US
              :sets:
            1966/03/03/1:
              :uuid: bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb
              :venue: Second Show
              :city: X
              :state: CA
              :country: US
              :sets:
        """.trimIndent() + "\n"
        val shows = GdshowsYamlParser.parseYearFile(yaml)
        assertEquals(2, shows.size)
        assertEquals("1966-03-03", shows[0].dateIso)
        assertEquals("1966-03-03", shows[1].dateIso)
        assertEquals("First Show", shows[0].venue)
        assertEquals("Second Show", shows[1].venue)
    }

    @Test
    fun `skips YAML document markers`() {
        val shows = GdshowsYamlParser.parseYearFile("---\n$CORNELL_FIXTURE...\n")
        assertEquals(1, shows.size)
        assertEquals("1977-05-08", shows.single().dateIso)
    }

    @Test
    fun `parses consecutive shows and empty setlist`() {
        val shows = GdshowsYamlParser.parseYearFile(TWO_SHOWS_ONE_EMPTY)
        assertEquals(2, shows.size)
        assertEquals("1977-05-08", shows[0].dateIso)
        assertEquals(20, shows[0].songCount)
        assertEquals("1977-05-09", shows[1].dateIso)
        assertEquals(0, shows[1].sets.size)
        assertEquals(0, shows[1].songCount)
    }

    @Test
    fun `setTypeNames treats last of 3+ as encore`() {
        assertEquals(listOf("SET_1"), setTypeNamesFor(1))
        assertEquals(listOf("SET_1", "SET_2"), setTypeNamesFor(2))
        assertEquals(listOf("SET_1", "SET_2", "ENCORE"), setTypeNamesFor(3))
        assertEquals(listOf("SET_1", "SET_2", "SET_3", "ENCORE"), setTypeNamesFor(4))
    }

    companion object {
        // Trimmed from gdshowsdb 1977.yaml (Barton Hall). Keep in sync with corpus shape only.
        private val CORNELL_FIXTURE = """
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
                - :uuid: 0cc886b9-cc1f-4d71-aa58-02dfab36c590
                  :name: Loser
                  :segued: false
                - :uuid: 8dc5b077-e3b5-4a28-9284-d43aefd5fe30
                  :name: El Paso
                  :segued: false
                - :uuid: 05f5a2b7-4aec-4952-a176-1b92e859fe59
                  :name: They Love Each Other
                  :segued: false
                - :uuid: d5073d3c-826e-41c5-85d9-7d00dea72c72
                  :name: Jack Straw
                  :segued: false
                - :uuid: 59f25539-0890-4918-9169-cb8566fe29e0
                  :name: Deal
                  :segued: false
                - :uuid: ae3913d2-7fa6-4e67-ba3e-34802bb6acbd
                  :name: Lazy Lightning
                  :segued: true
                - :uuid: 193b52ce-7abf-4923-bc62-6b1de725ec7d
                  :name: Supplication
                  :segued: false
                - :uuid: b6056d50-b146-4c41-9289-af63b1e9ae68
                  :name: Brown Eyed Women
                  :segued: false
                - :uuid: e89dfad4-7d76-4274-9ac1-258a56c5be89
                  :name: Mama Tried
                  :segued: false
                - :uuid: bc55b051-0657-4a7e-87ae-5c27e2d76620
                  :name: Row Jimmy
                  :segued: false
                - :uuid: e5c4e19b-8eb2-4e80-af12-8df7f6aaf106
                  :name: Dancin' In The Streets
                  :segued: false
              - :uuid: 9feb1c81-00dd-437a-9498-6e0a35d631a8
                :songs:
                - :uuid: c171f74a-ba46-409e-931d-6c6b38e23e9d
                  :name: Scarlet Begonias
                  :segued: true
                - :uuid: 70ff0939-572d-4e71-a606-f004577385b3
                  :name: Fire On The Mountain
                  :segued: false
                - :uuid: b2f59372-d345-44f3-8c2b-b6f4d46ebbdd
                  :name: Estimated Prophet
                  :segued: false
                - :uuid: 9ff34d14-145b-44c4-8ccf-0fe9a15a3934
                  :name: Saint Stephen
                  :segued: true
                - :uuid: e1c18b4d-c569-4304-89e7-da4bcb4ad12a
                  :name: Not Fade Away
                  :segued: true
                - :uuid: aaae97aa-8492-49aa-b42e-1c5f22573b1a
                  :name: Saint Stephen
                  :segued: true
                - :uuid: 61b9ce7c-6e31-4fda-94b1-12794e4131d1
                  :name: Morning Dew
                  :segued: false
              - :uuid: ac54d13e-b4a1-4118-ab04-8c7ddc29f9db
                :songs:
                - :uuid: 2244c5b5-76a1-4e28-bbac-3752e94ba0f3
                  :name: One More Saturday Night
                  :segued: false
        """.trimIndent() + "\n"

        private val TWO_SHOWS_ONE_EMPTY = CORNELL_FIXTURE + """
            1977/05/09:
              :uuid: 0172aa31-bd9f-46d7-8a24-a0caea34b864
              :venue: Buffalo Memorial Auditorium
              :city: Buffalo
              :state: NY
              :country: US
              :sets:
        """.trimIndent() + "\n"
    }
}
