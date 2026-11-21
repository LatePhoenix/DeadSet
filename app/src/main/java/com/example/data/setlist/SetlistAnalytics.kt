package com.example.data.setlist

import com.example.data.local.ShowEntity
import com.example.data.model.SetType
import com.example.data.model.SongEntry
import com.example.data.model.TransitionType
import com.example.data.songs.SongCatalog
import com.example.data.songs.songKey

/**
 * A well-known group of songs the band often played back to back.
 *
 * Only [chain] and [note] are hand-written. The note describes the music, not history: dates,
 * counts and example shows are always computed from the catalog by [SetlistAnalytics.familyStats],
 * so the app never states a fact it can't show the evidence for.
 */
data class SegueFamily(
    val id: String,
    val nickname: String,
    val chain: List<String>,
    val note: String,
    val emoji: String
) {
    /** Label in the format [SegueMatcher] and the show filter understand. */
    val label: String get() = chain.joinToString(" ➔ ")
}

object SegueFamilies {
    val all: List<SegueFamily> = listOf(
        SegueFamily(
            "scarlet-fire", "Scarlet > Fire", listOf("Scarlet Begonias", "Fire on the Mountain"),
            "A long, upbeat jam carries the bright groove of Scarlet into Fire's two-chord vamp.", "🔥"
        ),
        SegueFamily(
            "china-rider", "China > Rider", listOf("China Cat Sunflower", "I Know You Rider"),
            "Interlocking guitar lines bridge a psychedelic tune into a traditional folk song.", "🌻"
        ),
        SegueFamily(
            "help-slip-frank", "Help > Slip > Franklin's",
            listOf("Help on the Way", "Slipknot!", "Franklin's Tower"),
            "A three-part suite: a tricky composed opener, an angular instrumental, then a joyful release.", "⚡"
        ),
        SegueFamily(
            "estimated-eyes", "Estimated > Eyes", listOf("Estimated Prophet", "Eyes of the World"),
            "An odd-meter, brooding groove opens up into a light, jazzy one.", "👁️"
        ),
        SegueFamily(
            "playin-ujb", "Playin' > UJB", listOf("Playing in the Band", "Uncle John's Band"),
            "Often a \"sandwich\": the band leaves Playin' mid-jam and returns to it later in the set.", "🥪"
        ),
        SegueFamily(
            "primal-suite", "Dark Star > St. Stephen > The Eleven",
            listOf("Dark Star", "Saint Stephen", "The Eleven"),
            "Open-ended space jamming lands on a short anthem, then the driving odd-meter Eleven.", "⭐"
        ),
        SegueFamily(
            "stephen-nfa", "St. Stephen > NFA", listOf("Saint Stephen", "Not Fade Away"),
            "A short anthem kicks straight into a Bo Diddley beat.", "🌹"
        ),
        SegueFamily(
            "drums-space", "Drums > Space", listOf("Drums", "Space"),
            "The percussion feature hands off to free-form, unstructured improvisation.", "🥁"
        ),
        SegueFamily(
            "space-wheel", "Space > The Wheel", listOf("Space", "The Wheel"),
            "A common way out of Space: a gentle, cyclical tune emerges from the noise.", "☸️"
        ),
        SegueFamily(
            "lost-sailor-saint", "Lost Sailor > Saint", listOf("Lost Sailor", "Saint of Circumstance"),
            "A slow, drifting ballad that builds into an upbeat rocker.", "⛵"
        )
    )
}

/** Computed evidence for a [SegueFamily] in the user's catalog. */
data class FamilyStat(
    val family: SegueFamily,
    val occurrences: Int,
    val firstDate: String?,
    val lastDate: String?,
    val showIds: List<String>
)

/** One neighboring song and how often it was connected to the song being explored. */
data class SongLink(val songName: String, val count: Int, val share: Float)

/** Where a song tends to come from and go to (connected transitions only). */
data class SongFlow(
    val songName: String,
    val timesPlayed: Int,
    val into: List<SongLink>,
    val outOf: List<SongLink>
)

object SetlistAnalytics {

    /** Resolves catalog aliases so "St. Stephen" and "Saint Stephen" count together. */
    fun canonicalKey(name: String): String {
        val key = songKey(name)
        return SongCatalog.canonicalByKey[key]?.let(::songKey) ?: key
    }

    fun familyStats(
        shows: List<ShowEntity>,
        songsFor: (ShowEntity) -> List<SongEntry>,
        families: List<SegueFamily> = SegueFamilies.all
    ): List<FamilyStat> = families.map { family ->
        val matching = shows.filter { SegueMatcher.matches(songsFor(it), family.label) }
            .sortedBy { it.date }
        FamilyStat(
            family = family,
            occurrences = matching.size,
            firstDate = matching.firstOrNull()?.date,
            lastDate = matching.lastOrNull()?.date,
            showIds = matching.map { it.id }
        )
    }

    /**
     * Connected transitions into and out of [songName] across [shows]. Shares are fractions of
     * that direction's total; only links with a transition (not a stop) are counted.
     */
    fun songFlow(
        songName: String,
        shows: List<ShowEntity>,
        songsFor: (ShowEntity) -> List<SongEntry>
    ): SongFlow {
        val target = canonicalKey(songName)
        var played = 0
        val into = mutableMapOf<String, Int>()
        val out = mutableMapOf<String, Int>()
        val display = mutableMapOf<String, String>()
        for (show in shows) {
            val songs = songsFor(show)
            songs.forEachIndexed { i, song ->
                if (canonicalKey(song.name) != target) return@forEachIndexed
                played++
                val prev = songs.getOrNull(i - 1)
                if (prev != null && prev.isConnected) {
                    val k = canonicalKey(prev.name)
                    into[k] = (into[k] ?: 0) + 1
                    display.putIfAbsent(k, prev.name)
                }
                val next = songs.getOrNull(i + 1)
                if (next != null && song.isConnected) {
                    val k = canonicalKey(next.name)
                    out[k] = (out[k] ?: 0) + 1
                    display.putIfAbsent(k, next.name)
                }
            }
        }
        fun links(m: Map<String, Int>): List<SongLink> {
            val total = m.values.sum().coerceAtLeast(1)
            return m.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .map { SongLink(display.getValue(it.key), it.value, it.value.toFloat() / total) }
        }
        return SongFlow(songName, played, links(into), links(out))
    }

    /**
     * A set written in standard setlist notation, e.g. "Scarlet Begonias -> Fire on the Mountain,
     * Estimated Prophet". Songs separated by a stop get a comma; connected songs get their symbol.
     */
    fun notation(songs: List<SongEntry>): String = buildString {
        songs.forEachIndexed { i, song ->
            append(song.name)
            if (i < songs.lastIndex) {
                append(
                    when (song.transitionOut) {
                        TransitionType.NONE -> ", "
                        else -> " ${song.transitionOut.symbol} "
                    }
                )
            }
        }
    }

    /** Each set's notation, in set order, skipping empty sets. */
    fun notationBySet(songs: List<SongEntry>): List<Pair<SetType, String>> =
        SetType.entries.mapNotNull { set ->
            val inSet = songs.filter { it.setType == set }
            if (inSet.isEmpty()) null else set to notation(inSet)
        }
}
