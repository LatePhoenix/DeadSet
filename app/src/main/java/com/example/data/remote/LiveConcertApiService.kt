package com.example.data.remote

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Normalized upcoming event from the Bandsintown API or the Ticketmaster Discovery API.
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
    val tourNote: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null
)

/**
 * Live concert client using OkHttp + org.json (no Retrofit/Moshi).
 * Only events returned by Bandsintown / Ticketmaster are shown.
 *
 * [bandsintownBaseUrl] and [ticketmasterAccessOverride] exist so unit tests can point at
 * MockWebServer without changing production defaults (6.7).
 */
class LiveConcertApiClient(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build(),
    private val bandsintownBaseUrl: String = "https://rest.bandsintown.com/",
    private val ticketmasterAccessOverride: TicketmasterAccess? = null,
    private val bandsintownAppIdOverride: String? = null
) {

    fun ticketmasterAccess(): TicketmasterAccess =
        ticketmasterAccessOverride ?: TicketmasterAccess.fromBuildConfig()

    /** True when a proxy URL is set, or (debug only) a local Discovery API key. */
    fun isTicketmasterConfigured(): Boolean = ticketmasterAccess().isConfigured

    fun getBandsintownAppId(): String {
        bandsintownAppIdOverride?.let { return it }
        val raw = runCatching {
            BuildConfig::class.java.getDeclaredField("BANDSINTOWN_APP_ID").get(null) as? String
        }.getOrNull().orEmpty().trim()
        return if (raw.isBlank() || raw == "null" || raw == "YOUR_BANDSINTOWN_APP_ID") "" else raw
    }

    /**
     * Upcoming events for each of [bandNames] from Bandsintown and (when a proxy or debug key
     * is configured) Ticketmaster, de-duplicated across sources, past dates dropped. Each source
     * fails quietly; [FetchResult.sourcesReached] says which ones answered so the UI can explain
     * an empty list.
     */
    suspend fun fetchEventsForBands(bandNames: List<String>): FetchResult = withContext(Dispatchers.IO) {
        val events = mutableListOf<LiveConcertEvent>()
        val reached = mutableSetOf<String>()
        val tm = ticketmasterAccess()
        val bandsintownAppId = getBandsintownAppId()
        for (band in bandNames) {
            if (bandsintownAppId.isNotBlank()) {
                fetchBandsintownArtistEvents(band)?.let { events += it; reached += "Bandsintown" }
            }
            if (tm.isConfigured) {
                fetchTicketmasterEvents(band, tm)?.let { list ->
                    reached += "Ticketmaster"
                    events += list.filter {
                        it.bandName.contains(band, ignoreCase = true) ||
                            it.eventTitle.contains(band, ignoreCase = true)
                    }.map { it.copy(bandName = band) }
                }
            }
        }
        val today = com.example.data.todayIsoDate()
        FetchResult(
            events = events
                .filter { it.date >= today }
                .distinctBy { "${it.bandName.lowercase()}-${it.date}-${it.city.lowercase()}" },
            sourcesReached = reached,
            ticketmasterConfigured = tm.isConfigured
        )
    }

    data class FetchResult(
        val events: List<LiveConcertEvent>,
        val sourcesReached: Set<String>,
        val ticketmasterConfigured: Boolean
    )

    /** Null when the request fails or is rejected (e.g. unregistered app_id), empty when none. */
    private fun fetchBandsintownArtistEvents(artistName: String): List<LiveConcertEvent>? {
        return runCatching {
            val base = bandsintownBaseUrl.trimEnd('/')
            val url = "$base/artists/${artistName.encodePathSegment()}/events"
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter("app_id", getBandsintownAppId())
                .addQueryParameter("date", "upcoming")
                .build()
            val body = httpGet(url.toString()) ?: return null
            LiveConcertJson.parseBandsintownEvents(body, artistName)
        }.getOrNull()
    }

    /** Null when the request fails or is rejected, empty when there are no events. */
    private fun fetchTicketmasterEvents(
        keyword: String,
        access: TicketmasterAccess
    ): List<LiveConcertEvent>? {
        return runCatching {
            val base = access.baseUrl.trimEnd('/')
            val builder = "$base/discovery/v2/events.json".toHttpUrl().newBuilder()
                .addQueryParameter("keyword", keyword)
                .addQueryParameter("classificationName", "music")
                .addQueryParameter("sort", "date,asc")
                .addQueryParameter("size", "25")
            // Proxy injects the secret; omit apikey so it never leaves the device unused.
            if (access.apiKey.isNotBlank()) {
                builder.addQueryParameter("apikey", access.apiKey)
            }
            val body = httpGet(builder.build().toString(), access.requestHeaders()) ?: return null
            LiveConcertJson.parseTicketmasterEvents(body, keyword)
        }.getOrNull()
    }

    private fun httpGet(url: String, headers: Map<String, String> = emptyMap()): String? {
        val builder = Request.Builder().url(url).get()
        for ((name, value) in headers) {
            builder.header(name, value)
        }
        http.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) return null
            return response.body?.string()
        }
    }

    /** Path-segment encode without turning spaces into `+` (Bandsintown expects `%20`). */
    private fun String.encodePathSegment(): String =
        java.net.URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")
}
