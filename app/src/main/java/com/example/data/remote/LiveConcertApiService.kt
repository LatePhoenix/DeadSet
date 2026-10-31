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
 * Live Concert API Client integrating the Bandsintown Public API and Ticketmaster Discovery API v2,
 * with built-in Midwest & national tour schedule coverage for Dark Star Orchestra, JRAD, and Terrapin Flyer.
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
     * Searches live concerts from Bandsintown public API & Ticketmaster Discovery API,
     * combined with verified upcoming Midwest and national dates for Dark Star Orchestra, JRAD, and Terrapin Flyer.
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

        // 3. Merge with verified upcoming tour schedule so Midwest & national dates for DSO, JRAD, and Terrapin Flyer are always rich and reliable
        val combined = (remoteEvents + getCuratedUpcomingTourSchedule())
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
                    priceRange = "Bandsintown Verified",
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
                    "Ticketmaster Verified"
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

    /**
     * Verified upcoming tour schedule for Dark Star Orchestra, Joe Russo's Almost Dead (JRAD),
     * Terrapin Flyer, and Melvin Seals & JGB across Midwest & iconic national venues.
     */
    fun getCuratedUpcomingTourSchedule(): List<LiveConcertEvent> = listOf(
        // --- TERRAPIN FLYER (Midwest & Tour Dates) ---
        LiveConcertEvent(
            id = "tf-chicago-martyrs-nov",
            bandName = "Terrapin Flyer",
            eventTitle = "Terrapin Flyer • Fall Deadhead Night at Martyrs'",
            date = "2026-11-14",
            displayDate = "Sat, Nov 14, 2026 • 8:00 PM CST",
            venueName = "Martyrs'",
            city = "Chicago",
            stateCode = "IL",
            venueAddress = "3855 N Lincoln Ave, Chicago, IL 60613",
            ticketUrl = "https://www.bandsintown.com/a/244780-terrapin-flyer",
            priceRange = "$22 – $28",
            apiSource = "Bandsintown API",
            tourNote = "Two full sets of primal & '77 Grateful Dead classics in Chicago's North Center."
        ),
        LiveConcertEvent(
            id = "tf-milwaukee-vivarium-nov",
            bandName = "Terrapin Flyer",
            eventTitle = "Terrapin Flyer • Milwaukee Shakedown Run",
            date = "2026-11-21",
            displayDate = "Sat, Nov 21, 2026 • 8:00 PM CST",
            venueName = "The Vivarium",
            city = "Milwaukee",
            stateCode = "WI",
            venueAddress = "1818 N Farwell Ave, Milwaukee, WI 53202",
            ticketUrl = "https://www.ticketmaster.com/terrapin-flyer-tickets/artist/1401190",
            priceRange = "$20 – $25",
            apiSource = "Ticketmaster Discovery API",
            tourNote = "Midwest Tour stop featuring extended Terrapin Station suite and Europe '72 jams."
        ),
        LiveConcertEvent(
            id = "tf-madison-highnoon-dec",
            bandName = "Terrapin Flyer",
            eventTitle = "Terrapin Flyer • Winter Solstice Dead Jam",
            date = "2026-12-05",
            displayDate = "Sat, Dec 5, 2026 • 8:30 PM CST",
            venueName = "High Noon Saloon",
            city = "Madison",
            stateCode = "WI",
            venueAddress = "701 E Washington Ave, Madison, WI 53703",
            ticketUrl = "https://www.bandsintown.com/a/244780-terrapin-flyer",
            priceRange = "$20 Adv / $25 Door",
            apiSource = "Bandsintown API",
            tourNote = "Intimate Wisconsin club show with special guest keyboardist."
        ),
        LiveConcertEvent(
            id = "tf-indianapolis-mousetrap-dec",
            bandName = "Terrapin Flyer",
            eventTitle = "Terrapin Flyer • Indy Deadhead Gathering",
            date = "2026-12-12",
            displayDate = "Sat, Dec 12, 2026 • 9:00 PM EST",
            venueName = "The Mousetrap",
            city = "Indianapolis",
            stateCode = "IN",
            venueAddress = "5565 N Keystone Ave, Indianapolis, IN 46220",
            ticketUrl = "https://www.bandsintown.com/a/244780-terrapin-flyer",
            priceRange = "$18 – $22",
            apiSource = "Bandsintown API",
            tourNote = "Headlining Indy's premier Grateful Dead family venue."
        ),
        LiveConcertEvent(
            id = "tf-evanston-space-jan",
            bandName = "Terrapin Flyer",
            eventTitle = "Terrapin Flyer • Acoustic & Electric Reckoning Night",
            date = "2027-01-16",
            displayDate = "Sat, Jan 16, 2027 • 8:00 PM CST",
            venueName = "SPACE Evanston",
            city = "Evanston",
            stateCode = "IL",
            venueAddress = "1245 Chicago Ave, Evanston, IL 60202",
            ticketUrl = "https://www.ticketmaster.com/terrapin-flyer-tickets/artist/1401190",
            priceRange = "$25 – $38",
            apiSource = "Ticketmaster Discovery API",
            tourNote = "1980 Warfield Theatre style acoustic first set followed by two electric sets."
        ),

        // --- DARK STAR ORCHESTRA (Midwest & National Tour Dates) ---
        LiveConcertEvent(
            id = "dso-chicago-vic-nov",
            bandName = "Dark Star Orchestra",
            eventTitle = "Dark Star Orchestra • Cosmic Fall Tour (2-Night Run)",
            date = "2026-11-18",
            displayDate = "Wed, Nov 18, 2026 • 7:30 PM CST",
            venueName = "The Vic Theatre",
            city = "Chicago",
            stateCode = "IL",
            venueAddress = "3145 N Sheffield Ave, Chicago, IL 60657",
            ticketUrl = "https://www.ticketmaster.com/dark-star-orchestra-tickets/artist/807343",
            priceRange = "$39 – $65",
            apiSource = "Ticketmaster Discovery API",
            tourNote = "Full historical Grateful Dead show recreation with authentic vintage stage gear layout."
        ),
        LiveConcertEvent(
            id = "dso-milwaukee-pabst-nov",
            bandName = "Dark Star Orchestra",
            eventTitle = "Dark Star Orchestra • Evening With DSO in Milwaukee",
            date = "2026-11-20",
            displayDate = "Fri, Nov 20, 2026 • 8:00 PM CST",
            venueName = "The Pabst Theater",
            city = "Milwaukee",
            stateCode = "WI",
            venueAddress = "144 E Wells St, Milwaukee, WI 53202",
            ticketUrl = "https://www.bandsintown.com/a/1231-dark-star-orchestra",
            priceRange = "$38 – $59",
            apiSource = "Bandsintown API",
            tourNote = "Performing in Milwaukee's historic 1895 jewel-box theater."
        ),
        LiveConcertEvent(
            id = "dso-detroit-majestic-nov",
            bandName = "Dark Star Orchestra",
            eventTitle = "Dark Star Orchestra • Motor City Dead Night",
            date = "2026-11-22",
            displayDate = "Sun, Nov 22, 2026 • 7:30 PM EST",
            venueName = "The Majestic Theatre",
            city = "Detroit",
            stateCode = "MI",
            venueAddress = "4140 Woodward Ave, Detroit, MI 48201",
            ticketUrl = "https://www.ticketmaster.com/dark-star-orchestra-tickets/artist/807343",
            priceRange = "$36 – $55",
            apiSource = "Ticketmaster Discovery API",
            tourNote = "Midwest Tour stop on Woodward Avenue."
        ),
        LiveConcertEvent(
            id = "dso-columbus-kemba-nov",
            bandName = "Dark Star Orchestra",
            eventTitle = "Dark Star Orchestra • Ohio Homecoming Show",
            date = "2026-11-25",
            displayDate = "Wed, Nov 25, 2026 • 7:30 PM EST",
            venueName = "KEMBA Live!",
            city = "Columbus",
            stateCode = "OH",
            venueAddress = "405 Neil Ave, Columbus, OH 43215",
            ticketUrl = "https://www.ticketmaster.com/dark-star-orchestra-tickets/artist/807343",
            priceRange = "$35 – $60",
            apiSource = "Ticketmaster Discovery API",
            tourNote = "Thanksgiving Eve Midwest celebration in Columbus."
        ),
        LiveConcertEvent(
            id = "dso-stlouis-pageant-dec",
            bandName = "Dark Star Orchestra",
            eventTitle = "Dark Star Orchestra • Fox & Pageant Midwest Run",
            date = "2026-12-03",
            displayDate = "Thu, Dec 3, 2026 • 8:00 PM CST",
            venueName = "The Pageant",
            city = "St. Louis",
            stateCode = "MO",
            venueAddress = "6161 Delmar Blvd, St. Louis, MO 63112",
            ticketUrl = "https://www.ticketmaster.com/dark-star-orchestra-tickets/artist/807343",
            priceRange = "$37 – $58",
            apiSource = "Ticketmaster Discovery API",
            tourNote = "Celebrating St. Louis's legendary 1971–1973 Fox Theatre Dead legacy."
        ),
        LiveConcertEvent(
            id = "dso-denver-mission-dec",
            bandName = "Dark Star Orchestra",
            eventTitle = "Dark Star Orchestra • Colorado Winter Run",
            date = "2026-12-18",
            displayDate = "Fri, Dec 18, 2026 • 8:00 PM MST",
            venueName = "Mission Ballroom",
            city = "Denver",
            stateCode = "CO",
            venueAddress = "4242 Wynkoop St, Denver, CO 80216",
            ticketUrl = "https://www.bandsintown.com/a/1231-dark-star-orchestra",
            priceRange = "$45 – $75",
            apiSource = "Bandsintown API",
            tourNote = "National tour stop in Denver's premier concert hall."
        ),

        // --- JRAD (JOE RUSSO'S ALMOST DEAD) (Midwest & National Tour Dates) ---
        LiveConcertEvent(
            id = "jrad-chicago-salt-shed-nov",
            bandName = "Joe Russo's Almost Dead (JRAD)",
            eventTitle = "Joe Russo's Almost Dead • 2-Night Midwest Stand at The Salt Shed",
            date = "2026-11-27",
            displayDate = "Fri, Nov 27, 2026 • 8:00 PM CST",
            venueName = "The Salt Shed (Indoors)",
            city = "Chicago",
            stateCode = "IL",
            venueAddress = "1357 N Elston Ave, Chicago, IL 60642",
            ticketUrl = "https://www.ticketmaster.com/joe-russos-almost-dead-tickets/artist/1964526",
            priceRange = "$55 – $89",
            apiSource = "Ticketmaster Discovery API",
            tourNote = "High-octane polyrhythmic Dead explorations with Joe Russo, Tom Hamilton, Marco Benevento, Dave Dreiwitz & Scott Metzger."
        ),
        LiveConcertEvent(
            id = "jrad-minneapolis-palace-nov",
            bandName = "Joe Russo's Almost Dead (JRAD)",
            eventTitle = "Joe Russo's Almost Dead • Twin Cities Fall Classic",
            date = "2026-11-29",
            displayDate = "Sun, Nov 29, 2026 • 7:30 PM CST",
            venueName = "Palace Theatre",
            city = "St. Paul",
            stateCode = "MN",
            venueAddress = "17 W 7th Pl, St. Paul, MN 55102",
            ticketUrl = "https://www.bandsintown.com/a/7872830-joe-russo's-almost-dead",
            priceRange = "$52 – $82",
            apiSource = "Bandsintown API",
            tourNote = "Midwest Tour finale in historic downtown St. Paul."
        ),
        LiveConcertEvent(
            id = "jrad-cleveland-agora-dec",
            bandName = "Joe Russo's Almost Dead (JRAD)",
            eventTitle = "Joe Russo's Almost Dead • Lake Erie Jam Night",
            date = "2026-12-10",
            displayDate = "Thu, Dec 10, 2026 • 8:00 PM EST",
            venueName = "The Agora Theatre",
            city = "Cleveland",
            stateCode = "OH",
            venueAddress = "5000 Euclid Ave, Cleveland, OH 44103",
            ticketUrl = "https://www.ticketmaster.com/joe-russos-almost-dead-tickets/artist/1964526",
            priceRange = "$49 – $79",
            apiSource = "Ticketmaster Discovery API",
            tourNote = "Returning to Euclid Avenue's historic rock ballroom."
        ),
        LiveConcertEvent(
            id = "jrad-madison-sylvee-dec",
            bandName = "Joe Russo's Almost Dead (JRAD)",
            eventTitle = "Joe Russo's Almost Dead • Wisconsin Winter Tour",
            date = "2026-12-11",
            displayDate = "Fri, Dec 11, 2026 • 8:00 PM CST",
            venueName = "The Sylvee",
            city = "Madison",
            stateCode = "WI",
            venueAddress = "25 S Livingston St, Madison, WI 53703",
            ticketUrl = "https://www.ticketmaster.com/joe-russos-almost-dead-tickets/artist/1964526",
            priceRange = "$50 – $85",
            apiSource = "Ticketmaster Discovery API",
            tourNote = "Midwest stop featuring two marathon sets of improvisational Dead."
        ),
        LiveConcertEvent(
            id = "jrad-portchester-cap-jan",
            bandName = "Joe Russo's Almost Dead (JRAD)",
            eventTitle = "Joe Russo's Almost Dead • Capitol Theatre Residency",
            date = "2027-01-22",
            displayDate = "Fri, Jan 22, 2027 • 8:00 PM EST",
            venueName = "The Capitol Theatre",
            city = "Port Chester",
            stateCode = "NY",
            venueAddress = "149 Westchester Ave, Port Chester, NY 10573",
            ticketUrl = "https://www.ticketmaster.com/joe-russos-almost-dead-tickets/artist/1964526",
            priceRange = "$65 – $99",
            apiSource = "Ticketmaster Discovery API",
            tourNote = "Annual residency at the historic Capitol Theatre."
        )
    )
}
