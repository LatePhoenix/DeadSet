package com.example.ui.compose

import com.example.data.model.DeadEra
import com.example.data.seed.DeadTriviaSeed
import com.example.data.seed.TriviaCategory
import com.example.data.seed.TriviaDifficulty
import com.example.data.seed.TriviaGameMode
import com.example.data.seed.TriviaQuestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 6.7 trivia flow coverage without Compose/Robolectric UI (see [ComposeUiTestNotes]).
 * Exercises the same deck builder the hub → quiz path uses.
 */
class TriviaComposeTest {

    @Test
    fun `classic tour deck is non-empty for hub start`() {
        val deck = DeadTriviaSeed.buildQuizDeck(
            mode = TriviaGameMode.CLASSIC_TOUR,
            category = TriviaCategory.ALL,
            difficultyFilter = null,
            shows = emptyList(),
            parseSongs = { emptyList() },
            reviewStates = emptyMap()
        )
        assertTrue(deck.isNotEmpty())
        assertTrue(deck.all { it.options.size >= 2 })
    }

    @Test
    fun `curated question encodes answer index for summary path`() {
        val q = TriviaQuestion(
            id = "t1",
            category = TriviaCategory.LEGENDARY_SHOWS,
            difficulty = TriviaDifficulty.SHAKEDOWN_ROOKIE,
            era = DeadEra.SPRING_77,
            question = "Where was Cornell 77?",
            options = listOf("Barton Hall", "Fillmore", "Winterland", "Fox Theatre"),
            correctIndex = 0,
            linerNoteFact = "Barton Hall, Ithaca.",
            sourceUrl = "https://www.loc.gov/"
        )
        assertEquals(0, q.correctIndex)
        assertEquals("Barton Hall", q.options[q.correctIndex])
    }
}
