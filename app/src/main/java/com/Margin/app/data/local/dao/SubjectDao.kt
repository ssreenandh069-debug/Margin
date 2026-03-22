package com.attendease.app.data.local.dao

import androidx.room.*
import com.attendease.app.data.local.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {

    @Query("SELECT * FROM subjects WHERE sessionId = :sessionId ORDER BY code ASC")
    fun getSubjectsBySession(sessionId: String): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE sessionId = :sessionId ORDER BY code ASC")
    suspend fun getSubjectsBySessionOnce(sessionId: String): List<SubjectEntity>

    @Query("SELECT * FROM subjects WHERE isSynced = 0")
    suspend fun getUnsyncedSubjects(): List<SubjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<SubjectEntity>)

    @Delete
    suspend fun deleteSubject(subject: SubjectEntity)

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getSubjectById(id: String): SubjectEntity?
}
