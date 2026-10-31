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

data class SongEntry(
    val name: String,
    val setType: SetType = SetType.SET_1,
    val position: Int = 0,
    val isSegue: Boolean = false, // True if transitions seamlessly `->` to next song!
    val durationMinutes: Int? = null,
    val notes: String? = null,
    val isJamHighlight: Boolean = false
)

data class LegendarySegue(
    val songA: String,
    val songB: String,
    val thirdSong: String? = null,
    val description: String,
    val iconEmoji: String
) {
    val displayTitle: String
        get() = if (thirdSong != null) "$songA ➔ $songB ➔ $thirdSong" else "$songA ➔ $songB"
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

