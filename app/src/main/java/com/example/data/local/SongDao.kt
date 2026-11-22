package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SongDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAliases(aliases: List<SongAliasEntity>)

    @Query("SELECT * FROM songs")
    suspend fun getAllSongs(): List<SongEntity>

    @Query("SELECT * FROM song_aliases")
    suspend fun getAllAliases(): List<SongAliasEntity>

    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
    suspend fun getSong(id: String): SongEntity?

    /** Fills sourced writers/debut only; never overwrites a non-null value with null. */
    @Query(
        "UPDATE songs SET writers = COALESCE(:writers, writers), " +
            "debutDate = COALESCE(:debutDate, debutDate) WHERE id = :id"
    )
    suspend fun updateSongFacts(id: String, writers: String?, debutDate: String?)
}
