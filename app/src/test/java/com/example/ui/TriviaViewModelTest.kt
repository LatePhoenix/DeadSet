package com.example.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.UserPrefsStore
import com.example.data.seed.TriviaGameMode
import com.example.data.seed.TriviaUserProgress
import com.example.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TriviaViewModelTest {

    private lateinit var db: AppDatabase
    private lateinit var prefs: UserPrefsStore
    private lateinit var ui: MutableStateFlow<UiState>
    private lateinit var vm: TriviaViewModel

    @Before
    fun setUp() {
        AppContainer.resetForTests()
        val app = ApplicationProvider.getApplicationContext<Application>()
        db = AppDatabase.createInMemoryDatabase(app)
        prefs = UserPrefsStore(app)
        prefs.saveTriviaProgress(TriviaUserProgress())
        ui = MutableStateFlow(UiState(triviaProgress = TriviaUserProgress()))
        vm = TriviaViewModel(db, prefs, ui)
    }

    @After
    fun tearDown() {
        db.close()
        AppContainer.resetForTests()
    }

    @Test
    fun `quiz completion awards points and first miracle badge`() {
        vm.recordTriviaQuizCompletion(
            answeredCount = 3,
            correctCount = 2,
            pointsEarned = 40,
            maxStreakInRound = 2,
            mode = TriviaGameMode.CLASSIC_TOUR
        )
        val progress = ui.value.triviaProgress
        assertEquals(3, progress.totalAnswered)
        assertEquals(2, progress.totalCorrect)
        assertEquals(40, progress.totalPoints)
        assertEquals(1, progress.quizzesCompleted)
        assertTrue(progress.unlockedBadgeIds.contains("first_miracle"))
        assertEquals(40, prefs.loadTriviaProgress().totalPoints)
    }
}
