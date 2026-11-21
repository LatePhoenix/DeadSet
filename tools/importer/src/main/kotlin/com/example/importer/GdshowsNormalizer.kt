package com.example.importer

import com.example.data.songs.SongCatalog
import com.example.data.songs.SongResolver
import com.example.data.songs.cleanSongName
import com.example.data.songs.songKey
import java.io.File

/**
 * Resolves every song title in parsed shows through [SongResolver].
 *
 * Titles whose [songKey] is not already in [SongCatalog] are listed in unmatched.csv with
 * occurrence counts. Aliases are **never** auto-inserted — humans add them to SongCatalog.
 */
object GdshowsNormalizer {

    data class UnmatchedTitle(
        val rawName: String,
        val songKey: String,
        val count: Int
    )

    data class NormalizeResult(
        val showCount: Int,
        val performanceCount: Int,
        val matchedPerformances: Int,
        val unmatchedPerformances: Int,
        val unmatched: List<UnmatchedTitle>
    )

    fun normalize(shows: List<ParsedShow>): NormalizeResult {
        val catalogKeys = SongCatalog.canonicalByKey.keys
        val resolver = SongResolver.fromCatalog()
        val unmatchedCounts = linkedMapOf<String, Int>() // songKey → count
        val unmatchedSample = linkedMapOf<String, String>() // songKey → first raw spelling
        var performances = 0
        var matched = 0
        var unmatchedPerf = 0

        for (show in shows) {
            for (set in show.sets) {
                for (song in set.songs) {
                    performances++
                    val key = songKey(song.name)
                    resolver.resolve(song.name)
                    if (key in catalogKeys) {
                        matched++
                    } else {
                        unmatchedPerf++
                        unmatchedCounts[key] = (unmatchedCounts[key] ?: 0) + 1
                        unmatchedSample.putIfAbsent(key, cleanSongName(song.name))
                    }
                }
            }
        }

        val unmatched = unmatchedCounts.entries
            .map { (key, count) -> UnmatchedTitle(unmatchedSample.getValue(key), key, count) }
            .sortedWith(compareByDescending<UnmatchedTitle> { it.count }.thenBy { it.rawName })

        return NormalizeResult(
            showCount = shows.size,
            performanceCount = performances,
            matchedPerformances = matched,
            unmatchedPerformances = unmatchedPerf,
            unmatched = unmatched
        )
    }

    fun writeUnmatchedCsv(file: File, unmatched: List<UnmatchedTitle>) {
        file.parentFile?.mkdirs()
        file.writeText(buildString {
            append("count,song_key,sample_name\n")
            for (row in unmatched) {
                append(row.count).append(',')
                append(csvEscape(row.songKey)).append(',')
                append(csvEscape(row.rawName)).append('\n')
            }
        })
    }

    private fun csvEscape(value: String): String =
        if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
}
