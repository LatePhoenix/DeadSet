package com.example.data.alerts

import com.example.data.local.ShowEntity
import com.example.data.remote.LiveConcertEvent

/** What to tell the user after a background check (step 4.6). Pure, so it's unit-tested. */
object ConcertAlertRules {

    /** Events that weren't in the cache before this refresh. Nothing on the very first sync. */
    fun newEvents(knownIdsBefore: Set<String>, eventsAfter: List<LiveConcertEvent>, firstSync: Boolean): List<LiveConcertEvent> =
        if (firstSync) emptyList() else eventsAfter.filter { it.id !in knownIdsBefore }.sortedBy { it.date }

    /** Catalog shows happening today that haven't been reminded about yet today. */
    fun tonightShows(shows: List<ShowEntity>, today: String, lastReminderDate: String): List<ShowEntity> =
        if (lastReminderDate == today) emptyList() else shows.filter { it.date == today }

    fun newDatesTitle(events: List<LiveConcertEvent>): String =
        if (events.size == 1) "New date: ${events.single().bandName}" else "${events.size} new dates from bands you follow"

    fun newDatesBody(events: List<LiveConcertEvent>): String =
        events.take(3).joinToString("\n") { "${it.bandName} · ${it.date} · ${it.venueName}, ${it.city}" } +
            if (events.size > 3) "\n+${events.size - 3} more" else ""

    fun tonightTitle(shows: List<ShowEntity>): String =
        if (shows.size == 1) "Tonight: ${shows.single().bandName}" else "${shows.size} shows tonight"

    fun tonightBody(shows: List<ShowEntity>): String =
        shows.joinToString("\n") { "${it.bandName} at ${it.venue}, ${it.city}" } +
            "\nOpen DeadSet to track the setlist live."
}
