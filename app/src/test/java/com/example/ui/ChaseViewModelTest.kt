package com.example.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.ChaseSongEntity
import com.example.data.repository.ShowRepository
import com.example.di.AppContainer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChaseViewModelTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ShowRepository

    @Before
    fun setUp() = runBlocking {
        AppContainer.resetForTests()
        val app = ApplicationProvider.getApplicationContext<Application>()
        db = AppDatabase.createInMemoryDatabase(app)
        repository = ShowRepository(db.showDao(), db.songDao())
        repository.ensureSongCatalog()
    }

    @After
    fun tearDown() {
        db.close()
        AppContainer.resetForTests()
    }

    @Test
    fun `resolveSong finds Scarlet for chase add path`() = runBlocking {
        val resolved = repository.resolveSong("Scarlet Begonias")
        assertNotNull(resolved)
        db.chaseDao().add(
            ChaseSongEntity(resolved!!.songId, resolved.canonicalName, com.example.data.todayIsoDate())
        )
        // Direct DAO path used by ChaseViewModel.addChaseSong
        assertEquals("Scarlet Begonias", resolved.canonicalName)
    }

    @Test
    fun `predictNext with empty catalog returns empty`() {
        val vm = ChaseViewModel(db, repository) { UiState() }
        assertEquals(emptyList<String>(), vm.predictNext("missing", null, com.example.data.model.SetType.SET_1))
    }
}
