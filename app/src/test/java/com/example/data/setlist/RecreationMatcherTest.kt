package com.example.data.setlist

import com.example.TestSeeds
import com.example.data.model.SongEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecreationMatcherTest {

    private fun songs(vararg names: String) = names.map { SongEntry(name = it) }

    @Test
    fun `identical setlists match fully`() {
        val c = RecreationMatcher.compare(songs("A", "B", "C"), songs("A", "B", "C"))
        assertEquals(1f, c.score)
        assertTrue(c.rows.all { it.matches })
    }

    @Test
    fun `skipped and extra songs are aligned, not cascaded`() {
        // Tribute skipped B and added an encore Z; aliases count as matches.
        val c = RecreationMatcher.compare(songs("A", "St. Stephen", "D", "Z"), songs("A", "B", "Saint Stephen", "D"))
        assertEquals(3, c.matchedInOrder)
        assertEquals(0.75f, c.score)
        assertEquals(
            listOf(
                ComparedRow("A", "A"), ComparedRow(null, "B"), ComparedRow("St. Stephen", "Saint Stephen"),
                ComparedRow("D", "D"), ComparedRow("Z", null)
            ),
            c.rows
        )
    }

    @Test
    fun `best match finds the recreated Grateful Dead show`() {
        val cornell = TestSeeds.seed("1977-05-08-cornell")
        // A DSO-style night: Cornell's setlist with one song dropped and one swapped.
        val tribute = cornell.songs.drop(1).toMutableList().also { it[3] = it[3].copy(name = "Something Else") }
        val best = RecreationMatcher.bestMatch(tribute, TestSeeds.shows, TestSeeds::songsFor)
        assertEquals(cornell.id, best?.first?.id)
        assertTrue(best!!.second >= 0.8f)
    }

    @Test
    fun `no suggestion for short or unrelated setlists`() {
        assertNull(RecreationMatcher.bestMatch(songs("A", "B"), TestSeeds.shows, TestSeeds::songsFor))
        assertNull(RecreationMatcher.bestMatch(songs("Q1", "Q2", "Q3", "Q4", "Q5"), TestSeeds.shows, TestSeeds::songsFor))
    }
}
