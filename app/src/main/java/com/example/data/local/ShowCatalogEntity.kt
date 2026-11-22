package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Catalog columns for a show (bundled / shared facts). User fields live in [UserShowDataEntity].
 * UI code reads the joined [ShowEntity] view; writers go through [ShowDao] helpers that split rows.
 */
@Entity(
    tableName = "shows",
    indices = [
        Index(value = ["date"]),
        Index(value = ["venue"]),
        Index(value = ["city"]),
        Index(value = ["eraId"])
    ]
)
data class ShowCatalogEntity(
    @PrimaryKey
    val id: String,
    val date: String,
    val venue: String,
    val city: String,
    val stateOrCountry: String,
    val eraId: String,
    val tourName: String,
    val archiveIdentifier: String = "",
    val tapeQuality: String = "",
    val isCustomShow: Boolean = false,
    val tags: String = "",
    val historicTrivia: String = "",
    val bandName: String = "Grateful Dead",
    val venueAddress: String = "",
    val recreatesShowId: String? = null,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)

fun ShowEntity.toCatalog(): ShowCatalogEntity = ShowCatalogEntity(
    id = id,
    date = date,
    venue = venue,
    city = city,
    stateOrCountry = stateOrCountry,
    eraId = eraId,
    tourName = tourName,
    archiveIdentifier = archiveIdentifier,
    tapeQuality = tapeQuality,
    isCustomShow = isCustomShow,
    tags = tags,
    historicTrivia = historicTrivia,
    bandName = bandName,
    venueAddress = venueAddress,
    recreatesShowId = recreatesShowId,
    lastUpdatedTimestamp = lastUpdatedTimestamp
)

fun ShowEntity.toUserData(): UserShowDataEntity = UserShowDataEntity(
    showId = id,
    isAttended = isAttended,
    userRating = userRating,
    userReview = userReview,
    attendedWith = attendedWith,
    favoriteSong = favoriteSong
)
