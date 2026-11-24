package com.example.ui.share

import com.example.data.local.ShowEntity
import com.example.data.model.SongEntry
import com.example.data.model.TransitionType

const val SETLIST_CREDIT = "Setlist data: gdshowsdb / Deadlists"

/** Pure setlist card lines from app data only (6.5). */
fun buildSetlistShareLines(
    show: ShowEntity,
    songs: List<SongEntry>,
    maxSongs: Int = 24
): List<String> {
    val header = buildString {
        append(show.date)
        append(" · ")
        append(show.venue)
        if (show.city.isNotBlank()) {
            append(", ")
            append(show.city)
        }
    }
    val band = show.bandName.ifBlank { "Grateful Dead" }
    val body = songs.take(maxSongs).map { entry ->
        buildString {
            append(entry.name)
            if (entry.transitionOut != TransitionType.NONE) {
                append(' ')
                append(entry.transitionOut.symbol)
            }
        }
    }
    val more = if (songs.size > maxSongs) listOf("… +${songs.size - maxSongs} more") else emptyList()
    return listOf("DeadSet", band, header, "") + body + more + listOf("", SETLIST_CREDIT)
}

/** Pure trivia-score card lines from round results (6.5). */
fun buildTriviaShareLines(
    modeTitle: String,
    correctCount: Int,
    answeredCount: Int,
    sessionPoints: Int,
    maxStreak: Int,
    rankTitle: String
): List<String> {
    val accuracy = if (answeredCount > 0) {
        ((correctCount.toFloat() / answeredCount.toFloat()) * 100f).toInt()
    } else {
        0
    }
    return listOf(
        "DeadSet Trivia",
        modeTitle,
        "",
        "$correctCount / $answeredCount correct ($accuracy%)",
        "+$sessionPoints points · best streak $maxStreak",
        "Rank: $rankTitle"
    )
}
