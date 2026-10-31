package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import com.example.data.local.ShowEntity
import java.text.SimpleDateFormat
import java.util.*

object ConcertIntentHelper {

    /**
     * Opens the show venue location in Google Maps or the user's default navigation app.
     */
    fun openVenueInMaps(context: Context, show: ShowEntity) {
        val locationQuery = if (show.venueAddress.isNotBlank()) {
            "${show.venue}, ${show.venueAddress}, ${show.city}, ${show.stateOrCountry}"
        } else {
            "${show.venue}, ${show.city}, ${show.stateOrCountry}"
        }

        val encodedQuery = Uri.encode(locationQuery)
        val geoUri = Uri.parse("geo:0,0?q=$encodedQuery")

        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            setPackage("com.google.android.apps.maps")
        }

        try {
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            // Fallback to web browser or any map provider installed
            try {
                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedQuery")
                context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
            } catch (err: Exception) {
                Toast.makeText(context, "Could not open map navigation: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Adds the concert or historic show to the device calendar via standard Calendar Intent.
     */
    fun addConcertToCalendar(context: Context, show: ShowEntity) {
        val title = if (show.bandName.equals("Grateful Dead", ignoreCase = true)) {
            "Grateful Dead at ${show.venue}"
        } else {
            "${show.bandName} Live at ${show.venue}"
        }

        val location = "${show.venue}, ${show.city}, ${show.stateOrCountry}"
        val description = buildString {
            append("Tour: ${show.tourName}\n")
            if (show.bandName != "Grateful Dead") {
                append("Band: ${show.bandName}\n")
            }
            if (show.historicTrivia.isNotBlank()) {
                append("Notes: ${show.historicTrivia}\n")
            }
            if (show.tags.isNotBlank()) {
                append("Highlights: ${show.tags}\n")
            }
            append("\nLogged with DeadSet Tour Companion ⚡")
        }

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.EVENT_LOCATION, location)
            putExtra(CalendarContract.Events.DESCRIPTION, description)

            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val parsedDate = sdf.parse(show.date)
                if (parsedDate != null) {
                    val cal = Calendar.getInstance().apply {
                        time = parsedDate
                        // Typical evening concert start: 7:30 PM
                        set(Calendar.HOUR_OF_DAY, 19)
                        set(Calendar.MINUTE, 30)
                        set(Calendar.SECOND, 0)
                    }
                    val startTime = cal.timeInMillis
                    val endTime = startTime + (3.5 * 60 * 60 * 1000).toLong() // 3.5 hours show

                    putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime)
                    putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime)
                }
            } catch (e: Exception) {
                // If date parsing fails, calendar still opens with date field for user to confirm
            }
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open calendar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
