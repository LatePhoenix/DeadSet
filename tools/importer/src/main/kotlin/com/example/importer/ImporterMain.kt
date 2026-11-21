package com.example.importer

import java.io.File

/**
 * Entry point for the build-time importer.
 *
 * Usage:
 * - `./gradlew :tools:importer:run --args="--fetch"` — download cache + write provenance.json
 * - `./gradlew :tools:importer:run --args="--fetch --force"` — re-download even if cache exists
 * - `./gradlew :tools:importer:run --args="<year.yaml> [year.yaml…]"` — parse + summarize
 */
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        printUsage()
        kotlin.system.exitProcess(1)
    }
    if (args[0] == "--fetch") {
        val force = args.contains("--force")
        val moduleDir = File(System.getProperty("user.dir"))
        val cacheDir = File(moduleDir, "cache")
        val provenance = File(moduleDir, "provenance.json")
        val result = GdshowsFetcher.fetch(cacheDir, provenance, force = force)
        println(
            "fetched ${result.provenance.commitSha.take(12)}… " +
                "downloaded=${result.downloaded} skipped=${result.skipped} " +
                "→ ${cacheDir.path}"
        )
        println("provenance → ${provenance.path}")
        return
    }

    var shows = 0
    var songs = 0
    var segues = 0
    var empty = 0
    for (path in args) {
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
    System.err.println("  ImporterMain <year.yaml> [year.yaml…]")
}
