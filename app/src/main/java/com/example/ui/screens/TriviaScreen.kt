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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StealieMidnight)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (stage != TriviaStage.HUB) {
                            IconButton(
                                onClick = { stage = TriviaStage.HUB },
                                modifier = Modifier
                                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                    .testTag("trivia_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Trivia Hub",
                                    tint = StealieGold
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        } else {
                            StealieEmblem(size = 38.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Column {
                            Text(
                                text = when (stage) {
                                    TriviaStage.HUB -> "Deadhead Trivia Vault"
                                    TriviaStage.ACTIVE_QUIZ -> activeMode.title
                                    TriviaStage.QUIZ_SUMMARY -> "Setlist Encore Scorecard"
                                    TriviaStage.FLASHCARD_DECK -> "Archivist Lore Flashcards"
                                },
                                color = TextPrimaryDark,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.semantics { heading() }
                            )
                            Text(
                                text = triviaProgress.rankTitle,
                                color = StealieGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Points pill
                    Surface(
                        color = StealieGold.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, StealieGold.copy(alpha = 0.45f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = StealieGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${triviaProgress.totalPoints} pts",
                                color = StealieGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
                HorizontalDivider(
                    color = BorderSubtle,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        },
        containerColor = StealieMidnight,
        modifier = modifier.testTag("trivia_screen_root")
    ) { innerPadding ->
        when (stage) {
            TriviaStage.HUB -> {
                TriviaHubContent(
                    shows = shows,
                    parseSongs = parseSongs,
                    triviaProgress = triviaProgress,
                    selectedCategory = selectedCategory,
                    onSelectCategory = { selectedCategory = it },
                    selectedDifficulty = selectedDifficulty,
                    onSelectDifficulty = { selectedDifficulty = it },
                    onStartMode = { mode -> startQuiz(mode) },
                    reviewStates = reviewStates,
                    onOpenAlmanac = onOpenAlmanac,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            TriviaStage.ACTIVE_QUIZ -> {
                val currentQ = activeDeck.getOrNull(currentIndex)
                if (currentQ != null) {
                    ActiveQuizContent(
                        mode = activeMode,
                        question = currentQ,
                        questionIndex = currentIndex,
                        totalQuestions = activeDeck.size,
                        selectedOptionIndex = selectedOptionIndex,
                        hiddenOptionIndices = hiddenOptionIndices,
                        usedAskBear5050 = usedAskBear5050,
                        usedFlipTapeSkip = usedFlipTapeSkip,
                        sessionStreak = sessionStreak,
                        sessionPoints = sessionPoints,
                        remainingSeconds = remainingSeconds,
                        onSelectOption = { idx ->
                            if (selectedOptionIndex == null) {
                                selectedOptionIndex = idx
                                val isCorrect = idx == currentQ.correctIndex
                                // Every answer feeds per-question history (step 5.1) and Leitner review.
                                onCardAnswered(currentQ.id, isCorrect)
                                val newStreak = if (isCorrect) sessionStreak + 1 else 0
                                sessionStreak = newStreak
                                if (newStreak > maxSessionStreak) {
                                    maxSessionStreak = newStreak
                                }
                                val streakBonus = if (isCorrect) (newStreak - 1).coerceAtLeast(0) * 35 else 0
                                val speedBonus = if (isCorrect && activeMode == TriviaGameMode.TERRAPIN_BLITZ) 40 else 0
                                val earned = if (isCorrect) currentQ.difficulty.basePoints + streakBonus + speedBonus else 0
                                sessionPoints += earned
                                answeredHistory = answeredHistory + AnsweredQuestionRecord(
                                    question = currentQ,
                                    selectedOptionIndex = idx,
                                    isCorrect = isCorrect,
                                    pointsEarned = earned
                                )
                            }
                        },
                        onUseAskBear5050 = {
                            // Need ≥4 options so hiding two wrong answers leaves a real choice
                            // (True/False and some typed formats only have 2).
                            if (!usedAskBear5050 && selectedOptionIndex == null &&
                                currentQ.options.size >= 4
                            ) {
                                usedAskBear5050 = true
                                val wrongIndices = currentQ.options.indices
                                    .filter { it != currentQ.correctIndex }
                                    .shuffled()
                                    .take(2)
                                    .toSet()
                                hiddenOptionIndices = wrongIndices
                            }
                        },
                        onUseFlipTapeSkip = {
                            if (!usedFlipTapeSkip && selectedOptionIndex == null) {
                                usedFlipTapeSkip = true
                                if (currentIndex + 1 < activeDeck.size) {
                                    currentIndex += 1
                                    selectedOptionIndex = null
                                    hiddenOptionIndices = emptySet()
                                } else {
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
                        },
                        onNextQuestion = {
                            if (currentIndex + 1 < activeDeck.size) {
                                currentIndex += 1
                                selectedOptionIndex = null
                                hiddenOptionIndices = emptySet()
                            } else {
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
                        },
                        onOpenShowDetail = onOpenShowDetail,
                        onOpenAlmanacArticle = onOpenAlmanacArticle,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }

            TriviaStage.QUIZ_SUMMARY -> {
                QuizSummaryContent(
                    mode = activeMode,
                    answeredHistory = answeredHistory,
                    sessionPoints = sessionPoints,
                    maxSessionStreak = maxSessionStreak,
                    triviaProgress = triviaProgress,
                    onPlayAgain = { startQuiz(activeMode) },
                    onBackToHub = { stage = TriviaStage.HUB },
                    onOpenShowDetail = onOpenShowDetail,
                    onOpenAlmanacArticle = onOpenAlmanacArticle,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            TriviaStage.FLASHCARD_DECK -> {
                FlashcardDeckContent(
                    questions = remember(shows) {
                        DeadTriviaSeed.getAllAvailableQuestions(shows, parseSongs)
                    },
                    onOpenShowDetail = onOpenShowDetail,
                    onOpenAlmanacArticle = onOpenAlmanacArticle,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

