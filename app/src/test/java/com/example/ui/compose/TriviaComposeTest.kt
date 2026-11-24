package com.example.ui.compose

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.model.DeadEra
import com.example.data.seed.TriviaCategory
import com.example.data.seed.TriviaDifficulty
import com.example.data.seed.TriviaGameMode
import com.example.data.seed.TriviaQuestion
import com.example.ui.screens.ActiveQuizContent
import com.example.ui.screens.AnsweredQuestionRecord
import com.example.ui.screens.QuizSummaryContent
import com.example.ui.screens.TriviaHubContent
import com.example.ui.screens.TriviaScreen
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** H4: real trivia hub / screen flows plus active quiz + summary panels. */
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
    fun `trivia hub renders mode cards and start callback`() {
        var started: TriviaGameMode? = null
        composeRule.setContent {
            TriviaHubContent(
                shows = emptyList(),
                parseSongs = { emptyList() },
                triviaProgress = emptyTriviaProgress(),
                selectedCategory = TriviaCategory.ALL,
                onSelectCategory = {},
                selectedDifficulty = null,
                onSelectDifficulty = {},
                onStartMode = { started = it }
            )
        }
        composeRule.advanceUi()
        composeRule.onNodeWithTag("trivia_rank_scoreboard_card").assertIsDisplayed()
        composeRule.scrollListToTag("trivia_hub_list", "start_classic_trivia_btn")
        composeRule.clickTag("start_classic_trivia_btn")
        assertEquals(TriviaGameMode.CLASSIC_TOUR, started)
    }

    @Test
    fun `trivia screen hub start answer reaches summary`() {
        composeRule.setContent {
            TriviaScreen(
                shows = emptyList(),
                parseSongs = { emptyList() },
                triviaProgress = emptyTriviaProgress(),
                onRecordQuizCompletion = { _, _, _, _, _ -> },
                onOpenShowDetail = {}
            )
        }
        composeRule.advanceUi()
        composeRule.onNodeWithTag("trivia_screen_root").assertIsDisplayed()
        // Daily Five is a short real deck (5 Qs) — still the production path.
        composeRule.scrollListToTag("trivia_hub_list", "start_daily_trivia_btn")
        composeRule.clickTag("start_daily_trivia_btn")
        composeRule.advanceUi(200)
        composeRule.onNodeWithTag("active_quiz_container").assertIsDisplayed()
        // Quiz stage has no infinite bear animations — auto-advance so option/next clicks idle.
        composeRule.mainClock.autoAdvance = true

        for (i in 0 until 12) {
            composeRule.waitForIdle()
            if (composeRule.onAllNodesWithTag("trivia_summary_container")
                    .fetchSemanticsNodes().isNotEmpty()
            ) {
                break
            }
            composeRule.onNodeWithTag("trivia_option_0").performScrollTo().performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("trivia_next_question_btn").performClick()
        }
        composeRule.onNodeWithTag("trivia_summary_container").assertIsDisplayed()
    }

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
        composeRule.advanceUi()
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
                triviaProgress = emptyTriviaProgress(),
                onBackToHub = {},
                onPlayAgain = {},
                onOpenShowDetail = {}
            )
        }
        composeRule.advanceUi()
        composeRule.onNodeWithTag("trivia_summary_container").assertIsDisplayed()
        composeRule.onNodeWithTag("summary_back_to_hub_btn").assertIsDisplayed()
    }
}
