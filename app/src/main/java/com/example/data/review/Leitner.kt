package com.example.data.review

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Where one quiz card stands in the user's spaced-repetition review. */
data class ReviewState(
    val box: Int,
    val lastReviewedDate: String,
    val timesSeen: Int = 0,
    val timesCorrect: Int = 0
)

/**
 * Leitner spaced repetition (step 3.5): five boxes. A right answer moves a card up one box; a
 * wrong answer sends it back to box 1. A card in box N comes back after [intervalDays] days.
 */
object Leitner {
    const val MIN_BOX = 1
    const val MAX_BOX = 5
    private val intervals = mapOf(1 to 0, 2 to 1, 3 to 3, 4 to 7, 5 to 14)

    fun intervalDays(box: Int): Int = intervals[box.coerceIn(MIN_BOX, MAX_BOX)] ?: 0

    fun afterAnswer(previous: ReviewState?, correct: Boolean, today: String): ReviewState {
        val box = when {
            !correct -> MIN_BOX
            previous == null -> MIN_BOX + 1
            else -> (previous.box + 1).coerceAtMost(MAX_BOX)
        }
        return ReviewState(
            box = box,
            lastReviewedDate = today,
            timesSeen = (previous?.timesSeen ?: 0) + 1,
            timesCorrect = (previous?.timesCorrect ?: 0) + if (correct) 1 else 0
        )
    }

    fun isDue(state: ReviewState, today: String): Boolean =
        daysBetween(state.lastReviewedDate, today) >= intervalDays(state.box)

    /**
     * Orders card ids for a session: due cards first (lowest box, then longest-unseen), then cards
     * never seen, then not-yet-due cards (so a session is never empty).
     */
    fun order(cardIds: List<String>, states: Map<String, ReviewState>, today: String): List<String> {
        val (seen, fresh) = cardIds.partition { it in states }
        val (due, notDue) = seen.partition { isDue(states.getValue(it), today) }
        val byUrgency = compareBy<String>({ states.getValue(it).box }, { states.getValue(it).lastReviewedDate })
        return due.sortedWith(byUrgency) + fresh + notDue.sortedWith(byUrgency)
    }

    /** Whole days from [from] to [to] (ISO yyyy-MM-dd); negative if [to] is earlier. */
    fun daysBetween(from: String, to: String): Int {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val a = runCatching { fmt.parse(from)?.time }.getOrNull() ?: return Int.MAX_VALUE
        val b = runCatching { fmt.parse(to)?.time }.getOrNull() ?: return Int.MAX_VALUE
        return ((b - a) / 86_400_000L).toInt()
    }
}
