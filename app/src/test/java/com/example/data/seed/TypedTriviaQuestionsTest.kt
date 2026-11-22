package com.example.data.seed

import com.example.TestSeeds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TypedTriviaQuestionsTest {

    @Test
    fun `typed deck covers every 5_3 format`() {
        val byFormat = typedTriviaQuestions().groupBy { it.format }
        assertTrue(byFormat.containsKey(TriviaFormat.TRUE_FALSE))
        assertTrue(byFormat.containsKey(TriviaFormat.ORDER_DATES))
        assertTrue(byFormat.containsKey(TriviaFormat.WHO_SANG))
        assertTrue(byFormat.containsKey(TriviaFormat.VENUE_CITY))
        assertTrue(byFormat.containsKey(TriviaFormat.LYRIC_PARAPHRASE))
    }

    @Test
    fun `typed questions all have source urls and non-blank options`() {
        for (q in typedTriviaQuestions()) {
            assertFalse(q.id, q.sourceUrl.isNullOrBlank())
            assertTrue(q.id, q.options.size >= 2)
            assertTrue(q.id, q.correctIndex in q.options.indices)
            assertTrue(q.id, q.question.isNotBlank())
        }
    }

    @Test
    fun `true false questions have exactly two options`() {
        typedTriviaQuestions().filter { it.format == TriviaFormat.TRUE_FALSE }.forEach { q ->
            assertEquals(q.id, listOf("True", "False"), q.options)
        }
    }

    @Test
    fun `lyric paraphrases do not embed quoted lyric lines`() {
        typedTriviaQuestions().filter { it.format == TriviaFormat.LYRIC_PARAPHRASE }.forEach { q ->
            assertFalse(q.id, q.question.contains('"'))
            assertFalse(q.id, q.linerNoteFact.contains('"'))
            // Common lyric fragments we must not ship as quotes
            assertFalse(q.id, q.question.contains("Reach out your hand", ignoreCase = true))
            assertFalse(q.id, q.question.contains("may it be again", ignoreCase = true))
        }
    }

    @Test
    fun `vault generates venue-city format questions from catalog`() {
        val vault = DeadTriviaSeed.generateDynamicVaultQuestions(TestSeeds.shows, TestSeeds::songsFor)
        val venueCity = vault.filter { it.format == TriviaFormat.VENUE_CITY }
        assertTrue(venueCity.isNotEmpty())
        assertTrue(venueCity.all { it.isVaultGenerated && it.id.startsWith("vault_venue_city_") })
    }

    @Test
    fun `curated pool includes typed questions with unique ids`() {
        val all = DeadTriviaSeed.getCuratedQuestions()
        val typedIds = typedTriviaQuestions().map { it.id }.toSet()
        assertTrue(all.map { it.id }.containsAll(typedIds))
        assertEquals(all.size, all.map { it.id }.toSet().size)
    }
}
