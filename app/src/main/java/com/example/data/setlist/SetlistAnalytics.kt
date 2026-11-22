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
                if (prev != null && prev.isConnected && prev.setType == song.setType) {
                    val k = canonicalKey(prev.name)
                    into[k] = (into[k] ?: 0) + 1
                    display.putIfAbsent(k, prev.name)
                }
                val next = songs.getOrNull(i + 1)
                if (next != null && song.isConnected && next.setType == song.setType) {
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

    /**
     * Setlist-at-a-glance segments. Set I and Encore stay whole; Set II/III with Drums and/or
     * Space split into pre-Drums · Drums/Space · post-Space. Sets without Drums/Space stay one line.
     */
    fun structureSegments(songs: List<SongEntry>): List<StructureSegment> {
        if (songs.isEmpty()) return emptyList()
        val out = mutableListOf<StructureSegment>()
        for (set in SetType.entries) {
            val inSet = songs.filter { it.setType == set }
            if (inSet.isEmpty()) continue
            when (set) {
                SetType.SET_1 -> out += StructureSegment("Set I", notation(inSet))
                SetType.ENCORE -> out += StructureSegment("Encore", notation(inSet))
                SetType.SET_2, SetType.SET_3 -> out += splitAroundDrumsSpace(set, inSet)
            }
        }
        return out
    }

    private fun splitAroundDrumsSpace(set: SetType, inSet: List<SongEntry>): List<StructureSegment> {
        val firstDs = inSet.indexOfFirst(::isDrumsOrSpace)
        val lastDs = inSet.indexOfLast(::isDrumsOrSpace)
        if (firstDs < 0) {
            return listOf(StructureSegment(set.shortLabel, notation(inSet)))
        }
        val segments = mutableListOf<StructureSegment>()
        if (firstDs > 0) {
            segments += StructureSegment(
                "${set.shortLabel} (pre-Drums)",
                notation(inSet.subList(0, firstDs))
            )
        }
        segments += StructureSegment(
            "Drums/Space",
            notation(inSet.subList(firstDs, lastDs + 1))
        )
        if (lastDs < inSet.lastIndex) {
            segments += StructureSegment(
                "${set.shortLabel} (post-Space)",
                notation(inSet.subList(lastDs + 1, inSet.size))
            )
        }
        return segments
    }

    private fun isDrumsOrSpace(song: SongEntry): Boolean {
        val key = canonicalKey(song.name)
        return key == songKey("Drums") || key == songKey("Space")
    }
}

/** One labeled block of setlist notation for the Show Detail glance card. */
data class StructureSegment(val label: String, val notation: String)

/** One directed, connected transition between two songs and how often it happened. */
data class GraphEdge(val from: String, val to: String, val count: Int) {
    val label: String get() = "$from ➔ $to"
}

/** Songs (nodes) and the connected transitions among them (edges), for the transition graph. */
data class TransitionGraph(val nodes: List<String>, val edges: List<GraphEdge>) {
    fun edgesOf(node: String): List<GraphEdge> =
        edges.filter { it.from == node || it.to == node }.sortedByDescending { it.count }
}

/**
 * Builds the transition graph (step 3.6): the [maxNodes] songs with the most connected
 * transitions, and every connected transition between two of them. Titles resolve through
 * catalog aliases; each node shows the first spelling seen.
 */
fun SetlistAnalytics.transitionGraph(
    shows: List<ShowEntity>,
    songsFor: (ShowEntity) -> List<SongEntry>,
    maxNodes: Int = 14
): TransitionGraph {
    val counts = mutableMapOf<Pair<String, String>, Int>()
    val display = mutableMapOf<String, String>()
    for (show in shows) {
        val songs = songsFor(show)
        for (i in 0 until songs.size - 1) {
            if (!songs[i].isConnected) continue
            if (songs[i].setType != songs[i + 1].setType) continue
            val a = canonicalKey(songs[i].name)
            val b = canonicalKey(songs[i + 1].name)
            if (a == b) continue
            display.putIfAbsent(a, songs[i].name)
            display.putIfAbsent(b, songs[i + 1].name)
            counts[a to b] = (counts[a to b] ?: 0) + 1
        }
    }
    val degree = mutableMapOf<String, Int>()
    for ((pair, n) in counts) {
        degree[pair.first] = (degree[pair.first] ?: 0) + n
        degree[pair.second] = (degree[pair.second] ?: 0) + n
    }
    val keep = degree.entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .take(maxNodes)
        .map { it.key }
        .toSet()
    val nodes = keep.sortedBy { display.getValue(it) }
    val edges = counts.filterKeys { (a, b) -> a in keep && b in keep }
        .map { (pair, n) -> GraphEdge(display.getValue(pair.first), display.getValue(pair.second), n) }
        .sortedWith(compareByDescending<GraphEdge> { it.count }.thenBy { it.label })
    return TransitionGraph(nodes.map { display.getValue(it) }, edges)
}

/**
 * Most likely next songs for the live tracker (step 4.9): what followed [lastSong] (same set,
 * any transition) across the catalog, most often first. With no last song, the usual openers of
 * [setType]. Songs already in [alreadyPlayed] are skipped, since repeats within a show are rare.
 */
fun SetlistAnalytics.predictNext(
    lastSong: String?,
    setType: com.example.data.model.SetType,
    shows: List<ShowEntity>,
    songsFor: (ShowEntity) -> List<SongEntry>,
    alreadyPlayed: Collection<String> = emptyList(),
    limit: Int = 3
): List<String> {
    val played = alreadyPlayed.map(::canonicalKey).toSet()
    val counts = mutableMapOf<String, Int>()
    val display = mutableMapOf<String, String>()
    fun count(name: String) {
        val key = canonicalKey(name)
        if (key in played) return
        counts[key] = (counts[key] ?: 0) + 1
        display.putIfAbsent(key, name)
    }
    val target = lastSong?.let(::canonicalKey)
    for (show in shows) {
        val songs = songsFor(show)
        if (target == null) {
            songs.firstOrNull { it.setType == setType }?.let { count(it.name) }
        } else {
            for (i in 0 until songs.size - 1) {
                if (canonicalKey(songs[i].name) == target && songs[i + 1].setType == songs[i].setType) {
                    count(songs[i + 1].name)
                }
            }
        }
    }
    return counts.entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .take(limit)
        .map { display.getValue(it.key) }
}
