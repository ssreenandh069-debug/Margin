package com.Margin.app.data.local

import com.Margin.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class AttendanceRepository(private val db: AppDatabase) {

    private val sessionDao = db.sessionDao()
    private val subjectDao = db.subjectDao()
    private val timetableDao = db.timetableDao()
    private val attendanceDao = db.attendanceRecordDao()
    private val taskDao = db.taskDao()

    // ── Sessions ─────────────────────────────────────────────────────────────
    val sessionsFlow: Flow<List<SessionEntity>> = sessionDao.getAllSessions()

    suspend fun getActiveSession(): SessionEntity? = sessionDao.getActiveSession()

    suspend fun createSessionWithSubjects(
        name: String,
        startDate: Long,
        subjects: List<Pair<String, String>>,
        activate: Boolean = true
    ): String {
        val sessionId = UUID.randomUUID().toString()
        if (activate) {
            sessionDao.deactivateAllSessions()
        }
        val entity = SessionEntity(
            id = sessionId,
            name = name,
            startDate = startDate,
            isActive = activate,
            isCompleted = false
        )
        sessionDao.insertSession(entity)
        val subjectEntities = subjects.map { (code, subjectName) ->
            SubjectEntity(sessionId = sessionId, name = subjectName, code = code)
        }
        subjectDao.insertSubjects(subjectEntities)
        return sessionId
    }

    suspend fun setActiveSession(sessionId: String) {
        sessionDao.deactivateAllSessions()
        sessionDao.setActive(sessionId)
    }

    suspend fun endSession(sessionId: String) {
        sessionDao.setCompleted(sessionId)
    }

    suspend fun deleteSession(sessionId: String) {
        sessionDao.deleteSessionById(sessionId)
    }

    // ── Subjects ─────────────────────────────────────────────────────────────
    fun subjectsFlow(sessionId: String): Flow<List<SubjectEntity>> = subjectDao.getSubjectsBySession(sessionId)

    suspend fun getSubjectsBySessionOnce(sessionId: String): List<SubjectEntity> =
        subjectDao.getSubjectsBySessionOnce(sessionId)

    suspend fun addSubject(name: String, code: String, sessionId: String) {
        subjectDao.insertSubject(SubjectEntity(name = name, code = code, sessionId = sessionId))
    }

    // ── Attendance ───────────────────────────────────────────────────────────
    fun recordsForDateFlow(date: Long): Flow<List<AttendanceRecordEntity>> =
        attendanceDao.getRecordsByDate(date)

    fun recordsForSubjectFlow(subjectId: String): Flow<List<AttendanceRecordEntity>> =
        attendanceDao.getRecordsBySubject(subjectId)

    fun recordsForSubjectsFlow(subjectIds: List<String>): Flow<List<AttendanceRecordEntity>> =
        attendanceDao.getRecordsForSubjects(subjectIds)

    suspend fun recordsForSubjectsOnce(subjectIds: List<String>): List<AttendanceRecordEntity> =
        attendanceDao.getRecordsForSubjectsOnce(subjectIds)

    suspend fun markAttendance(subjectId: String, slotId: String?, date: Long, status: String, note: String = "") {
        val existing = attendanceDao.getRecord(subjectId, date, slotId)
        if (existing != null) {
            if (status == "NONE") {
                attendanceDao.deleteRecordById(existing.id)
            } else {
                attendanceDao.insertRecord(
                    existing.copy(status = status, note = if (note.isNotBlank()) note else existing.note)
                )
            }
        } else {
            if (status != "NONE") {
                attendanceDao.insertRecord(
                    AttendanceRecordEntity(subjectId = subjectId, date = date, slotId = slotId, status = status, note = note)
                )
            }
        }
    }

    suspend fun incrementPresent(subjectId: String, date: Long) {
        attendanceDao.insertRecord(
            AttendanceRecordEntity(subjectId = subjectId, date = date, slotId = null, status = "PRESENT")
        )
    }

    suspend fun incrementBunk(subjectId: String, date: Long) {
        attendanceDao.insertRecord(
            AttendanceRecordEntity(subjectId = subjectId, date = date, slotId = null, status = "BUNK")
        )
    }

    suspend fun decrementAttendance(subjectId: String) {
        val latest = attendanceDao.getLatestRecordForSubject(subjectId)
        if (latest != null) {
            attendanceDao.deleteRecordById(latest.id)
        }
    }

    suspend fun swapClassSubject(
        oldSubjectId: String,
        newSubjectId: String,
        slotId: String,
        date: Long,
        status: String,
        note: String
    ) {
        val existingOld = attendanceDao.getRecord(oldSubjectId, date, slotId)
        if (existingOld != null) {
            attendanceDao.deleteRecordById(existingOld.id)
        }
        val existingNew = attendanceDao.getRecord(newSubjectId, date, slotId)
        if (existingNew != null) {
            attendanceDao.insertRecord(existingNew.copy(status = status, note = note))
        } else {
            attendanceDao.insertRecord(
                AttendanceRecordEntity(
                    subjectId = newSubjectId,
                    slotId = slotId,
                    date = date,
                    status = status,
                    note = note
                )
            )
        }
    }

    fun dutyRecordsFlow(subjectId: String): Flow<List<AttendanceRecordEntity>> =
        attendanceDao.getDutyRecordsForSubject(subjectId)

    suspend fun addDutyRecord(subjectId: String, date: Long, status: String) {
        attendanceDao.insertRecord(
            AttendanceRecordEntity(subjectId = subjectId, date = date, slotId = null, status = status)
        )
    }

    suspend fun deleteRecord(recordId: String) {
        attendanceDao.deleteRecordById(recordId)
    }

    // ── Timetable ────────────────────────────────────────────────────────────
    fun timetableByDayFlow(dayOfWeek: Int, sessionId: String): Flow<List<TimetableEntity>> =
        timetableDao.getEntriesByDay(dayOfWeek, sessionId)

    fun fullTimetableFlow(sessionId: String): Flow<List<TimetableEntity>> =
        timetableDao.getAllEntries(sessionId)

    suspend fun getAllTimetableEntriesOnce(sessionId: String): List<TimetableEntity> =
        timetableDao.getAllEntriesOnce(sessionId)

    suspend fun addTimetableEntryWithSubject(
        dayOfWeek: Int,
        startTime: String,
        room: String,
        subjectCode: String,
        subjectName: String,
        sessionId: String
    ) {
        val subjects = subjectDao.getSubjectsBySessionOnce(sessionId)
        var subjectId = subjects.find { it.code.trim().equals(subjectCode.trim(), ignoreCase = true) }?.id
        if (subjectId == null) {
            subjectId = UUID.randomUUID().toString()
            subjectDao.insertSubject(SubjectEntity(id = subjectId, name = subjectName, code = subjectCode, sessionId = sessionId))
        }
        timetableDao.insertEntry(
            TimetableEntity(subjectId = subjectId, dayOfWeek = dayOfWeek, startTime = startTime, room = room)
        )
    }

    suspend fun removeTimetableEntry(entryId: String) {
        timetableDao.deleteEntry(entryId)
    }

    suspend fun importTimetable(
        sessionId: String,
        subjects: List<Pair<String, String>>,
        slots: List<Triple<String, Int, String>>
    ) {
        val existingSubjects = subjectDao.getSubjectsBySessionOnce(sessionId)
        val codeToId = existingSubjects.associate { it.code.trim().lowercase() to it.id }.toMutableMap()

        subjects.forEach { (code, name) ->
            val key = code.trim().lowercase()
            if (key !in codeToId) {
                val newId = UUID.randomUUID().toString()
                subjectDao.insertSubject(SubjectEntity(id = newId, name = name, code = code, sessionId = sessionId))
                codeToId[key] = newId
            }
        }

        val entities = slots.mapNotNull { (code, day, time) ->
            val subjectId = codeToId[code.trim().lowercase()] ?: return@mapNotNull null
            TimetableEntity(subjectId = subjectId, dayOfWeek = day, startTime = time)
        }
        timetableDao.insertEntries(entities)
    }

    // ── Tasks ────────────────────────────────────────────────────────────────
    fun tasksFlow(type: String, sessionId: String): Flow<List<TaskEntity>> =
        taskDao.getTasksByType(type, sessionId)

    fun allTasksFlow(sessionId: String): Flow<List<TaskEntity>> =
        taskDao.getAllTasks(sessionId)

    suspend fun addTask(
        title: String,
        type: String,
        subjectId: String,
        dueDate: Long,
        hasReminder: Boolean
    ): TaskEntity {
        val entity = TaskEntity(
            title = title,
            type = type,
            subjectId = subjectId,
            dueDate = dueDate,
            hasReminder = hasReminder
        )
        taskDao.insertTask(entity)
        return entity
    }

    suspend fun toggleTaskComplete(taskId: String) {
        taskDao.toggleComplete(taskId)
    }

    suspend fun deleteTask(taskId: String) {
        taskDao.deleteTask(taskId)
    }

    suspend fun heaviestWorkloadSubjectId(sessionId: String): String? =
        taskDao.getHeaviestWorkloadSubjectId(sessionId)

    suspend fun getPendingTasksWithReminders(): List<TaskEntity> =
        taskDao.getPendingTasksWithReminders()
}
