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
            val datetime = dto.jsonString("datetime")
            val datePart = datetime.substringBefore("T")
            if (datePart.isBlank()) continue
            val id = dto.jsonString("id")
            val title = dto.jsonString("title")
            val description = dto.jsonString("description")
            val url = dto.jsonString("url")
            val lineup = dto.optJSONArray("lineup")
            val band = lineup?.jsonString(0)?.takeIf { it.isNotBlank() } ?: artistName
            val offers = dto.optJSONArray("offers")
            val offerUrl = firstOfferUrl(offers)
            val ticketLink = when {
                offerUrl.isNotBlank() -> offerUrl
                url.isNotBlank() -> url
                else -> "https://www.bandsintown.com/e/$id"
            }
            val city = venue.jsonString("city")
            val region = venue.jsonString("region")
            val street = venue.jsonString("street_address")
            out += LiveConcertEvent(
                id = "bit-${id.ifBlank { "$band-$datePart" }}",
                bandName = band,
                eventTitle = title.ifBlank { "$band Live in $city" },
                date = datePart,
                displayDate = datePart,
                venueName = venue.jsonString("name"),
                city = city,
                stateCode = region,
                country = venue.jsonString("country").ifBlank { "US" },
                venueAddress = listOf(street, city, region).filter { it.isNotBlank() }.joinToString(", "),
                ticketUrl = ticketLink,
                priceRange = "See tickets",
                apiSource = "Bandsintown API",
                tourNote = description.take(120),
                latitude = venue.jsonString("latitude").toDoubleOrNull(),
                longitude = venue.jsonString("longitude").toDoubleOrNull()
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
                ?.jsonString("localDate")
                .orEmpty()
            if (dateStr.isBlank()) continue
            val city = venue.optJSONObject("city")?.jsonString("name").orEmpty()
            val state = venue.optJSONObject("state")?.jsonString("stateCode").orEmpty()
            val line1 = venue.optJSONObject("address")?.jsonString("line1").orEmpty()
            val location = venue.optJSONObject("location")
            val name = dto.jsonString("name")
            val id = dto.jsonString("id")
            val url = dto.jsonString("url")
            val info = dto.jsonString("info")
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
                venueName = venue.jsonString("name"),
                city = city,
                stateCode = state,
                venueAddress = listOf(line1, city, state).filter { it.isNotBlank() }.joinToString(", "),
                ticketUrl = url.ifBlank { "https://www.ticketmaster.com/search?q=$encodedKeyword" },
                priceRange = priceText,
                apiSource = "Ticketmaster Discovery API",
                tourNote = info.take(120),
                latitude = location?.jsonString("latitude")?.toDoubleOrNull(),
                longitude = location?.jsonString("longitude")?.toDoubleOrNull()
            )
        }
        return out
    }

    private fun firstOfferUrl(offers: JSONArray?): String {
        if (offers == null) return ""
        for (i in 0 until offers.length()) {
            val url = offers.optJSONObject(i)?.jsonString("url").orEmpty()
            if (url.isNotBlank()) return url
        }
        return ""
    }

    /**
     * Android's [JSONObject.optString] turns JSON `null` into the literal `"null"`.
     * Treat missing / JSON-null / the string `"null"` as blank.
     */
    private fun JSONObject.jsonString(key: String): String {
        if (!has(key) || isNull(key)) return ""
        val value = optString(key, "")
        return if (value == "null") "" else value
    }

    private fun JSONArray.jsonString(index: Int): String {
        if (isNull(index)) return ""
        val value = optString(index, "")
        return if (value == "null") "" else value
    }
}
