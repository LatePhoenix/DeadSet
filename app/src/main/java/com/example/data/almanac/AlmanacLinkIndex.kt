package com.example.data.almanac

import com.example.data.songs.songKey

/**
 * Resolves Almanac deep links from bundled articles (relatedSongs / relatedShows / date in ids).
 * Prefer Song Stories for song taps; Show Spotlights for calendar dates.
 */
class AlmanacLinkIndex(articles: List<AlmanacArticle>) {

    private val bySong: Map<String, String>
    private val byShowId: Map<String, String>
    private val byMonthDay: Map<String, String>

    init {
        val songMap = linkedMapOf<String, String>()
        val showMap = linkedMapOf<String, String>()
        val dayMap = linkedMapOf<String, String>()
        for (article in articles) {
            for (song in article.relatedSongs) {
                val key = songKey(song)
                val existing = songMap[key]
                val existingArticle = articles.firstOrNull { it.id == existing }
                // Prefer song_story over other types when multiple articles list the song.
                if (existing == null ||
                    (article.type == AlmanacArticleType.SONG_STORY &&
                        existingArticle?.type != AlmanacArticleType.SONG_STORY)
                ) {
                    songMap[key] = article.id
                }
            }
            for (showId in article.relatedShows) {
                showMap[showId] = article.id
                monthDay(showId)?.let { md -> putDay(dayMap, md, article) }
            }
            monthDay(article.id)?.let { md -> putDay(dayMap, md, article) }
        }
        bySong = songMap
        byShowId = showMap
        byMonthDay = dayMap
    }

    fun articleForSong(name: String): String? = bySong[songKey(name)]

    fun articleForShowId(showId: String): String? = byShowId[showId]

    /** Spotlight (or other) article whose id/relatedShows contain this ISO date's month-day. */
    fun articleForIsoDate(isoDate: String): String? {
        val md = monthDay(isoDate) ?: return null
        return byMonthDay[md]
    }

    companion object {
        private val ISO_DATE = Regex("""(\d{4})-(\d{2})-(\d{2})""")

        fun monthDay(raw: String): String? {
            val m = ISO_DATE.find(raw) ?: return null
            return "${m.groupValues[2]}-${m.groupValues[3]}"
        }

        /** Prefer Show Spotlights for calendar dates; keep first non-spotlight as fallback. */
        private fun putDay(
            dayMap: MutableMap<String, String>,
            md: String,
            article: AlmanacArticle
        ) {
            if (article.type == AlmanacArticleType.SHOW_SPOTLIGHT) {
                dayMap[md] = article.id
            } else {
                dayMap.putIfAbsent(md, article.id)
            }
        }
    }
}
