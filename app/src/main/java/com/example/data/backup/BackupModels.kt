package com.example.data.backup

import com.example.data.local.BandEntity
import com.example.data.local.ChaseSongEntity
import com.example.data.local.ReviewCardEntity
import com.example.data.local.ShowCatalogEntity
import com.example.data.local.SongPerformanceEntity
import com.example.data.local.UserPrefsStore
import com.example.data.local.UserShowDataEntity
import com.example.data.seed.TriviaUserProgress

const val BACKUP_FORMAT = "deadset-backup"
const val BACKUP_VERSION = 1

data class BackupDocument(
    val version: Int,
    val exportedAt: String,
    val appVersion: String,
    val userShowData: List<UserShowDataEntity>,
    val customShows: List<BackupCustomShow>,
    val chaseSongs: List<ChaseSongEntity>,
    val reviewCards: List<ReviewCardEntity>,
    val bands: List<BandEntity>,
    val prefs: BackupPrefs
)

data class BackupCustomShow(
    val catalog: ShowCatalogEntity,
    val performances: List<SongPerformanceEntity>
)

data class BackupPrefs(
    val profile: UserPrefsStore.StoredProfile?,
    val triviaProgress: TriviaUserProgress,
    val alertsEnabled: Boolean
)

data class BackupPreview(
    val attendedCount: Int,
    val userShowDataCount: Int,
    val customShowCount: Int,
    val chaseSongCount: Int,
    val reviewCardCount: Int,
    val bandCount: Int,
    val hasProfile: Boolean,
    val alertsEnabled: Boolean
) {
    fun summaryLines(): String = buildString {
        append("$attendedCount attended show")
        if (attendedCount != 1) append('s')
        append(", $customShowCount custom show")
        if (customShowCount != 1) append('s')
        append(", $chaseSongCount chase song")
        if (chaseSongCount != 1) append('s')
        append(", $reviewCardCount review card")
        if (reviewCardCount != 1) append('s')
        append(", $bandCount band")
        if (bandCount != 1) append('s')
        if (hasProfile) append(", profile")
        append(", alerts ")
        append(if (alertsEnabled) "on" else "off")
    }
}

sealed class BackupParseResult {
    data class Ok(val document: BackupDocument) : BackupParseResult()
    data class Err(val message: String) : BackupParseResult()
}

data class BackupImportResult(
    val userShowDataUpserted: Int,
    val customShowsUpserted: Int,
    val chaseSongsUpserted: Int,
    /** Review cards taken from the backup (not ones where the local copy was kept). */
    val reviewCardsUpserted: Int,
    val bandsUpserted: Int,
    val skippedUnknownShowIds: Int,
    val skippedCatalogCollisions: Int = 0,
    /** Matching cards where the phone's copy was preferred over the backup (H3). */
    val reviewCardsKeptLocal: Int = 0
) {
    fun feedbackMessage(): String = buildString {
        append("Restored: $userShowDataUpserted show memories, $customShowsUpserted custom shows, ")
        append("$chaseSongsUpserted chase songs, $reviewCardsUpserted review cards, $bandsUpserted bands")
        val skipped = skippedUnknownShowIds + skippedCatalogCollisions
        if (skipped > 0) {
            append(". Skipped $skipped item")
            if (skipped != 1) append('s')
            val parts = mutableListOf<String>()
            if (skippedUnknownShowIds > 0) {
                parts += "$skippedUnknownShowIds unknown show id" +
                    if (skippedUnknownShowIds != 1) "s" else ""
            }
            if (skippedCatalogCollisions > 0) {
                parts += "$skippedCatalogCollisions catalog id collision" +
                    if (skippedCatalogCollisions != 1) "s" else ""
            }
            append(" (${parts.joinToString("; ")})")
        }
        if (reviewCardsKeptLocal > 0) {
            append(". Kept $reviewCardsKeptLocal newer card")
            if (reviewCardsKeptLocal != 1) append('s')
            append(" on this phone")
        }
        append('.')
    }
}

/** Max backup file size accepted on restore (H2). */
const val BACKUP_MAX_BYTES = 5 * 1024 * 1024

/**
 * Reads UTF-8 from [input], stopping once more than [maxBytes] have been read (H3).
 * Returns null when the stream exceeds the limit (nothing usable is returned).
 */
fun readBoundedUtf8(input: java.io.InputStream, maxBytes: Int = BACKUP_MAX_BYTES): String? {
    val buffer = ByteArray(8192)
    val out = java.io.ByteArrayOutputStream()
    var total = 0
    while (true) {
        val n = input.read(buffer)
        if (n < 0) break
        if (total + n > maxBytes) return null
        out.write(buffer, 0, n)
        total += n
    }
    return out.toString(Charsets.UTF_8.name())
}

/** Pending restore held in [com.example.ui.ProfileViewModel] so rotation doesn't serialize JSON (H3). */
data class PendingRestore(
    val document: BackupDocument,
    val preview: BackupPreview
)
