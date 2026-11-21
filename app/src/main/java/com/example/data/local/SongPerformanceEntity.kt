package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.data.model.SetType
import com.example.data.model.SongEntry
import com.example.data.model.TransitionType

/**
 * Normalized Room entity storing individual song performances for each show.
 * Linked to [ShowEntity] (CASCADE delete) and to [SongEntity] via [songId]. [songName] holds the
 * song's canonical name at write time, kept for display and existing LIKE queries.
 */
@Entity(
    tableName = "song_performances",
    foreignKeys = [
        ForeignKey(
            entity = ShowEntity::class,
            parentColumns = ["id"],
            childColumns = ["showId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"]
        )
    ],
    indices = [
        Index(value = ["showId"]),
        Index(value = ["songName"]),
        Index(value = ["songId"]),
        Index(value = ["showId", "trackOrder"], unique = true)
    ]
)
data class SongPerformanceEntity(
    @PrimaryKey(autoGenerate = true)
    val performanceId: Long = 0L,
    val showId: String,
    val songId: String,
    val songName: String,
    val setType: String = SetType.SET_1.name,
    val trackOrder: Int,
    /** [TransitionType] name into the next song; text (like [setType]) so rows stay readable. */
    val transitionOut: String = TransitionType.NONE.name,
    val durationMinutes: Int? = null,
    val notes: String? = null,
    val isJamHighlight: Boolean = false
) {
    fun toSongEntry(): SongEntry {
        val resolvedSetType = runCatching { SetType.valueOf(setType) }.getOrDefault(SetType.SET_1)
        val resolvedTransition = runCatching { TransitionType.valueOf(transitionOut) }
            .getOrDefault(TransitionType.NONE)
        return SongEntry(
            name = songName,
            setType = resolvedSetType,
            transitionOut = resolvedTransition,
            durationMinutes = durationMinutes,
            notes = notes,
            isJamHighlight = isJamHighlight
        )
    }

    companion object {
        fun fromSongEntry(
            showId: String,
            index: Int,
            entry: SongEntry,
            songId: String
        ): SongPerformanceEntity {
            return SongPerformanceEntity(
                showId = showId,
                songId = songId,
                songName = entry.name,
                setType = entry.setType.name,
                trackOrder = index,
                transitionOut = entry.transitionOut.name,
                durationMinutes = entry.durationMinutes,
                notes = entry.notes,
                isJamHighlight = entry.isJamHighlight
            )
        }
    }
}

/**
 * Relational Room projection joining a [ShowEntity] (date, venue, city, metadata)
 * with its ordered list of [SongPerformanceEntity] rows.
 */
data class ShowWithSongs(
    @Embedded
    val show: ShowEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "showId"
    )
    val songsPerformed: List<SongPerformanceEntity>
) {
    /** The setlist in performance order, with [SongEntry.position] = 1-based slot within its set. */
    val orderedSongs: List<SongEntry>
        get() = orderSongs(songsPerformed)

    companion object {
        fun orderSongs(rows: List<SongPerformanceEntity>): List<SongEntry> {
            val perSet = mutableMapOf<SetType, Int>()
            return rows.sortedBy { it.trackOrder }.map { row ->
                val entry = row.toSongEntry()
                val slot = (perSet[entry.setType] ?: 0) + 1
                perSet[entry.setType] = slot
                entry.copy(position = slot)
            }
        }
    }
}
