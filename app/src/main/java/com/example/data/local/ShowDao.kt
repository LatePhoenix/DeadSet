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

    /**
     * Insert-or-update in place. REPLACE would delete the existing row first, and with foreign
     * keys on that cascade-deletes its whole setlist.
     */
    @Upsert
    suspend fun insertShow(show: ShowEntity)

    /** Saves a show and its full setlist atomically; an interruption leaves the old version intact. */
    @Transaction
    suspend fun saveShowWithSetlist(show: ShowEntity, performances: List<SongPerformanceEntity>) {
        insertShow(show)
        replaceSongPerformances(show.id, performances)
    }

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

    /**
     * Clears invented personal data from a seed show, only if every invented field (rating,
     * favorite song, companions, review opening) is still exactly what the app shipped. Attendance
     * is reset only when [clearAttendance] (the attendance itself was invented).
     */
    @Query(
        "UPDATE shows SET userRating = 0, userReview = '', attendedWith = '', favoriteSong = '', " +
            "isAttended = CASE WHEN :clearAttendance THEN 0 ELSE isAttended END " +
            "WHERE id = :id AND userRating = :inventedRating AND favoriteSong = :inventedFavorite " +
            "AND attendedWith = :inventedCompanions AND substr(userReview, 1, length(:inventedReviewPrefix)) = :inventedReviewPrefix"
    )
    suspend fun clearInventedPersonalData(
        id: String,
        inventedRating: Float,
        inventedFavorite: String,
        inventedCompanions: String,
        inventedReviewPrefix: String,
        clearAttendance: Boolean
    )

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
