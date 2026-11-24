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
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.ui.platform.LocalContext
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
import com.example.R
import com.example.data.local.ShowEntity
import com.example.data.model.DeadEra
import com.example.data.model.SongEntry
import com.example.data.seed.*
import com.example.ui.components.BearYellow
import com.example.ui.components.DancingBear
import com.example.ui.components.StealieEmblem
import com.example.ui.share.ShareHelper
import com.example.ui.share.buildTriviaShareLines
import com.example.ui.share.renderShareCardBitmap
import com.example.ui.theme.*
import java.util.UUID
import kotlinx.coroutines.delay



// Extracted for 6.3 screen breakup.

@Composable
internal fun TriviaHubContent(
    shows: List<ShowEntity>,
    parseSongs: (ShowEntity) -> List<SongEntry>,
    triviaProgress: TriviaUserProgress,
    selectedCategory: TriviaCategory,
    onSelectCategory: (TriviaCategory) -> Unit,
    selectedDifficulty: TriviaDifficulty?,
    onSelectDifficulty: (TriviaDifficulty?) -> Unit,
    onStartMode: (TriviaGameMode) -> Unit,
    reviewStates: Map<String, com.example.data.review.ReviewState> = emptyMap(),
    onOpenAlmanac: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val allAvailableQuestions = remember(shows) {
        DeadTriviaSeed.getAllAvailableQuestions(shows, parseSongs)
    }
    // Per-question history grouped by category (step 5.1); covers Segue School questions too.
    val accuracy = remember(allAvailableQuestions, reviewStates, shows) {
        val categoryById = allAvailableQuestions.associate { it.id to it.category.shortLabel } +
            com.example.data.setlist.TransitionQuiz.questions(shows, parseSongs).associate { it.id to it.category.shortLabel }
        com.example.data.review.AnswerHistory.byCategory(reviewStates) { categoryById[it] }
    }
    val vaultGeneratedCount = remember(allAvailableQuestions) {
        allAvailableQuestions.count { it.isVaultGenerated }
    }
    val today = remember { com.example.data.todayIsoDate() }
    val isDailyCompletedToday = triviaProgress.dailyCompletedDate == today

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 88.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("trivia_hub_list")
    ) {
        item {
            TriviaRankScoreboardCard(
                triviaProgress = triviaProgress,
                totalQuestionsCount = allAvailableQuestions.size,
                vaultGeneratedCount = vaultGeneratedCount
            )
        }

        item {
            TriviaSetupFiltersCard(
                selectedCategory = selectedCategory,
                onSelectCategory = onSelectCategory,
                selectedDifficulty = selectedDifficulty,
                onSelectDifficulty = onSelectDifficulty
            )
        }

        triviaHubGameModeItems(
            selectedCategory = selectedCategory,
            allAvailableQuestionsCount = allAvailableQuestions.size,
            isDailyCompletedToday = isDailyCompletedToday,
            onStartMode = onStartMode,
            onOpenAlmanac = onOpenAlmanac
        )

        if (accuracy.isNotEmpty()) {
            item {
                TriviaAccuracyByCategoryCard(accuracy = accuracy)
            }
        }

        item {
            TriviaMeritBadgesCard(triviaProgress = triviaProgress)
        }
    }
}

@Composable
internal fun ActiveQuizContent(
    mode: TriviaGameMode,
    question: TriviaQuestion,
    questionIndex: Int,
    totalQuestions: Int,
    selectedOptionIndex: Int?,
    hiddenOptionIndices: Set<Int>,
    usedAskBear5050: Boolean,
    usedFlipTapeSkip: Boolean,
    sessionStreak: Int,
    sessionPoints: Int,
    remainingSeconds: Int,
    onSelectOption: (Int) -> Unit,
    onUseAskBear5050: () -> Unit,
    onUseFlipTapeSkip: () -> Unit,
    onNextQuestion: () -> Unit,
    onOpenShowDetail: (String) -> Unit,
    onOpenAlmanacArticle: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val progressTarget = ((questionIndex + 1).toFloat() / totalQuestions.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "quizProgress"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("active_quiz_container")
    ) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 10.dp,
            bottom = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
    ) {
        item {
            TriviaActiveQuizStatusBar(
                mode = mode,
                questionIndex = questionIndex,
                totalQuestions = totalQuestions,
                sessionStreak = sessionStreak,
                sessionPoints = sessionPoints,
                remainingSeconds = remainingSeconds,
                animatedProgress = animatedProgress
            )
        }

        item {
            TriviaActiveQuestionCard(question = question)
        }

        item {
            TriviaQuizLifelinesRow(
                question = question,
                selectedOptionIndex = selectedOptionIndex,
                usedAskBear5050 = usedAskBear5050,
                usedFlipTapeSkip = usedFlipTapeSkip,
                onUseAskBear5050 = onUseAskBear5050,
                onUseFlipTapeSkip = onUseFlipTapeSkip
            )
        }

        itemsIndexed(question.options) { idx, optionText ->
            TriviaQuizAnswerOption(
                index = idx,
                optionText = optionText,
                question = question,
                selectedOptionIndex = selectedOptionIndex,
                hiddenOptionIndices = hiddenOptionIndices,
                onSelectOption = onSelectOption
            )
        }

    } // LazyColumn

        val selected = selectedOptionIndex
        if (selected != null) {
            TriviaQuizExplanationPanel(
                question = question,
                questionIndex = questionIndex,
                totalQuestions = totalQuestions,
                selectedOptionIndex = selected,
                onNextQuestion = onNextQuestion,
                onOpenShowDetail = onOpenShowDetail,
                onOpenAlmanacArticle = onOpenAlmanacArticle
            )
        }
    } // Column wrapper
}

@Composable
internal fun QuizSummaryContent(
    mode: TriviaGameMode,
    answeredHistory: List<AnsweredQuestionRecord>,
    sessionPoints: Int,
    maxSessionStreak: Int,
    triviaProgress: TriviaUserProgress,
    onPlayAgain: () -> Unit,
    onBackToHub: () -> Unit,
    onOpenShowDetail: (String) -> Unit,
    onOpenAlmanacArticle: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val totalAnswered = answeredHistory.size.coerceAtLeast(1)
    val correctCount = answeredHistory.count { it.isCorrect }
    val accuracy = ((correctCount.toFloat() / totalAnswered.toFloat()) * 100f).toInt()

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = 88.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("trivia_summary_container")
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = StealieCardDark),
                border = BorderStroke(1.5.dp, StealieGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    StealieEmblem(size = 58.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = when {
                            accuracy >= 80 -> "⚡ Cornell '77 Level Performance!"
                            accuracy >= 50 -> "🌹 Solid Second-Set Jam!"
                            else -> "🚌 Keep Riding the Bus!"
                        },
                        color = StealieGold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = "${mode.title} Complete • Current Rank: ${triviaProgress.rankTitle}",
                        color = TextSecondaryDark,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TriviaStatPill(
                            value = "+$sessionPoints",
                            label = "Points Earned",
                            accent = StealieGold,
                            modifier = Modifier.weight(1f)
                        )
                        TriviaStatPill(
                            value = "$correctCount/${answeredHistory.size}",
                            label = "$accuracy% Accuracy",
                            accent = EraEurope72Color,
                            modifier = Modifier.weight(1f)
                        )
                        TriviaStatPill(
                            value = "${maxSessionStreak}🔥",
                            label = "Peak Streak",
                            accent = StealieCrimsonLight,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    val context = LocalContext.current
                    OutlinedButton(
                        onClick = {
                            val lines = buildTriviaShareLines(
                                modeTitle = mode.title,
                                correctCount = correctCount,
                                answeredCount = answeredHistory.size,
                                sessionPoints = sessionPoints,
                                maxStreak = maxSessionStreak,
                                rankTitle = triviaProgress.rankTitle
                            )
                            ShareHelper.shareBitmap(
                                context,
                                renderShareCardBitmap(lines),
                                "trivia-${UUID.randomUUID()}.png",
                                context.getString(R.string.share_trivia)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, StealieGold.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("summary_share_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = null,
                            tint = StealieGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = context.getString(R.string.share_trivia),
                            color = StealieGold,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = onBackToHub,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("summary_back_to_hub_btn")
                        ) {
                            Text(
                                text = "Trivia Hub",
                                color = TextPrimaryDark,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onPlayAgain,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StealieGold,
                                contentColor = StealieMidnight
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("summary_play_again_btn")
                        ) {
                            Text(
                                text = "Encore Round ⚡",
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Round Setlist Breakdown",
                color = TextPrimaryDark,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() }
            )
        }

        itemsIndexed(answeredHistory) { index, rec ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = StealieCardDark),
                border = BorderStroke(
                    1.dp,
                    if (rec.isCorrect) EraEurope72Color.copy(alpha = 0.45f) else StealieCrimsonLight.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Q${index + 1} • ${rec.question.category.shortLabel}",
                            color = rec.question.category.accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (rec.isCorrect) "✓ +${rec.pointsEarned} pts" else "✗ Missed",
                            color = if (rec.isCorrect) EraEurope72Color else StealieCrimsonLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = rec.question.question,
                        color = TextPrimaryDark,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Answer: ${rec.question.options[rec.question.correctIndex]}",
                        color = EraEurope72Color,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = rec.question.linerNoteFact,
                        color = TextSecondaryDark,
                        fontSize = 11.sp
                    )
                    if (rec.question.relatedShowId != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Open Show Setlist (${rec.question.relatedShowId}) ➔",
                            color = StealieCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                onOpenShowDetail(rec.question.relatedShowId)
                            }
                        )
                    }
                    rec.question.relatedArticleId?.let { articleId ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Read more →",
                            color = StealieGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onOpenAlmanacArticle(articleId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun FlashcardDeckContent(
    questions: List<TriviaQuestion>,
    onOpenShowDetail: (String) -> Unit,
    onOpenAlmanacArticle: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(TriviaCategory.ALL) }
    var flippedCardIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val filteredQuestions = remember(questions, selectedCategory) {
        questions.filter { selectedCategory == TriviaCategory.ALL || it.category == selectedCategory }
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 10.dp,
            bottom = 88.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("trivia_flashcards_container")
    ) {
        item {
            Text(
                text = "Tap any card to flip between the question and the Archivist's Answer + Liner Notes.",
                color = TextSecondaryDark,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                TriviaCategory.entries.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        color = if (isSelected) cat.accentColor.copy(alpha = 0.22f) else StealieCardDark,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, if (isSelected) cat.accentColor else BorderSubtle),
                        modifier = Modifier.clickable { selectedCategory = cat }
                    ) {
                        Text(
                            text = "${cat.emoji} ${cat.shortLabel}",
                            color = if (isSelected) cat.accentColor else TextSecondaryDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        itemsIndexed(filteredQuestions, key = { _, q -> q.id }) { idx, q ->
            val isFlipped = q.id in flippedCardIds
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFlipped) StealieMidnight else StealieCardDark
                ),
                border = BorderStroke(
                    1.dp,
                    if (isFlipped) StealieGold else q.category.accentColor.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        flippedCardIds = if (isFlipped) flippedCardIds - q.id else flippedCardIds + q.id
                    }
                    .testTag("flashcard_item_$idx")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${q.category.emoji} ${q.category.shortLabel}",
                            color = q.category.accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isFlipped) "🔄 Tap to Hide Answer" else "👆 Tap to Reveal Answer",
                            color = if (isFlipped) StealieGold else TextTertiaryDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = q.question,
                        color = TextPrimaryDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    AnimatedVisibility(visible = isFlipped) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "✓ ${q.options[q.correctIndex]}",
                                color = EraEurope72Color,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = q.linerNoteFact,
                                color = TextSecondaryDark,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                            if (q.relatedShowId != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Inspect Show Setlist (${q.relatedShowId}) ➔",
                                    color = StealieCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        onOpenShowDetail(q.relatedShowId)
                                    }
                                )
                            }
                            q.relatedArticleId?.let { articleId ->
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Read more →",
                                    color = StealieGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { onOpenAlmanacArticle(articleId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun TriviaStatPill(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StealieMidnight,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
        ) {
            Text(
                text = value,
                color = accent,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
            Text(
                text = label,
                color = TextTertiaryDark,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun TriviaModeCard(
    title: String,
    subtitle: String,
    badgeText: String,
    accentColor: Color,
    buttonText: String,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StealieCardDark),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    color = TextPrimaryDark,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Surface(
                    color = accentColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                color = TextSecondaryDark,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = StealieMidnight
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag(testTag)
            ) {
                Text(
                    text = buttonText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

internal fun LazyListScope.triviaHubGameModeItems(
    selectedCategory: TriviaCategory,
    allAvailableQuestionsCount: Int,
    isDailyCompletedToday: Boolean,
    onStartMode: (TriviaGameMode) -> Unit,
    onOpenAlmanac: () -> Unit
) {
    item {
        Text(
            text = "Launch a Trivia Mode",
            color = StealieGold,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.8.sp
        )
    }

    item {
        TriviaModeCard(
            title = TriviaGameMode.CLASSIC_TOUR.title,
            subtitle = TriviaGameMode.CLASSIC_TOUR.subtitle,
            badgeText = "10 Qs • ${selectedCategory.shortLabel}",
            accentColor = StealieGold,
            buttonText = "Start Classic Tour Quiz ⚡",
            testTag = "start_classic_trivia_btn",
            onClick = { onStartMode(TriviaGameMode.CLASSIC_TOUR) }
        )
    }

    item {
        TriviaModeCard(
            title = TriviaGameMode.TERRAPIN_BLITZ.title,
            subtitle = TriviaGameMode.TERRAPIN_BLITZ.subtitle,
            badgeText = "⏱️ 60s Speed Multiplier",
            accentColor = StealieCyan,
            buttonText = "Launch 60s Terrapin Blitz 🐢",
            testTag = "start_blitz_trivia_btn",
            onClick = { onStartMode(TriviaGameMode.TERRAPIN_BLITZ) }
        )
    }

    item {
        TriviaModeCard(
            title = TriviaGameMode.DAILY_DEADHEAD.title,
            subtitle = TriviaGameMode.DAILY_DEADHEAD.subtitle,
            badgeText = if (isDailyCompletedToday) "✅ Completed Today" else "🌟 Daily Trophy Available",
            accentColor = EraSpring77Color,
            buttonText = if (isDailyCompletedToday) "Replay Today's Daily Five 🌹" else "Play Daily Deadhead Five 🌹",
            testTag = "start_daily_trivia_btn",
            onClick = { onStartMode(TriviaGameMode.DAILY_DEADHEAD) }
        )
    }

    item {
        TriviaModeCard(
            title = "Almanac",
            subtitle = "Bundled Markdown essays with cited sources — song stories, show spotlights, band guides, tributes",
            badgeText = "📖 Sourced articles",
            accentColor = StealieGold,
            buttonText = "Open Almanac →",
            testTag = "open_almanac_btn",
            onClick = onOpenAlmanac
        )
    }

    item {
        TriviaModeCard(
            title = TriviaGameMode.LORE_FLASHCARDS.title,
            subtitle = TriviaGameMode.LORE_FLASHCARDS.subtitle,
            badgeText = "$allAvailableQuestionsCount Interactive Cards",
            accentColor = StealiePurple,
            buttonText = "Open Archivist Flashcard Deck 📜",
            testTag = "open_flashcards_btn",
            onClick = { onStartMode(TriviaGameMode.LORE_FLASHCARDS) }
        )
    }

    item {
        TriviaModeCard(
            title = TriviaGameMode.SEGUE_SCHOOL.title,
            subtitle = TriviaGameMode.SEGUE_SCHOOL.subtitle,
            badgeText = "🎶 ->  >  ~>  Spaced repetition",
            accentColor = StealieCrimsonLight,
            buttonText = "Start Segue School ->",
            testTag = "start_segue_school_btn",
            onClick = { onStartMode(TriviaGameMode.SEGUE_SCHOOL) }
        )
    }
}

@Composable
internal fun TriviaRankScoreboardCard(
    triviaProgress: TriviaUserProgress,
    totalQuestionsCount: Int,
    vaultGeneratedCount: Int
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = StealieCardDark),
        border = BorderStroke(1.5.dp, StealieGold.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trivia_rank_scoreboard_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            StealieCrimson.copy(alpha = 0.22f),
                            StealieCardDark
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DEADHEAD TRIVIA STANDING",
                        color = StealieGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = triviaProgress.rankTitle,
                        color = TextPrimaryDark,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "$totalQuestionsCount total questions ($vaultGeneratedCount live from your Room Concert Vault)",
                        color = TextSecondaryDark,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                DancingBear(color = BearYellow, size = 44.dp)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TriviaStatPill(
                    value = "${triviaProgress.totalCorrect}/${triviaProgress.totalAnswered}",
                    label = "Correct",
                    accent = StealieCyan,
                    modifier = Modifier.weight(1f)
                )
                TriviaStatPill(
                    value = "${triviaProgress.accuracyPercent}%",
                    label = "Accuracy",
                    accent = StealieGold,
                    modifier = Modifier.weight(1f)
                )
                TriviaStatPill(
                    value = "${triviaProgress.bestStreak}🔥",
                    label = "Best Streak",
                    accent = StealieCrimsonLight,
                    modifier = Modifier.weight(1f)
                )
                TriviaStatPill(
                    value = "${triviaProgress.quizzesCompleted}",
                    label = "Tours Played",
                    accent = EraEurope72Color,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
internal fun TriviaSetupFiltersCard(
    selectedCategory: TriviaCategory,
    onSelectCategory: (TriviaCategory) -> Unit,
    selectedDifficulty: TriviaDifficulty?,
    onSelectDifficulty: (TriviaDifficulty?) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StealieCardDark),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trivia_setup_filters_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "1. Choose Trivia Topic & Era Lore",
                color = TextPrimaryDark,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                TriviaCategory.entries.forEach { category ->
                    val isSelected = selectedCategory == category
                    Surface(
                        color = if (isSelected) category.accentColor.copy(alpha = 0.22f) else StealieMidnight,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) category.accentColor else BorderSubtle
                        ),
                        modifier = Modifier
                            .clickable { onSelectCategory(category) }
                            .testTag("trivia_category_chip_${category.id}")
                    ) {
                        Text(
                            text = "${category.emoji} ${category.shortLabel}",
                            color = if (isSelected) category.accentColor else TextSecondaryDark,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "2. Select Difficulty Tier",
                color = TextPrimaryDark,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                val isAllDiff = selectedDifficulty == null
                Surface(
                    color = if (isAllDiff) StealieGold.copy(alpha = 0.2f) else StealieMidnight,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isAllDiff) StealieGold else BorderSubtle),
                    modifier = Modifier
                        .clickable { onSelectDifficulty(null) }
                        .testTag("trivia_diff_all")
                ) {
                    Text(
                        text = "All Tiers (Mixed)",
                        color = if (isAllDiff) StealieGold else TextSecondaryDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                    )
                }

                TriviaDifficulty.entries.forEach { diff ->
                    val isSelected = selectedDifficulty == diff
                    Surface(
                        color = if (isSelected) diff.badgeColor.copy(alpha = 0.2f) else StealieMidnight,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isSelected) diff.badgeColor else BorderSubtle),
                        modifier = Modifier
                            .clickable { onSelectDifficulty(diff) }
                            .testTag("trivia_diff_${diff.id}")
                    ) {
                        Text(
                            text = "${diff.label} (+${diff.basePoints})",
                            color = if (isSelected) diff.badgeColor else TextSecondaryDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun TriviaAccuracyByCategoryCard(
    accuracy: List<com.example.data.review.CategoryAccuracy>
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StealieCardDark),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("accuracy_by_category_card")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Your accuracy by category", color = TextPrimaryDark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            for (a in accuracy) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(a.label, color = TextSecondaryDark, fontSize = 12.sp, modifier = Modifier.width(120.dp))
                    LinearProgressIndicator(
                        progress = { a.percent / 100f },
                        color = StealieGold,
                        trackColor = StealieMidnight,
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${a.percent}% (${a.correct}/${a.answered})", color = TextSecondaryDark, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
internal fun TriviaMeritBadgesCard(triviaProgress: TriviaUserProgress) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StealieCardDark),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trivia_badges_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Deadhead Trivia Merit Badges",
                    color = TextPrimaryDark,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${triviaProgress.unlockedBadgeIds.size}/${DeadTriviaSeed.allBadges.size} Unlocked",
                    color = StealieGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DeadTriviaSeed.allBadges.forEach { badge ->
                    val unlocked = badge.id in triviaProgress.unlockedBadgeIds
                    Surface(
                        color = if (unlocked) badge.color.copy(alpha = 0.14f) else StealieMidnight,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (unlocked) badge.color.copy(alpha = 0.5f) else BorderSubtle
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (unlocked) badge.color.copy(alpha = 0.25f)
                                        else StealieCardDark
                                    )
                            ) {
                                Text(
                                    text = if (unlocked) badge.iconText else "🔒",
                                    fontSize = 18.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = badge.title,
                                    color = if (unlocked) TextPrimaryDark else TextSecondaryDark,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = badge.description,
                                    color = TextTertiaryDark,
                                    fontSize = 11.sp
                                )
                            }
                            if (unlocked) {
                                Surface(
                                    color = badge.color.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "EARNED",
                                        color = badge.color,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun TriviaActiveQuizStatusBar(
    mode: TriviaGameMode,
    questionIndex: Int,
    totalQuestions: Int,
    sessionStreak: Int,
    sessionPoints: Int,
    remainingSeconds: Int,
    animatedProgress: Float
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Question ${questionIndex + 1} of $totalQuestions",
                color = TextPrimaryDark,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (mode == TriviaGameMode.TERRAPIN_BLITZ) {
                    Surface(
                        color = if (remainingSeconds <= 15) StealieCrimson.copy(alpha = 0.25f) else StealieCyan.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            if (remainingSeconds <= 15) StealieCrimsonLight else StealieCyan
                        )
                    ) {
                        Text(
                            text = "⏱️ ${remainingSeconds}s",
                            color = if (remainingSeconds <= 15) StealieCrimsonLight else StealieCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }

                if (sessionStreak > 0) {
                    Surface(
                        color = StealieCrimsonLight.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, StealieCrimsonLight)
                    ) {
                        Text(
                            text = "🔥 ${sessionStreak}x Streak",
                            color = StealieCrimsonLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }

                Surface(
                    color = StealieGold.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "+$sessionPoints pts",
                        color = StealieGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { animatedProgress },
            color = StealieGold,
            trackColor = StealieCardDark,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )
    }
}

@Composable
internal fun TriviaActiveQuestionCard(question: TriviaQuestion) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = StealieCardDark),
        border = BorderStroke(1.dp, question.category.accentColor.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trivia_question_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = question.category.accentColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${question.category.emoji} ${question.category.shortLabel}",
                        color = question.category.accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (question.format != TriviaFormat.MULTIPLE_CHOICE) {
                        Surface(
                            color = StealiePurple.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = question.format.label,
                                color = StealiePurple,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (question.era != DeadEra.ALL) {
                        Surface(
                            color = question.era.color.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = question.era.years,
                                color = question.era.color,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Surface(
                        color = question.difficulty.badgeColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${question.difficulty.basePoints} PTS",
                            color = question.difficulty.badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = question.question,
                color = TextPrimaryDark,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 24.sp
            )
        }
    }
}

@Composable
internal fun TriviaQuizLifelinesRow(
    question: TriviaQuestion,
    selectedOptionIndex: Int?,
    usedAskBear5050: Boolean,
    usedFlipTapeSkip: Boolean,
    onUseAskBear5050: () -> Unit,
    onUseFlipTapeSkip: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        val canUse5050 = !usedAskBear5050 && selectedOptionIndex == null &&
            question.options.size >= 4
        OutlinedButton(
            onClick = onUseAskBear5050,
            enabled = canUse5050,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
                1.dp,
                if (canUse5050) StealieCyan else BorderSubtle
            ),
            modifier = Modifier
                .weight(1f)
                .testTag("lifeline_5050_btn")
        ) {
            Text(
                text = if (usedAskBear5050) "🎛️ Bear 50/50 Used" else "🎛️ Ask Bear (50/50)",
                color = if (canUse5050) StealieCyan else TextTertiaryDark,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        val canUseSkip = !usedFlipTapeSkip && selectedOptionIndex == null
        OutlinedButton(
            onClick = onUseFlipTapeSkip,
            enabled = canUseSkip,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
                1.dp,
                if (canUseSkip) StealieGold else BorderSubtle
            ),
            modifier = Modifier
                .weight(1f)
                .testTag("lifeline_skip_btn")
        ) {
            Text(
                text = if (usedFlipTapeSkip) "📼 Tape Flipped" else "📼 Flip Tape (Skip)",
                color = if (canUseSkip) StealieGold else TextTertiaryDark,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
internal fun TriviaQuizAnswerOption(
    index: Int,
    optionText: String,
    question: TriviaQuestion,
    selectedOptionIndex: Int?,
    hiddenOptionIndices: Set<Int>,
    onSelectOption: (Int) -> Unit
) {
    val isHiddenBy5050 = index in hiddenOptionIndices
    val isAnswered = selectedOptionIndex != null
    val isSelected = selectedOptionIndex == index
    val isCorrectOption = index == question.correctIndex

    val containerColor = when {
        isHiddenBy5050 -> StealieMidnight.copy(alpha = 0.35f)
        !isAnswered -> StealieCardDark
        isCorrectOption -> EraEurope72Color.copy(alpha = 0.22f)
        isSelected && !isCorrectOption -> StealieCrimson.copy(alpha = 0.25f)
        else -> StealieCardDark.copy(alpha = 0.6f)
    }

    val borderColor = when {
        isHiddenBy5050 -> BorderSubtle.copy(alpha = 0.3f)
        !isAnswered -> BorderSubtle
        isCorrectOption -> EraEurope72Color
        isSelected && !isCorrectOption -> StealieCrimsonLight
        else -> BorderSubtle
    }

    val optionLetter = ('A' + index).toString()

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, borderColor),
        onClick = {
            if (!isAnswered && !isHiddenBy5050) onSelectOption(index)
        },
        enabled = !isAnswered && !isHiddenBy5050,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trivia_option_$index")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isAnswered && isCorrectOption -> EraEurope72Color
                            isAnswered && isSelected -> StealieCrimsonLight
                            else -> StealieMidnight
                        }
                    )
            ) {
                Text(
                    text = when {
                        isAnswered && isCorrectOption -> "✓"
                        isAnswered && isSelected -> "✗"
                        else -> optionLetter
                    },
                    color = if (isAnswered && (isCorrectOption || isSelected)) StealieMidnight else StealieGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = if (isHiddenBy5050) "— Eliminated by Bear's Soundboard —" else optionText,
                color = if (isHiddenBy5050) TextTertiaryDark else TextPrimaryDark,
                fontSize = 14.sp,
                fontWeight = if (isAnswered && isCorrectOption) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
internal fun TriviaQuizExplanationPanel(
    question: TriviaQuestion,
    questionIndex: Int,
    totalQuestions: Int,
    selectedOptionIndex: Int,
    onNextQuestion: () -> Unit,
    onOpenShowDetail: (String) -> Unit,
    onOpenAlmanacArticle: (String) -> Unit
) {
    val isCorrect = selectedOptionIndex == question.correctIndex
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StealieCardDark),
        border = BorderStroke(
            1.dp,
            if (isCorrect) EraEurope72Color else StealieGold
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("trivia_explanation_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isCorrect) "⚡ Right On! Deadhead Bullseye" else "🌹 Archivist Liner Note",
                    color = if (isCorrect) EraEurope72Color else StealieGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                if (question.relatedShowId != null) {
                    TextButton(
                        onClick = { onOpenShowDetail(question.relatedShowId) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Inspect Show Setlist ➔",
                            color = StealieCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = question.linerNoteFact,
                color = TextSecondaryDark,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
            val sourceUrl = question.sourceUrl
            if (!sourceUrl.isNullOrBlank()) {
                val uriHandler = LocalUriHandler.current
                TextButton(
                    onClick = { uriHandler.openUri(sourceUrl) },
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Source",
                        color = StealieCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            question.relatedArticleId?.let { articleId ->
                TextButton(
                    onClick = { onOpenAlmanacArticle(articleId) },
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("trivia_read_more")
                ) {
                    Text(
                        text = "Read more →",
                        color = StealieGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onNextQuestion,
                colors = ButtonDefaults.buttonColors(
                    containerColor = StealieGold,
                    contentColor = StealieMidnight
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("trivia_next_question_btn")
            ) {
                Text(
                    text = if (questionIndex + 1 < totalQuestions) {
                        "Next Question ➔"
                    } else {
                        "View Encore Scorecard 🏆"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
internal fun TriviaScreenStageBody(
    stage: TriviaStage,
    shows: List<ShowEntity>,
    parseSongs: (ShowEntity) -> List<SongEntry>,
    triviaProgress: TriviaUserProgress,
    selectedCategory: TriviaCategory,
    onSelectedCategoryChange: (TriviaCategory) -> Unit,
    selectedDifficulty: TriviaDifficulty?,
    onSelectedDifficultyChange: (TriviaDifficulty?) -> Unit,
    activeMode: TriviaGameMode,
    activeDeck: List<TriviaQuestion>,
    currentIndex: Int,
    onCurrentIndexChange: (Int) -> Unit,
    selectedOptionIndex: Int?,
    onSelectedOptionIndexChange: (Int?) -> Unit,
    hiddenOptionIndices: Set<Int>,
    onHiddenOptionIndicesChange: (Set<Int>) -> Unit,
    usedAskBear5050: Boolean,
    onUsedAskBear5050Change: (Boolean) -> Unit,
    usedFlipTapeSkip: Boolean,
    onUsedFlipTapeSkipChange: (Boolean) -> Unit,
    sessionStreak: Int,
    onSessionStreakChange: (Int) -> Unit,
    maxSessionStreak: Int,
    onMaxSessionStreakChange: (Int) -> Unit,
    sessionPoints: Int,
    onSessionPointsChange: (Int) -> Unit,
    answeredHistory: List<AnsweredQuestionRecord>,
    onAnsweredHistoryChange: (List<AnsweredQuestionRecord>) -> Unit,
    remainingSeconds: Int,
    onStageChange: (TriviaStage) -> Unit,
    onStartMode: (TriviaGameMode) -> Unit,
    reviewStates: Map<String, com.example.data.review.ReviewState>,
    onOpenAlmanac: () -> Unit,
    onRecordQuizCompletion: (
        answeredCount: Int,
        correctCount: Int,
        pointsEarned: Int,
        maxStreakInRound: Int,
        mode: TriviaGameMode
    ) -> Unit,
    onCardAnswered: (cardId: String, correct: Boolean) -> Unit,
    onOpenShowDetail: (String) -> Unit,
    onOpenAlmanacArticle: (String) -> Unit,
    innerPadding: PaddingValues
) {
    when (stage) {
        TriviaStage.HUB -> {
            TriviaHubContent(
                shows = shows,
                parseSongs = parseSongs,
                triviaProgress = triviaProgress,
                selectedCategory = selectedCategory,
                onSelectCategory = onSelectedCategoryChange,
                selectedDifficulty = selectedDifficulty,
                onSelectDifficulty = onSelectedDifficultyChange,
                onStartMode = onStartMode,
                reviewStates = reviewStates,
                onOpenAlmanac = onOpenAlmanac,
                modifier = Modifier.padding(innerPadding)
            )
        }

        TriviaStage.ACTIVE_QUIZ -> {
            TriviaActiveQuizStage(
                activeMode = activeMode,
                activeDeck = activeDeck,
                currentIndex = currentIndex,
                onCurrentIndexChange = onCurrentIndexChange,
                selectedOptionIndex = selectedOptionIndex,
                onSelectedOptionIndexChange = onSelectedOptionIndexChange,
                hiddenOptionIndices = hiddenOptionIndices,
                onHiddenOptionIndicesChange = onHiddenOptionIndicesChange,
                usedAskBear5050 = usedAskBear5050,
                onUsedAskBear5050Change = onUsedAskBear5050Change,
                usedFlipTapeSkip = usedFlipTapeSkip,
                onUsedFlipTapeSkipChange = onUsedFlipTapeSkipChange,
                sessionStreak = sessionStreak,
                onSessionStreakChange = onSessionStreakChange,
                maxSessionStreak = maxSessionStreak,
                onMaxSessionStreakChange = onMaxSessionStreakChange,
                sessionPoints = sessionPoints,
                onSessionPointsChange = onSessionPointsChange,
                answeredHistory = answeredHistory,
                onAnsweredHistoryChange = onAnsweredHistoryChange,
                remainingSeconds = remainingSeconds,
                onStageChange = onStageChange,
                onRecordQuizCompletion = onRecordQuizCompletion,
                onCardAnswered = onCardAnswered,
                onOpenShowDetail = onOpenShowDetail,
                onOpenAlmanacArticle = onOpenAlmanacArticle,
                innerPadding = innerPadding
            )
        }

        TriviaStage.QUIZ_SUMMARY -> {
            QuizSummaryContent(
                mode = activeMode,
                answeredHistory = answeredHistory,
                sessionPoints = sessionPoints,
                maxSessionStreak = maxSessionStreak,
                triviaProgress = triviaProgress,
                onPlayAgain = { onStartMode(activeMode) },
                onBackToHub = { onStageChange(TriviaStage.HUB) },
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

@Composable
internal fun TriviaScreenTopBar(
    stage: TriviaStage,
    activeMode: TriviaGameMode,
    triviaProgress: TriviaUserProgress,
    onBackToHub: () -> Unit
) {
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
                        onClick = onBackToHub,
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
}

@Composable
internal fun TriviaActiveQuizStage(
    activeMode: TriviaGameMode,
    activeDeck: List<TriviaQuestion>,
    currentIndex: Int,
    onCurrentIndexChange: (Int) -> Unit,
    selectedOptionIndex: Int?,
    onSelectedOptionIndexChange: (Int?) -> Unit,
    hiddenOptionIndices: Set<Int>,
    onHiddenOptionIndicesChange: (Set<Int>) -> Unit,
    usedAskBear5050: Boolean,
    onUsedAskBear5050Change: (Boolean) -> Unit,
    usedFlipTapeSkip: Boolean,
    onUsedFlipTapeSkipChange: (Boolean) -> Unit,
    sessionStreak: Int,
    onSessionStreakChange: (Int) -> Unit,
    maxSessionStreak: Int,
    onMaxSessionStreakChange: (Int) -> Unit,
    sessionPoints: Int,
    onSessionPointsChange: (Int) -> Unit,
    answeredHistory: List<AnsweredQuestionRecord>,
    onAnsweredHistoryChange: (List<AnsweredQuestionRecord>) -> Unit,
    remainingSeconds: Int,
    onStageChange: (TriviaStage) -> Unit,
    onRecordQuizCompletion: (
        answeredCount: Int,
        correctCount: Int,
        pointsEarned: Int,
        maxStreakInRound: Int,
        mode: TriviaGameMode
    ) -> Unit,
    onCardAnswered: (cardId: String, correct: Boolean) -> Unit,
    onOpenShowDetail: (String) -> Unit,
    onOpenAlmanacArticle: (String) -> Unit,
    innerPadding: PaddingValues
) {
    val currentQ = activeDeck.getOrNull(currentIndex)
    if (currentQ != null) {
        fun finishQuiz() {
            val correctCount = answeredHistory.count { it.isCorrect }
            onRecordQuizCompletion(
                answeredHistory.size,
                correctCount,
                sessionPoints,
                maxSessionStreak,
                activeMode
            )
            onStageChange(TriviaStage.QUIZ_SUMMARY)
        }

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
                    onSelectedOptionIndexChange(idx)
                    val isCorrect = idx == currentQ.correctIndex
                    onCardAnswered(currentQ.id, isCorrect)
                    val newStreak = if (isCorrect) sessionStreak + 1 else 0
                    onSessionStreakChange(newStreak)
                    if (newStreak > maxSessionStreak) {
                        onMaxSessionStreakChange(newStreak)
                    }
                    val streakBonus = if (isCorrect) (newStreak - 1).coerceAtLeast(0) * 35 else 0
                    val speedBonus = if (isCorrect && activeMode == TriviaGameMode.TERRAPIN_BLITZ) 40 else 0
                    val earned = if (isCorrect) currentQ.difficulty.basePoints + streakBonus + speedBonus else 0
                    onSessionPointsChange(sessionPoints + earned)
                    onAnsweredHistoryChange(
                        answeredHistory + AnsweredQuestionRecord(
                            question = currentQ,
                            selectedOptionIndex = idx,
                            isCorrect = isCorrect,
                            pointsEarned = earned
                        )
                    )
                }
            },
            onUseAskBear5050 = {
                if (!usedAskBear5050 && selectedOptionIndex == null &&
                    currentQ.options.size >= 4
                ) {
                    onUsedAskBear5050Change(true)
                    val wrongIndices = currentQ.options.indices
                        .filter { it != currentQ.correctIndex }
                        .shuffled()
                        .take(2)
                        .toSet()
                    onHiddenOptionIndicesChange(wrongIndices)
                }
            },
            onUseFlipTapeSkip = {
                if (!usedFlipTapeSkip && selectedOptionIndex == null) {
                    onUsedFlipTapeSkipChange(true)
                    if (currentIndex + 1 < activeDeck.size) {
                        onCurrentIndexChange(currentIndex + 1)
                        onSelectedOptionIndexChange(null)
                        onHiddenOptionIndicesChange(emptySet())
                    } else {
                        finishQuiz()
                    }
                }
            },
            onNextQuestion = {
                if (currentIndex + 1 < activeDeck.size) {
                    onCurrentIndexChange(currentIndex + 1)
                    onSelectedOptionIndexChange(null)
                    onHiddenOptionIndicesChange(emptySet())
                } else {
                    finishQuiz()
                }
            },
            onOpenShowDetail = onOpenShowDetail,
            onOpenAlmanacArticle = onOpenAlmanacArticle,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
