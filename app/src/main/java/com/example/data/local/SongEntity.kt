package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per distinct song. [id] is [com.example.data.songs.songKey] of the canonical name, so it
 * is stable and readable (e.g. "scarlet begonias"). Writers/debut dates are deliberately absent
 * until they can be filled from sourced data (see docs/PROGRESS.md, Phase 5).
 */
@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val canonicalName: String
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
