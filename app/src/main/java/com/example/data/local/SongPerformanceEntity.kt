package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.data.model.SetType
import com.example.data.model.SongEntry

/**
 * Normalized Room entity storing individual song performances for each Grateful Dead show.
 * Linked via a ForeignKey to [ShowEntity] with CASCADE delete, and indexed by [showId] and
 * [songName] for fast offline SQLite lookups across historical concert setlists.
 */
@Entity(
    tableName = "song_performances",
    foreignKeys = [
        ForeignKey(
            entity = ShowEntity::class,
            parentColumns = ["id"],
            childColumns = ["showId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["showId"]),
        Index(value = ["songName"]),
        Index(value = ["showId", "trackOrder"], unique = true)
    ]
)
data class SongPerformanceEntity(
    @PrimaryKey(autoGenerate = true)
    val performanceId: Long = 0L,
    val showId: String,
    val songName: String,
    val setType: String = SetType.SET_1.name,
    val trackOrder: Int,
    val isSegue: Boolean = false,
    val durationMinutes: Int? = null,
    val notes: String? = null,
    val isJamHighlight: Boolean = false
) {
    fun toSongEntry(): SongEntry {
        val resolvedSetType = runCatching { SetType.valueOf(setType) }.getOrDefault(SetType.SET_1)
        return SongEntry(
            name = songName,
            setType = resolvedSetType,
            isSegue = isSegue,
            durationMinutes = durationMinutes,
            notes = notes,
            isJamHighlight = isJamHighlight
        )
    }

    companion object {
        fun fromSongEntry(showId: String, index: Int, entry: SongEntry): SongPerformanceEntity {
            return SongPerformanceEntity(
                showId = showId,
                songName = entry.name,
                setType = entry.setType.name,
                trackOrder = index,
                isSegue = entry.isSegue,
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
    val orderedSongs: List<SongEntry>
        get() = songsPerformed
            .sortedBy { it.trackOrder }
            .map { it.toSongEntry() }
}
