package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.data.almanac.AlmanacArticle
import com.example.data.local.ShowEntity
import com.example.data.model.SongEntry
import com.example.data.review.ReviewState
import com.example.data.seed.TriviaGameMode
import com.example.data.seed.TriviaUserProgress
import com.example.ui.theme.StealieGold
import com.example.ui.theme.StealieMidnight
import com.example.ui.theme.TextSecondaryDark

private val ALMANAC_PANES = listOf("Trivia", "Articles")

/**
 * Almanac tab (6.1): Trivia hub + bundled Articles behind a secondary tab row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlmanacTabScreen(
    articles: List<AlmanacArticle>,
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
    reviewStates: Map<String, ReviewState>,
    onCardAnswered: (cardId: String, correct: Boolean) -> Unit,
    onOpenAlmanacArticle: (String) -> Unit,
    /** When true, switch to the Articles pane (e.g. deep link from elsewhere). */
    preferArticles: Boolean = false,
    onArticlesShown: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var pane by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(preferArticles) {
        if (preferArticles) {
            pane = 1
            onArticlesShown()
        }
    }
    Column(modifier.fillMaxSize()) {
        SecondaryTabRow(
            selectedTabIndex = pane,
            containerColor = StealieMidnight,
            contentColor = StealieGold,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("almanac_pane_tabs")
        ) {
            ALMANAC_PANES.forEachIndexed { index, title ->
                Tab(
                    selected = pane == index,
                    onClick = { pane = index },
                    text = {
                        Text(
                            title,
                            fontSize = 13.sp,
                            fontWeight = if (pane == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (pane == index) StealieGold else TextSecondaryDark
                        )
                    },
                    modifier = Modifier.testTag("almanac_pane_$index")
                )
            }
        }
        when (pane) {
            0 -> TriviaScreen(
                shows = shows,
                parseSongs = parseSongs,
                triviaProgress = triviaProgress,
                onRecordQuizCompletion = onRecordQuizCompletion,
                onOpenShowDetail = onOpenShowDetail,
                reviewStates = reviewStates,
                onCardAnswered = onCardAnswered,
                onOpenAlmanac = { pane = 1 },
                onOpenAlmanacArticle = onOpenAlmanacArticle
            )
            else -> AlmanacListScreen(
                articles = articles,
                onOpenArticle = onOpenAlmanacArticle,
                onBack = { pane = 0 },
                showBack = false
            )
        }
    }
}
