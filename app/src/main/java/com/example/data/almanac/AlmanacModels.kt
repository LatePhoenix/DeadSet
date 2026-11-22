package com.example.data.almanac

enum class AlmanacArticleType(val id: String, val title: String) {
    SONG_STORY("song_story", "Song Stories"),
    SHOW_SPOTLIGHT("show_spotlight", "Show Spotlights"),
    BAND_ERA_GUIDE("band_era", "Band & Era Guides"),
    TRIBUTE_PROFILE("tribute", "Tribute Profiles");

    companion object {
        fun fromId(raw: String): AlmanacArticleType =
            entries.firstOrNull { it.id.equals(raw.trim(), ignoreCase = true) }
                ?: BAND_ERA_GUIDE
    }
}

data class AlmanacArticle(
    val id: String,
    val type: AlmanacArticleType,
    val title: String,
    val summary: String,
    val relatedSongs: List<String>,
    val relatedShows: List<String>,
    val sources: List<String>,
    val bodyMarkdown: String,
    val assetPath: String
)
