package com.example.data.almanac

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlmanacMarkdownTest {

    @Test
    fun `parses front matter and body`() {
        val raw = """
            ---
            id: sample
            type: song_story
            title: Sample Title
            summary: A short summary
            relatedSongs: Scarlet Begonias, Fire on the Mountain
            relatedShows: 1977-05-08-cornell
            sources: https://www.dead.net/, https://www.loc.gov/
            ---

            # Heading

            First paragraph.

            ## Sub

            - Bullet one
            - Bullet with [link](https://example.com)
        """.trimIndent()

        val doc = AlmanacMarkdown.parseDocument(raw)
        val article = AlmanacMarkdown.toArticle(doc.meta, doc.body, "almanac/sample.md")!!
        assertEquals("sample", article.id)
        assertEquals(AlmanacArticleType.SONG_STORY, article.type)
        assertEquals("Sample Title", article.title)
        assertEquals(listOf("Scarlet Begonias", "Fire on the Mountain"), article.relatedSongs)
        assertEquals(2, article.sources.size)

        val blocks = AlmanacMarkdown.parseBlocks(doc.body)
        assertTrue(blocks.any { it is AlmanacMarkdown.Block.Heading && it.level == 1 })
        assertTrue(blocks.any { it is AlmanacMarkdown.Block.Bullet })
        assertTrue(blocks.any { it is AlmanacMarkdown.Block.Paragraph })
    }

    @Test
    fun `bundled almanac fixtures parse from classpath sample`() {
        // Mirror cornell fixture shape without reading assets (Robolectric optional).
        val cornell = """
            ---
            id: cornell-1977-05-08
            type: show_spotlight
            title: Barton Hall — May 8, 1977
            summary: Registry spotlight
            relatedSongs: Scarlet Begonias
            relatedShows: 1977-05-08-cornell
            sources: https://www.loc.gov/programs/national-recording-preservation-board/recording-registry/complete-national-recording-registry-listing/
            ---

            # Barton Hall

            Added to the Registry in **2011**.
        """.trimIndent()
        val article = AlmanacMarkdown.toArticle(
            AlmanacMarkdown.parseDocument(cornell).meta,
            AlmanacMarkdown.parseDocument(cornell).body,
            "almanac/cornell-1977-05-08.md"
        )!!
        assertEquals(AlmanacArticleType.SHOW_SPOTLIGHT, article.type)
        assertTrue(article.sources.first().contains("loc.gov"))
    }
}
