package com.Margin.app.data.local.dao

import androidx.room.*
import com.Margin.app.data.local.entity.AttendanceRecordEntity
import com.Margin.app.data.local.entity.MonthlySummaryEntity
import kotlinx.coroutines.flow.Flow

data class SubjectBunkCount(
    val subjectId: String,
    val bunkCount: Int
)

@Dao
interface MonthlySummaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummaries(summaries: List<MonthlySummaryEntity>)

    @Query("SELECT * FROM monthly_summaries WHERE sessionId = :sessionId")
    fun getSummariesForSession(sessionId: String): Flow<List<MonthlySummaryEntity>>

    @Query("SELECT * FROM monthly_summaries WHERE subjectId IN (:subjectIds)")
    fun getSummariesForSubjects(subjectIds: List<String>): Flow<List<MonthlySummaryEntity>>

    @Query("SELECT * FROM monthly_summaries WHERE subjectId IN (:subjectIds)")
    suspend fun getSummariesForSubjectsOnce(subjectIds: List<String>): List<MonthlySummaryEntity>

    /**
     * Returns all distinct locked month strings (e.g. "2025-03") for a given session.
     * Used by the UI to mark months as already locked.
     */
    @Query("SELECT DISTINCT monthYearText FROM monthly_summaries WHERE sessionId = :sessionId")
    fun getLockedMonthsForSession(sessionId: String): Flow<List<String>>

    /**
     * Returns true if at least one summary row exists for this session + month.
     * This is the "soft lock" check — no records are deleted, we just use summaries
     * as a locked-month marker.
     */
    @Query(
        "SELECT COUNT(*) > 0 FROM monthly_summaries " +
        "WHERE sessionId = :sessionId AND monthYearText = :monthYearText"
    )
    suspend fun isMonthLocked(sessionId: String, monthYearText: String): Boolean

    /** Total bunks compressed into summaries for given subjects */
    @Query("SELECT COALESCE(SUM(bunkCount), 0) FROM monthly_summaries WHERE subjectId IN (:subjectIds)")
    fun getTotalBunksFromSummaries(subjectIds: List<String>): Flow<Int>

    /** Wall of Shame: subject with most bunks in summaries */
    @Query("""
        SELECT subjectId, SUM(bunkCount) AS bunkCount 
        FROM monthly_summaries 
        WHERE subjectId IN (:subjectIds) 
        GROUP BY subjectId 
        ORDER BY bunkCount DESC 
        LIMIT 1
    """)
    fun getMostBunkedSubjectFromSummaries(subjectIds: List<String>): Flow<SubjectBunkCount?>

    @Query("DELETE FROM monthly_summaries WHERE sessionId = :sessionId AND monthYearText = :monthYearText")
    suspend fun deleteSummariesForMonth(sessionId: String, monthYearText: String)

    /**
     * Atomic Soft Lock transaction:
     *  1. Insert the pre-computed per-subject summaries.
     *  2. Raw attendance_records are NOT deleted — this is a soft lock.
     *     The summary's existence is used as the locked-month marker.
     *
     * Using @Transaction on a DAO suspend function is the correct way to
     * perform multi-step Room operations atomically — avoids the
     * runInTransaction { runBlocking {} } deadlock pattern.
     */
    @Transaction
    suspend fun softLockMonth(summaries: List<MonthlySummaryEntity>) {
        insertSummaries(summaries)
    }
}
