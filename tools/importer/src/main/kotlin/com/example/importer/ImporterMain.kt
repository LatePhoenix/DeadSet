package com.example.importer

import java.io.File

/**
 * Entry point for the build-time importer.
 *
 * Usage:
 * - `--fetch [--force]` — download cache + write provenance.json
 * - `--normalize` — parse cache year files, write out/unmatched.csv
 * - `<year.yaml>…` — parse + summarize
 */
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        printUsage()
        kotlin.system.exitProcess(1)
    }
    val moduleDir = File(System.getProperty("user.dir"))
    when (args[0]) {
        "--fetch" -> {
            val force = args.contains("--force")
            val cacheDir = File(moduleDir, "cache")
            val provenance = File(moduleDir, "provenance.json")
            val result = GdshowsFetcher.fetch(cacheDir, provenance, force = force)
            println(
                "fetched ${result.provenance.commitSha.take(12)}… " +
                    "downloaded=${result.downloaded} skipped=${result.skipped} " +
                    "→ ${cacheDir.path}"
            )
            println("provenance → ${provenance.path}")
        }
        "--normalize" -> {
            val cacheDir = File(moduleDir, "cache")
            val outDir = File(moduleDir, "out")
            val shows = loadCachedShows(cacheDir)
            val result = GdshowsNormalizer.normalize(shows)
            val csv = File(outDir, "unmatched.csv")
            GdshowsNormalizer.writeUnmatchedCsv(csv, result.unmatched)
            println(
                "normalize: ${result.showCount} shows, ${result.performanceCount} songs, " +
                    "${result.matchedPerformances} catalog-matched, " +
                    "${result.unmatchedPerformances} unmatched performances " +
                    "(${result.unmatched.size} distinct keys)"
            )
            println("unmatched → ${csv.path}")
            result.unmatched.take(15).forEach { row ->
                println("  ${row.count}\t${row.rawName}")
            }
            if (result.unmatched.size > 15) {
                println("  … ${result.unmatched.size - 15} more")
            }
        }
        else -> summarizeYears(args)
    }
}

fun loadCachedShows(cacheDir: File): List<ParsedShow> {
    if (!cacheDir.isDirectory) {
        throw IllegalStateException("Cache missing at ${cacheDir.path}; run --fetch first")
    }
    val shows = mutableListOf<ParsedShow>()
    for (name in GdshowsFetcher.YEAR_FILES) {
        val file = File(cacheDir, name)
        if (!file.isFile) {
            throw IllegalStateException("Missing $name in cache; run --fetch")
        }
        shows += GdshowsYamlParser.parseYearFile(file.readText())
    }
    return shows
}

private fun summarizeYears(paths: Array<String>) {
    var shows = 0
    var songs = 0
    var segues = 0
    var empty = 0
    for (path in paths) {
        val file = File(path)
        val parsed = GdshowsYamlParser.parseYearFile(file.readText())
        shows += parsed.size
        for (show in parsed) {
            songs += show.songCount
            segues += show.segueCount
            if (show.songCount == 0) empty++
        }
        println("${file.name}: ${parsed.size} shows")
    }
    println("total: $shows shows, $songs songs, $segues segued, $empty empty setlists")
}

private fun printUsage() {
    System.err.println("Usage:")
    System.err.println("  ImporterMain --fetch [--force]")
    System.err.println("  ImporterMain --normalize")
    System.err.println("  ImporterMain <year.yaml> [year.yaml…]")
}
