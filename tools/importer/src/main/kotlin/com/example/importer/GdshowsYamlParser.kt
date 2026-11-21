package com.example.importer

/**
 * Minimal hand-rolled parser for gdshowsdb year YAML files.
 *
 * Accepts only the shapes used by that corpus (no YAML library). Unexpected structure
 * throws [GdshowsParseException] with a 1-based line number when possible.
 *
 * Show keys: `YYYY/MM/DD:`. Show fields use Psych-style keys (`:venue`, `:city`, …).
 * `:sets` is a list of maps, each with `:uuid` and `:songs`; each song has `:uuid`,
 * `:name`, and `:segued`.
 */
class GdshowsParseException(message: String) : Exception(message)

object GdshowsYamlParser {

    fun parseYearFile(text: String): List<ParsedShow> {
        val lines = text.lines()
        val shows = mutableListOf<ParsedShow>()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()
            if (line.isBlank() || trimmed.startsWith("#") || trimmed == "---" || trimmed == "...") {
                i++
                continue
            }
            val dateMatch = SHOW_DATE.matchEntire(line)
                ?: throw GdshowsParseException("line ${i + 1}: expected show date key, got: ${line.take(80)}")
            val (y, m, d) = dateMatch.destructured
            val dateIso = "%s-%s-%s".format(y, m, d)
            val (show, next) = parseShow(lines, i + 1, dateIso)
            shows += show
            i = next
        }
        return shows
    }

    private fun parseShow(lines: List<String>, start: Int, dateIso: String): Pair<ParsedShow, Int> {
        var i = start
        var uuid = ""
        var venue = ""
        var city = ""
        var state = ""
        var country = ""
        var sets: List<ParsedSet> = emptyList()

        while (i < lines.size) {
            val line = lines[i]
            if (line.isBlank()) {
                i++
                continue
            }
            if (SHOW_DATE.matches(line)) break
            if (!line.startsWith("  ")) {
                throw GdshowsParseException("line ${i + 1}: expected indented show field, got: ${line.take(80)}")
            }

            when {
                line.startsWith("  :uuid:") -> {
                    uuid = scalarAfter(line, "  :uuid:")
                    i++
                }
                line.startsWith("  :venue:") -> {
                    venue = scalarAfter(line, "  :venue:")
                    i++
                }
                line.startsWith("  :city:") -> {
                    city = scalarAfter(line, "  :city:")
                    i++
                }
                line.startsWith("  :state:") -> {
                    state = scalarAfter(line, "  :state:")
                    i++
                }
                line.startsWith("  :country:") -> {
                    country = scalarAfter(line, "  :country:")
                    i++
                }
                line == "  :sets:" || line.trimEnd() == "  :sets:" -> {
                    val (parsed, next) = parseSets(lines, i + 1)
                    sets = parsed
                    i = next
                }
                else -> {
                    // Unknown show-level key (e.g. future fields): skip one line or a nested block.
                    i = skipUnknown(lines, i)
                }
            }
        }

        if (uuid.isEmpty()) {
            throw GdshowsParseException("show $dateIso: missing :uuid")
        }
        return ParsedShow(dateIso, uuid, venue, city, state, country, sets) to i
    }

    private fun parseSets(lines: List<String>, start: Int): Pair<List<ParsedSet>, Int> {
        val sets = mutableListOf<ParsedSet>()
        var i = start
        while (i < lines.size) {
            val line = lines[i]
            if (line.isBlank()) {
                i++
                continue
            }
            if (SHOW_DATE.matches(line) || isShowField(line)) break
            if (!line.startsWith("  - ")) break

            // `  - :uuid: <id>`
            val setUuid = when {
                line.startsWith("  - :uuid:") -> scalarAfter(line, "  - :uuid:")
                else -> throw GdshowsParseException("line ${i + 1}: expected set `- :uuid:`, got: ${line.take(80)}")
            }
            i++
            var songs: List<ParsedSong> = emptyList()
            while (i < lines.size) {
                val inner = lines[i]
                if (inner.isBlank()) {
                    i++
                    continue
                }
                if (inner.startsWith("  - ") || SHOW_DATE.matches(inner) || isShowField(inner)) break
                if (inner.trimEnd() == "    :songs:" || inner == "    :songs:") {
                    val (parsed, next) = parseSongs(lines, i + 1)
                    songs = parsed
                    i = next
                } else if (inner.startsWith("    :uuid:")) {
                    // Rare alternate: uuid repeated under the list item.
                    i++
                } else {
                    i = skipUnknown(lines, i)
                }
            }
            sets += ParsedSet(setUuid, songs)
        }
        return sets to i
    }

    private fun parseSongs(lines: List<String>, start: Int): Pair<List<ParsedSong>, Int> {
        val songs = mutableListOf<ParsedSong>()
        var i = start
        while (i < lines.size) {
            val line = lines[i]
            if (line.isBlank()) {
                i++
                continue
            }
            if (SHOW_DATE.matches(line) || isShowField(line) || line.startsWith("  - ")) break
            if (!line.startsWith("    - ")) break

            val songUuid = when {
                line.startsWith("    - :uuid:") -> scalarAfter(line, "    - :uuid:")
                else -> throw GdshowsParseException("line ${i + 1}: expected song `- :uuid:`, got: ${line.take(80)}")
            }
            i++
            var name = ""
            var segued = false
            while (i < lines.size) {
                val f = lines[i]
                if (f.isBlank()) {
                    i++
                    continue
                }
                if (!f.startsWith("      ") || f.startsWith("    - ") || f.startsWith("  - ") ||
                    SHOW_DATE.matches(f) || isShowField(f)
                ) {
                    break
                }
                val trimmed = f.trimStart()
                when {
                    trimmed.startsWith(":name:") -> {
                        name = unquote(trimmed.removePrefix(":name:").trim())
                        i++
                    }
                    trimmed.startsWith(":segued:") -> {
                        segued = trimmed.removePrefix(":segued:").trim().equals("true", ignoreCase = true)
                        i++
                    }
                    trimmed.startsWith(":uuid:") -> {
                        i++ // already have uuid from list item
                    }
                    else -> i++
                }
            }
            if (name.isEmpty()) {
                throw GdshowsParseException("song $songUuid: missing :name")
            }
            songs += ParsedSong(songUuid, name, segued)
        }
        return songs to i
    }

    /** Skip an unknown key; if the next lines are more deeply indented, skip those too. */
    private fun skipUnknown(lines: List<String>, start: Int): Int {
        val baseIndent = lines[start].takeWhile { it == ' ' }.length
        var i = start + 1
        while (i < lines.size) {
            val line = lines[i]
            if (line.isBlank()) {
                i++
                continue
            }
            val indent = line.takeWhile { it == ' ' }.length
            if (indent <= baseIndent) return i
            i++
        }
        return i
    }

    private fun isShowField(line: String): Boolean =
        line.startsWith("  :") && !line.startsWith("  - ")

    private fun scalarAfter(line: String, prefix: String): String =
        unquote(line.removePrefix(prefix).trim())

    private fun unquote(raw: String): String {
        val s = raw.trim()
        return when {
            s.length >= 2 && s.startsWith('"') && s.endsWith('"') -> s.substring(1, s.length - 1)
            s.length >= 2 && s.startsWith('\'') && s.endsWith('\'') -> s.substring(1, s.length - 1)
            else -> s
        }
    }

    private val SHOW_DATE = Regex("""^(\d{4})/(\d{2})/(\d{2}):\s*$""")
}
