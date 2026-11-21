package com.example.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class GdshowsNormalizerTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `catalog titles count as matched and unknowns go to unmatched csv`() {
        val shows = listOf(
            ParsedShow(
                dateIso = "1977-05-08",
                uuid = "s1",
                venue = "Barton Hall",
                city = "Ithaca",
                state = "NY",
                country = "US",
                sets = listOf(
                    ParsedSet(
                        "set1",
                        listOf(
                            ParsedSong("a", "Scarlet Begonias", true),
                            ParsedSong("b", "Fire On The Mountain", false),
                            ParsedSong("c", "Completely Made Up Jam", false),
                            ParsedSong("d", "completely made up jam!", false)
                        )
                    )
                )
            )
        )
        val result = GdshowsNormalizer.normalize(shows)
        assertEquals(4, result.performanceCount)
        assertEquals(2, result.matchedPerformances)
        assertEquals(2, result.unmatchedPerformances)
        assertEquals(1, result.unmatched.size)
        assertEquals(2, result.unmatched.single().count)
        assertEquals("completely made up jam", result.unmatched.single().songKey)

        val csv = File(tmp.newFolder("out"), "unmatched.csv")
        GdshowsNormalizer.writeUnmatchedCsv(csv, result.unmatched)
        val text = csv.readText()
        assertTrue(text.startsWith("count,song_key,sample_name\n"))
        assertTrue(text.contains("2,completely made up jam,"))
        assertFalse(text.contains("Scarlet"))
    }

    @Test
    fun `csv formula-leading titles are neutralized`() {
        assertEquals("'=CMD", GdshowsNormalizer.neutralizeCsvFormula("=CMD"))
        assertEquals("'+1", GdshowsNormalizer.neutralizeCsvFormula("+1"))
        assertEquals("Jack Straw", GdshowsNormalizer.neutralizeCsvFormula("Jack Straw"))
        val csv = File(tmp.newFolder("out2"), "unmatched.csv")
        GdshowsNormalizer.writeUnmatchedCsv(
            csv,
            listOf(GdshowsNormalizer.UnmatchedTitle(rawName = "=Weird", songKey = "=weird", count = 1))
        )
        assertTrue(csv.readText().contains("'=weird"))
        assertTrue(csv.readText().contains("'=Weird"))
    }
}
