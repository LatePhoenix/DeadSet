package com.example.data.almanac

import com.example.data.local.ShowEntity
import com.example.data.model.SongEntry
import com.example.data.setlist.SetlistAnalytics
import com.example.data.setlist.SongFlow
import com.example.data.songs.songKey

/**
 * Data-driven Song Story extras: catalog play counts / common ins-outs plus sourced
 * writers/debut from [SongStoryFacts] (or Room once applied). Never invents dates.
 */
data class SongStoryPanel(
    val songName: String,
    val writers: String?,
    val debutDate: String?,
    val sourceUrl: String?,
    val timesPlayedInCatalog: Int,
    val firstCatalogDate: String?,
    val lastCatalogDate: String?,
    val topInto: List<String>,
    val topOutOf: List<String>
)

object SongStoryComposer {

    fun panelsFor(
        article: AlmanacArticle,
        shows: List<ShowEntity>,
        songsFor: (ShowEntity) -> List<SongEntry>,
        roomWritersBySongId: Map<String, String?> = emptyMap(),
        roomDebutBySongId: Map<String, String?> = emptyMap()
    ): List<SongStoryPanel> {
        if (article.type != AlmanacArticleType.SONG_STORY) return emptyList()
        val facts = SongStoryFacts.bySongId()
        return article.relatedSongs.map { name ->
            val id = songKey(name)
            val fact = facts[id]
            val flow: SongFlow = SetlistAnalytics.songFlow(name, shows, songsFor)
            val span = playSpan(name, shows, songsFor)
            SongStoryPanel(
                songName = name,
                writers = roomWritersBySongId[id] ?: fact?.writers,
                debutDate = roomDebutBySongId[id] ?: fact?.debutDate,
                sourceUrl = fact?.sourceUrl,
                timesPlayedInCatalog = flow.timesPlayed,
                firstCatalogDate = span.first,
                lastCatalogDate = span.second,
                topInto = flow.into.take(3).map { "${it.songName} (${it.count})" },
                topOutOf = flow.outOf.take(3).map { "${it.songName} (${it.count})" }
            )
        }
    }

    /** First/last show dates in [shows] where the song appears (catalog-derived, not invented). */
    fun playSpan(
        songName: String,
        shows: List<ShowEntity>,
        songsFor: (ShowEntity) -> List<SongEntry>
    ): Pair<String?, String?> {
        val target = SetlistAnalytics.canonicalKey(songName)
        val dates = shows.asSequence()
            .filter { show -> songsFor(show).any { SetlistAnalytics.canonicalKey(it.name) == target } }
            .map { it.date }
            .sorted()
            .toList()
        return dates.firstOrNull() to dates.lastOrNull()
    }
}
