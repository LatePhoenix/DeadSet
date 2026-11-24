package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShowDao {
    @Query("SELECT * FROM shows_with_user ORDER BY date DESC")
    fun getAllShows(): Flow<List<ShowEntity>>

    @Transaction
    @Query("SELECT * FROM shows_with_user ORDER BY date DESC")
    fun getAllShowsWithSongs(): Flow<List<ShowWithSongs>>

    @Query("SELECT * FROM shows_with_user WHERE id = :id LIMIT 1")
    fun getShowById(id: String): Flow<ShowEntity?>

    @Transaction
    @Query("SELECT * FROM shows_with_user WHERE id = :id LIMIT 1")
    fun getShowWithSongsById(id: String): Flow<ShowWithSongs?>

    @Query("SELECT * FROM shows_with_user WHERE isAttended = 1 ORDER BY date DESC")
    fun getAttendedShows(): Flow<List<ShowEntity>>

    @Query(
        """
        SELECT * FROM shows_with_user
        WHERE date LIKE '%' || :query || '%'
           OR venue LIKE '%' || :query || '%'
           OR city LIKE '%' || :query || '%'
           OR stateOrCountry LIKE '%' || :query || '%'
           OR EXISTS (
               SELECT 1 FROM song_performances sp
               WHERE sp.showId = shows_with_user.id AND sp.songName LIKE '%' || :query || '%'
           )
        ORDER BY date DESC
        """
    )
    fun searchShowsOffline(query: String): Flow<List<ShowEntity>>

    @Query("SELECT * FROM shows_with_user WHERE city LIKE :city ORDER BY date DESC")
    fun getShowsByCity(city: String): Flow<List<ShowEntity>>

    @Query("SELECT * FROM shows_with_user WHERE venue LIKE '%' || :venue || '%' ORDER BY date DESC")
    fun getShowsByVenue(venue: String): Flow<List<ShowEntity>>

    @Query(
        """
        SELECT s.* FROM shows_with_user s
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

    @Query("SELECT * FROM shows_with_user")
    suspend fun getAllShowsOnce(): List<ShowEntity>

    @Query("SELECT COUNT(*) FROM shows")
    suspend fun getShowsCount(): Int

    @Query("SELECT COUNT(*) FROM song_performances")
    suspend fun getSongPerformancesCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCatalogShows(shows: List<ShowCatalogEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUserDataIgnore(rows: List<UserShowDataEntity>)

    @Upsert
    suspend fun upsertCatalog(show: ShowCatalogEntity)

    @Upsert
    suspend fun upsertCatalogShows(shows: List<ShowCatalogEntity>)

    @Upsert
    suspend fun upsertUserData(data: UserShowDataEntity)

    @Query("SELECT id FROM shows WHERE isCustomShow = 1")
    suspend fun getCustomShowIds(): List<String>

    @Query("SELECT * FROM user_show_data")
    suspend fun getAllUserShowData(): List<UserShowDataEntity>

    @Query("SELECT * FROM shows WHERE isCustomShow = 1")
    suspend fun getCustomCatalogShows(): List<ShowCatalogEntity>

    @Query("SELECT id FROM shows")
    suspend fun getAllShowIds(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM shows WHERE id = :id)")
    suspend fun showExists(id: String): Boolean

    /**
     * Insert-or-update catalog + user rows. Upsert (not REPLACE) so FKs on song_performances
     * are not cascade-deleted.
     */
    @Transaction
    suspend fun insertShow(show: ShowEntity) {
        upsertCatalog(show.toCatalog())
        upsertUserData(show.toUserData())
    }

    @Transaction
    suspend fun insertShows(shows: List<ShowEntity>) {
        insertCatalogShows(shows.map { it.toCatalog() })
        // Only seed empty user rows; never overwrite existing attendance/reviews on restore.
        insertUserDataIgnore(shows.map { it.toUserData() })
    }

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

    @Transaction
    suspend fun updateShow(show: ShowEntity) {
        upsertCatalog(show.toCatalog())
        upsertUserData(show.toUserData())
    }

    @Query("DELETE FROM shows WHERE id = :id")
    suspend fun deleteShowById(id: String)

    @Transaction
    suspend fun deleteShow(show: ShowEntity) {
        deleteShowById(show.id)
    }

    /**
     * F2: consolidate a curated seed-id duplicate onto its catalog UUID match in one transaction.
     * - Optionally replace the catalog setlist with remapped seed performances.
     * - If [moveUserData] is non-null, upsert it onto [catalogId] (caller ensures catalog is unclaimed).
     * - Delete the seed row when [deleteSeed] is true.
     */
    @Transaction
    suspend fun consolidateDuplicateSeed(
        seedId: String,
        catalogId: String,
        remappedPerformances: List<SongPerformanceEntity>?,
        moveUserData: UserShowDataEntity?,
        deleteSeed: Boolean
    ) {
        if (remappedPerformances != null) {
            replaceSongPerformances(catalogId, remappedPerformances)
        }
        if (moveUserData != null) {
            upsertUserData(moveUserData.copy(showId = catalogId))
        }
        if (deleteSeed) {
            deleteShowById(seedId)
        }
    }

    /**
     * Removes retired seed rows the user never claimed (not attended, nobody logged as
     * attendedWith). userReview isn't checked because the retired seeds shipped with filler text.
     */
    @Query(
        """
        DELETE FROM shows WHERE id IN (:ids) AND isCustomShow = 0
          AND NOT EXISTS (
            SELECT 1 FROM user_show_data u
            WHERE u.showId = shows.id AND (u.isAttended = 1 OR u.attendedWith != '')
          )
        """
    )
    suspend fun deleteUntouchedSeedShows(ids: List<String>)

    @Query("UPDATE shows SET recreatesShowId = :originalId WHERE id = :id")
    suspend fun setRecreation(id: String, originalId: String?)

    /**
     * Clears invented personal data from a seed show, only if every invented field (rating,
     * favorite song, companions, full review) is still exactly what the app shipped. Attendance
     * is reset only when [clearAttendance] (the attendance itself was invented).
     */
    @Query(
        "UPDATE user_show_data SET userRating = 0, userReview = '', attendedWith = '', favoriteSong = '', " +
            "isAttended = CASE WHEN :clearAttendance THEN 0 ELSE isAttended END " +
            "WHERE showId = :id AND userRating = :inventedRating AND favoriteSong = :inventedFavorite " +
            "AND attendedWith = :inventedCompanions AND userReview = :inventedReview"
    )
    suspend fun clearInventedPersonalData(
        id: String,
        inventedRating: Float,
        inventedFavorite: String,
        inventedCompanions: String,
        inventedReview: String,
        clearAttendance: Boolean
    )

    @Query("UPDATE user_show_data SET isAttended = :isAttended WHERE showId = :id")
    suspend fun updateAttendance(id: String, isAttended: Boolean)

    @Transaction
    suspend fun updateReview(
        id: String,
        review: String,
        rating: Float,
        attendedWith: String,
        favoriteSong: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        ensureUserRow(id)
        updateReviewFields(id, review, rating, attendedWith, favoriteSong)
        touchShowTimestamp(id, timestamp)
    }

    @Query(
        "UPDATE user_show_data SET userReview = :review, userRating = :rating, " +
            "attendedWith = :attendedWith, favoriteSong = :favoriteSong WHERE showId = :id"
    )
    suspend fun updateReviewFields(
        id: String,
        review: String,
        rating: Float,
        attendedWith: String,
        favoriteSong: String
    )

    @Query("UPDATE shows SET lastUpdatedTimestamp = :timestamp WHERE id = :id")
    suspend fun touchShowTimestamp(id: String, timestamp: Long)

    @Query("INSERT OR IGNORE INTO user_show_data (showId, isAttended, userRating, userReview, attendedWith, favoriteSong) VALUES (:id, 0, 0, '', '', '')")
    suspend fun ensureUserRow(id: String)

    @Transaction
    suspend fun updateAttendanceEnsured(id: String, isAttended: Boolean) {
        ensureUserRow(id)
        updateAttendance(id, isAttended)
    }

    /**
     * Shows where [fromSongId] is immediately followed by [toSongId] (adjacent trackOrder).
     * When [requireSegue] is true, the first song must have `transitionOut = 'SEGUE'`.
     */
    @Query(
        """
        SELECT DISTINCT a.showId FROM song_performances a
        INNER JOIN song_performances b
          ON a.showId = b.showId AND b.trackOrder = a.trackOrder + 1
        WHERE a.songId = :fromSongId AND b.songId = :toSongId
          AND (:requireSegue = 0 OR a.transitionOut = 'SEGUE')
        """
    )
    suspend fun showIdsWithAdjacentSongs(
        fromSongId: String,
        toSongId: String,
        requireSegue: Boolean
    ): List<String>
}
