package com.example.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.UserPrefsStore
import com.example.data.repository.ConcertRepository
import com.example.di.AppContainer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConcertsViewModelTest {

    private lateinit var db: AppDatabase
    private lateinit var prefs: UserPrefsStore
    private lateinit var repository: ConcertRepository
    private lateinit var vm: ConcertsViewModel

    @Before
    fun setUp() {
        AppContainer.resetForTests()
        val app = ApplicationProvider.getApplicationContext<Application>()
        db = AppDatabase.createInMemoryDatabase(app)
        prefs = UserPrefsStore(app)
        prefs.setAlertsEnabled(false)
        repository = ConcertRepository(db.concertDao())
        vm = ConcertsViewModel(app, repository, prefs)
    }

    @After
    fun tearDown() {
        db.close()
        AppContainer.resetForTests()
    }

    @Test
    fun `starts with alerts disabled when prefs say so`() {
        assertFalse(prefs.alertsEnabled())
        assertFalse(vm.concerts.value.alertsEnabled)
    }

    @Test
    fun `addBand writes followed band`() = runBlocking {
        vm.addBand("Test Tribute Band")
        // Give the viewModelScope launch a tick
        kotlinx.coroutines.delay(100)
        val names = db.concertDao().followedBands().map { it.name }
        assertTrue(names.any { it.equals("Test Tribute Band", ignoreCase = true) })
    }
}
