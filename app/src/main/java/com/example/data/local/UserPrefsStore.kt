package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.seed.TriviaUserProgress

/**
 * Persists small per-user state (trivia progress, profile fields) that previously lived only in
 * memory and reset whenever the app process died. Backed by SharedPreferences to avoid adding a
 * dependency; a later phase can move this to DataStore or Room.
 */
class UserPrefsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    data class StoredProfile(
        val handle: String,
        val homeCity: String,
        val favoriteJam: String,
        val favoriteEraTitle: String
    )

    fun loadTriviaProgress(): TriviaUserProgress = TriviaUserProgress(
        totalAnswered = prefs.getInt(K_ANSWERED, 0),
        totalCorrect = prefs.getInt(K_CORRECT, 0),
        currentStreak = prefs.getInt(K_CURRENT_STREAK, 0),
        bestStreak = prefs.getInt(K_BEST_STREAK, 0),
        totalPoints = prefs.getInt(K_POINTS, 0),
        quizzesCompleted = prefs.getInt(K_QUIZZES, 0),
        dailyCompletedDate = prefs.getString(K_DAILY_DATE, "").orEmpty(),
        unlockedBadgeIds = prefs.getStringSet(K_BADGES, emptySet()).orEmpty().toSet()
    )

    fun saveTriviaProgress(p: TriviaUserProgress) {
        prefs.edit()
            .putInt(K_ANSWERED, p.totalAnswered)
            .putInt(K_CORRECT, p.totalCorrect)
            .putInt(K_CURRENT_STREAK, p.currentStreak)
            .putInt(K_BEST_STREAK, p.bestStreak)
            .putInt(K_POINTS, p.totalPoints)
            .putInt(K_QUIZZES, p.quizzesCompleted)
            .putString(K_DAILY_DATE, p.dailyCompletedDate)
            // Copy: SharedPreferences must not hold a reference to a set we might mutate.
            .putStringSet(K_BADGES, HashSet(p.unlockedBadgeIds))
            .apply()
    }

    /** Returns null when no profile has been saved yet, so callers keep their defaults. */
    fun loadProfile(): StoredProfile? {
        if (!prefs.contains(K_HANDLE)) return null
        return StoredProfile(
            handle = prefs.getString(K_HANDLE, "").orEmpty(),
            homeCity = prefs.getString(K_HOME_CITY, "").orEmpty(),
            favoriteJam = prefs.getString(K_FAV_JAM, "").orEmpty(),
            favoriteEraTitle = prefs.getString(K_FAV_ERA, "").orEmpty()
        )
    }

    fun saveProfile(p: StoredProfile) {
        prefs.edit()
            .putString(K_HANDLE, p.handle)
            .putString(K_HOME_CITY, p.homeCity)
            .putString(K_FAV_JAM, p.favoriteJam)
            .putString(K_FAV_ERA, p.favoriteEraTitle)
            .apply()
    }

    // --- Show alerts (step 4.6) ---
    fun alertsEnabled(): Boolean = prefs.getBoolean(K_ALERTS, false)
    fun setAlertsEnabled(enabled: Boolean) = prefs.edit().putBoolean(K_ALERTS, enabled).apply()
    /** False until a background refresh has filled the cache once, so the first sync never alerts. */
    fun hasSyncedEvents(): Boolean = prefs.getBoolean(K_EVENTS_SYNCED, false)
    fun markEventsSynced() = prefs.edit().putBoolean(K_EVENTS_SYNCED, true).apply()
    fun lastTonightReminder(): String = prefs.getString(K_TONIGHT, "").orEmpty()
    fun setLastTonightReminder(date: String) = prefs.edit().putString(K_TONIGHT, date).apply()

    private companion object {
        const val K_ALERTS = "alerts_enabled"
        const val K_EVENTS_SYNCED = "alerts_events_synced"
        const val K_TONIGHT = "alerts_last_tonight_reminder"
        const val PREFS_NAME = "deadset_user_prefs"
        const val K_ANSWERED = "trivia_total_answered"
        const val K_CORRECT = "trivia_total_correct"
        const val K_CURRENT_STREAK = "trivia_current_streak"
        const val K_BEST_STREAK = "trivia_best_streak"
        const val K_POINTS = "trivia_total_points"
        const val K_QUIZZES = "trivia_quizzes_completed"
        const val K_DAILY_DATE = "trivia_daily_completed_date"
        const val K_BADGES = "trivia_unlocked_badges"
        const val K_HANDLE = "profile_handle"
        const val K_HOME_CITY = "profile_home_city"
        const val K_FAV_JAM = "profile_favorite_jam"
        const val K_FAV_ERA = "profile_favorite_era"
    }
}
