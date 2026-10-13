package com.Margin.app.data.local

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.Margin.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

// ── Consolidated DAOs ─────────────────────────────────────────────────────────

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startDate DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveSession(): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Query("UPDATE sessions SET isActive = 0")
    suspend fun deactivateAllSessions()

    @Query("UPDATE sessions SET isActive = 1 WHERE id = :id")
    suspend fun setActive(id: String)

    @Query("UPDATE sessions SET isCompleted = 1, isActive = 0 WHERE id = :id")
    suspend fun setCompleted(id: String)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSessionById(id: String)
}

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects WHERE sessionId = :sessionId ORDER BY code ASC")
    fun getSubjectsBySession(sessionId: String): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE sessionId = :sessionId ORDER BY code ASC")
    suspend fun getSubjectsBySessionOnce(sessionId: String): List<SubjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<SubjectEntity>)

    @Delete
    suspend fun deleteSubject(subject: SubjectEntity)
}

@Dao
interface TimetableDao {
    @Query("SELECT timetable.* FROM timetable INNER JOIN subjects ON timetable.subjectId = subjects.id WHERE dayOfWeek = :day AND subjects.sessionId = :sessionId ORDER BY startTime ASC")
    fun getEntriesByDay(day: Int, sessionId: String): Flow<List<TimetableEntity>>

    @Query("SELECT timetable.* FROM timetable INNER JOIN subjects ON timetable.subjectId = subjects.id WHERE subjects.sessionId = :sessionId ORDER BY dayOfWeek ASC, startTime ASC")
    fun getAllEntries(sessionId: String): Flow<List<TimetableEntity>>

    @Query("SELECT timetable.* FROM timetable INNER JOIN subjects ON timetable.subjectId = subjects.id WHERE subjects.sessionId = :sessionId ORDER BY dayOfWeek ASC, startTime ASC")
    suspend fun getAllEntriesOnce(sessionId: String): List<TimetableEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: TimetableEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<TimetableEntity>)

    @Query("DELETE FROM timetable WHERE id = :id")
    suspend fun deleteEntry(id: String)
}

@Dao
interface AttendanceRecordDao {
    @Query("SELECT * FROM attendance_records WHERE date = :date")
    fun getRecordsByDate(date: Long): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE date >= :startMs AND date <= :endMs")
    fun getRecordsInRange(startMs: Long, endMs: Long): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE subjectId = :subjectId ORDER BY date DESC")
    fun getRecordsBySubject(subjectId: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE subjectId IN (:subjectIds)")
    fun getRecordsForSubjects(subjectIds: List<String>): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE subjectId IN (:subjectIds)")
    suspend fun getRecordsForSubjectsOnce(subjectIds: List<String>): List<AttendanceRecordEntity>

    @Query("SELECT * FROM attendance_records WHERE subjectId = :subjectId AND status IN ('DUTY', 'DUTY_ABSENT') ORDER BY date DESC")
    fun getDutyRecordsForSubject(subjectId: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE subjectId = :subjectId AND date = :date AND (slotId = :slotId OR (slotId IS NULL AND :slotId IS NULL)) LIMIT 1")
    suspend fun getRecord(subjectId: String, date: Long, slotId: String?): AttendanceRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecordEntity)

    @Query("UPDATE attendance_records SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("SELECT * FROM attendance_records WHERE subjectId = :subjectId AND date = :date ORDER BY rowid DESC LIMIT 1")
    suspend fun getLatestRecordForSubjectOnDate(subjectId: String, date: Long): AttendanceRecordEntity?

    @Query("SELECT * FROM attendance_records WHERE subjectId = :subjectId ORDER BY date DESC, rowid DESC LIMIT 1")
    suspend fun getLatestRecordForSubject(subjectId: String): AttendanceRecordEntity?

    @Query("DELETE FROM attendance_records WHERE id = :recordId")
    suspend fun deleteRecordById(recordId: String)
}

@Dao
interface TaskDao {
    @Query("SELECT tasks.* FROM tasks INNER JOIN subjects ON tasks.subjectId = subjects.id WHERE type = :type AND subjects.sessionId = :sessionId ORDER BY dueDate ASC")
    fun getTasksByType(type: String, sessionId: String): Flow<List<TaskEntity>>

    @Query("SELECT tasks.* FROM tasks INNER JOIN subjects ON tasks.subjectId = subjects.id WHERE subjects.sessionId = :sessionId ORDER BY dueDate ASC")
    fun getAllTasks(sessionId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND hasReminder = 1 AND dueDate > :now ORDER BY dueDate ASC")
    suspend fun getPendingTasksWithReminders(now: Long): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = NOT isCompleted WHERE id = :id")
    suspend fun toggleComplete(id: String)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: String)

    @Query("SELECT tasks.subjectId FROM tasks INNER JOIN subjects ON tasks.subjectId = subjects.id WHERE subjects.sessionId = :sessionId GROUP BY tasks.subjectId ORDER BY COUNT(tasks.id) DESC LIMIT 1")
    suspend fun getHeaviestWorkloadSubjectId(sessionId: String): String?
}

// ── AppDatabase ───────────────────────────────────────────────────────────────

@Database(
    entities = [
        SessionEntity::class,
        SubjectEntity::class,
        TimetableEntity::class,
        AttendanceRecordEntity::class,
        TaskEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao
    abstract fun subjectDao(): SubjectDao
    abstract fun timetableDao(): TimetableDao
    abstract fun attendanceRecordDao(): AttendanceRecordDao
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private fun reconcileSchema(db: SupportSQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys = OFF;")

            fun tableExists(tableName: String): Boolean {
                val cursor = db.query("SELECT 1 FROM sqlite_master WHERE type='table' AND name='$tableName'")
                val exists = cursor.count > 0
                cursor.close()
                return exists
            }

            fun columnExists(tableName: String, columnName: String): Boolean {
                val cursor = db.query("PRAGMA table_info(`$tableName`)")
                var exists = false
                val nameIdx = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (nameIdx != -1 && cursor.getString(nameIdx).equals(columnName, ignoreCase = true)) {
                        exists = true
                        break
                    }
                }
                cursor.close()
                return exists
            }

            // 1. Reconcile sessions
            if (tableExists("sessions")) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `sessions_new` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `startDate` INTEGER NOT NULL, `isActive` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                val isCompletedCol = if (columnExists("sessions", "isCompleted")) "COALESCE(isCompleted, 0)" else "0"
                val isActiveCol = if (columnExists("sessions", "isActive")) "COALESCE(isActive, 0)" else "0"
                db.execSQL("INSERT OR IGNORE INTO `sessions_new` (`id`, `name`, `startDate`, `isActive`, `isCompleted`) SELECT `id`, `name`, `startDate`, $isActiveCol, $isCompletedCol FROM `sessions`")
                db.execSQL("DROP TABLE `sessions`")
                db.execSQL("ALTER TABLE `sessions_new` RENAME TO `sessions`")
            } else {
                db.execSQL("CREATE TABLE IF NOT EXISTS `sessions` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `startDate` INTEGER NOT NULL, `isActive` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }

            // 2. Reconcile subjects
            if (tableExists("subjects")) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `subjects_new` (`id` TEXT NOT NULL, `sessionId` TEXT NOT NULL, `name` TEXT NOT NULL, `code` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("INSERT OR IGNORE INTO `subjects_new` (`id`, `sessionId`, `name`, `code`) SELECT `id`, `sessionId`, `name`, `code` FROM `subjects`")
                db.execSQL("DROP TABLE `subjects`")
                db.execSQL("ALTER TABLE `subjects_new` RENAME TO `subjects`")
            } else {
                db.execSQL("CREATE TABLE IF NOT EXISTS `subjects` (`id` TEXT NOT NULL, `sessionId` TEXT NOT NULL, `name` TEXT NOT NULL, `code` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
            }
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_subjects_sessionId` ON `subjects` (`sessionId`)")

            // 3. Reconcile timetable
            if (tableExists("timetable")) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `timetable_new` (`id` TEXT NOT NULL, `subjectId` TEXT NOT NULL, `dayOfWeek` INTEGER NOT NULL, `startTime` TEXT NOT NULL, `room` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                val roomCol = if (columnExists("timetable", "room")) "COALESCE(room, '')" else "''"
                db.execSQL("INSERT OR IGNORE INTO `timetable_new` (`id`, `subjectId`, `dayOfWeek`, `startTime`, `room`) SELECT `id`, `subjectId`, `dayOfWeek`, `startTime`, $roomCol FROM `timetable`")
                db.execSQL("DROP TABLE `timetable`")
                db.execSQL("ALTER TABLE `timetable_new` RENAME TO `timetable`")
            } else {
                db.execSQL("CREATE TABLE IF NOT EXISTS `timetable` (`id` TEXT NOT NULL, `subjectId` TEXT NOT NULL, `dayOfWeek` INTEGER NOT NULL, `startTime` TEXT NOT NULL, `room` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
            }
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_timetable_subjectId` ON `timetable` (`subjectId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_timetable_dayOfWeek` ON `timetable` (`dayOfWeek`)")

            // 4. Reconcile attendance_records
            if (tableExists("attendance_records")) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `attendance_records_new` (`id` TEXT NOT NULL, `subjectId` TEXT NOT NULL, `date` INTEGER NOT NULL, `slotId` TEXT, `status` TEXT NOT NULL, `note` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                val slotIdCol = if (columnExists("attendance_records", "slotId")) "`slotId`" else "NULL"
                val noteCol = if (columnExists("attendance_records", "note")) "COALESCE(`note`, '')" else "''"
                val statusCol = if (columnExists("attendance_records", "status")) "COALESCE(`status`, 'NONE')" else "'NONE'"
                db.execSQL("INSERT OR IGNORE INTO `attendance_records_new` (`id`, `subjectId`, `date`, `slotId`, `status`, `note`) SELECT `id`, `subjectId`, `date`, $slotIdCol, $statusCol, $noteCol FROM `attendance_records`")
                db.execSQL("DROP TABLE `attendance_records`")
                db.execSQL("ALTER TABLE `attendance_records_new` RENAME TO `attendance_records`")
            } else {
                db.execSQL("CREATE TABLE IF NOT EXISTS `attendance_records` (`id` TEXT NOT NULL, `subjectId` TEXT NOT NULL, `date` INTEGER NOT NULL, `slotId` TEXT, `status` TEXT NOT NULL, `note` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
            }
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_attendance_records_subjectId` ON `attendance_records` (`subjectId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_attendance_records_date` ON `attendance_records` (`date`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_attendance_records_slotId` ON `attendance_records` (`slotId`)")

            // 5. Reconcile tasks
            if (tableExists("tasks")) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `tasks_new` (`id` TEXT NOT NULL, `subjectId` TEXT NOT NULL, `title` TEXT NOT NULL, `type` TEXT NOT NULL, `dueDate` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL, `hasReminder` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                val isCompletedCol = if (columnExists("tasks", "isCompleted")) "COALESCE(isCompleted, 0)" else "0"
                val hasReminderCol = if (columnExists("tasks", "hasReminder")) "COALESCE(hasReminder, 0)" else "0"
                db.execSQL("INSERT OR IGNORE INTO `tasks_new` (`id`, `subjectId`, `title`, `type`, `dueDate`, `isCompleted`, `hasReminder`) SELECT `id`, `subjectId`, `title`, `type`, `dueDate`, $isCompletedCol, $hasReminderCol FROM `tasks`")
                db.execSQL("DROP TABLE `tasks`")
                db.execSQL("ALTER TABLE `tasks_new` RENAME TO `tasks`")
            } else {
                db.execSQL("CREATE TABLE IF NOT EXISTS `tasks` (`id` TEXT NOT NULL, `subjectId` TEXT NOT NULL, `title` TEXT NOT NULL, `type` TEXT NOT NULL, `dueDate` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL, `hasReminder` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
            }
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_subjectId` ON `tasks` (`subjectId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_type` ON `tasks` (`type`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_dueDate` ON `tasks` (`dueDate`)")

            // 6. Clean up obsolete tables
            db.execSQL("DROP TABLE IF EXISTS monthly_summaries")

            db.execSQL("PRAGMA foreign_keys = ON;")
        }

        val MIGRATION_1_6 = object : Migration(1, 6) {
            override fun migrate(db: SupportSQLiteDatabase) { reconcileSchema(db) }
        }

        val MIGRATION_2_6 = object : Migration(2, 6) {
            override fun migrate(db: SupportSQLiteDatabase) { reconcileSchema(db) }
        }

        val MIGRATION_3_6 = object : Migration(3, 6) {
            override fun migrate(db: SupportSQLiteDatabase) { reconcileSchema(db) }
        }

        val MIGRATION_4_6 = object : Migration(4, 6) {
            override fun migrate(db: SupportSQLiteDatabase) { reconcileSchema(db) }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) { reconcileSchema(db) }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "margin.db"
                )
                .addMigrations(MIGRATION_1_6, MIGRATION_2_6, MIGRATION_3_6, MIGRATION_4_6, MIGRATION_5_6)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        db.execSQL("PRAGMA foreign_keys = ON;")
                    }
                })
                .fallbackToDestructiveMigration()
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
