package com.Margin.app.data.local

import com.Margin.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

class AttendanceRepository(private val db: AppDatabase) {

    private val sessionDao = db.sessionDao()
    private val subjectDao = db.subjectDao()
    private val attendanceRecordDao = db.attendanceRecordDao()
    private val taskDao = db.taskDao()
    private val timetableDao = db.timetableDao()

    // ── Sessions ─────────────────────────────────────────────────────────────
    val sessionsFlow: Flow<List<SessionEntity>> = sessionDao.getAllSessions()

    suspend fun getActiveSession(): SessionEntity? = sessionDao.getActiveSession()

    suspend fun createSessionWithSubjects(name: String, startDate: Long, subjects: List<Pair<String, String>>) {
        val sessionId = java.util.UUID.randomUUID().toString()
        val entity = SessionEntity(id = sessionId, name = name, startDate = startDate, isActive = false)
        sessionDao.insertSession(entity)
        
        subjects.forEach { (code, subjectName) ->
            subjectDao.insertSubject(SubjectEntity(name = subjectName, code = code, sessionId = sessionId))
        }
    }

    suspend fun setActiveSession(sessionId: String) {
        sessionDao.deactivateAllSessions()
        sessionDao.setActive(sessionId)
    }

    suspend fun deleteSession(sessionId: String) {
        sessionDao.deleteSessionById(sessionId)
    }

    // ── Subjects ─────────────────────────────────────────────────────────────
    fun subjectsFlow(sessionId: String): Flow<List<SubjectEntity>> = subjectDao.getSubjectsBySession(sessionId)

    suspend fun getSubjectsBySessionOnce(sessionId: String): List<SubjectEntity> = subjectDao.getSubjectsBySessionOnce(sessionId)

    suspend fun addSubject(name: String, code: String, sessionId: String) {
        subjectDao.insertSubject(SubjectEntity(name = name, code = code, sessionId = sessionId))
    }

    // ── Attendance ───────────────────────────────────────────────────────────
    fun todayRecordsFlow(date: Long): Flow<List<AttendanceRecordEntity>> = attendanceRecordDao.getRecordsByDate(date)
    
    fun getRecordsBySubject(subjectId: String): Flow<List<AttendanceRecordEntity>> = attendanceRecordDao.getRecordsBySubject(subjectId)
    
    fun getRecordsForSubjects(subjectIds: List<String>): Flow<List<AttendanceRecordEntity>> = attendanceRecordDao.getRecordsForSubjects(subjectIds)

    suspend fun markAttendance(subjectId: String, date: Long, status: String) {
        // Upsert record: if exists, change status; if not, insert new
        val existing = attendanceRecordDao.getRecord(subjectId, date)
        if (existing != null) {
            attendanceRecordDao.updateStatus(existing.id, status)
        } else {
            attendanceRecordDao.insertRecord(AttendanceRecordEntity(subjectId = subjectId, date = date, status = status))
        }
    }

    // ── Tasks ────────────────────────────────────────────────────────────────
    fun tasksFlow(type: String, sessionId: String): Flow<List<TaskEntity>> = taskDao.getTasksByType(type, sessionId)

    fun allTasksFlow(sessionId: String): Flow<List<TaskEntity>> = taskDao.getAllTasks(sessionId)

    suspend fun addTask(title: String, type: String, subjectId: String, dueDate: Long) {
        taskDao.insertTask(TaskEntity(title = title, type = type, subjectId = subjectId, dueDate = dueDate))
    }

    suspend fun toggleTaskComplete(taskId: String) {
        taskDao.toggleComplete(taskId)
    }

    suspend fun deleteTask(taskId: String) {
        taskDao.deleteTask(taskId)
    }

    // ── Timetable ────────────────────────────────────────────────────────────
    fun timetableByDayFlow(dayOfWeek: Int, sessionId: String): Flow<List<TimetableEntity>> = timetableDao.getEntriesByDay(dayOfWeek, sessionId)

    fun fullTimetableFlow(sessionId: String): Flow<List<TimetableEntity>> = timetableDao.getAllEntries(sessionId)
    
    fun getClassesPerDayCount(sessionId: String): Flow<Map<Int, Int>> = timetableDao.getClassesPerDayCount(sessionId)
    
    fun getTimetableForSubject(subjectId: String): Flow<List<TimetableEntity>> = timetableDao.getEntriesBySubject(subjectId)

    suspend fun addTimetableEntry(subjectId: String, dayOfWeek: Int, startTime: String) {
        timetableDao.insertEntry(TimetableEntity(subjectId = subjectId, dayOfWeek = dayOfWeek, startTime = startTime))
    }

    suspend fun addTimetableEntryWithSubject(dayOfWeek: Int, startTime: String, subjectCode: String, subjectName: String, sessionId: String) {
        val subjects = subjectDao.getSubjectsBySessionOnce(sessionId)
        var subjectId = subjects.find { it.code.trim().equals(subjectCode.trim(), ignoreCase = true) }?.id
        
        if (subjectId == null) {
            subjectId = java.util.UUID.randomUUID().toString()
            subjectDao.insertSubject(SubjectEntity(id = subjectId, name = subjectName, code = subjectCode, sessionId = sessionId))
        }
        
        timetableDao.insertEntry(TimetableEntity(subjectId = subjectId, dayOfWeek = dayOfWeek, startTime = startTime))
    }

    suspend fun removeTimetableEntry(entryId: String) {
        timetableDao.deleteEntry(entryId)
    }
}
