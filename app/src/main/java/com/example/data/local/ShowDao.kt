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
           OR EXISTS (
               SELECT 1 FROM song_performances sp
               WHERE sp.showId = shows.id AND sp.songName LIKE '%' || :query || '%'
           )
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

    @Query("SELECT * FROM shows")
    suspend fun getAllShowsOnce(): List<ShowEntity>

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

    @Query("SELECT * FROM song_performances WHERE showId = :showId ORDER BY trackOrder ASC")
    suspend fun getSongsForShowOnce(showId: String): List<SongPerformanceEntity>

    @Query("SELECT DISTINCT showId FROM song_performances")
    suspend fun getShowIdsWithPerformances(): List<String>

    /** Replaces a show's whole setlist atomically. */
    @Transaction
    suspend fun replaceSongPerformances(showId: String, performances: List<SongPerformanceEntity>) {
        deleteSongPerformancesForShow(showId)
        if (performances.isNotEmpty()) insertSongPerformances(performances)
    }

    @Update
    suspend fun updateShow(show: ShowEntity)

    @Delete
    suspend fun deleteShow(show: ShowEntity)

    /**
     * Removes retired seed rows the user never claimed (not attended, nobody logged as
     * attendedWith). userReview isn't checked because the retired seeds shipped with filler text.
     */
    @Query("DELETE FROM shows WHERE id IN (:ids) AND isAttended = 0 AND attendedWith = '' AND isCustomShow = 0")
    suspend fun deleteUntouchedSeedShows(ids: List<String>)

    @Query("UPDATE shows SET recreatesShowId = :originalId WHERE id = :id")
    suspend fun setRecreation(id: String, originalId: String?)

    /** Clears invented personal data from a seed show, only if it still matches what was invented. */
    @Query(
        "UPDATE shows SET isAttended = 0, userRating = 0, userReview = '', attendedWith = '', favoriteSong = '' " +
            "WHERE id = :id AND attendedWith = :inventedCompanions AND userReview LIKE :inventedReviewPrefix || '%'"
    )
    suspend fun clearInventedPersonalData(id: String, inventedCompanions: String, inventedReviewPrefix: String)

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
}
