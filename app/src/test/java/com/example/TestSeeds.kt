package com.example

import com.example.data.local.ShowEntity
import com.example.data.model.SongEntry
import com.example.data.seed.CuratedShows

/** Test helpers over the built-in seed shows. */
object TestSeeds {
    val seeds = CuratedShows.getInitialShows()
    val shows: List<ShowEntity> = seeds.map { it.show }
    private val songsById = seeds.associate { it.id to it.songs }

    /** The seed setlist for [show], keyed by id so copies with changed fields still resolve. */
    fun songsFor(show: ShowEntity): List<SongEntry> = songsById[show.id].orEmpty()

    fun seed(id: String) = seeds.first { it.id == id }
}
