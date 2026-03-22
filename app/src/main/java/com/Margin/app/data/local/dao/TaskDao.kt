package com.Margin.app.data.local.dao

import androidx.room.*
import com.Margin.app.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT tasks.* FROM tasks INNER JOIN subjects ON tasks.subjectId = subjects.id WHERE type = :type AND subjects.sessionId = :sessionId ORDER BY dueDate ASC")
    fun getTasksByType(type: String, sessionId: String): Flow<List<TaskEntity>>

    @Query("SELECT tasks.* FROM tasks INNER JOIN subjects ON tasks.subjectId = subjects.id WHERE subjects.sessionId = :sessionId ORDER BY dueDate ASC")
    fun getAllTasks(sessionId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isSynced = 0")
    suspend fun getUnsyncedTasks(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = NOT isCompleted, lastModified = :ts, isSynced = 0 WHERE id = :id")
    suspend fun toggleComplete(id: String, ts: Long = System.currentTimeMillis())

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: String)

    @Update
    suspend fun updateTask(task: TaskEntity)
}
