package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ReviewCardEntity
import com.example.data.local.UserPrefsStore
import com.example.data.review.Leitner
import com.example.data.seed.TriviaGameMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Trivia vault + Segue School Leitner answers (6.2). */
class TriviaViewModel(
    database: AppDatabase,
    private val userPrefs: UserPrefsStore,
    private val ui: MutableStateFlow<UiState>
) : ViewModel() {

    private val reviewDao = database.reviewDao()
    private val reviewMutex = Mutex()

    init {
        viewModelScope.launch {
            reviewDao.observeAll().collect { cards ->
                ui.update { s -> s.copy(reviewStates = cards.associate { it.cardId to it.toState() }) }
            }
        }
    }

    fun recordTriviaQuizCompletion(
        answeredCount: Int,
        correctCount: Int,
        pointsEarned: Int,
        maxStreakInRound: Int,
        mode: TriviaGameMode
    ) {
        ui.update { current ->
            val prev = current.triviaProgress
            val newTotalAnswered = prev.totalAnswered + answeredCount
            val newTotalCorrect = prev.totalCorrect + correctCount
            val newTotalPoints = prev.totalPoints + pointsEarned
            val newBestStreak = maxOf(prev.bestStreak, maxStreakInRound)
            val newQuizzesCompleted = prev.quizzesCompleted + 1
            val newDailyDate = if (mode == TriviaGameMode.DAILY_DEADHEAD) {
                com.example.data.todayIsoDate()
            } else {
                prev.dailyCompletedDate
            }

            val newBadges = prev.unlockedBadgeIds.toMutableSet()
            if (answeredCount > 0) {
                newBadges.add("first_miracle")
            }
            if (newBestStreak >= 5) {
                newBadges.add("scarlet_fire_streak")
            }
            if (mode == TriviaGameMode.CLASSIC_TOUR && answeredCount > 0 &&
                (correctCount.toFloat() / answeredCount.toFloat()) >= 0.8f
            ) {
                newBadges.add("cornell_scholar")
            }
            if (newTotalPoints >= 1500) {
                newBadges.add("owsley_soundman")
            }
            if (mode == TriviaGameMode.DAILY_DEADHEAD) {
                newBadges.add("daily_stealie")
            }
            if (mode == TriviaGameMode.TERRAPIN_BLITZ && correctCount >= 6) {
                newBadges.add("terrapin_speedster")
            }

            val updatedProgress = prev.copy(
                totalAnswered = newTotalAnswered,
                totalCorrect = newTotalCorrect,
                currentStreak = maxStreakInRound,
                bestStreak = newBestStreak,
                totalPoints = newTotalPoints,
                quizzesCompleted = newQuizzesCompleted,
                dailyCompletedDate = newDailyDate,
                unlockedBadgeIds = newBadges
            )

            val newlyUnlockedCount = newBadges.size - prev.unlockedBadgeIds.size
            val feedback = if (newlyUnlockedCount > 0) {
                "⚡ +$pointsEarned Trivia pts & $newlyUnlockedCount new Deadhead Badge unlocked!"
            } else {
                "⚡ +$pointsEarned Deadhead Trivia pts added to your Vault standing!"
            }

            current.copy(
                triviaProgress = updatedProgress,
                userFeedbackMessage = feedback
            )
        }
        userPrefs.saveTriviaProgress(ui.value.triviaProgress)
    }

    fun recordCardAnswer(cardId: String, correct: Boolean) {
        viewModelScope.launch {
            reviewMutex.withLock {
                val previous = reviewDao.get(cardId)?.toState()
                val next = Leitner.afterAnswer(previous, correct, com.example.data.todayIsoDate())
                reviewDao.upsert(ReviewCardEntity.from(cardId, next))
            }
        }
    }
}
