package com.example.data.setlist

import com.example.TestSeeds
import com.example.data.local.ShowEntity
import com.example.data.model.SetType
import com.example.data.model.SongEntry
import com.example.data.model.TransitionType
import com.example.data.model.TransitionType.JAM
import com.example.data.model.TransitionType.NONE
import com.example.data.model.TransitionType.SEGUE
import com.example.data.model.TransitionType.TRANSITION
import com.example.ui.screens.familyEvidence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SetlistAnalyticsTest {

    private fun show(id: String, date: String) = ShowEntity(
        id = id, date = date, venue = "V", city = "C", stateOrCountry = "S", eraId = "all", tourName = "T"
    )

    private fun s(name: String, t: TransitionType = NONE, set: SetType = SetType.SET_2) =
        SongEntry(name = name, setType = set, transitionOut = t)

    private val a = show("a", "1977-05-08")
    private val b = show("b", "1978-07-08")
    private val c = show("c", "1990-03-29")
    private val setlists = mapOf(
        "a" to listOf(s("Scarlet Begonias", SEGUE), s("Fire on the Mountain"), s("Estimated Prophet", SEGUE), s("Eyes of the World")),
        "b" to listOf(s("Scarlet Begonias", TRANSITION), s("Fire on the Mountain"), s("Drums", SEGUE), s("Space", JAM), s("The Wheel")),
        "c" to listOf(s("Scarlet Begonias"), s("Fire on the Mountain"), s("Drums", SEGUE), s("Space", SEGUE), s("Morning Dew"))
    )
    private val shows = listOf(c, a, b)
    private val songsFor = { sh: ShowEntity -> setlists[sh.id].orEmpty() }

    @Test
    fun `family stats count only connected occurrences and span their dates`() {
        val byId = SetlistAnalytics.familyStats(shows, songsFor).associateBy { it.family.id }
        val scarlet = byId.getValue("scarlet-fire")
        assertEquals(2, scarlet.occurrences) // show c stops between them
        assertEquals("1977-05-08", scarlet.firstDate)
        assertEquals("1978-07-08", scarlet.lastDate)
        assertEquals(listOf("a", "b"), scarlet.showIds)
        assertEquals(2, byId.getValue("drums-space").occurrences)
        assertEquals(0, byId.getValue("china-rider").occurrences)
    }

    @Test
    fun `song flow lists where a song comes from and goes to, with shares`() {
        val flow = SetlistAnalytics.songFlow("Space", shows, songsFor)
        assertEquals(2, flow.timesPlayed)
        assertEquals(listOf(SongLink("Drums", 2, 1f)), flow.into)
        assertEquals(listOf("Morning Dew", "The Wheel"), flow.outOf.map { it.songName })
        assertEquals(listOf(0.5f, 0.5f), flow.outOf.map { it.share })
    }

    @Test
    fun `song flow ignores stops and resolves aliases`() {
        val flow = SetlistAnalytics.songFlow("Scarlet", shows, songsFor) // "Scarlet" is a catalog alias
        assertEquals(3, flow.timesPlayed)
        assertEquals(listOf(SongLink("Fire on the Mountain", 2, 1f)), flow.outOf)
        assertTrue(flow.into.isEmpty())
    }

    @Test
    fun `notation uses commas for stops and symbols for transitions`() {
        assertEquals(
            "Drums -> Space ~> The Wheel",
            SetlistAnalytics.notation(listOf(s("Drums", SEGUE), s("Space", JAM), s("The Wheel")))
        )
        assertEquals(
            "Scarlet Begonias > Fire on the Mountain, Ripple",
            SetlistAnalytics.notation(listOf(s("Scarlet Begonias", TRANSITION), s("Fire on the Mountain"), s("Ripple")))
        )
    }

    @Test
    fun `notation groups by set in set order`() {
        val songs = listOf(s("Ripple", set = SetType.ENCORE), s("Bertha", set = SetType.SET_1), s("Deal", set = SetType.SET_1))
        assertEquals(
            listOf(SetType.SET_1 to "Bertha, Deal", SetType.ENCORE to "Ripple"),
            SetlistAnalytics.notationBySet(songs)
        )
    }

    @Test
    fun `family evidence text matches the counts`() {
        val stats = SetlistAnalytics.familyStats(shows, songsFor).associateBy { it.family.id }
        assertEquals("2 shows in your catalog · 1977–1978", familyEvidence(stats.getValue("scarlet-fire")))
        assertEquals("1 show in your catalog · 1977-05-08", familyEvidence(stats.getValue("estimated-eyes")))
        assertEquals("Not in your catalog yet", familyEvidence(stats.getValue("china-rider")))
    }

    @Test
    fun `every family chain uses canonical titles and parses back to itself`() {
        for (family in SegueFamilies.all) {
            assertEquals(family.id, family.chain, SegueMatcher.parseChain(family.label))
            assertTrue(family.id, family.chain.size >= 2)
        }
        assertEquals(SegueFamilies.all.size, SegueFamilies.all.map { it.id }.toSet().size)
    }

    @Test
    fun `transition graph keeps the busiest songs and only connected links`() {
        val graph = SetlistAnalytics.transitionGraph(shows, songsFor)
        // Show c's Scarlet/Fire stop is not a link; show b's Scarlet > Fire is.
        assertEquals(2, graph.edges.single { it.label == "Scarlet Begonias ➔ Fire on the Mountain" }.count)
        assertEquals(2, graph.edges.single { it.label == "Drums ➔ Space" }.count)
        assertTrue(graph.edges.none { it.from == "Fire on the Mountain" })
        assertTrue(graph.edges.all { it.from in graph.nodes && it.to in graph.nodes })
        assertEquals(graph.edges.sortedByDescending { it.count }.map { it.count }, graph.edges.map { it.count })

        val small = SetlistAnalytics.transitionGraph(shows, songsFor, maxNodes = 3)
        assertEquals(3, small.nodes.size)
        assertTrue(small.edges.all { it.from in small.nodes && it.to in small.nodes })
        assertEquals(listOf("Drums ➔ Space", "Space ➔ Morning Dew", "Space ➔ The Wheel").toSet().intersect(
            small.edgesOf("Space").map { it.label }.toSet()
        ), small.edgesOf("Space").map { it.label }.toSet())
    }

    @Test
    fun `seed catalog contains the families its curated notes rely on`() {
        val stats = SetlistAnalytics.familyStats(TestSeeds.shows, TestSeeds::songsFor).associateBy { it.family.id }
        assertTrue(stats.getValue("scarlet-fire").occurrences > 0)
        assertTrue(stats.getValue("help-slip-frank").occurrences > 0)
    }
}
