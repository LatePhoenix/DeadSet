package com.example.data.almanac

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlmanacLinkIndexTest {

    @Test
    fun `indexes songs shows and month-day from articles`() {
        val articles = listOf(
            article(
                id = "cornell-1977-05-08",
                type = AlmanacArticleType.SHOW_SPOTLIGHT,
                songs = listOf("Scarlet Begonias"),
                shows = listOf("1977-05-08-cornell")
            ),
            article(
                id = "scarlet-fire",
                type = AlmanacArticleType.SONG_STORY,
                songs = listOf("Scarlet Begonias", "Fire on the Mountain"),
                shows = emptyList()
            )
        )
        val index = AlmanacLinkIndex(articles)
        assertEquals("scarlet-fire", index.articleForSong("Scarlet Begonias"))
        assertEquals("scarlet-fire", index.articleForSong("Fire on the Mountain"))
        assertEquals("cornell-1977-05-08", index.articleForShowId("1977-05-08-cornell"))
        assertEquals("cornell-1977-05-08", index.articleForIsoDate("1977-05-08"))
        assertNull(index.articleForSong("Ripple"))
    }

    @Test
    fun `curated trivia maps include known article ids`() {
        val linked = com.example.data.seed.DeadTriviaSeed.getAllAvailableQuestions(
            emptyList()
        ) { emptyList() }
            .filter { !it.relatedArticleId.isNullOrBlank() }
        assertEquals(true, linked.any { it.id == "q_cornell_77_venue" && it.relatedArticleId == "cornell-1977-05-08" })
        assertEquals(true, linked.any { it.id == "tf_scarlet_fire_76" && it.relatedArticleId == "scarlet-fire" })
    }

    private fun article(
        id: String,
        type: AlmanacArticleType,
        songs: List<String>,
        shows: List<String>
    ) = AlmanacArticle(
        id = id,
        type = type,
        title = id,
        summary = "",
        relatedSongs = songs,
        relatedShows = shows,
        sources = emptyList(),
        bodyMarkdown = "",
        assetPath = "almanac/$id.md"
    )
}
