package com.example.ui

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.CatalogAssetImporter
import com.example.data.local.ShowEntity
import com.example.data.local.UserPrefsStore
import com.example.data.model.DeadEra
import com.example.data.model.SongEntry
import com.example.data.model.TransitionType
import com.example.data.repository.ShowRepository
import com.example.di.AppContainer
import com.example.ui.navigation.NavCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShowsViewModelRobolectricTest {

    private lateinit var app: Application
    private lateinit var db: AppDatabase
    private lateinit var prefs: UserPrefsStore
    private lateinit var repository: ShowRepository
    private lateinit var ui: MutableStateFlow<UiState>
    private lateinit var store: ViewModelStore
    private lateinit var navigation: NavigationViewModel
    private lateinit var vm: ShowsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        AppContainer.resetForTests()
        app = ApplicationProvider.getApplicationContext()
        db = AppDatabase.createInMemoryDatabase(app)
        prefs = UserPrefsStore(app)
        prefs.setImportedCatalogVersion(CatalogAssetImporter.BUNDLED_CATALOG_VERSION)
        repository = ShowRepository(db.showDao(), db.songDao())
        ui = MutableStateFlow(UiState())
        store = ViewModelStore()
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return when {
                    modelClass.isAssignableFrom(NavigationViewModel::class.java) ->
                        NavigationViewModel(ui) as T
                    modelClass.isAssignableFrom(ShowsViewModel::class.java) ->
                        ShowsViewModel(app, db, repository, prefs, ui, navigation) as T
                    else -> throw IllegalArgumentException(modelClass.name)
                }
            }
        }
        navigation = ViewModelProvider(store, factory)[NavigationViewModel::class.java]
        vm = ViewModelProvider(store, factory)[ShowsViewModel::class.java]
    }

    @After
    fun tearDown() {
        // Cancel ViewModel scopes (seed/collect) before closing the in-memory DB.
        store.clear()
        db.close()
        AppContainer.resetForTests()
        Dispatchers.resetMain()
    }

    @Test
    fun `filterBySegue sets filter tab and nav then clears on second call`() = runBlocking {
        repository.ensureSongCatalog()
        val match = showEntity("h1-scarlet-fire", "1977-05-08", DeadEra.SPRING_77.id)
        val other = showEntity("h1-truckin-only", "1978-01-01", DeadEra.SPRING_77.id)
        repository.saveShow(
            match,
            listOf(
                SongEntry("Scarlet Begonias", transitionOut = TransitionType.SEGUE),
                SongEntry("Fire on the Mountain", transitionOut = TransitionType.NONE)
            )
        )
        repository.saveShow(
            other,
            listOf(SongEntry("Truckin'", transitionOut = TransitionType.NONE))
        )
        awaitShow("h1-scarlet-fire")
        awaitShow("h1-truckin-only")

        val navDeferred = async { navigation.navigation.take(2).toList() }

        val chain = "Scarlet Begonias ➔ Fire on the Mountain"
        vm.filterBySegue(chain)
        assertEquals(chain, ui.value.selectedSegueFilter)
        assertEquals(MainTab.SHOWS, ui.value.currentTab)
        assertTrue(ui.value.filteredShows.any { it.id == "h1-scarlet-fire" })
        assertTrue(ui.value.filteredShows.none { it.id == "h1-truckin-only" })

        vm.filterBySegue(chain)
        assertNull(ui.value.selectedSegueFilter)
        assertTrue(ui.value.filteredShows.any { it.id == "h1-scarlet-fire" })
        assertTrue(ui.value.filteredShows.any { it.id == "h1-truckin-only" })

        val cmds = withTimeout(5_000) { navDeferred.await() }
        assertEquals(listOf(NavCommand.ToShowsTab, NavCommand.ToShowsTab), cmds)
    }

    @Test
    fun `clearSegueFilter clears selectedSegueFilter`() = runBlocking {
        repository.ensureSongCatalog()
        val match = showEntity("h1-scarlet-fire", "1977-05-08", DeadEra.SPRING_77.id)
        val other = showEntity("h1-truckin-only", "1978-01-01", DeadEra.SPRING_77.id)
        repository.saveShow(
            match,
            listOf(
                SongEntry("Scarlet Begonias", transitionOut = TransitionType.SEGUE),
                SongEntry("Fire on the Mountain", transitionOut = TransitionType.NONE)
            )
        )
        repository.saveShow(
            other,
            listOf(SongEntry("Truckin'", transitionOut = TransitionType.NONE))
        )
        awaitShow("h1-scarlet-fire")
        awaitShow("h1-truckin-only")
        vm.filterBySegue("Scarlet Begonias ➔ Fire on the Mountain")
        assertEquals("Scarlet Begonias ➔ Fire on the Mountain", ui.value.selectedSegueFilter)
        assertTrue(ui.value.filteredShows.none { it.id == "h1-truckin-only" })

        vm.clearSegueFilter()
        assertNull(ui.value.selectedSegueFilter)
        assertTrue(ui.value.filteredShows.any { it.id == "h1-scarlet-fire" })
        assertTrue(ui.value.filteredShows.any { it.id == "h1-truckin-only" })
    }

    @Test
    fun `deleteShow sends Back and sets feedback`() = runBlocking {
        repository.ensureSongCatalog()
        val show = showEntity("h1-delete-me", "1977-05-08", DeadEra.SPRING_77.id)
        repository.saveShow(show, emptyList())
        awaitShow("h1-delete-me")
        val stored = ui.value.shows.first { it.id == "h1-delete-me" }

        val navDeferred = async { navigation.navigation.first() }
        vm.deleteShow(stored)
        withTimeout(5_000) {
            ui.first { it.userFeedbackMessage == "Show deleted from catalog" }
        }
        assertEquals(NavCommand.Back, withTimeout(5_000) { navDeferred.await() })
    }

    @Test
    fun `setEra refilters filteredShows`() = runBlocking {
        repository.ensureSongCatalog()
        val spring = showEntity("h1-spring", "1977-05-08", DeadEra.SPRING_77.id)
        val early = showEntity("h1-early", "1972-05-01", DeadEra.EARLY_70S.id)
        repository.saveShow(spring, emptyList())
        repository.saveShow(early, emptyList())
        awaitShow("h1-spring")
        awaitShow("h1-early")

        vm.setEra(DeadEra.SPRING_77)
        assertEquals(DeadEra.SPRING_77, ui.value.selectedEra)
        assertTrue(ui.value.filteredShows.any { it.id == "h1-spring" })
        assertTrue(ui.value.filteredShows.none { it.id == "h1-early" })
    }

    private suspend fun awaitShow(id: String) {
        withTimeout(15_000) {
            ui.first { state -> state.shows.any { it.id == id } }
        }
    }

    private fun showEntity(id: String, date: String, eraId: String) = ShowEntity(
        id = id,
        date = date,
        venue = "Test Venue",
        city = "Test City",
        stateOrCountry = "CA",
        eraId = eraId,
        tourName = "",
        bandName = "Grateful Dead"
    )
}
