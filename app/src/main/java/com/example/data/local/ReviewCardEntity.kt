package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.example.data.review.ReviewState
import kotlinx.coroutines.flow.Flow

/** Spaced-repetition state for one quiz card (a TriviaQuestion id), step 3.5. */
@Entity(tableName = "review_cards")
data class ReviewCardEntity(
    @PrimaryKey val cardId: String,
    val box: Int,
    val lastReviewedDate: String,
    val timesSeen: Int,
    val timesCorrect: Int
) {
    fun toState() = ReviewState(box, lastReviewedDate, timesSeen, timesCorrect)

    companion object {
        fun from(cardId: String, s: ReviewState) =
            ReviewCardEntity(cardId, s.box, s.lastReviewedDate, s.timesSeen, s.timesCorrect)
    }
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM review_cards")
    fun observeAll(): Flow<List<ReviewCardEntity>>

    @Query("SELECT * FROM review_cards WHERE cardId = :cardId LIMIT 1")
    suspend fun get(cardId: String): ReviewCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(card: ReviewCardEntity)
}
