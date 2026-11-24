package com.example.di

import android.app.Application
import com.example.BuildConfig
import com.example.data.backup.BackupManager
import com.example.data.local.AppDatabase
import com.example.data.local.UserPrefsStore
import com.example.data.repository.ConcertRepository
import com.example.data.repository.ShowRepository

/**
 * Hand-written DI root (6.2). No Hilt/Koin — swap fakes in tests by constructing
 * feature ViewModels with alternate dependencies.
 */
class AppContainer(val app: Application) {
    val database: AppDatabase by lazy { AppDatabase.getInstance(app) }
    val userPrefs: UserPrefsStore by lazy { UserPrefsStore(app) }
    val showRepository: ShowRepository by lazy {
        ShowRepository(database.showDao(), database.songDao())
    }
    val concertRepository: ConcertRepository by lazy {
        ConcertRepository(database.concertDao())
    }
    val backupManager: BackupManager by lazy {
        BackupManager(database, userPrefs, BuildConfig.VERSION_NAME)
    }

    companion object {
        @Volatile
        private var instance: AppContainer? = null

        fun get(app: Application): AppContainer =
            instance ?: synchronized(this) {
                instance ?: AppContainer(app).also { instance = it }
            }

        /** Test-only: drop the process singleton between Robolectric cases. */
        fun resetForTests() {
            instance = null
        }
    }
}
