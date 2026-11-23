package com.example.data.almanac

import com.example.data.songs.songKey

/**
 * Resolves Almanac deep links from bundled articles (relatedSongs / relatedShows / date in ids).
 * Prefer Song Stories for song taps; Show Spotlights for calendar dates.
 *
 * Full ISO dates (`yyyy-MM-dd`) drive show spotlights (R4). Month-day (`MM-DD`) is only for an
 * explicit "on this day" card that names the year.
 */
class AlmanacLinkIndex(articles: List<AlmanacArticle>) {

    private val bySong: Map<String, String>
    private val byShowId: Map<String, String>
    private val byFullDate: Map<String, String>
    private val byMonthDay: Map<String, String>

    init {
        val songMap = linkedMapOf<String, String>()
        val showMap = linkedMapOf<String, String>()
        val fullDateMap = linkedMapOf<String, String>()
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
                fullDate(showId)?.let { fd -> putDate(fullDateMap, fd, article) }
                monthDay(showId)?.let { md -> putDay(dayMap, md, article) }
            }
            fullDate(article.id)?.let { fd -> putDate(fullDateMap, fd, article) }
            monthDay(article.id)?.let { md -> putDay(dayMap, md, article) }
        }
        bySong = songMap
        byShowId = showMap
        byFullDate = fullDateMap
        byMonthDay = dayMap
    }

    fun articleForSong(name: String): String? = bySong[songKey(name)]

    fun articleForShowId(showId: String): String? = byShowId[showId]

    /** Spotlight (or other) article for this exact ISO date (`yyyy-MM-dd`), not other years. */
    fun articleForIsoDate(isoDate: String): String? {
        val fd = fullDate(isoDate) ?: return null
        return byFullDate[fd]
    }

    /**
     * "On this day" lookup by month-day only (`MM-DD`). Callers must name the year in UI —
     * not used for per-show spotlight links (see [articleForIsoDate]).
     */
    fun articleForMonthDay(monthDay: String): String? = byMonthDay[monthDay]

    companion object {
        private val ISO_DATE = Regex("""(\d{4})-(\d{2})-(\d{2})""")

        fun fullDate(raw: String): String? {
            val m = ISO_DATE.find(raw) ?: return null
            return "${m.groupValues[1]}-${m.groupValues[2]}-${m.groupValues[3]}"
        }

        fun monthDay(raw: String): String? {
            val m = ISO_DATE.find(raw) ?: return null
            return "${m.groupValues[2]}-${m.groupValues[3]}"
        }

        private fun putDate(
            dateMap: MutableMap<String, String>,
            iso: String,
            article: AlmanacArticle
        ) {
            if (article.type == AlmanacArticleType.SHOW_SPOTLIGHT) {
                dateMap[iso] = article.id
            } else {
                dateMap.putIfAbsent(iso, article.id)
            }
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
