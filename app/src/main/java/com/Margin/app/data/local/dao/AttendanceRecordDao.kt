package com.attendease.app.data.local.dao

import androidx.room.*
import com.attendease.app.data.local.entity.AttendanceRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceRecordDao {

    /** All records for a given subject, ordered by date descending */
    @Query("SELECT * FROM attendance_records WHERE subjectId = :subjectId ORDER BY date DESC")
    fun getRecordsBySubject(subjectId: String): Flow<List<AttendanceRecordEntity>>

    /** All records for multiple subjects */
    @Query("SELECT * FROM attendance_records WHERE subjectId IN (:subjectIds)")
    fun getRecordsForSubjects(subjectIds: List<String>): Flow<List<AttendanceRecordEntity>>

    /** All records on a specific day (start-of-day millis window) */
    @Query("SELECT * FROM attendance_records WHERE date = :date")
    fun getRecordsByDate(date: Long): Flow<List<AttendanceRecordEntity>>

    /** Single record for a subject on a specific day */
    @Query("SELECT * FROM attendance_records WHERE subjectId = :subjectId AND date = :date LIMIT 1")
    suspend fun getRecord(subjectId: String, date: Long): AttendanceRecordEntity?

    @Query("SELECT * FROM attendance_records WHERE isSynced = 0")
    suspend fun getUnsyncedRecords(): List<AttendanceRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<AttendanceRecordEntity>)

    @Update
    suspend fun updateRecord(record: AttendanceRecordEntity)

    @Query("UPDATE attendance_records SET status = :status, lastModified = :ts, isSynced = 0 WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, ts: Long = System.currentTimeMillis())
}
