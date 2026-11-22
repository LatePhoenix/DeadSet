package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Per-show user fields, kept out of [ShowCatalogEntity] so a bundled catalog refresh can never
 * overwrite attendance, ratings, or reviews.
 */
@Entity(
    tableName = "user_show_data",
    foreignKeys = [
        ForeignKey(
            entity = ShowCatalogEntity::class,
            parentColumns = ["id"],
            childColumns = ["showId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["showId"])]
)
data class UserShowDataEntity(
    @PrimaryKey
    val showId: String,
    val isAttended: Boolean = false,
    val userRating: Float = 0f,
    val userReview: String = "",
    val attendedWith: String = "",
    val favoriteSong: String = ""
)
