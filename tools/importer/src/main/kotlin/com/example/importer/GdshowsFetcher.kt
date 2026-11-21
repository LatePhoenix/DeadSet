package com.example.importer

import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Downloads gdshowsdb year YAML + `song_refs.yaml` into a local cache and writes provenance.
 *
 * Network only; cache stays gitignored. [provenanceFile] is meant to be committed so builds
 * record which upstream commit the cache came from.
 */
object GdshowsFetcher {

    const val REPO = "jefmsmit/gdshowsdb"
    const val DATA_PATH = "data/gdshowsdb"
    val YEAR_FILES: List<String> = (1965..1995).map { "$it.yaml" }
    const val SONG_REFS = "song_refs.yaml"

    /** Every file C2 expects in the cache (31 years + song_refs). */
    val CACHE_FILES: List<String> = YEAR_FILES + SONG_REFS

    data class Provenance(
        val repo: String,
        val commitSha: String,
        val fetchedAtIso: String,
        val files: List<String>
    ) {
        fun toJson(): String = buildString {
            append("{\n")
            append("  \"repo\": ").append(jsonString(repo)).append(",\n")
            append("  \"commitSha\": ").append(jsonString(commitSha)).append(",\n")
            append("  \"fetchedAtIso\": ").append(jsonString(fetchedAtIso)).append(",\n")
            append("  \"files\": [\n")
            files.forEachIndexed { i, name ->
                append("    ").append(jsonString(name))
                if (i < files.lastIndex) append(",")
                append("\n")
            }
            append("  ]\n")
            append("}\n")
        }
    }

    data class FetchResult(
        val provenance: Provenance,
        val downloaded: Int,
        val skipped: Int
    )

    /**
     * Resolve the tip SHA of [branch], download missing cache files (or all if [force]),
     * and write [provenanceFile].
     */
    fun fetch(
        cacheDir: File,
        provenanceFile: File,
        branch: String = "main",
        force: Boolean = false,
        httpGet: (String) -> String = ::httpGetText
    ): FetchResult {
        cacheDir.mkdirs()
        val sha = resolveCommitSha(branch, httpGet)
        var downloaded = 0
        var skipped = 0
        for (name in CACHE_FILES) {
            val dest = File(cacheDir, name)
            if (!force && dest.isFile && dest.length() > 0L) {
                skipped++
                continue
            }
            val url = rawUrl(sha, name)
            dest.writeText(httpGet(url))
            downloaded++
        }
        val provenance = Provenance(
            repo = REPO,
            commitSha = sha,
            fetchedAtIso = utcNowIso(),
            files = CACHE_FILES
        )
        provenanceFile.parentFile?.mkdirs()
        provenanceFile.writeText(provenance.toJson())
        return FetchResult(provenance, downloaded, skipped)
    }

    fun rawUrl(commitSha: String, fileName: String): String =
        "https://raw.githubusercontent.com/$REPO/$commitSha/$DATA_PATH/$fileName"

    fun resolveCommitSha(branch: String, httpGet: (String) -> String = ::httpGetText): String {
        val body = httpGet("https://api.github.com/repos/$REPO/commits/$branch")
        val match = Regex(""""sha"\s*:\s*"([0-9a-f]{40})"""").find(body)
            ?: throw IOException("Could not parse commit SHA for $REPO@$branch")
        return match.groupValues[1]
    }

    fun utcNowIso(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date())
    }

    private fun jsonString(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private fun httpGetText(url: String): String {
        val conn = (URI(url).toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", "DeadSet-Importer (local build tool)")
            setRequestProperty("Accept", "application/vnd.github+json, text/plain, */*")
            connectTimeout = 30_000
            readTimeout = 60_000
            instanceFollowRedirects = true
        }
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                throw IOException("HTTP $code for $url: ${body.take(200)}")
            }
            return body
        } finally {
            conn.disconnect()
        }
    }
}
