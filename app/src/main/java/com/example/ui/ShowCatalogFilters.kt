package com.example.ui

import com.example.data.local.ShowEntity
import com.example.data.local.isUpcoming
import com.example.data.model.DeadEra
import com.example.data.model.SongEntry
import com.example.data.setlist.SegueMatcher

/** Pure catalog list filtering (6.2) — shared by [ShowsViewModel] and unit tests. */
object ShowCatalogFilters {
    fun apply(
        shows: List<ShowEntity>,
        query: String,
        era: DeadEra,
        attendance: AttendanceFilter,
        sort: SortOrder,
        segueFilter: String?,
        songsByShow: Map<String, List<SongEntry>>
    ): List<ShowEntity> {
        val q = query.trim().lowercase()

        return shows.filter { show ->
            val matchesQuery = if (q.isEmpty()) {
                true
            } else {
                show.venue.lowercase().contains(q) ||
                    show.city.lowercase().contains(q) ||
                    show.stateOrCountry.lowercase().contains(q) ||
                    show.date.contains(q) ||
                    show.tourName.lowercase().contains(q) ||
                    show.tags.lowercase().contains(q) ||
                    songsByShow[show.id].orEmpty().any { it.name.lowercase().contains(q) } ||
                    show.userReview.lowercase().contains(q) ||
                    show.bandName.lowercase().contains(q)
            }

            val matchesEra = (era == DeadEra.ALL) || show.eraId.equals(era.id, ignoreCase = true)

            val matchesAttendance = when (attendance) {
                AttendanceFilter.ALL -> true
                AttendanceFilter.ATTENDED_ONLY -> show.isAttended
                AttendanceFilter.UNATTENDED_ONLY -> !show.isAttended
                AttendanceFilter.TERRAPIN_FLYER -> show.bandName.contains("Terrapin Flyer", ignoreCase = true)
                AttendanceFilter.UPCOMING_SHOWS -> show.isUpcoming
            }

            val matchesSegue = if (segueFilter.isNullOrBlank()) {
                true
            } else {
                SegueMatcher.matches(songsByShow[show.id].orEmpty(), segueFilter)
            }

            matchesQuery && matchesEra && matchesAttendance && matchesSegue
        }.let { list ->
            when (sort) {
                SortOrder.DATE_DESC -> list.sortedByDescending { it.date }
                SortOrder.DATE_ASC -> list.sortedBy { it.date }
                SortOrder.RATING_DESC -> list.sortedWith(
                    compareByDescending<ShowEntity> { it.userRating }.thenByDescending { it.date }
                )
            }
        }
    }
}
