package com.example.data.setlist

import com.example.TestSeeds
import com.example.data.local.ShowEntity
import com.example.data.model.DeadEra
import com.example.data.model.SetType
import com.example.data.model.SongEntry
import com.example.data.model.TransitionType
import com.example.data.model.TransitionType.NONE
import com.example.data.model.TransitionType.SEGUE
import com.example.data.review.Leitner
import com.example.data.review.ReviewState
import com.example.data.seed.DeadTriviaSeed
import com.example.data.seed.TriviaCategory
import com.example.data.seed.TriviaGameMode
import com.example.data.seed.TriviaQuestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransitionQuizTest {

    private fun show(id: String, date: String, era: DeadEra) = ShowEntity(
        id = id, date = date, venue = "Venue $id", city = "C", stateOrCountry = "S", eraId = era.id, tourName = "T"
    )

    private fun s(name: String, t: TransitionType = NONE) = SongEntry(name = name, setType = SetType.SET_2, transitionOut = t)

    private val a = show("a", "1977-05-08", DeadEra.SPRING_77)
    private val b = show("b", "1978-07-08", DeadEra.SPRING_77)
    private val c = show("c", "1990-03-29", DeadEra.HORNSBY_VINCE_90S)
    private val setlists = mapOf(
        "a" to listOf(s("Playing in the Band", SEGUE), s("Uncle John's Band", SEGUE), s("Playing in the Band"), s("Ripple")),
        "b" to listOf(s("Drums", SEGUE), s("Space", SEGUE), s("The Wheel"), s("Deal"), s("Bertha")),
        "c" to listOf(s("Drums", SEGUE), s("Space", SEGUE), s("Morning Dew"), s("Jack Straw"), s("Sugaree"))
    )
    private val shows = listOf(a, b, c)
    private val songsFor = { sh: ShowEntity -> setlists[sh.id].orEmpty() }

    private fun assertWellFormed(q: TriviaQuestion) {
        assertEquals(q.id, 4, q.options.size)
        assertTrue(q.id, q.correctIndex in 0..3)
        assertEquals(q.id, 4, q.options.map(SetlistAnalytics::canonicalKey).toSet().size) // no duplicate options
        assertEquals(TriviaCategory.SEGUES_SETLISTS, q.category)
        assertTrue(q.linerNoteFact.isNotBlank())
    }

    @Test
    fun `complete the chain blanks a middle or second song and has one right answer`() {
        val pool = setlists.values.flatten().map { it.name }
        val qs = TransitionQuiz.completeTheChain(pool)
        qs.forEach(::assertWellFormed)
        val help = qs.single { it.id == "segue_chain_help-slip-frank_1" }
        assertEquals("Slipknot!", help.options[help.correctIndex])
        assertTrue(help.question.contains("Help on the Way -> ??? -> Franklin's Tower"))
        // Other songs from the same family are never offered as wrong answers.
        assertFalse(help.options.contains("Franklin's Tower"))
    }

    @Test
    fun `most-often-next needs a clear leader`() {
        val pool = setlists.values.flatten().map { it.name }
        val qs = TransitionQuiz.mostOftenNext(shows, songsFor, pool).associateBy { it.id }
        val drums = qs.getValue("segue_next_drums")
        assertWellFormed(drums)
        assertEquals("Space", drums.options[drums.correctIndex])
        assertTrue(drums.linerNoteFact.contains("2 of 2"))
        // Space -> The Wheel once and -> Morning Dew once is a tie, so no question.
        assertFalse("segue_next_space" in qs)
    }

    @Test
    fun `sandwich question finds the filling`() {
        val q = TransitionQuiz.fillTheSandwich(listOf(a), songsFor).singleOrNull()
        // Show a has only Ripple outside the sandwich: too few distractors, so it is skipped.
        assertEquals(null, q)

        val bigger = mapOf("a" to setlists.getValue("a") + listOf(s("Deal"), s("Bertha"), s("Sugaree")))
        val q2 = TransitionQuiz.fillTheSandwich(listOf(a)) { bigger[it.id].orEmpty() }.single()
        assertWellFormed(q2)
        assertEquals("Uncle John's Band", q2.options[q2.correctIndex])
        assertEquals("a", q2.relatedShowId)
    }

    @Test
    fun `name that era only asks about pairs seen in one era`() {
        val qs = TransitionQuiz.nameThatEra(shows, songsFor).associateBy { it.id }
        qs.values.forEach(::assertWellFormed)
        val wheel = qs.getValue("segue_era_space_wheel")
        assertEquals(DeadEra.SPRING_77.title, wheel.options[wheel.correctIndex])
        // Era titles may already end in "Era"; the note must not say "Era era".
        qs.values.forEach { assertFalse(it.linerNoteFact, it.linerNoteFact.contains("era era", ignoreCase = true)) }
        // Drums -> Space is in two eras, so it can't be asked.
        assertFalse("segue_era_drums_space" in qs)
    }

    @Test
    fun `question ids are unique across the generated set`() {
        val all = TransitionQuiz.questions(TestSeeds.shows, TestSeeds::songsFor)
        assertTrue("seed catalog should yield Segue School questions", all.size >= 10)
        assertEquals(all.size, all.map { it.id }.toSet().size)
        all.forEach(::assertWellFormed)
    }

    @Test
    fun `segue school deck puts due and weak cards first`() {
        val ids = TransitionQuiz.questions(TestSeeds.shows, TestSeeds::songsFor).map { it.id }
        val missedYesterday = ids[3]
        val masteredToday = ids[0]
        val states = mapOf(
            missedYesterday to ReviewState(box = 1, lastReviewedDate = "2026-11-20"),
            masteredToday to ReviewState(box = 5, lastReviewedDate = "2026-11-21")
        )
        val deck = DeadTriviaSeed.buildQuizDeck(
            mode = TriviaGameMode.SEGUE_SCHOOL,
            category = TriviaCategory.ALL,
            difficultyFilter = null,
            shows = TestSeeds.shows,
            parseSongs = TestSeeds::songsFor,
            seed = 1L,
            reviewStates = states,
            today = "2026-11-21"
        )
        assertEquals(TriviaGameMode.SEGUE_SCHOOL.questionCount, deck.size)
        assertEquals(missedYesterday, deck.first().id)
        assertFalse("a mastered, not-due card shouldn't crowd out new ones", deck.any { it.id == masteredToday })
        assertNotNull(deck.firstOrNull { it.id !in states })
    }
}
