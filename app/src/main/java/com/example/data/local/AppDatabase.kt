package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities = [
        ShowEntity::class,
        SongPerformanceEntity::class,
        SongEntity::class,
        SongAliasEntity::class,
        ReviewCardEntity::class
    ],
    version = AppDatabase.VERSION,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun showDao(): ShowDao
    abstract fun songDao(): SongDao
    abstract fun reviewDao(): ReviewDao

    companion object {
        /** Current schema version. Bump it, add a Migration, and commit the new schemas/N.json. */
        const val VERSION = 7

        /** First version with an exported schema; every version from here on must be migratable. */
        const val BASELINE_VERSION = 3

        const val DATABASE_NAME = "deadset_database"

        /**
         * Every schema migration, in order. Add new ones here whenever [Database.version] is
         * bumped, together with the exported `schemas/.../N.json`.
         */
        val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)

        /**
         * Versions released before schema export existed (step 1.1). Their exact schemas are
         * unknown, so they can't be migrated and are rebuilt from scratch. Data from v3 onward
         * is never dropped: a missing migration crashes loudly instead of silently wiping it.
         */
        val UNMIGRATABLE_PRE_BASELINE_VERSIONS = intArrayOf(1, 2)

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).applyMigrationPolicy().build()
                INSTANCE = instance
                instance
            }
        }

        /** Shared by the app and migration tests so tests exercise the real configuration. */
        fun RoomDatabase.Builder<AppDatabase>.applyMigrationPolicy(): RoomDatabase.Builder<AppDatabase> =
            addMigrations(*ALL_MIGRATIONS)
                .fallbackToDestructiveMigrationFrom(
                    dropAllTables = true,
                    *UNMIGRATABLE_PRE_BASELINE_VERSIONS
                )

        fun createInMemoryDatabase(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppDatabase::class.java
            ).allowMainThreadQueries().build()
        }
    }
}
