package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.SongEntry

/**
 * Room Entity storing historical Grateful Dead and tribute concerts for offline browsing.
 * Indexed by [date], [venue], [city], and [eraId] for instantaneous local SQLite queries.
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
    val setlistJson: String,
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
    val isUpcoming: Boolean = false,   // True if upcoming tour date
    val venueAddress: String = "",     // Optional specific street address for Google Maps navigation
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
) {
    /**
     * Convenience accessor to read the typed list of songs performed at this show offline.
     */
    fun getSongsPerformed(): List<SongEntry> {
        return sharedConverters.toSongList(setlistJson)
    }

    companion object {
        private val sharedConverters = Converters()
    }
}
