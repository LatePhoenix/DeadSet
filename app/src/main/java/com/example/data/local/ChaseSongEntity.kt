package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** A song the user wants to hear live (step 4.7). Keyed by the canonical song id. */
@Entity(tableName = "chase_songs")
data class ChaseSongEntity(
    @PrimaryKey val songId: String,
    val songName: String,
    val addedDate: String
)

@Dao
interface ChaseDao {
    @Query("SELECT * FROM chase_songs ORDER BY songName ASC")
    fun observeAll(): Flow<List<ChaseSongEntity>>

    @Query("SELECT * FROM chase_songs ORDER BY songName ASC")
    suspend fun getAllOnce(): List<ChaseSongEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(song: ChaseSongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(song: ChaseSongEntity)

    @Query("DELETE FROM chase_songs WHERE songId = :songId")
    suspend fun remove(songId: String)
}
