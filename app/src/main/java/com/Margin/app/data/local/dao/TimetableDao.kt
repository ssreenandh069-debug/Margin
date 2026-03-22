package com.Margin.app.data.local.dao

import androidx.room.*
import com.Margin.app.data.local.entity.TimetableEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableDao {

    @Query("SELECT timetable.* FROM timetable INNER JOIN subjects ON timetable.subjectId = subjects.id WHERE dayOfWeek = :day AND subjects.sessionId = :sessionId ORDER BY startTime ASC")
    fun getEntriesByDay(day: Int, sessionId: String): Flow<List<TimetableEntity>>

    @Query("SELECT timetable.* FROM timetable INNER JOIN subjects ON timetable.subjectId = subjects.id WHERE timetable.subjectId = :subjectId ORDER BY dayOfWeek ASC")
    fun getEntriesBySubject(subjectId: String): Flow<List<TimetableEntity>>

    @MapInfo(keyColumn = "dayOfWeek", valueColumn = "count")
    @Query("SELECT dayOfWeek, COUNT(timetable.id) as count FROM timetable INNER JOIN subjects ON timetable.subjectId = subjects.id WHERE subjects.sessionId = :sessionId GROUP BY dayOfWeek")
    fun getClassesPerDayCount(sessionId: String): Flow<Map<Int, Int>>

    /** All entries for all days — used to compute week overview counts */
    @Query("SELECT timetable.* FROM timetable INNER JOIN subjects ON timetable.subjectId = subjects.id WHERE subjects.sessionId = :sessionId ORDER BY dayOfWeek ASC, startTime ASC")
    fun getAllEntries(sessionId: String): Flow<List<TimetableEntity>>

    @Query("SELECT * FROM timetable WHERE isSynced = 0")
    suspend fun getUnsyncedEntries(): List<TimetableEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: TimetableEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<TimetableEntity>)

    @Query("DELETE FROM timetable WHERE id = :id")
    suspend fun deleteEntry(id: String)
}
