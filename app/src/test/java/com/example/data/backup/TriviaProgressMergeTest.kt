package com.example.data.backup

import com.example.data.seed.TriviaUserProgress
import org.junit.Assert.assertEquals
import org.junit.Test

class TriviaProgressMergeTest {

    @Test
    fun `keeps higher points and unions badges`() {
        val local = TriviaUserProgress(
            totalAnswered = 10,
            totalCorrect = 8,
            currentStreak = 2,
            bestStreak = 5,
            totalPoints = 900,
            quizzesCompleted = 3,
            dailyCompletedDate = "2026-11-20",
            unlockedBadgeIds = setOf("first_miracle")
        )
        val backup = TriviaUserProgress(
            totalAnswered = 4,
            totalCorrect = 3,
            currentStreak = 4,
            bestStreak = 4,
            totalPoints = 200,
            quizzesCompleted = 1,
            dailyCompletedDate = "2026-11-22",
            unlockedBadgeIds = setOf("first_miracle", "golden_road")
        )
        val merged = mergeTriviaProgress(local, backup)
        assertEquals(10, merged.totalAnswered)
        assertEquals(8, merged.totalCorrect)
        assertEquals(900, merged.totalPoints)
        assertEquals(5, merged.bestStreak)
        assertEquals(3, merged.quizzesCompleted)
        assertEquals("2026-11-22", merged.dailyCompletedDate)
        assertEquals(4, merged.currentStreak) // from later daily side (backup)
        assertEquals(setOf("first_miracle", "golden_road"), merged.unlockedBadgeIds)
    }

    @Test
    fun `tie on daily date keeps local currentStreak`() {
        val local = TriviaUserProgress(
            currentStreak = 7,
            dailyCompletedDate = "2026-11-21",
            totalPoints = 100
        )
        val backup = TriviaUserProgress(
            currentStreak = 1,
            dailyCompletedDate = "2026-11-21",
            totalPoints = 50
        )
        val merged = mergeTriviaProgress(local, backup)
        assertEquals(7, merged.currentStreak)
        assertEquals(100, merged.totalPoints)
    }

    @Test
    fun `blank daily loses to a real date`() {
        assertEquals("2026-11-01", laterIsoDate("", "2026-11-01"))
        assertEquals("2026-11-01", laterIsoDate("2026-11-01", ""))
    }
}
