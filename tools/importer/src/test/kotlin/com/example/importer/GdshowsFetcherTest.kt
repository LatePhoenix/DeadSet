package com.example.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class GdshowsFetcherTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `cache file list is 31 years plus song_refs`() {
        assertEquals(32, GdshowsFetcher.CACHE_FILES.size)
        assertEquals("1965.yaml", GdshowsFetcher.CACHE_FILES.first())
        assertEquals("1995.yaml", GdshowsFetcher.YEAR_FILES.last())
        assertEquals("song_refs.yaml", GdshowsFetcher.CACHE_FILES.last())
    }

    @Test
    fun `rawUrl pins commit and data path`() {
        assertEquals(
            "https://raw.githubusercontent.com/jefmsmit/gdshowsdb/abc123/data/gdshowsdb/1977.yaml",
            GdshowsFetcher.rawUrl("abc123", "1977.yaml")
        )
    }

    @Test
    fun `fetch writes cache files and provenance using injected http`() {
        val cache = tmp.newFolder("cache")
        val provenance = File(tmp.root, "provenance.json")
        val sha = "2ccd86c1f0db284ed1f3967df17f8defc80a387e"
        val responses = mutableMapOf<String, String>()
        responses["https://api.github.com/repos/jefmsmit/gdshowsdb/commits/main"] =
            """{"sha":"$sha","commit":{}}"""
        for (name in GdshowsFetcher.CACHE_FILES) {
            responses[GdshowsFetcher.rawUrl(sha, name)] = "# $name\n"
        }

        val result = GdshowsFetcher.fetch(
            cacheDir = cache,
            provenanceFile = provenance,
            force = true,
            httpGet = { url -> responses[url] ?: error("unexpected URL $url") }
        )

        assertEquals(sha, result.provenance.commitSha)
        assertEquals(32, result.downloaded)
        assertEquals(0, result.skipped)
        assertEquals(32, cache.listFiles()!!.size)
        assertTrue(provenance.readText().contains(sha))
        assertTrue(provenance.readText().contains("\"1965.yaml\""))
        assertTrue(File(cache, "1977.yaml").readText().startsWith("# 1977.yaml"))
    }

    @Test
    fun `fetch skips existing files unless forced`() {
        val cache = tmp.newFolder("cache")
        val provenance = File(tmp.root, "provenance.json")
        val sha = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        File(cache, "1977.yaml").writeText("already here")
        val hits = mutableListOf<String>()
        val responses = mutableMapOf<String, String>()
        responses["https://api.github.com/repos/jefmsmit/gdshowsdb/commits/main"] =
            """{"sha":"$sha"}"""
        for (name in GdshowsFetcher.CACHE_FILES) {
            responses[GdshowsFetcher.rawUrl(sha, name)] = "new $name"
        }

        val result = GdshowsFetcher.fetch(
            cacheDir = cache,
            provenanceFile = provenance,
            force = false,
            httpGet = { url ->
                hits += url
                responses[url] ?: error("unexpected URL $url")
            }
        )

        assertEquals(31, result.downloaded)
        assertEquals(1, result.skipped)
        assertEquals("already here", File(cache, "1977.yaml").readText())
        assertTrue(hits.none { it.endsWith("/1977.yaml") })
    }
}
