package com.example.data.almanac

import com.example.data.songs.songKey

/**
 * Sourced writer / debut facts for Almanac Song Stories. Only songs with a cited [sourceUrl]
 * appear here — never invent credits or dates. Writer strings follow dead.net Music By / Lyrics By.
 * [debutDate] is set only when a cited show/feature page states the first live performance.
 */
data class SongStoryFact(
    val canonicalName: String,
    val writers: String?,
    val debutDate: String?,
    val sourceUrl: String
)

object SongStoryFacts {

    val all: List<SongStoryFact> = listOf(
        SongStoryFact(
            canonicalName = "Scarlet Begonias",
            writers = "Garcia / Hunter",
            debutDate = null,
            sourceUrl = "https://www.dead.net/song/scarlet-begonias"
        ),
        SongStoryFact(
            canonicalName = "Fire on the Mountain",
            writers = "Hart / Hunter",
            debutDate = "1977-03-18",
            sourceUrl = "https://www.dead.net/features/greatest-stories-ever-told/greatest-stories-ever-told-fire-mountain"
        ),
        SongStoryFact(
            canonicalName = "Help on the Way",
            writers = "Garcia / Hunter",
            debutDate = null,
            sourceUrl = "https://www.dead.net/song/help-way"
        ),
        SongStoryFact(
            canonicalName = "Slipknot!",
            // R6: sole "Kreutzmann" was disputed (band co-write often cited). Agent could not
            // re-fetch dead.net this session — leave null until owner confirms the page.
            writers = null,
            debutDate = null,
            sourceUrl = "https://www.dead.net/song/slipknot"
        ),
        SongStoryFact(
            canonicalName = "Franklin's Tower",
            writers = "Garcia / Kreutzmann / Hunter",
            debutDate = null,
            sourceUrl = "https://www.dead.net/song/franklins-tower"
        ),
        SongStoryFact(
            canonicalName = "Terrapin Station",
            writers = "Garcia / Hunter",
            debutDate = null,
            sourceUrl = "https://www.dead.net/song/terrapin-station"
        )
    )

    fun bySongId(): Map<String, SongStoryFact> =
        all.associateBy { songKey(it.canonicalName) }
}
