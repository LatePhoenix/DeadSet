package com.example.importer

/**
 * One song performance from a gdshowsdb year YAML.
 * [segued] maps later to SEGUE vs NONE (the source has no `>` / `~>` distinction).
 */
data class ParsedSong(
    val uuid: String,
    val name: String,
    val segued: Boolean
)

data class ParsedSet(
    val uuid: String,
    val songs: List<ParsedSong>
)

/**
 * One show from gdshowsdb. [dateIso] is `yyyy-MM-dd` (converted from the YAML key `yyyy/MM/dd`).
 */
data class ParsedShow(
    val dateIso: String,
    val uuid: String,
    val venue: String,
    val city: String,
    val state: String,
    val country: String,
    val sets: List<ParsedSet>
) {
    val songCount: Int get() = sets.sumOf { it.songs.size }
    val segueCount: Int get() = sets.sumOf { set -> set.songs.count { it.segued } }
}

/**
 * Assigns app [SetType] names for a show's ordered sets.
 *
 * Decision (reversible): with 1–2 sets use SET_1 / SET_2; with 3+ sets the last is ENCORE and
 * earlier ones are SET_1, SET_2, SET_3 (capped). Documented for owner review in PROGRESS.
 */
fun setTypeNamesFor(setCount: Int): List<String> {
    require(setCount >= 0)
    if (setCount == 0) return emptyList()
    if (setCount == 1) return listOf("SET_1")
    if (setCount == 2) return listOf("SET_1", "SET_2")
    val body = (0 until setCount - 1).map { i ->
        when (i) {
            0 -> "SET_1"
            1 -> "SET_2"
            else -> "SET_3"
        }
    }
    return body + "ENCORE"
}
