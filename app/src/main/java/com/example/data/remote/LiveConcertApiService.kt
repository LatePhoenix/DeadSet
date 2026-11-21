package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Filter categories for the Live Concert Search tab.
 */
enum class TributeBandFilter(val displayName: String, val searchKeyword: String) {
    ALL("All Dead Bands", ""),
    DARK_STAR_ORCHESTRA("Dark Star Orchestra", "Dark Star Orchestra"),
    JRAD("JRAD", "Joe Russo's Almost Dead"),
    TERRAPIN_FLYER("Terrapin Flyer", "Terrapin Flyer")
}

/**
 * Normalized Live Concert Event returned from Bandsintown API, Ticketmaster Discovery API,
 * or the live tour schedule index.
 */
data class LiveConcertEvent(
    val id: String,
    val bandName: String,
    val eventTitle: String,
    val date: String,           // YYYY-MM-DD
    val displayDate: String,    // e.g., "Sat, Nov 14, 2026 • 8:00 PM CST"
    val venueName: String,
    val city: String,
    val stateCode: String,      // e.g., "IL", "WI", "OH", "MO"
    val country: String = "US",
    val venueAddress: String,
    val ticketUrl: String,
    val priceRange: String = "Tickets Available",
    val apiSource: String = "Bandsintown / Ticketmaster",
    val tourNote: String = ""
) {
    val isMidwest: Boolean
        get() = stateCode.uppercase() in MIDWEST_STATE_CODES

    companion object {
        val MIDWEST_STATE_CODES = setOf(
            "IL", "WI", "IN", "MI", "OH", "MN", "MO", "IA", "KS", "NE", "ND", "SD"
        )
    }
}

// --- Bandsintown Public API v4 DTOs & Retrofit Interface ---

data class BandsintownEventDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "title") val title: String = "",
    @Json(name = "datetime") val datetime: String = "",
    @Json(name = "description") val description: String = "",
    @Json(name = "url") val url: String = "",
    @Json(name = "lineup") val lineup: List<String> = emptyList(),
    @Json(name = "venue") val venue: BandsintownVenueDto? = null,
    @Json(name = "offers") val offers: List<BandsintownOfferDto> = emptyList()
)

data class BandsintownVenueDto(
    @Json(name = "name") val name: String = "",
    @Json(name = "city") val city: String = "",
    @Json(name = "region") val region: String = "",
    @Json(name = "country") val country: String = "US",
    @Json(name = "street_address") val streetAddress: String = ""
)

data class BandsintownOfferDto(
    @Json(name = "type") val type: String = "Tickets",
    @Json(name = "url") val url: String = "",
    @Json(name = "status") val status: String = "available"
)

interface BandsintownApiService {
    @GET("artists/{artist}/events")
    suspend fun getArtistEvents(
        @Path("artist", encoded = false) artistName: String,
        @Query("app_id") appId: String,
        @Query("date") date: String = "upcoming"
    ): Response<List<BandsintownEventDto>>
}

// --- Ticketmaster Discovery API v2 DTOs & Retrofit Interface ---

data class TicketmasterDiscoveryResponse(
    @Json(name = "_embedded") val embedded: TicketmasterEmbeddedEvents? = null
)

data class TicketmasterEmbeddedEvents(
    @Json(name = "events") val events: List<TicketmasterEventDto> = emptyList()
)

data class TicketmasterEventDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "url") val url: String = "",
    @Json(name = "info") val info: String = "",
    @Json(name = "dates") val dates: TicketmasterDatesDto? = null,
    @Json(name = "priceRanges") val priceRanges: List<TicketmasterPriceRangeDto> = emptyList(),
    @Json(name = "_embedded") val embedded: TicketmasterEventVenueEmbedded? = null
)

data class TicketmasterDatesDto(
    @Json(name = "start") val start: TicketmasterStartDateDto? = null
)

data class TicketmasterStartDateDto(
    @Json(name = "localDate") val localDate: String = "",
    @Json(name = "localTime") val localTime: String = "20:00:00"
)

data class TicketmasterPriceRangeDto(
    @Json(name = "min") val min: Double = 0.0,
    @Json(name = "max") val max: Double = 0.0,
    @Json(name = "currency") val currency: String = "USD"
)

data class TicketmasterEventVenueEmbedded(
    @Json(name = "venues") val venues: List<TicketmasterVenueDto> = emptyList()
)

data class TicketmasterVenueDto(
    @Json(name = "name") val name: String = "",
    @Json(name = "city") val city: TicketmasterCityDto? = null,
    @Json(name = "state") val state: TicketmasterStateDto? = null,
    @Json(name = "address") val address: TicketmasterAddressDto? = null
)

data class TicketmasterCityDto(
    @Json(name = "name") val name: String = ""
)

data class TicketmasterStateDto(
    @Json(name = "stateCode") val stateCode: String = ""
)

data class TicketmasterAddressDto(
    @Json(name = "line1") val line1: String = ""
)

interface TicketmasterDiscoveryApiService {
    @GET("discovery/v2/events.json")
    suspend fun searchMusicEvents(
        @Query("apikey") apiKey: String,
        @Query("keyword") keyword: String,
        @Query("classificationName") classificationName: String = "music",
        @Query("sort") sort: String = "date,asc",
        @Query("size") size: Int = 25
    ): Response<TicketmasterDiscoveryResponse>
}

/**
 * Live Concert API Client integrating the Bandsintown Public API and Ticketmaster Discovery API v2.
 * Only events returned by those sources are shown; there is no hard-coded fallback schedule.
 */
class LiveConcertApiClient {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val bandsintownService: BandsintownApiService = Retrofit.Builder()
        .baseUrl("https://rest.bandsintown.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(BandsintownApiService::class.java)

    private val ticketmasterService: TicketmasterDiscoveryApiService = Retrofit.Builder()
        .baseUrl("https://app.ticketmaster.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(TicketmasterDiscoveryApiService::class.java)

    fun getTicketmasterApiKey(): String {
        val raw = runCatching {
            BuildConfig::class.java.getDeclaredField("TICKETMASTER_API_KEY").get(null) as? String
        }.getOrNull().orEmpty().trim()
        return if (raw == "YOUR_TICKETMASTER_API_KEY" || raw == "null") "" else raw
    }

    fun getBandsintownAppId(): String {
        val raw = runCatching {
            BuildConfig::class.java.getDeclaredField("BANDSINTOWN_APP_ID").get(null) as? String
        }.getOrNull().orEmpty().trim()
        return if (raw.isBlank() || raw == "null") "deadset_companion_app" else raw
    }

    /**
     * Searches live concerts from the Bandsintown public API & Ticketmaster Discovery API.
     * Returns an empty list when neither source responds (e.g. offline or unconfigured keys).
     */
    suspend fun searchLiveConcerts(
        searchQuery: String = "",
        bandFilter: TributeBandFilter = TributeBandFilter.ALL,
        midwestOnly: Boolean = false
    ): List<LiveConcertEvent> = withContext(Dispatchers.IO) {
        val remoteEvents = mutableListOf<LiveConcertEvent>()

        val effectiveKeyword = when {
            searchQuery.isNotBlank() -> searchQuery.trim()
            bandFilter != TributeBandFilter.ALL -> bandFilter.searchKeyword
            else -> ""
        }

        // 1. Query Bandsintown Public API for the target artist(s)
        val artistsToQuery = if (effectiveKeyword.isNotBlank()) {
            listOf(effectiveKeyword)
        } else {
            listOf("Dark Star Orchestra", "Joe Russo's Almost Dead", "Terrapin Flyer")
        }

        for (artist in artistsToQuery) {
            val bitEvents = fetchBandsintownArtistEvents(artist)
            remoteEvents.addAll(bitEvents)
        }

        // 2. Query Ticketmaster Discovery API v2 if API key is configured
        val tmKey = getTicketmasterApiKey()
        if (tmKey.isNotBlank()) {
            val tmQuery = effectiveKeyword.ifBlank { "Grateful Dead" }
            remoteEvents.addAll(fetchTicketmasterEvents(tmQuery, tmKey))
        }

        // 3. De-duplicate events reported by both sources and drop dates that have already passed
        val today = com.example.data.todayIsoDate()
        val combined = remoteEvents
            .filter { it.date >= today }
            .distinctBy { "${it.bandName.lowercase()}-${it.date}-${it.city.lowercase()}" }

        filterConcertEvents(
            events = combined,
            searchQuery = searchQuery,
            bandFilter = bandFilter,
            midwestOnly = midwestOnly
        )
    }

    /**
     * Pure filtering function for live concert events (used both after API fetch and for instant UI filter updates).
     */
    fun filterConcertEvents(
        events: List<LiveConcertEvent>,
        searchQuery: String = "",
        bandFilter: TributeBandFilter = TributeBandFilter.ALL,
        midwestOnly: Boolean = false
    ): List<LiveConcertEvent> {
        val q = searchQuery.trim().lowercase()
        return events.filter { event ->
            val matchesMidwest = !midwestOnly || event.isMidwest
            val matchesBandChip = when (bandFilter) {
                TributeBandFilter.ALL -> true
                TributeBandFilter.DARK_STAR_ORCHESTRA ->
                    event.bandName.contains("Dark Star Orchestra", ignoreCase = true) ||
                        event.bandName.equals("DSO", ignoreCase = true)
                TributeBandFilter.JRAD ->
                    event.bandName.contains("Joe Russo", ignoreCase = true) ||
                        event.bandName.contains("JRAD", ignoreCase = true)
                TributeBandFilter.TERRAPIN_FLYER ->
                    event.bandName.contains("Terrapin Flyer", ignoreCase = true)
            }
            val matchesSearch = q.isEmpty() ||
                event.bandName.lowercase().contains(q) ||
                event.eventTitle.lowercase().contains(q) ||
                event.venueName.lowercase().contains(q) ||
                event.city.lowercase().contains(q) ||
                event.stateCode.lowercase().contains(q) ||
                event.tourNote.lowercase().contains(q)

            matchesMidwest && matchesBandChip && matchesSearch
        }.sortedBy { it.date }
    }

    private suspend fun fetchBandsintownArtistEvents(artistName: String): List<LiveConcertEvent> {
        return runCatching {
            val response = bandsintownService.getArtistEvents(
                artistName = artistName,
                appId = getBandsintownAppId()
            )
            if (!response.isSuccessful) return emptyList()
            val dtos = response.body().orEmpty()
            dtos.mapNotNull { dto ->
                val venue = dto.venue ?: return@mapNotNull null
                val datePart = dto.datetime.substringBefore("T").ifBlank { return@mapNotNull null }
                val offerUrl = dto.offers.firstOrNull { it.url.isNotBlank() }?.url.orEmpty()
                val ticketLink = when {
                    offerUrl.isNotBlank() -> offerUrl
                    dto.url.isNotBlank() -> dto.url
                    else -> "https://www.bandsintown.com/e/${dto.id}"
                }
                val band = dto.lineup.firstOrNull() ?: artistName
                LiveConcertEvent(
                    id = "bit-${dto.id.ifBlank { "$band-$datePart" }}",
                    bandName = band,
                    eventTitle = dto.title.ifBlank { "$band Live in ${venue.city}" },
                    date = datePart,
                    displayDate = datePart,
                    venueName = venue.name,
                    city = venue.city,
                    stateCode = venue.region,
                    country = venue.country,
                    venueAddress = listOf(venue.streetAddress, venue.city, venue.region)
                        .filter { it.isNotBlank() }
                        .joinToString(", "),
                    ticketUrl = ticketLink,
                    priceRange = "See tickets",
                    apiSource = "Bandsintown API",
                    tourNote = dto.description.take(120)
                )
            }
        }.getOrDefault(emptyList())
    }

    private suspend fun fetchTicketmasterEvents(keyword: String, apiKey: String): List<LiveConcertEvent> {
        return runCatching {
            val response = ticketmasterService.searchMusicEvents(
                apiKey = apiKey,
                keyword = keyword
            )
            if (!response.isSuccessful) return emptyList()
            val events = response.body()?.embedded?.events.orEmpty()
            val encodedKeyword = URLEncoder.encode(keyword, "UTF-8")
            events.mapNotNull { dto ->
                val venue = dto.embedded?.venues?.firstOrNull() ?: return@mapNotNull null
                val dateStr = dto.dates?.start?.localDate.orEmpty().ifBlank { return@mapNotNull null }
                val city = venue.city?.name.orEmpty()
                val state = venue.state?.stateCode.orEmpty()
                val pr = dto.priceRanges.firstOrNull()
                val priceText = if (pr != null && pr.min > 0.0 && pr.max > 0.0) {
                    "$${pr.min.toInt()} – $${pr.max.toInt()}"
                } else {
                    "See tickets"
                }

                LiveConcertEvent(
                    id = "tm-${dto.id}",
                    bandName = dto.name,
                    eventTitle = dto.name,
                    date = dateStr,
                    displayDate = dateStr,
                    venueName = venue.name,
                    city = city,
                    stateCode = state,
                    venueAddress = listOf(venue.address?.line1.orEmpty(), city, state)
                        .filter { it.isNotBlank() }
                        .joinToString(", "),
                    ticketUrl = dto.url.ifBlank { "https://www.ticketmaster.com/search?q=$encodedKeyword" },
                    priceRange = priceText,
                    apiSource = "Ticketmaster Discovery API",
                    tourNote = dto.info.take(120)
                )
            }
        }.getOrDefault(emptyList())
    }
}
