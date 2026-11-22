package com.example.data.repository

import com.example.data.local.BandEntity
import com.example.data.local.ConcertDao
import com.example.data.local.EventEntity
import com.example.data.remote.LiveConcertApiClient
import com.example.data.remote.LiveConcertEvent
import com.example.data.todayIsoDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Outcome of a refresh, so the Tickets tab can explain an empty list honestly. */
data class RefreshOutcome(
    val eventCount: Int,
    val sourcesReached: Set<String>,
    val ticketmasterConfigured: Boolean
) {
    val anySourceReached: Boolean get() = sourcesReached.isNotEmpty()
}

/**
 * Followed bands and their cached upcoming events (Phase 4). Events come only from real sources
 * (Bandsintown, Ticketmaster) and are cached in Room with their source and last-checked time.
 */
class ConcertRepository(
    private val dao: ConcertDao,
    private val api: LiveConcertApiClient = LiveConcertApiClient(),
    /** Where events come from; defaults to the real APIs, swapped for a fake in tests. */
    private val fetchEvents: suspend (List<String>) -> LiveConcertApiClient.FetchResult = api::fetchEventsForBands,
    private val clock: () -> Long = System::currentTimeMillis
) {
    val bands: Flow<List<BandEntity>> = dao.observeBands()

    fun upcomingEvents(today: String = todayIsoDate()): Flow<List<Pair<LiveConcertEvent, Long>>> =
        dao.observeUpcomingEvents(today).map { rows -> rows.map { it.toEvent() to it.lastCheckedAt } }

    /** Seeds the built-in bands once; Terrapin Flyer, DSO and JRAD start followed. */
    suspend fun ensureDefaultBands() = withContext(Dispatchers.IO) {
        dao.insertBands(DEFAULT_BANDS)
    }

    suspend fun addBand(name: String) = withContext(Dispatchers.IO) {
        val clean = name.trim().replace(Regex("\\s+"), " ")
        if (clean.isNotEmpty()) {
            dao.insertBands(listOf(BandEntity(BandEntity.idFor(clean), clean, isFollowed = true)))
            dao.setFollowed(BandEntity.idFor(clean), true)
        }
    }

    suspend fun setFollowed(bandId: String, followed: Boolean) = withContext(Dispatchers.IO) {
        dao.setFollowed(bandId, followed)
    }

    suspend fun removeBand(bandId: String) = withContext(Dispatchers.IO) { dao.deleteUserBand(bandId) }

    /**
     * Fetches followed bands' dates. The cache for those bands is replaced only if at least one
     * source answered; otherwise the existing (still dated) cache is kept.
     */
    suspend fun refresh(): RefreshOutcome = withContext(Dispatchers.IO) {
        val today = todayIsoDate()
        dao.deletePastEvents(today)
        val followed = dao.followedBands().map { it.name }
        if (followed.isEmpty()) return@withContext RefreshOutcome(0, emptySet(), api.isTicketmasterConfigured())
        val result = fetchEvents(followed)
        if (result.sourcesReached.isNotEmpty()) {
            val now = clock()
            dao.replaceEventsForBands(followed, result.events.map { EventEntity.from(it, now) })
        }
        RefreshOutcome(result.events.size, result.sourcesReached, result.ticketmasterConfigured)
    }

    companion object {
        val DEFAULT_BANDS = listOf(
            BandEntity("terrapin flyer", "Terrapin Flyer", isFollowed = true, isBuiltIn = true),
            BandEntity("dark star orchestra", "Dark Star Orchestra", isFollowed = true, isBuiltIn = true),
            BandEntity("joe russo's almost dead", "Joe Russo's Almost Dead", isFollowed = true, isBuiltIn = true),
            BandEntity("melvin seals & jgb", "Melvin Seals & JGB", isFollowed = false, isBuiltIn = true),
            BandEntity("bob weir & wolf bros", "Bob Weir & Wolf Bros", isFollowed = false, isBuiltIn = true),
            BandEntity("dead & company", "Dead & Company", isFollowed = false, isBuiltIn = true)
        )
    }
}
