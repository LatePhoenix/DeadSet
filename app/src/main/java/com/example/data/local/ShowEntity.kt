package com.example.data.local

import androidx.room.DatabaseView
import com.example.data.todayIsoDate

/**
 * Read model: catalog [shows] LEFT JOIN [user_show_data].
 *
 * Writers must not @Insert/@Update this view — use [ShowDao.insertShow] / [ShowDao.updateShow],
 * which split into [ShowCatalogEntity] + [UserShowDataEntity].
 *
 * View SQL must stay identical to [SHOW_WITH_USER_VIEW_CREATE_SQL] (used by MIGRATION_10_11).
 */
@DatabaseView(
    viewName = "shows_with_user",
    value = """
        SELECT s.id AS id, s.date AS date, s.venue AS venue, s.city AS city,
            s.stateOrCountry AS stateOrCountry, s.eraId AS eraId, s.tourName AS tourName,
            COALESCE(u.isAttended, 0) AS isAttended,
            COALESCE(u.userRating, 0) AS userRating,
            COALESCE(u.userReview, '') AS userReview,
            COALESCE(u.attendedWith, '') AS attendedWith,
            COALESCE(u.favoriteSong, '') AS favoriteSong,
            s.archiveIdentifier AS archiveIdentifier, s.tapeQuality AS tapeQuality,
            s.isCustomShow AS isCustomShow, s.tags AS tags, s.historicTrivia AS historicTrivia,
            s.bandName AS bandName, s.venueAddress AS venueAddress,
            s.recreatesShowId AS recreatesShowId, s.lastUpdatedTimestamp AS lastUpdatedTimestamp
        FROM shows AS s
        LEFT JOIN user_show_data AS u ON u.showId = s.id
    """
)
data class ShowEntity(
    val id: String,
    val date: String,
    val venue: String,
    val city: String,
    val stateOrCountry: String,
    val eraId: String,
    val tourName: String,
    val isAttended: Boolean = false,
    val userRating: Float = 0f,
    val userReview: String = "",
    val attendedWith: String = "",
    val favoriteSong: String = "",
    val archiveIdentifier: String = "",
    val tapeQuality: String = "",
    val isCustomShow: Boolean = false,
    val tags: String = "",
    val historicTrivia: String = "",
    val bandName: String = "Grateful Dead",
    val venueAddress: String = "",
    val recreatesShowId: String? = null,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)

/**
 * True when the show hasn't happened yet. Computed from [ShowEntity.date] (never stored, so it
 * can't go stale); a show dated today still counts as upcoming until the day is over.
 */
val ShowEntity.isUpcoming: Boolean
    get() = date >= todayIsoDate()
