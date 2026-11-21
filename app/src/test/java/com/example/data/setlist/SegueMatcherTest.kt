package com.example.data.setlist

import com.example.data.model.SongEntry
import com.example.data.model.TransitionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SegueMatcherTest {

    private fun s(name: String, t: TransitionType = TransitionType.NONE) = SongEntry(name = name, transitionOut = t)

    private val playingIntoUjb = listOf(
        s("Playing in the Band", TransitionType.SEGUE),
        s("Uncle John's Band"),
        s("Morning Dew")
    )

    @Test
    fun `parses labels with arrows`() {
        assertEquals(listOf("Help on the Way", "Slipknot!", "Franklin's Tower"),
            SegueMatcher.parseChain("Help on the Way ➔ Slipknot! ➔ Franklin's Tower"))
        assertEquals(listOf("Scarlet", "Fire"), SegueMatcher.parseChain("Scarlet -> Fire"))
    }

    @Test
    fun `matches adjacent connected songs in order`() {
        assertTrue(SegueMatcher.matches(playingIntoUjb, "Playing in the Band ➔ Uncle John's Band"))
    }

    @Test
    fun `no longer matches shows that merely share words`() {
        // The old filter split the label into words ("the", "band", ...) and matched this.
        val unrelated = listOf(
            s("Playing in the Band"),
            s("The Music Never Stopped"),
            s("Uncle John's Band")
        )
        assertFalse(SegueMatcher.matches(unrelated, "Playing in the Band ➔ Uncle John's Band"))
    }

    @Test
    fun `requires a transition, the right order, and adjacency`() {
        val stopped = listOf(s("Scarlet Begonias"), s("Fire on the Mountain"))
        assertFalse(SegueMatcher.matches(stopped, "Scarlet Begonias ➔ Fire on the Mountain"))

        val reversed = listOf(s("Fire on the Mountain", TransitionType.SEGUE), s("Scarlet Begonias"))
        assertFalse(SegueMatcher.matches(reversed, "Scarlet Begonias ➔ Fire on the Mountain"))

        val gap = listOf(s("Scarlet Begonias", TransitionType.SEGUE), s("Drums", TransitionType.SEGUE), s("Fire on the Mountain"))
        assertFalse(SegueMatcher.matches(gap, "Scarlet Begonias ➔ Fire on the Mountain"))
    }

    @Test
    fun `any connected type counts and titles match by key`() {
        val jam = listOf(s("St. Stephen", TransitionType.JAM), s("The Eleven"))
        assertTrue(SegueMatcher.matches(jam, "Saint Stephen ➔ The Eleven"))
    }

    @Test
    fun `three-song chains need every link`() {
        val full = listOf(
            s("Help on the Way", TransitionType.SEGUE),
            s("Slipknot!", TransitionType.SEGUE),
            s("Franklin's Tower")
        )
        assertTrue(SegueMatcher.matches(full, "Help on the Way ➔ Slipknot! ➔ Franklin's Tower"))
        val broken = full.toMutableList().also { it[1] = s("Slipknot!") }
        assertFalse(SegueMatcher.matches(broken, "Help on the Way ➔ Slipknot! ➔ Franklin's Tower"))
    }

    @Test
    fun `single-song or empty labels never match`() {
        assertFalse(SegueMatcher.matches(playingIntoUjb, "Playing in the Band"))
        assertFalse(SegueMatcher.matches(emptyList(), "A ➔ B"))
    }
}
