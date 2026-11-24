package com.example.ui.compose

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.data.model.DeadEra
import com.example.data.seed.TriviaCategory
import com.example.data.seed.TriviaDifficulty
import com.example.data.seed.TriviaGameMode
import com.example.data.seed.TriviaQuestion
import com.example.data.seed.TriviaUserProgress
import com.example.ui.screens.ActiveQuizContent
import com.example.ui.screens.AnsweredQuestionRecord
import com.example.ui.screens.QuizSummaryContent
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 6.7: active quiz + summary panels (flows 6.3b will touch). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TriviaComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun pauseAnims() {
        composeRule.pauseInfiniteAnimations()
    }

    private val question = TriviaQuestion(
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

    @Test
    fun `active quiz shows question options`() {
        composeRule.setContent {
            ActiveQuizContent(
                mode = TriviaGameMode.CLASSIC_TOUR,
                question = question,
                questionIndex = 0,
                totalQuestions = 1,
                selectedOptionIndex = null,
                hiddenOptionIndices = emptySet(),
                usedAskBear5050 = false,
                usedFlipTapeSkip = false,
                sessionStreak = 0,
                sessionPoints = 0,
                remainingSeconds = 60,
                onSelectOption = {},
                onUseAskBear5050 = {},
                onUseFlipTapeSkip = {},
                onNextQuestion = {},
                onOpenShowDetail = {}
            )
        }
        composeRule.onNodeWithTag("active_quiz_container").assertIsDisplayed()
        composeRule.onNodeWithTag("trivia_option_0").assertIsDisplayed()
        composeRule.onNodeWithText("Where was Cornell 77?").assertIsDisplayed()
    }

    @Test
    fun `quiz summary renders scorecard actions`() {
        composeRule.setContent {
            QuizSummaryContent(
                mode = TriviaGameMode.CLASSIC_TOUR,
                answeredHistory = listOf(
                    AnsweredQuestionRecord(
                        question = question,
                        selectedOptionIndex = 0,
                        isCorrect = true,
                        pointsEarned = 10
                    )
                ),
                sessionPoints = 10,
                maxSessionStreak = 1,
                triviaProgress = TriviaUserProgress(totalPoints = 10),
                onBackToHub = {},
                onPlayAgain = {},
                onOpenShowDetail = {}
            )
        }
        composeRule.onNodeWithTag("trivia_summary_container").assertIsDisplayed()
        composeRule.onNodeWithTag("summary_back_to_hub_btn").assertIsDisplayed()
    }
}
