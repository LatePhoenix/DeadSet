package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ShowEntity
import com.example.data.model.DeadEra
import com.example.data.model.SongEntry
import com.example.data.seed.*
import com.example.ui.components.BearYellow
import com.example.ui.components.DancingBear
import com.example.ui.components.StealieEmblem
import com.example.ui.theme.*
import kotlinx.coroutines.delay


@Composable
fun TriviaScreen(
    shows: List<ShowEntity>,
    parseSongs: (ShowEntity) -> List<SongEntry>,
    triviaProgress: TriviaUserProgress,
    onRecordQuizCompletion: (
        answeredCount: Int,
        correctCount: Int,
        pointsEarned: Int,
        maxStreakInRound: Int,
        mode: TriviaGameMode
    ) -> Unit,
    onOpenShowDetail: (String) -> Unit,
    reviewStates: Map<String, com.example.data.review.ReviewState> = emptyMap(),
    onCardAnswered: (cardId: String, correct: Boolean) -> Unit = { _, _ -> },
    onOpenAlmanac: () -> Unit = {},
    onOpenAlmanacArticle: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var stage by remember { mutableStateOf(TriviaStage.HUB) }
    var selectedCategory by remember { mutableStateOf(TriviaCategory.ALL) }
    var selectedDifficulty by remember { mutableStateOf<TriviaDifficulty?>(null) }
    var activeMode by remember { mutableStateOf(TriviaGameMode.CLASSIC_TOUR) }

    // Active quiz session state
    var activeDeck by remember { mutableStateOf<List<TriviaQuestion>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var hiddenOptionIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var usedAskBear5050 by remember { mutableStateOf(false) }
    var usedFlipTapeSkip by remember { mutableStateOf(false) }
    var sessionStreak by remember { mutableIntStateOf(0) }
    var maxSessionStreak by remember { mutableIntStateOf(0) }
    var sessionPoints by remember { mutableIntStateOf(0) }
    var answeredHistory by remember { mutableStateOf<List<AnsweredQuestionRecord>>(emptyList()) }
    var remainingSeconds by remember { mutableIntStateOf(60) }

    // Handle back navigation when inside Active Quiz, Summary, or Flashcards
    BackHandler(enabled = stage != TriviaStage.HUB) {
        stage = TriviaStage.HUB
    }

    // Countdown timer for Terrapin Blitz mode
    LaunchedEffect(stage, activeMode, remainingSeconds) {
        if (stage == TriviaStage.ACTIVE_QUIZ && activeMode == TriviaGameMode.TERRAPIN_BLITZ) {
            if (remainingSeconds > 0) {
                delay(1000L)
                remainingSeconds -= 1
            } else {
                // Time's up! Finalize blitz round
                val correctCount = answeredHistory.count { it.isCorrect }
                onRecordQuizCompletion(
                    answeredHistory.size,
                    correctCount,
                    sessionPoints,
                    maxSessionStreak,
                    activeMode
                )
                stage = TriviaStage.QUIZ_SUMMARY
            }
        }
    }

    fun startQuiz(mode: TriviaGameMode) {
        activeMode = mode
        if (mode == TriviaGameMode.LORE_FLASHCARDS) {
            stage = TriviaStage.FLASHCARD_DECK
            return
        }
        val deck = DeadTriviaSeed.buildQuizDeck(
            mode = mode,
            category = selectedCategory,
            difficultyFilter = selectedDifficulty,
            shows = shows,
            parseSongs = parseSongs,
            reviewStates = reviewStates
        )
        activeDeck = deck
        currentIndex = 0
        selectedOptionIndex = null
        hiddenOptionIndices = emptySet()
        usedAskBear5050 = false
        usedFlipTapeSkip = false
        sessionStreak = 0
        maxSessionStreak = 0
        sessionPoints = 0
        answeredHistory = emptyList()
        remainingSeconds = mode.timeLimitSeconds ?: 60
        stage = TriviaStage.ACTIVE_QUIZ
    }

    Scaffold(
        topBar = {
            TriviaScreenTopBar(
                stage = stage,
                activeMode = activeMode,
                triviaProgress = triviaProgress,
                onBackToHub = { stage = TriviaStage.HUB }
            )
        },
        containerColor = StealieMidnight,
        modifier = modifier.testTag("trivia_screen_root")
    ) { innerPadding ->
        TriviaScreenStageBody(
            stage = stage,
            shows = shows,
            parseSongs = parseSongs,
            triviaProgress = triviaProgress,
            selectedCategory = selectedCategory,
            onSelectedCategoryChange = { selectedCategory = it },
            selectedDifficulty = selectedDifficulty,
            onSelectedDifficultyChange = { selectedDifficulty = it },
            activeMode = activeMode,
            activeDeck = activeDeck,
            currentIndex = currentIndex,
            onCurrentIndexChange = { currentIndex = it },
            selectedOptionIndex = selectedOptionIndex,
            onSelectedOptionIndexChange = { selectedOptionIndex = it },
            hiddenOptionIndices = hiddenOptionIndices,
            onHiddenOptionIndicesChange = { hiddenOptionIndices = it },
            usedAskBear5050 = usedAskBear5050,
            onUsedAskBear5050Change = { usedAskBear5050 = it },
            usedFlipTapeSkip = usedFlipTapeSkip,
            onUsedFlipTapeSkipChange = { usedFlipTapeSkip = it },
            sessionStreak = sessionStreak,
            onSessionStreakChange = { sessionStreak = it },
            maxSessionStreak = maxSessionStreak,
            onMaxSessionStreakChange = { maxSessionStreak = it },
            sessionPoints = sessionPoints,
            onSessionPointsChange = { sessionPoints = it },
            answeredHistory = answeredHistory,
            onAnsweredHistoryChange = { answeredHistory = it },
            remainingSeconds = remainingSeconds,
            onStageChange = { stage = it },
            onStartMode = { mode -> startQuiz(mode) },
            reviewStates = reviewStates,
            onOpenAlmanac = onOpenAlmanac,
            onRecordQuizCompletion = onRecordQuizCompletion,
            onCardAnswered = onCardAnswered,
            onOpenShowDetail = onOpenShowDetail,
            onOpenAlmanacArticle = onOpenAlmanacArticle,
            innerPadding = innerPadding
        )
    }
}

