package com.example.ui.screens

import com.example.data.seed.TriviaQuestion

internal enum class TriviaStage {
    HUB,
    ACTIVE_QUIZ,
    QUIZ_SUMMARY,
    FLASHCARD_DECK
}

internal data class AnsweredQuestionRecord(
    val question: TriviaQuestion,
    val selectedOptionIndex: Int,
    val isCorrect: Boolean,
    val pointsEarned: Int
)
