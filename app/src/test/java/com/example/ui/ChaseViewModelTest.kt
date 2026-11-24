package com.example.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.ShowRepository
import com.example.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChaseViewModelTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ShowRepository
    private lateinit var vm: ChaseViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        AppContainer.resetForTests()
        val app = ApplicationProvider.getApplicationContext<Application>()
        db = AppDatabase.createInMemoryDatabase(app)
        repository = ShowRepository(db.showDao(), db.songDao())
        vm = ChaseViewModel(db, repository) { UiState() }
    }

    @After
    fun tearDown() {
        db.close()
        AppContainer.resetForTests()
        Dispatchers.resetMain()
    }

    @Test
    fun `predictNext with empty catalog returns empty`() {
        assertEquals(emptyList<String>(), vm.predictNext("missing", null, com.example.data.model.SetType.SET_1))
    }

    @Test
    fun `addChaseSong persists Scarlet Begonias`() = runBlocking {
        repository.ensureSongCatalog()
        vm.addChaseSong("Scarlet Begonias")
        withTimeout(5_000) {
            vm.chaseSongs.first { list ->
                list.any { it.songName.equals("Scarlet Begonias", ignoreCase = true) }
            }
        }
        assertTrue(
            vm.chaseSongs.value.any { it.songName.equals("Scarlet Begonias", ignoreCase = true) }
        )
    }
}
