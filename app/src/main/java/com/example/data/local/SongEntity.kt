package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per distinct song. [id] is [com.example.data.songs.songKey] of the canonical name, so it
 * is stable and readable (e.g. "scarlet begonias"). [writers] / [debutDate] stay null until a
 * sourced fact is applied (see [com.example.data.almanac.SongStoryFacts]).
 */
@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val canonicalName: String,
    /** e.g. "Garcia / Hunter"; null when unsourced. */
    val writers: String? = null,
    /** ISO date of first known live performance when sourced; null otherwise. */
    val debutDate: String? = null
)

/** Maps a normalized alias key ("st stephen", "gdtrfb") to a song. Every song maps its own key. */
@Entity(
    tableName = "song_aliases",
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["songId"])]
)
data class SongAliasEntity(
    @PrimaryKey val aliasKey: String,
    val songId: String
)
