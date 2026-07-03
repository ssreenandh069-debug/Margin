package com.Margin.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.Margin.app.data.local.dao.*
import com.Margin.app.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
//  AppDatabase — Room database definition for the Margin app
//
//  VERSION HISTORY
//  ───────────────
//  v1  Initial schema: sessions, subjects, attendance_records, tasks, timetable
//  v2  Added monthly_summaries table (MonthlySummaryEntity)
//  v3  Added proxyCount column to monthly_summaries (MIGRATION_2_3)
//
//  MIGRATION POLICY (READ BEFORE CHANGING THE SCHEMA)
//  ────────────────────────────────────────────────────
//  ❌ DO NOT add `.fallbackToDestructiveMigration()` to the builder.
//     This would silently wipe all user attendance data whenever the
//     version number is bumped without a matching Migration object.
//     That is an unrecoverable, catastrophic data loss for the user.
//
//  ✅ Every schema change MUST:
//     1. Increment `version` by 1 (e.g. 3 → 4)
//     2. Write a Migration(old, new) object with the required SQL
//     3. Register it with `.addMigrations(MIGRATION_X_Y)` below
//
//  During development, if Room throws an IllegalStateException about a
//  missing migration, that is the correct behaviour — write the migration.
// ─────────────────────────────────────────────────────────────────────────────

@Database(
    entities = [
        SessionEntity::class,
        SubjectEntity::class,
        AttendanceRecordEntity::class,
        TaskEntity::class,
        TimetableEntity::class,
        MonthlySummaryEntity::class
    ],
    version = 3,
    exportSchema = false   // Set to true + configure schemaLocation in build.gradle
                           // if you want Room to generate schema JSON files for
                           // compile-time migration validation (recommended for CI).
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao
    abstract fun subjectDao(): SubjectDao
    abstract fun attendanceRecordDao(): AttendanceRecordDao
    abstract fun taskDao(): TaskDao
    abstract fun timetableDao(): TimetableDao
    abstract fun monthlySummaryDao(): MonthlySummaryDao

    fun vacuumAndCheckpoint() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(TRUNCATE)")
                openHelper.writableDatabase.execSQL("VACUUM")
            } catch (_: Exception) {}
        }
    }

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // ── Migrations ────────────────────────────────────────────────────────
        //
        // Add new Migration objects here when the schema changes.
        // Then register them in getInstance() with .addMigrations(...).
        //
        // Template for the next migration:
        //   val MIGRATION_3_4 = object : Migration(3, 4) {
        //       override fun migrate(db: SupportSQLiteDatabase) {
        //           db.execSQL("ALTER TABLE ... ADD COLUMN ...")
        //       }
        //   }

        /**
         * v2 → v3: Adds `proxyCount` column to `monthly_summaries`.
         * DEFAULT 0 means existing rows remain valid — no full table rebuild needed.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE monthly_summaries ADD COLUMN proxyCount INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "margin.db"
                )
                // ── Registered migrations (add new ones here as they are written) ──
                .addMigrations(MIGRATION_2_3)
                //
                // ⛔ NEVER add .fallbackToDestructiveMigration() here.
                //    See the policy comment at the top of this file.
                //
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
