package com.example.data.credits

/**
 * Static attribution for the bundled Grateful Dead catalog and related links.
 * Facts only — sources named in PROGRESS / verified licenses; nothing invented.
 */
object CatalogCredits {

    data class Source(
        val name: String,
        val role: String,
        val url: String?,
        val licenseNote: String?
    )

    val sources: List<Source> = listOf(
        Source(
            name = "gdshowsdb",
            role = "Primary setlist catalog (1965–1995), including per-song segue flags",
            url = "https://github.com/jefmsmit/gdshowsdb",
            licenseNote = "MIT License"
        ),
        Source(
            name = "Deadlists",
            role = "Likely underlying setlist research credited by gdshowsdb's deadlists_parser",
            url = "http://www.deadlists.com/",
            licenseNote = null
        ),
        Source(
            name = "Archive.org",
            role = "\"Listen on Archive.org\" links search the Grateful Dead collection by show date",
            url = "https://archive.org/details/GratefulDead",
            licenseNote = null
        )
    )

    /** Verified from gdshowsdb LICENSE.txt (Copyright (c) 2013 Jeff Smith). */
    val GDSHOWSDB_MIT_LICENSE: String = """
Copyright (c) 2013 Jeff Smith

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
""".trimIndent()

    const val INTRO =
        "DeadSet's historical Grateful Dead setlists are imported from open sources. " +
            "Segue markers in the catalog are yes/no only (imported as seamless segues); " +
            "finer notation (>, ~>) is left for user edits."
}
