package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.SongEntry
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val songListType = Types.newParameterizedType(List::class.java, SongEntry::class.java)
    private val adapter = moshi.adapter<List<SongEntry>>(songListType)

    @TypeConverter
    fun fromSongList(songs: List<SongEntry>?): String {
        return if (songs.isNullOrEmpty()) "[]" else adapter.toJson(songs)
    }

    @TypeConverter
    fun toSongList(json: String?): List<SongEntry> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
