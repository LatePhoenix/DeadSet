package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

enum class DeadEra(
    val id: String,
    val title: String,
    val years: String,
    val subhead: String,
    val color: Color
) {
    ALL("all", "All Eras", "1965–1995", "Complete 30-Year Catalog", StealieGold),
    PRIMAL_60S("primal_60s", "Primal Dead", "1965–1969", "Acid Tests & Live/Dead Era", EraPrimalColor),
    EARLY_70S("early_70s", "Europe '72 Era", "1970–1972", "Acoustic Sets & Keith Joins", EraEurope72Color),
    WALL_OF_SOUND("wall_of_sound", "Wall of Sound", "1973–1976", "Sonic Peaks & Hiatus", EraWallOfSoundColor),
    SPRING_77("spring_77", "Golden Peak", "1977–1979", "Cornell Barton Hall & Englishtown", EraSpring77Color),
    BRENT_80S("brent_80s", "Brent Mydland Era", "1979–1990", "Fox Theatre, Rich Stadium, Warlocks", EraBrent80sColor),
    HORNSBY_VINCE_90S("90s_era", "90s Final Tour", "1990–1995", "Marsalis Guest, Soldier Field Finale", Era90sColor);

    companion object {
        fun fromId(id: String): DeadEra {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: ALL
        }
    }
}

enum class SetType(val label: String, val shortLabel: String) {
    SET_1("Set I", "Set 1"),
    SET_2("Set II", "Set 2"),
    SET_3("Set III", "Set 3"),
    ENCORE("Encore", "Encore")
}

/**
 * How a song flows into the next one, using common setlist notation.
 * Stored by [name] in both `setlistJson` and `song_performances.transitionOut`, so never rename
 * a constant without a migration.
 *
 * Teases (another song quoted inside this one) aren't a junction between two songs, so they are
 * deliberately not a value here; they'll get their own field when needed.
 */
enum class TransitionType(val symbol: String, val label: String, val description: String) {
    /** The song ends; a pause, tuning or applause before the next one. */
    NONE("", "Stop", "stop"),
    /** Seamless, no break: `->`. */
    SEGUE("->", "Segue", "seamless segue"),
    /** Connected but not seamless (a brief pause or count-in): `>`. */
    TRANSITION(">", "Transition", "transition"),
    /** An extended jam bridges the two songs: `~>`. */
    JAM("~>", "Jam", "jams into");

    /** True for anything that carries the music into the next song. */
    val connects: Boolean get() = this != NONE

    /** Next value when tapping the setlist pill: Stop → Segue → Transition → Jam → Stop. */
    fun next(): TransitionType = entries[(ordinal + 1) % entries.size]
}

data class SongEntry(
    val name: String,
    val setType: SetType = SetType.SET_1,
    val position: Int = 0,
    val transitionOut: TransitionType = TransitionType.NONE,
    val durationMinutes: Int? = null,
    val notes: String? = null,
    val isJamHighlight: Boolean = false
) {
    /**
     * True if this song flows into the next one in any way (segue, transition or jam).
     * Computed from [transitionOut]; never stored.
     */
    val isConnected: Boolean get() = transitionOut.connects
}

data class EraStat(
    val era: DeadEra,
    val showCount: Int,
    val percentage: Float,
    val songPerformanceCount: Int = 0,
    val topSongsInEra: List<SongStat> = emptyList()
)

data class SongStat(
    val songName: String,
    val playCount: Int,
    val isAttendedCount: Int = 0
)

data class SongEraFrequency(
    val songName: String,
    val totalPlays: Int,
    val eraCounts: Map<DeadEra, Int>
)

data class SegueStat(
    val transitionLabel: String,
    val occurrences: Int
)

