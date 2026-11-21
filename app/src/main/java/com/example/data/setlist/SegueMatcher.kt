package com.example.data.setlist

import com.example.data.model.SongEntry
import com.example.data.songs.SongCatalog
import com.example.data.songs.songKey

/**
 * Exact segue matching over a setlist: "A ➔ B" (or a longer chain "A ➔ B ➔ C") matches only when
 * those songs appear back to back, in that order, and every song but the last flows into the next
 * one (any [com.example.data.model.TransitionType] other than NONE). Titles compare by [songKey]
 * after resolving catalog aliases,
 * so "St. Stephen" matches "Saint Stephen".
 *
 * Replaces the old word-by-word check, which matched any show whose setlist merely contained the
 * same words anywhere (e.g. "the", "band").
 */
object SegueMatcher {

    /** Splits a label like "Scarlet Begonias ➔ Fire on the Mountain" (also accepts "->" and ">"). */
    fun parseChain(label: String): List<String> =
        label.split("➔", "->", "~>", ">")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private fun canonicalKey(name: String): String {
        val key = songKey(name)
        return SongCatalog.canonicalByKey[key]?.let(::songKey) ?: key
    }

    fun matches(setlist: List<SongEntry>, label: String): Boolean {
        val chain = parseChain(label).map(::canonicalKey)
        if (chain.size < 2) return false
        val keys = setlist.map { canonicalKey(it.name) }
        for (start in 0..(setlist.size - chain.size)) {
            val sameSongs = chain.indices.all { keys[start + it] == chain[it] }
            val allConnected = (0 until chain.size - 1).all { setlist[start + it].isConnected }
            if (sameSongs && allConnected) return true
        }
        return false
    }
}
