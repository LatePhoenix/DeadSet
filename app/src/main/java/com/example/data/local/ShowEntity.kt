package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.todayIsoDate

/**
 * Room Entity storing historical Grateful Dead and tribute concerts for offline browsing.
 * Indexed by [date], [venue], [city], and [eraId] for instantaneous local SQLite queries.
 *
 * The setlist lives only in `song_performances` (see [ShowWithSongs]); there is no copy here.
 */
@Entity(
    tableName = "shows",
    indices = [
        Index(value = ["date"]),
        Index(value = ["venue"]),
        Index(value = ["city"]),
        Index(value = ["eraId"])
    ]
)
data class ShowEntity(
    @PrimaryKey
    val id: String,
    val date: String,
    val venue: String,
    val city: String,
    val stateOrCountry: String,
    val eraId: String,
    val tourName: String,
    val isAttended: Boolean = false,
    val userRating: Float = 0f,
    val userReview: String = "",       // User's personal thoughts, memories, or review of the performance
    val attendedWith: String = "",     // Who you were with
    val favoriteSong: String = "",     // Your personal favorite jam from this show
    val archiveIdentifier: String = "",// Archive.org SBD/Matrix tape reference
    val tapeQuality: String = "",      // e.g. "Betty Board Matrix"
    val isCustomShow: Boolean = false,
    val tags: String = "",
    val historicTrivia: String = "",   // Notable historical lore & trivia for Today in Dead History
    val bandName: String = "Grateful Dead", // "Grateful Dead", "Terrapin Flyer", etc.
    val venueAddress: String = "",     // Optional specific street address for Google Maps navigation
    /** For a tribute show: id of the Grateful Dead show it recreates (step 4.8), if linked. */
    val recreatesShowId: String? = null,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)

/**
 * True when the show hasn't happened yet. Computed from [ShowEntity.date] (never stored, so it
 * can't go stale); a show dated today still counts as upcoming until the day is over.
 */
val ShowEntity.isUpcoming: Boolean
    get() = date >= todayIsoDate()
