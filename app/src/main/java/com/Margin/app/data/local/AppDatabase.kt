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

    @Query("SELECT * FROM attendance_records WHERE subjectId = :subjectId ORDER BY date DESC LIMIT 1")
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
    suspend fun getPendingTasksWithReminders(now: Long = System.currentTimeMillis()): List<TaskEntity>

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
    version = 5,
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

        val MIGRATION_1_5 = object : Migration(1, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE timetable ADD COLUMN room TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE attendance_records ADD COLUMN slotId TEXT")
                db.execSQL("ALTER TABLE attendance_records ADD COLUMN note TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE tasks ADD COLUMN hasReminder INTEGER NOT NULL DEFAULT 0")
                db.execSQL("DROP TABLE IF EXISTS monthly_summaries")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE attendance_records ADD COLUMN note TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "margin.db"
                )
                .addMigrations(MIGRATION_1_5, MIGRATION_4_5)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
