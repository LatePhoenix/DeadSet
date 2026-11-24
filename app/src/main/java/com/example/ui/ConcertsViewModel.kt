package com.example.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LatLon
import com.example.data.local.UserPrefsStore
import com.example.data.repository.ConcertRepository
import com.example.ui.screens.ConcertsUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

/** Tickets / Live tab: followed bands, cached events, alerts, home geocode (6.2). */
class ConcertsViewModel(
    private val app: Application,
    private val concertRepository: ConcertRepository,
    private val userPrefs: UserPrefsStore
) : ViewModel() {

    private val homeLookupGeneration = AtomicInteger(0)

    private val _concerts = MutableStateFlow(ConcertsUiState())
    val concerts: StateFlow<ConcertsUiState> = _concerts.asStateFlow()

    init {
        viewModelScope.launch { concertRepository.ensureDefaultBands() }
        viewModelScope.launch {
            concertRepository.bands.collect { bands -> _concerts.update { it.copy(bands = bands) } }
        }
        viewModelScope.launch {
            concertRepository.upcomingEvents().collect { events ->
                _concerts.update { it.copy(events = events) }
            }
        }
        if (userPrefs.alertsEnabled()) setAlertsEnabled(true)
    }

    fun refreshConcerts() {
        if (_concerts.value.isRefreshing) return
        viewModelScope.launch {
            _concerts.update { it.copy(isRefreshing = true) }
            val outcome = runCatching { concertRepository.refresh() }.getOrNull()
            _concerts.update { it.copy(isRefreshing = false, lastOutcome = outcome ?: it.lastOutcome) }
        }
    }

    fun addBand(name: String) = viewModelScope.launch { concertRepository.addBand(name) }

    fun setBandFollowed(bandId: String, followed: Boolean) =
        viewModelScope.launch { concertRepository.setFollowed(bandId, followed) }

    fun removeBand(bandId: String) = viewModelScope.launch { concertRepository.removeBand(bandId) }

    fun setAlertsEnabled(enabled: Boolean) {
        userPrefs.setAlertsEnabled(enabled)
        // WorkManager may be unavailable in unit tests; prefs + UI state still update.
        runCatching {
            if (enabled) com.example.notifications.ConcertAlertsWorker.schedule(app)
            else com.example.notifications.ConcertAlertsWorker.cancel(app)
        }
        _concerts.update { it.copy(alertsEnabled = enabled) }
    }

    fun locateHome(homeCity: String) {
        val generation = homeLookupGeneration.incrementAndGet()
        viewModelScope.launch {
            val home = withContext(Dispatchers.IO) {
                runCatching {
                    @Suppress("DEPRECATION")
                    android.location.Geocoder(app)
                        .getFromLocationName(homeCity, 1)
                        ?.firstOrNull()
                        ?.let { LatLon(it.latitude, it.longitude) }
                }.getOrNull()
            }
            if (generation != homeLookupGeneration.get()) return@launch
            _concerts.update { it.copy(home = home, homeLabel = homeCity.substringBefore(",").trim()) }
        }
    }
}
