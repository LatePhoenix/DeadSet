package com.example.data.seed

import org.junit.Assert.assertTrue
import org.junit.Test

class CuratedTriviaSourceTest {

    @Test
    fun `every curated question has a sourceUrl`() {
        val missing = DeadTriviaSeed.getCuratedQuestions()
            .filter { it.sourceUrl.isNullOrBlank() }
            .map { it.id }
        assertTrue("Curated questions missing sourceUrl: $missing", missing.isEmpty())
    }

    @Test
    fun `cornell liner note cites 2011 registry year not 2012`() {
        val cornell = DeadTriviaSeed.getCuratedQuestions().first { it.id == "q_cornell_77_venue" }
        assertTrue(cornell.linerNoteFact.contains("2011"))
        assertTrue(!cornell.linerNoteFact.contains("2012"))
    }
}
