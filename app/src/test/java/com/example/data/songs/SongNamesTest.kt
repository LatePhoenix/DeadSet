package com.example.data.songs

import com.example.data.local.Converters
import com.example.data.seed.CuratedShows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SongNamesTest {

    @Test
    fun `songKey ignores case punctuation ampersands hyphens and a leading The`() {
        assertEquals(songKey("Saint Stephen"), songKey("saint stephen!"))
        assertEquals(songKey("St. Stephen"), songKey("st stephen"))
        assertEquals(songKey("Samson and Delilah"), songKey("Samson & Delilah"))
        assertEquals(songKey("Black-Throated Wind"), songKey("Black Throated Wind"))
        assertEquals(songKey("U.S. Blues"), songKey("US Blues"))
        assertEquals(songKey("Truckin'"), songKey("Truckin"))
        assertEquals(songKey("The Other One"), songKey("Other One"))
        assertEquals(songKey("  Morning   Dew "), songKey("Morning Dew"))
        assertEquals("unknown", songKey("  ?! "))
    }

    @Test
    fun `distinct songs keep distinct keys`() {
        assertNotEquals(songKey("The Other One"), songKey("That's It for the Other One"))
        assertNotEquals(songKey("Dark Star"), songKey("Dark Star Jam"))
        assertNotEquals(songKey("Saint Stephen"), songKey("Saint of Circumstance"))
    }

    @Test
    fun `common variants resolve to the canonical song`() {
        val resolver = SongResolver.fromCatalog()
        fun canonical(name: String) = resolver.resolve(name).canonicalName
        assertEquals("Saint Stephen", canonical("St. Stephen"))
        assertEquals("Turn On Your Love Light", canonical("Lovelight"))
        assertEquals("Slipknot!", canonical("Slipknot"))
        assertEquals("Goin' Down the Road Feeling Bad", canonical("GDTRFB"))
        assertEquals("Playing in the Band", canonical("Playin'"))
        assertEquals("Not Fade Away", canonical("NFA"))
        assertEquals(resolver.resolve("St Stephen").songId, resolver.resolve("Saint Stephen").songId)
    }

    @Test
    fun `unknown titles are kept as typed, cleaned, and recorded as new songs`() {
        val resolver = SongResolver.fromCatalog()
        val first = resolver.resolve("  My   Own Jam ")
        assertEquals("My Own Jam", first.canonicalName)
        assertEquals(mapOf(first.songId to "My Own Jam"), resolver.newSongs.toMap())
        // A later spelling variant joins the first one instead of creating a duplicate.
        assertEquals(first, resolver.resolve("my own jam!"))
        assertEquals(1, resolver.newSongs.size)
    }

    @Test
    fun `catalog aliases never point at two different songs`() {
        val claims = mutableMapOf<String, MutableSet<String>>()
        for (song in SongCatalog.songs) {
            for (name in listOf(song.canonicalName) + song.aliases) {
                claims.getOrPut(songKey(name)) { mutableSetOf() } += song.canonicalName
            }
        }
        val conflicts = claims.filterValues { it.size > 1 }
        assertTrue("Conflicting aliases: $conflicts", conflicts.isEmpty())
    }

    @Test
    fun `every seeded song title is already canonical`() {
        val resolver = SongResolver.fromCatalog()
        val converters = Converters()
        val names = CuratedShows.getInitialShows()
            .flatMap { converters.toSongList(it.setlistJson) }
            .map { it.name }
            .distinct()
        val changed = names.filter { resolver.resolve(it).canonicalName != it }
        assertTrue("Seed titles that would be renamed: $changed", changed.isEmpty())
    }
}
