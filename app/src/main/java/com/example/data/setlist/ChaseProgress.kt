package com.example.data.setlist

import com.example.data.local.ShowEntity
import com.example.data.model.SongEntry

/** Whether a chased song has been heard live yet, and where it was heard first. */
data class ChaseStatus(
    val songId: String,
    val songName: String,
    val firstHeardAt: ShowEntity?
) {
    val heard: Boolean get() = firstHeardAt != null
}

/** Personal live-listening stats across every band (step 4.7). */
object ChaseProgress {

    /**
     * For each chased song, the earliest attended show (any band) that included it. Titles match
     * through catalog aliases, so chasing "St. Stephen" counts a "Saint Stephen" you heard.
     */
    fun statuses(
        chased: List<Pair<String, String>>, // songId to songName
        attendedShows: List<ShowEntity>,
        songsFor: (ShowEntity) -> List<SongEntry>
    ): List<ChaseStatus> {
        val firstHeard = mutableMapOf<String, ShowEntity>()
        for (show in attendedShows.sortedBy { it.date }) {
            for (song in songsFor(show)) {
                firstHeard.putIfAbsent(SetlistAnalytics.canonicalKey(song.name), show)
            }
        }
        return chased.map { (id, name) ->
            ChaseStatus(id, name, firstHeard[SetlistAnalytics.canonicalKey(name)])
        }.sortedWith(compareBy<ChaseStatus> { it.heard }.thenBy { it.songName })
    }

    /** Distinct songs heard live across attended shows (aliases merged). */
    fun distinctSongsHeard(attendedShows: List<ShowEntity>, songsFor: (ShowEntity) -> List<SongEntry>): Int =
        attendedShows.flatMap(songsFor).map { SetlistAnalytics.canonicalKey(it.name) }.toSet().size

    /** Attended shows per band, most first. */
    fun showsByBand(attendedShows: List<ShowEntity>): List<Pair<String, Int>> =
        attendedShows.groupingBy { it.bandName }.eachCount().entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { it.key to it.value }
}
