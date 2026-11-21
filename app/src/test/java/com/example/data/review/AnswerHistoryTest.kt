package com.example.data.review

import com.example.TestSeeds
import com.example.data.seed.DeadTriviaSeed
import com.example.data.seed.TriviaCategory
import com.example.data.seed.TriviaGameMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerHistoryTest {

    @Test
    fun `every trivia question id is unique, since ids key the answer history`() {
        val all = DeadTriviaSeed.getAllAvailableQuestions(TestSeeds.shows, TestSeeds::songsFor)
        val dupes = all.groupBy { it.id }.filterValues { it.size > 1 }.keys
        assertTrue("Duplicate question ids: $dupes", dupes.isEmpty())
    }

    @Test
    fun `accuracy groups per-question history by category`() {
        val states = mapOf(
            "a" to ReviewState(2, "2026-11-20", timesSeen = 3, timesCorrect = 2),
            "b" to ReviewState(1, "2026-11-20", timesSeen = 1, timesCorrect = 0),
            "c" to ReviewState(3, "2026-11-20", timesSeen = 4, timesCorrect = 4),
            "gone" to ReviewState(1, "2026-11-20", timesSeen = 9, timesCorrect = 0)
        )
        val labels = mapOf("a" to "Segues", "b" to "Segues", "c" to "Gear")
        val result = AnswerHistory.byCategory(states) { labels[it] }
        assertEquals(
            listOf(CategoryAccuracy("Gear", 4, 4), CategoryAccuracy("Segues", 4, 2)),
            result
        )
        assertEquals(50, result.last().percent)
    }

    @Test
    fun `classic decks lead with unseen questions and skip mastered ones`() {
        val pool = DeadTriviaSeed.getAllAvailableQuestions(TestSeeds.shows, TestSeeds::songsFor)
        // Mark all but 12 questions as mastered and not due.
        val mastered = pool.drop(12).associate { it.id to ReviewState(5, "2026-11-21", 5, 5) }
        val deck = DeadTriviaSeed.buildQuizDeck(
            mode = TriviaGameMode.CLASSIC_TOUR,
            category = TriviaCategory.ALL,
            difficultyFilter = null,
            shows = TestSeeds.shows,
            parseSongs = TestSeeds::songsFor,
            seed = 7L,
            reviewStates = mastered,
            today = "2026-11-21"
        )
        assertEquals(10, deck.size)
        assertFalse("mastered questions should not crowd out unseen ones", deck.any { it.id in mastered })
    }
}
