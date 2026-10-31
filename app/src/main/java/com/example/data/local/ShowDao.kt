package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShowDao {
    @Query("SELECT * FROM shows ORDER BY date DESC")
    fun getAllShows(): Flow<List<ShowEntity>>

    @Transaction
    @Query("SELECT * FROM shows ORDER BY date DESC")
    fun getAllShowsWithSongs(): Flow<List<ShowWithSongs>>

    @Query("SELECT * FROM shows WHERE id = :id LIMIT 1")
    fun getShowById(id: String): Flow<ShowEntity?>

    @Transaction
    @Query("SELECT * FROM shows WHERE id = :id LIMIT 1")
    fun getShowWithSongsById(id: String): Flow<ShowWithSongs?>

    @Query("SELECT * FROM shows WHERE isAttended = 1 ORDER BY date DESC")
    fun getAttendedShows(): Flow<List<ShowEntity>>

    @Query(
        """
        SELECT * FROM shows
        WHERE date LIKE '%' || :query || '%'
           OR venue LIKE '%' || :query || '%'
           OR city LIKE '%' || :query || '%'
           OR stateOrCountry LIKE '%' || :query || '%'
           OR setlistJson LIKE '%' || :query || '%'
        ORDER BY date DESC
        """
    )
    fun searchShowsOffline(query: String): Flow<List<ShowEntity>>

    @Query("SELECT * FROM shows WHERE city LIKE :city ORDER BY date DESC")
    fun getShowsByCity(city: String): Flow<List<ShowEntity>>

    @Query("SELECT * FROM shows WHERE venue LIKE '%' || :venue || '%' ORDER BY date DESC")
    fun getShowsByVenue(venue: String): Flow<List<ShowEntity>>

    @Query(
        """
        SELECT s.* FROM shows s
        INNER JOIN song_performances sp ON s.id = sp.showId
        WHERE sp.songName LIKE '%' || :songName || '%'
        GROUP BY s.id
        ORDER BY s.date DESC
        """
    )
    fun getShowsPerformingSong(songName: String): Flow<List<ShowEntity>>

    @Query("SELECT DISTINCT city FROM shows ORDER BY city ASC")
    fun getDistinctCities(): Flow<List<String>>

    @Query("SELECT DISTINCT venue FROM shows ORDER BY venue ASC")
    fun getDistinctVenues(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM shows")
    suspend fun getShowsCount(): Int

    @Query("SELECT COUNT(*) FROM song_performances")
    suspend fun getSongPerformancesCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertShows(shows: List<ShowEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShow(show: ShowEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongPerformances(performances: List<SongPerformanceEntity>)

    @Query("DELETE FROM song_performances WHERE showId = :showId")
    suspend fun deleteSongPerformancesForShow(showId: String)

    @Query("SELECT * FROM song_performances WHERE showId = :showId ORDER BY trackOrder ASC")
    fun getSongsForShow(showId: String): Flow<List<SongPerformanceEntity>>

    @Update
    suspend fun updateShow(show: ShowEntity)

    @Delete
    suspend fun deleteShow(show: ShowEntity)

    @Query("UPDATE shows SET isAttended = :isAttended WHERE id = :id")
    suspend fun updateAttendance(id: String, isAttended: Boolean)

    @Query("UPDATE shows SET userReview = :review, userRating = :rating, attendedWith = :attendedWith, favoriteSong = :favoriteSong, lastUpdatedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateReview(
        id: String,
        review: String,
        rating: Float,
        attendedWith: String,
        favoriteSong: String,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE shows SET setlistJson = :setlistJson WHERE id = :id")
    suspend fun updateSetlist(id: String, setlistJson: String)
}
