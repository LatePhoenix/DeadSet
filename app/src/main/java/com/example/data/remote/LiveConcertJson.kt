package com.example.data.remote

import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Hand-parsed Bandsintown / Ticketmaster JSON → [LiveConcertEvent].
 * Kept free of OkHttp so unit tests can feed fixture strings without a network stack.
 */
object LiveConcertJson {

    fun parseBandsintownEvents(body: String, artistName: String): List<LiveConcertEvent> {
        val root = JSONArray(body)
        val out = ArrayList<LiveConcertEvent>(root.length())
        for (i in 0 until root.length()) {
            val dto = root.optJSONObject(i) ?: continue
            val venue = dto.optJSONObject("venue") ?: continue
            val datetime = dto.optString("datetime")
            val datePart = datetime.substringBefore("T")
            if (datePart.isBlank()) continue
            val id = dto.optString("id")
            val title = dto.optString("title")
            val description = dto.optString("description")
            val url = dto.optString("url")
            val lineup = dto.optJSONArray("lineup")
            val band = lineup?.optString(0)?.takeIf { it.isNotBlank() } ?: artistName
            val offers = dto.optJSONArray("offers")
            val offerUrl = firstOfferUrl(offers)
            val ticketLink = when {
                offerUrl.isNotBlank() -> offerUrl
                url.isNotBlank() -> url
                else -> "https://www.bandsintown.com/e/$id"
            }
            val city = venue.optString("city")
            val region = venue.optString("region")
            val street = venue.optString("street_address")
            out += LiveConcertEvent(
                id = "bit-${id.ifBlank { "$band-$datePart" }}",
                bandName = band,
                eventTitle = title.ifBlank { "$band Live in $city" },
                date = datePart,
                displayDate = datePart,
                venueName = venue.optString("name"),
                city = city,
                stateCode = region,
                country = venue.optString("country").ifBlank { "US" },
                venueAddress = listOf(street, city, region).filter { it.isNotBlank() }.joinToString(", "),
                ticketUrl = ticketLink,
                priceRange = "See tickets",
                apiSource = "Bandsintown API",
                tourNote = description.take(120),
                latitude = venue.optString("latitude").toDoubleOrNull(),
                longitude = venue.optString("longitude").toDoubleOrNull()
            )
        }
        return out
    }

    fun parseTicketmasterEvents(body: String, keyword: String): List<LiveConcertEvent> {
        val root = JSONObject(body)
        val events = root.optJSONObject("_embedded")?.optJSONArray("events") ?: return emptyList()
        val encodedKeyword = URLEncoder.encode(keyword, "UTF-8")
        val out = ArrayList<LiveConcertEvent>(events.length())
        for (i in 0 until events.length()) {
            val dto = events.optJSONObject(i) ?: continue
            val venues = dto.optJSONObject("_embedded")?.optJSONArray("venues")
            val venue = venues?.optJSONObject(0) ?: continue
            val dateStr = dto.optJSONObject("dates")
                ?.optJSONObject("start")
                ?.optString("localDate")
                .orEmpty()
            if (dateStr.isBlank()) continue
            val city = venue.optJSONObject("city")?.optString("name").orEmpty()
            val state = venue.optJSONObject("state")?.optString("stateCode").orEmpty()
            val line1 = venue.optJSONObject("address")?.optString("line1").orEmpty()
            val location = venue.optJSONObject("location")
            val name = dto.optString("name")
            val id = dto.optString("id")
            val url = dto.optString("url")
            val info = dto.optString("info")
            val priceRanges = dto.optJSONArray("priceRanges")
            val pr = priceRanges?.optJSONObject(0)
            val min = pr?.optDouble("min", 0.0) ?: 0.0
            val max = pr?.optDouble("max", 0.0) ?: 0.0
            val priceText = if (min > 0.0 && max > 0.0) {
                "$${min.toInt()} – $${max.toInt()}"
            } else {
                "See tickets"
            }
            out += LiveConcertEvent(
                id = "tm-$id",
                bandName = name,
                eventTitle = name,
                date = dateStr,
                displayDate = dateStr,
                venueName = venue.optString("name"),
                city = city,
                stateCode = state,
                venueAddress = listOf(line1, city, state).filter { it.isNotBlank() }.joinToString(", "),
                ticketUrl = url.ifBlank { "https://www.ticketmaster.com/search?q=$encodedKeyword" },
                priceRange = priceText,
                apiSource = "Ticketmaster Discovery API",
                tourNote = info.take(120),
                latitude = location?.optString("latitude")?.toDoubleOrNull(),
                longitude = location?.optString("longitude")?.toDoubleOrNull()
            )
        }
        return out
    }

    private fun firstOfferUrl(offers: JSONArray?): String {
        if (offers == null) return ""
        for (i in 0 until offers.length()) {
            val url = offers.optJSONObject(i)?.optString("url").orEmpty()
            if (url.isNotBlank()) return url
        }
        return ""
    }
}
