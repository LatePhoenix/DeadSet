package com.example.data.songs

/**
 * Matching key for a song title: case-, punctuation- and article-insensitive.
 * "St. Stephen", "st stephen" and "ST STEPHEN!" share a key; "Samson & Delilah" matches
 * "Samson and Delilah"; "Black-Throated Wind" matches "Black Throated Wind".
 * The key doubles as the stable `songs.id`.
 */
fun songKey(name: String): String {
    val folded = name.lowercase()
        .replace("&", " and ")
        .replace('-', ' ')
        .replace('/', ' ')
        .filter { it.isLetterOrDigit() || it.isWhitespace() }
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .let { words -> if (words.size > 1 && words.first() == "the") words.drop(1) else words }
        .joinToString(" ")
    return folded.ifEmpty { "unknown" }
}

/** Trims and collapses whitespace without changing spelling; used for songs not in the catalog. */
fun cleanSongName(name: String): String = name.trim().replace(Regex("\\s+"), " ")

data class CatalogSong(val canonicalName: String, val aliases: List<String> = emptyList())

/**
 * Canonical song titles plus common alternate spellings and setlist shorthand.
 *
 * Canonical names follow the app's existing seed spellings so nothing visible changes.
 * Songs not listed here are still normalized by [songKey]; the catalog is only needed when a
 * variant isn't a punctuation/case/"The" difference (e.g. "Lovelight", "GDTRFB", "Playin'").
 * Changing a canonical name later needs a migration: existing rows are not rewritten.
 */
object SongCatalog {
    val songs: List<CatalogSong> = listOf(
        CatalogSong("Saint Stephen", listOf("St. Stephen")),
        CatalogSong("Turn On Your Love Light", listOf("Lovelight", "Love Light", "Turn On Your Lovelight")),
        CatalogSong("Slipknot!"),
        CatalogSong(
            "Goin' Down the Road Feeling Bad",
            listOf("GDTRFB", "Going Down the Road Feeling Bad", "Goin' Down the Road Feelin' Bad")
        ),
        CatalogSong("Playing in the Band", listOf("Playin' in the Band", "Playin'", "Playin")),
        CatalogSong("Dancin' in the Street", listOf("Dancing in the Street", "Dancing in the Streets", "Dancin' in the Streets")),
        CatalogSong("Good Lovin'", listOf("Good Loving")),
        CatalogSong("Truckin'", listOf("Trucking")),
        CatalogSong("Mississippi Half-Step", listOf("Mississippi Half-Step Uptown Toodeloo", "Half-Step")),
        CatalogSong("New Minglewood Blues", listOf("All New Minglewood Blues", "Minglewood Blues", "Minglewood")),
        CatalogSong("Man Smart Woman Smarter", listOf("Women Are Smarter")),
        CatalogSong("Memphis Blues Again", listOf("Stuck Inside of Mobile with the Memphis Blues Again")),
        CatalogSong("The Mighty Quinn", listOf("Quinn the Eskimo", "Quinn the Eskimo (The Mighty Quinn)")),
        CatalogSong("Not Fade Away", listOf("NFA")),
        CatalogSong("Uncle John's Band", listOf("UJB")),
        CatalogSong("China Cat Sunflower", listOf("China Cat")),
        CatalogSong("I Know You Rider", listOf("Rider")),
        CatalogSong("Scarlet Begonias", listOf("Scarlet")),
        CatalogSong("Fire on the Mountain", listOf("Fire")),
        CatalogSong("Estimated Prophet", listOf("Estimated")),
        CatalogSong("Eyes of the World", listOf("Eyes")),
        CatalogSong("Franklin's Tower", listOf("Franklin's")),
        CatalogSong("Help on the Way", listOf("Help")),
        CatalogSong("Sugar Magnolia", listOf("Sugar Mag")),
        CatalogSong("Drums", listOf("Drumz")),
        CatalogSong("Space"),
        CatalogSong("Dark Star"),
        CatalogSong("The Other One"),
        CatalogSong("That's It for the Other One"),
        CatalogSong("Weather Report Suite"),
        CatalogSong("Terrapin Station", listOf("Terrapin")),
        CatalogSong("Morning Dew"),
        CatalogSong("Saint of Circumstance"),
        CatalogSong("Lost Sailor"),
        CatalogSong("Throwing Stones"),
        CatalogSong("The Wheel"),
        CatalogSong("The Eleven"),
        CatalogSong("Wharf Rat"),
        CatalogSong("Stella Blue")
    )

    /** alias key → canonical name, covering each canonical name's own key. */
    val canonicalByKey: Map<String, String> = buildMap {
        for (song in songs) {
            put(songKey(song.canonicalName), song.canonicalName)
            for (alias in song.aliases) put(songKey(alias), song.canonicalName)
        }
    }
}

/**
 * Resolves raw titles to `(songId, canonicalName)` against a set of known aliases and records
 * any titles it hasn't seen, so callers can persist them. Shared by the repository and the
 * 3→4 migration so both normalize identically.
 */
class SongResolver(
    aliasToSongId: Map<String, String>,
    songNames: Map<String, String>
) {
    data class Resolved(val songId: String, val canonicalName: String)

    private val aliasToSongId = aliasToSongId.toMutableMap()
    private val songNames = songNames.toMutableMap()

    /** Songs created during resolution (id → canonical name) that need inserting. */
    val newSongs = linkedMapOf<String, String>()

    /** Alias keys created during resolution (alias key → song id) that need inserting. */
    val newAliases = linkedMapOf<String, String>()

    fun resolve(rawName: String): Resolved {
        val key = songKey(rawName)
        aliasToSongId[key]?.let { id -> return Resolved(id, songNames[id] ?: cleanSongName(rawName)) }

        val name = cleanSongName(rawName).ifEmpty { "Unknown" }
        val id = key
        if (id !in songNames) {
            songNames[id] = name
            newSongs[id] = name
        }
        aliasToSongId[key] = id
        newAliases[key] = id
        return Resolved(id, songNames.getValue(id))
    }

    companion object {
        /** A resolver seeded only from [SongCatalog]. */
        fun fromCatalog(): SongResolver {
            val names = SongCatalog.songs.associate { songKey(it.canonicalName) to it.canonicalName }
            val aliases = SongCatalog.canonicalByKey.mapValues { (_, canonical) -> songKey(canonical) }
            return SongResolver(aliases, names)
        }
    }
}
