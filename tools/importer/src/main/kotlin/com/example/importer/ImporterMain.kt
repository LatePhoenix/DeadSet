package com.example.importer

import java.io.File

/**
 * Entry point for the build-time importer.
 *
 * C1 only wires parse + summary. C2+ will fetch, normalize, and emit the Room asset DB.
 *
 * Usage: `./gradlew :tools:importer:run --args="<year.yaml> [year.yaml…]"`
 */
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        System.err.println("Usage: ImporterMain <year.yaml> [year.yaml…]")
        kotlin.system.exitProcess(1)
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
