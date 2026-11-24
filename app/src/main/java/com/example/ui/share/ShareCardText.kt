package com.example.ui.share

import com.example.data.local.ShowEntity
import com.example.data.model.SongEntry
import com.example.data.model.TransitionType

const val SETLIST_CREDIT = "Setlist data: gdshowsdb / Deadlists"

/** Pure setlist card lines from app data only (6.5 / H3). Catalog shows get the data credit. */
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
    val credit = if (!show.isCustomShow) listOf("", SETLIST_CREDIT) else emptyList()
    return listOf("DeadSet", band, header, "") + body + more + credit
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

/**
 * Breaks [text] at spaces so each piece fits [maxWidth] under [measure] (H3).
 * Words longer than the width are kept intact (never drop content).
 */
fun wrapLineToWidth(text: String, maxWidth: Float, measure: (String) -> Float): List<String> {
    if (text.isEmpty()) return listOf("")
    if (measure(text) <= maxWidth) return listOf(text)
    val words = text.split(' ')
    if (words.size == 1) return listOf(text)
    val lines = mutableListOf<String>()
    var current = StringBuilder()
    for (word in words) {
        if (current.isEmpty()) {
            current.append(word)
            continue
        }
        val candidate = "$current $word"
        if (measure(candidate) <= maxWidth) {
            current.append(' ').append(word)
        } else {
            lines.add(current.toString())
            current = StringBuilder(word)
        }
    }
    if (current.isNotEmpty()) lines.add(current.toString())
    return lines
}

/** Wraps each input line; empty lines stay empty. */
fun wrapShareLines(lines: List<String>, maxWidth: Float, measure: (String) -> Float): List<String> =
    lines.flatMap { line ->
        if (line.isEmpty()) listOf("") else wrapLineToWidth(line, maxWidth, measure)
    }
