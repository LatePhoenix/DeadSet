package com.example.data.history

import com.example.data.local.ShowEntity
import com.example.data.todayIsoDate

/** Month-day (`MM-dd`) from an ISO `yyyy-MM-dd` date. */
fun monthDayFromIso(isoDate: String): String {
    require(isoDate.length >= 10) { "Expected yyyy-MM-dd, got: $isoDate" }
    return isoDate.substring(5, 10)
}

/**
 * Catalog shows whose calendar month-day matches [monthDay] (`MM-dd`).
 * Does not invent fillers — empty means none that day.
 */
fun showsOnMonthDay(shows: List<ShowEntity>, monthDay: String): List<ShowEntity> =
    shows.filter { it.date.length >= 10 && monthDayFromIso(it.date) == monthDay }
        .sortedBy { it.date }

/** Shows played on today's month-day (from [todayIsoDate]). */
fun showsOnTodayInHistory(
    shows: List<ShowEntity>,
    todayIso: String = todayIsoDate()
): List<ShowEntity> = showsOnMonthDay(shows, monthDayFromIso(todayIso))
