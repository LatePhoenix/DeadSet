package com.example.data.songs

import com.example.data.seed.CuratedShows
import org.junit.Assert.assertTrue
import org.junit.Test

/** App-only: seed titles must already match [SongCatalog] / [SongResolver] spelling. */
class SeedSongNamesTest {

    @Test
    fun `every seeded song title is already canonical`() {
        val resolver = SongResolver.fromCatalog()
        val names = CuratedShows.getInitialShows()
            .flatMap { it.songs }
            .map { it.name }
            .distinct()
        val changed = names.filter { resolver.resolve(it).canonicalName != it }
        assertTrue("Seed titles that would be renamed: $changed", changed.isEmpty())
    }
}
