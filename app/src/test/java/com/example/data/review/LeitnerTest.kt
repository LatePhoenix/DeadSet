package com.example.data.review

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LeitnerTest {

    @Test
    fun `right answers climb boxes, a miss drops to box 1, and the top box caps`() {
        var s = Leitner.afterAnswer(null, correct = true, today = "2026-11-01")
        assertEquals(2, s.box)
        repeat(5) { s = Leitner.afterAnswer(s, correct = true, today = "2026-11-02") }
        assertEquals(Leitner.MAX_BOX, s.box)
        s = Leitner.afterAnswer(s, correct = false, today = "2026-11-03")
        assertEquals(1, s.box)
        assertEquals(7, s.timesSeen)
        assertEquals(6, s.timesCorrect)
        assertEquals("2026-11-03", s.lastReviewedDate)
    }

    @Test
    fun `a first wrong answer starts in box 1`() {
        val s = Leitner.afterAnswer(null, correct = false, today = "2026-11-01")
        assertEquals(1, s.box)
        assertEquals(0, s.timesCorrect)
    }

    @Test
    fun `due dates follow the box interval`() {
        assertTrue(Leitner.isDue(ReviewState(1, "2026-11-21"), "2026-11-21"))
        assertFalse(Leitner.isDue(ReviewState(3, "2026-11-20"), "2026-11-21"))
        assertTrue(Leitner.isDue(ReviewState(3, "2026-11-18"), "2026-11-21"))
        assertFalse(Leitner.isDue(ReviewState(5, "2026-11-10"), "2026-11-21"))
    }

    @Test
    fun `days between handles month and year boundaries`() {
        assertEquals(1, Leitner.daysBetween("2026-12-31", "2027-01-01"))
        assertEquals(29, Leitner.daysBetween("2028-02-01", "2028-03-01")) // leap year
        assertEquals(-1, Leitner.daysBetween("2026-11-21", "2026-11-20"))
    }

    @Test
    fun `order is due-weakest first, then new, then not due`() {
        val states = mapOf(
            "due-box2" to ReviewState(2, "2026-11-19"),
            "due-box1" to ReviewState(1, "2026-11-20"),
            "not-due" to ReviewState(4, "2026-11-20")
        )
        val order = Leitner.order(listOf("not-due", "new-a", "due-box2", "due-box1", "new-b"), states, "2026-11-21")
        assertEquals(listOf("due-box1", "due-box2", "new-a", "new-b", "not-due"), order)
    }
}
