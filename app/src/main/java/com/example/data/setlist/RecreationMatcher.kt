package com.example.data.setlist

import com.example.data.local.ShowEntity
import com.example.data.model.SongEntry

/** One row of a side-by-side comparison; either side may be missing. */
data class ComparedRow(val tribute: String?, val original: String?) {
    val matches: Boolean get() =
        tribute != null && original != null &&
            SetlistAnalytics.canonicalKey(tribute) == SetlistAnalytics.canonicalKey(original)
}

data class RecreationComparison(val rows: List<ComparedRow>, val matchedInOrder: Int, val originalLength: Int) {
    /** Share of the original setlist the tribute played in the same order (0..1). */
    val score: Float get() = if (originalLength == 0) 0f else matchedInOrder.toFloat() / originalLength
}

/**
 * Compares a tribute band's setlist with the Grateful Dead show it recreates (step 4.8), e.g. a
 * Dark Star Orchestra night that replays 5/8/77. Alignment is a longest-common-subsequence over
 * canonical song keys, so skipped or extra songs don't throw off the rest of the comparison.
 */
object RecreationMatcher {

    fun compare(tribute: List<SongEntry>, original: List<SongEntry>): RecreationComparison {
        val a = tribute.map { SetlistAnalytics.canonicalKey(it.name) }
        val b = original.map { SetlistAnalytics.canonicalKey(it.name) }
        val lcs = Array(a.size + 1) { IntArray(b.size + 1) }
        for (i in a.indices.reversed()) for (j in b.indices.reversed()) {
            lcs[i][j] = if (a[i] == b[j]) lcs[i + 1][j + 1] + 1 else maxOf(lcs[i + 1][j], lcs[i][j + 1])
        }
        val rows = mutableListOf<ComparedRow>()
        var i = 0
        var j = 0
        while (i < a.size || j < b.size) {
            when {
                i < a.size && j < b.size && a[i] == b[j] -> { rows += ComparedRow(tribute[i].name, original[j].name); i++; j++ }
                j < b.size && (i >= a.size || lcs[i][j + 1] >= lcs[i + 1][j]) -> { rows += ComparedRow(null, original[j].name); j++ }
                else -> { rows += ComparedRow(tribute[i].name, null); i++ }
            }
        }
        return RecreationComparison(rows, lcs[0][0], b.size)
    }

    /**
     * The Grateful Dead show in [candidates] this tribute setlist most likely recreates, if it
     * follows at least [threshold] of that show's setlist in order. Needs 5+ tribute songs.
     */
    fun bestMatch(
        tribute: List<SongEntry>,
        candidates: List<ShowEntity>,
        songsFor: (ShowEntity) -> List<SongEntry>,
        threshold: Float = 0.6f
    ): Pair<ShowEntity, Float>? {
        if (tribute.size < 5) return null
        return candidates
            .filter { it.bandName.equals("Grateful Dead", ignoreCase = true) }
            .map { it to compare(tribute, songsFor(it)).score }
            .filter { it.second >= threshold }
            .maxByOrNull { it.second }
    }
}
