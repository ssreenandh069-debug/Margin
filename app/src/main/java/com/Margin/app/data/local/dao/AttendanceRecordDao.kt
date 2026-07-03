package com.Margin.app.data.local.dao

import androidx.room.*
import com.Margin.app.data.local.entity.AttendanceRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceRecordDao {

    /** All records for a given subject, ordered by date descending */
    @Query("SELECT * FROM attendance_records WHERE subjectId = :subjectId ORDER BY date DESC")
    fun getRecordsBySubject(subjectId: String): Flow<List<AttendanceRecordEntity>>

    /**
     * All DUTY and DUTY_ABSENT records for a subject, newest first.
     * Used by ExtraDutySheet to list and delete extra attendance entries.
     */
    @Query("""
        SELECT * FROM attendance_records 
        WHERE subjectId = :subjectId 
          AND status IN ('DUTY', 'DUTY_ABSENT') 
        ORDER BY date DESC
    """)
    fun getDutyRecordsForSubject(subjectId: String): Flow<List<AttendanceRecordEntity>>

    /** Delete a single record by its primary key */
    @Query("DELETE FROM attendance_records WHERE id = :recordId")
    suspend fun deleteRecordById(recordId: String)

    /** All records for multiple subjects */
    @Query("SELECT * FROM attendance_records WHERE subjectId IN (:subjectIds)")
    fun getRecordsForSubjects(subjectIds: List<String>): Flow<List<AttendanceRecordEntity>>

    /** One-shot suspend version for use in Workers / transactions */
    @Query("SELECT * FROM attendance_records WHERE subjectId IN (:subjectIds)")
    suspend fun getRecordsForSubjectsOnce(subjectIds: List<String>): List<AttendanceRecordEntity>

    /** Records in a date range for a list of subjects — used by Lock Month */
    @Query("SELECT * FROM attendance_records WHERE subjectId IN (:subjectIds) AND date >= :startMs AND date <= :endMs")
    suspend fun getRecordsInRange(subjectIds: List<String>, startMs: Long, endMs: Long): List<AttendanceRecordEntity>

    /** Delete all raw records in a date range for given subjects — Lock Month compression */
    @Query("DELETE FROM attendance_records WHERE subjectId IN (:subjectIds) AND date >= :startMs AND date <= :endMs")
    suspend fun deleteRecordsInRange(subjectIds: List<String>, startMs: Long, endMs: Long)

    /** All records on a specific day (exact midnight timestamp) */
    @Query("SELECT * FROM attendance_records WHERE date = :date")
    fun getRecordsByDate(date: Long): Flow<List<AttendanceRecordEntity>>

    /** Single record for a subject on a specific day */
    @Query("SELECT * FROM attendance_records WHERE subjectId = :subjectId AND date = :date LIMIT 1")
    suspend fun getRecord(subjectId: String, date: Long): AttendanceRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<AttendanceRecordEntity>)

    @Update
    suspend fun updateRecord(record: AttendanceRecordEntity)

    @Query("UPDATE attendance_records SET status = :status, lastModified = :ts, isSynced = 0 WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, ts: Long = System.currentTimeMillis())
}
