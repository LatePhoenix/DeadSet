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
    val reviewCardsUpserted: Int,
    val bandsUpserted: Int,
    val skippedUnknownShowIds: Int
) {
    fun feedbackMessage(): String = buildString {
        append("Restored: $userShowDataUpserted show memories, $customShowsUpserted custom shows, ")
        append("$chaseSongsUpserted chase songs, $reviewCardsUpserted review cards, $bandsUpserted bands")
        if (skippedUnknownShowIds > 0) {
            append(". Skipped $skippedUnknownShowIds unknown show id")
            if (skippedUnknownShowIds != 1) append('s')
            append(" (not in catalog)")
        }
        append('.')
    }
}
