package com.example.data.backup

import com.example.data.seed.TriviaUserProgress
import kotlin.math.max

/**
 * Merges backup trivia progress into local prefs so restoring never drops points or badges
 * earned after the backup was taken (H2).
 */
fun mergeTriviaProgress(local: TriviaUserProgress, backup: TriviaUserProgress): TriviaUserProgress {
    val laterDaily = laterIsoDate(local.dailyCompletedDate, backup.dailyCompletedDate)
    val currentStreak = when {
        local.dailyCompletedDate.isBlank() && backup.dailyCompletedDate.isNotBlank() ->
            backup.currentStreak
        backup.dailyCompletedDate.isBlank() && local.dailyCompletedDate.isNotBlank() ->
            local.currentStreak
        backup.dailyCompletedDate > local.dailyCompletedDate -> backup.currentStreak
        local.dailyCompletedDate > backup.dailyCompletedDate -> local.currentStreak
        else -> local.currentStreak // tie or both blank: local wins
    }
    return TriviaUserProgress(
        totalAnswered = max(local.totalAnswered, backup.totalAnswered),
        totalCorrect = max(local.totalCorrect, backup.totalCorrect),
        currentStreak = currentStreak,
        bestStreak = max(local.bestStreak, backup.bestStreak),
        totalPoints = max(local.totalPoints, backup.totalPoints),
        quizzesCompleted = max(local.quizzesCompleted, backup.quizzesCompleted),
        dailyCompletedDate = laterDaily,
        unlockedBadgeIds = local.unlockedBadgeIds + backup.unlockedBadgeIds
    )
}

/** ISO `yyyy-MM-dd` strings compare lexicographically; blank loses to a real date. */
internal fun laterIsoDate(a: String, b: String): String = when {
    a.isBlank() -> b
    b.isBlank() -> a
    b > a -> b
    else -> a
}
