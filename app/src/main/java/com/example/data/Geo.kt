package com.example.data

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** A latitude/longitude pair in degrees. */
data class LatLon(val lat: Double, val lon: Double)

object Geo {
    private const val EARTH_RADIUS_MILES = 3958.8

    /** Great-circle (haversine) distance in miles. */
    fun milesBetween(a: LatLon, b: LatLon): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val h = sin(dLat / 2).pow(2) + cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_MILES * asin(sqrt(h))
    }
}

/** "checked just now", "checked 5 min ago", "checked 3 h ago", "checked 2 d ago". */
fun checkedAgo(checkedAtMillis: Long, nowMillis: Long): String {
    val minutes = ((nowMillis - checkedAtMillis) / 60_000L).coerceAtLeast(0)
    return when {
        minutes < 1 -> "checked just now"
        minutes < 60 -> "checked $minutes min ago"
        minutes < 60 * 24 -> "checked ${minutes / 60} h ago"
        else -> "checked ${minutes / (60 * 24)} d ago"
    }
}
