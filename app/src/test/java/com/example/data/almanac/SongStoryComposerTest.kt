package com.example.data.almanac

import com.example.data.local.ShowEntity
import com.example.data.model.SetType
import com.example.data.model.SongEntry
import com.example.data.model.TransitionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SongStoryComposerTest {

    @Test
    fun `panels use sourced writers and catalog play counts`() {
        val article = AlmanacArticle(
            id = "scarlet-fire",
            type = AlmanacArticleType.SONG_STORY,
            title = "Scarlet → Fire",
            summary = "",
            relatedSongs = listOf("Scarlet Begonias", "Fire on the Mountain"),
            relatedShows = emptyList(),
            sources = emptyList(),
            bodyMarkdown = "",
            assetPath = "almanac/scarlet-fire.md"
        )
        val shows = listOf(
            show("a", "1977-03-18"),
            show("b", "1977-05-08")
        )
        val songsFor: (ShowEntity) -> List<SongEntry> = { show ->
            when (show.id) {
                "a", "b" -> listOf(
                    SongEntry("Scarlet Begonias", SetType.SET_2, 0, TransitionType.SEGUE),
                    SongEntry("Fire on the Mountain", SetType.SET_2, 1, TransitionType.NONE)
                )
                else -> emptyList()
            }
        }
        val panels = SongStoryComposer.panelsFor(article, shows, songsFor)
        assertEquals(2, panels.size)
        val scarlet = panels.first { it.songName == "Scarlet Begonias" }
        assertEquals("Garcia / Hunter", scarlet.writers)
        assertNull(scarlet.debutDate)
        assertEquals(2, scarlet.timesPlayedInCatalog)
        assertEquals("1977-03-18", scarlet.firstCatalogDate)
        assertEquals("1977-05-08", scarlet.lastCatalogDate)
        assertTrue(scarlet.topOutOf.any { it.startsWith("Fire on the Mountain") })

        val fire = panels.first { it.songName == "Fire on the Mountain" }
        assertEquals("Hart / Hunter", fire.writers)
        assertEquals("1977-03-18", fire.debutDate)
        assertTrue(fire.sourceUrl!!.contains("dead.net"))
    }

    @Test
    fun `every song story fact has a source url`() {
        assertTrue(SongStoryFacts.all.isNotEmpty())
        SongStoryFacts.all.forEach {
            assertTrue(it.canonicalName, it.sourceUrl.startsWith("https://"))
        }
    }

    private fun show(id: String, date: String) = ShowEntity(
        id = id,
        date = date,
        venue = "Test",
        city = "SF",
        stateOrCountry = "CA",
        eraId = "",
        tourName = "",
        bandName = "Grateful Dead"
    )
}
