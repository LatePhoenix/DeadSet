package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.remote.LiveConcertEvent
import kotlinx.coroutines.flow.Flow

/** A band the user can follow for upcoming dates (step 4.1). [id] is the lowercase name. */
@Entity(tableName = "bands")
data class BandEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isFollowed: Boolean = true,
    /** Built-in band (can be unfollowed but not deleted). */
    val isBuiltIn: Boolean = false
) {
    companion object {
        fun idFor(name: String) = name.trim().lowercase()
    }
}

/**
 * A cached upcoming event (step 4.2). Stored so the Tickets tab works offline and can say where
 * each date came from ([source]) and when it was last confirmed ([lastCheckedAt], epoch millis).
 */
@Entity(tableName = "events", indices = [Index("date"), Index("bandName")])
data class EventEntity(
    @PrimaryKey val id: String,
    val bandName: String,
    val title: String,
    val date: String,
    val displayDate: String,
    val venueName: String,
    val city: String,
    val stateCode: String,
    val country: String,
    val venueAddress: String,
    val ticketUrl: String,
    val priceRange: String,
    val source: String,
    val note: String,
    val latitude: Double?,
    val longitude: Double?,
    val lastCheckedAt: Long
) {
    fun toEvent() = LiveConcertEvent(
        id = id, bandName = bandName, eventTitle = title, date = date, displayDate = displayDate,
        venueName = venueName, city = city, stateCode = stateCode, country = country,
        venueAddress = venueAddress, ticketUrl = ticketUrl, priceRange = priceRange, apiSource = source,
        tourNote = note, latitude = latitude, longitude = longitude
    )

    companion object {
        fun from(e: LiveConcertEvent, checkedAt: Long) = EventEntity(
            id = e.id, bandName = e.bandName, title = e.eventTitle, date = e.date, displayDate = e.displayDate,
            venueName = e.venueName, city = e.city, stateCode = e.stateCode, country = e.country,
            venueAddress = e.venueAddress, ticketUrl = e.ticketUrl, priceRange = e.priceRange, source = e.apiSource,
            note = e.tourNote, latitude = e.latitude, longitude = e.longitude, lastCheckedAt = checkedAt
        )
    }
}

@Dao
interface ConcertDao {
    @Query("SELECT * FROM bands ORDER BY isFollowed DESC, name ASC")
    fun observeBands(): Flow<List<BandEntity>>

    @Query("SELECT * FROM bands ORDER BY name ASC")
    suspend fun getAllBands(): List<BandEntity>

    @Query("SELECT * FROM bands WHERE isFollowed = 1 ORDER BY name ASC")
    suspend fun followedBands(): List<BandEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBands(bands: List<BandEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBands(bands: List<BandEntity>)

    @Query("UPDATE bands SET isFollowed = :followed WHERE id = :id")
    suspend fun setFollowed(id: String, followed: Boolean)

    @Query("DELETE FROM bands WHERE id = :id AND isBuiltIn = 0")
    suspend fun deleteUserBand(id: String)

    @Query("SELECT * FROM events WHERE date >= :today ORDER BY date ASC")
    fun observeUpcomingEvents(today: String): Flow<List<EventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvents(events: List<EventEntity>)

    @Query("SELECT id FROM events")
    suspend fun eventIds(): List<String>

    @Query(
        """
        SELECT e.* FROM events e
        INNER JOIN bands b ON b.name = e.bandName AND b.isFollowed = 1
        WHERE e.date >= :today
        ORDER BY e.date ASC
        """
    )
    suspend fun upcomingEventsOnce(today: String): List<EventEntity>

    @Query("DELETE FROM events WHERE date < :today")
    suspend fun deletePastEvents(today: String)

    /**
     * Replaces the cached events for [bandNames] with a fresh fetch. Only called after at least one
     * source answered, so a failed refresh never wipes the cache.
     */
    @Transaction
    suspend fun replaceEventsForBands(bandNames: List<String>, events: List<EventEntity>) {
        deleteEventsForBands(bandNames)
        upsertEvents(events)
    }

    @Query("DELETE FROM events WHERE bandName IN (:bandNames)")
    suspend fun deleteEventsForBands(bandNames: List<String>)
}
