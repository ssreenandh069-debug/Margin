package com.Margin.app.data.local

import com.Margin.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class AttendanceRepository(private val db: AppDatabase) {

    private val sessionDao          = db.sessionDao()
    private val subjectDao          = db.subjectDao()
    private val attendanceRecordDao = db.attendanceRecordDao()
    private val taskDao             = db.taskDao()
    private val timetableDao        = db.timetableDao()
    private val summaryDao          = db.monthlySummaryDao()

    // ── Sessions ─────────────────────────────────────────────────────────────
    val sessionsFlow: Flow<List<SessionEntity>> = sessionDao.getAllSessions()

    suspend fun getActiveSession(): SessionEntity? = sessionDao.getActiveSession()

    suspend fun createSessionWithSubjects(name: String, startDate: Long, subjects: List<Pair<String, String>>): String {
        val sessionId = java.util.UUID.randomUUID().toString()
        val entity = SessionEntity(id = sessionId, name = name, startDate = startDate, isActive = false)
        sessionDao.insertSession(entity)
        subjects.forEach { (code, subjectName) ->
            subjectDao.insertSubject(SubjectEntity(name = subjectName, code = code, sessionId = sessionId))
        }
        return sessionId
    }

    suspend fun setActiveSession(sessionId: String) {
        sessionDao.deactivateAllSessions()
        sessionDao.setActive(sessionId)
    }

    suspend fun endSession(sessionId: String) {
        sessionDao.setCompleted(sessionId)
        // Vacuum after archiving to reclaim WAL space
        db.vacuumAndCheckpoint()
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
    fun todayRecordsFlow(date: Long): Flow<List<AttendanceRecordEntity>> =
        attendanceRecordDao.getRecordsByDate(date)

    fun getRecordsBySubject(subjectId: String): Flow<List<AttendanceRecordEntity>> =
        attendanceRecordDao.getRecordsBySubject(subjectId)

    fun getRecordsForSubjects(subjectIds: List<String>): Flow<List<AttendanceRecordEntity>> =
        attendanceRecordDao.getRecordsForSubjects(subjectIds)

    suspend fun markAttendance(subjectId: String, date: Long, status: String) {
        val existing = attendanceRecordDao.getRecord(subjectId, date)
        if (existing != null) {
            attendanceRecordDao.updateStatus(existing.id, status)
        } else {
            attendanceRecordDao.insertRecord(
                AttendanceRecordEntity(subjectId = subjectId, date = date, status = status)
            )
        }
    }

    /** Flow of all DUTY / DUTY_ABSENT records for a subject, for the ExtraDutySheet. */
    fun dutyRecordsFlow(subjectId: String): Flow<List<AttendanceRecordEntity>> =
        attendanceRecordDao.getDutyRecordsForSubject(subjectId)

    /**
     * Logs a DUTY or DUTY_ABSENT record for any date.
     * Uses INSERT (not upsert) so multiple duty entries on different dates are independent.
     * Each gets a unique UUID — the date uniqueness constraint used by markAttendance
     * does NOT apply here (duty records are bonus entries, not day-markers).
     */
    suspend fun addDutyRecord(subjectId: String, date: Long, status: String) {
        attendanceRecordDao.insertRecord(
            AttendanceRecordEntity(
                subjectId = subjectId,
                date      = date,
                status    = status
            )
        )
    }

    /** Hard-delete a single attendance record by ID (used to remove a duty entry). */
    suspend fun deleteDutyRecord(recordId: String) {
        attendanceRecordDao.deleteRecordById(recordId)
    }

    // ── Lock Month (Soft Lock — raw records are preserved) ───────────────────
    /**
     * Aggregates all raw AttendanceRecords for [monthYearText] (e.g. "2025-03") into
     * MonthlySummaryEntity rows per subject.
     *
     * SOFT LOCK design: raw records are NOT deleted. The presence of a summary row
     * for a given month acts as the "locked" marker, making that month read-only in
     * the UI. This allows an "Unlock" feature to be added later by simply deleting
     * the summary rows.
     *
     * Uses @Transaction on the DAO suspend function — this is the correct Room pattern
     * and avoids the runInTransaction { runBlocking {} } deadlock that caused crashes.
     *
     * Status math:
     *   attended = PRESENT | PROXY
     *   total    = PRESENT | PROXY | BUNK | EXCUSED
     *   bunks    = BUNK
     *   proxies  = PROXY
     */
    suspend fun lockMonth(sessionId: String, subjectIds: List<String>, monthYearText: String) {
        val (startMs, endMs) = monthBoundsFor(monthYearText)

        // 1. Fetch raw records for this month (one-shot suspend query — safe outside transactions)
        val records = attendanceRecordDao.getRecordsInRange(subjectIds, startMs, endMs)

        if (records.isEmpty()) return

        // 2. Aggregate per subject
        val summaries = subjectIds.mapNotNull { subjectId ->
            val subjectRecords = records.filter { it.subjectId == subjectId }
            if (subjectRecords.isEmpty()) return@mapNotNull null

            val attended = subjectRecords.count { it.status == "PRESENT" || it.status == "PROXY" }
            val total    = subjectRecords.count {
                it.status == "PRESENT" || it.status == "PROXY" ||
                it.status == "BUNK"    || it.status == "EXCUSED"
            }
            val bunks   = subjectRecords.count { it.status == "BUNK" }
            val proxies = subjectRecords.count { it.status == "PROXY" }

            MonthlySummaryEntity(
                sessionId     = sessionId,
                subjectId     = subjectId,
                monthYearText = monthYearText,
                attendedCount = attended,
                totalCount    = total,
                bunkCount     = bunks,
                proxyCount    = proxies
            )
        }

        // 3. Atomically insert summaries via @Transaction DAO function.
        //    Raw records are intentionally kept (Soft Lock).
        summaryDao.softLockMonth(summaries)
    }

    fun getMonthlySummariesForSubjects(subjectIds: List<String>): Flow<List<MonthlySummaryEntity>> =
        summaryDao.getSummariesForSubjects(subjectIds)

    /** Emits the set of locked month strings (e.g. ["2025-03", "2025-04"]) for a session. */
    fun lockedMonthsFlow(sessionId: String): Flow<List<String>> =
        summaryDao.getLockedMonthsForSession(sessionId)

    /** One-shot check: is this specific month already locked? */
    suspend fun isMonthLocked(sessionId: String, monthYearText: String): Boolean =
        summaryDao.isMonthLocked(sessionId, monthYearText)

    // ── Analytics (zero-storage queries) ─────────────────────────────────────
    fun totalBunksFromSummaries(subjectIds: List<String>): Flow<Int> =
        summaryDao.getTotalBunksFromSummaries(subjectIds)

    fun mostBunkedSubjectFromSummaries(subjectIds: List<String>) =
        summaryDao.getMostBunkedSubjectFromSummaries(subjectIds)

    suspend fun heaviestWorkloadSubjectId(sessionId: String): String? =
        taskDao.getHeaviestWorkloadSubjectId(sessionId)

    // ── Tasks ────────────────────────────────────────────────────────────────
    fun tasksFlow(type: String, sessionId: String): Flow<List<TaskEntity>> =
        taskDao.getTasksByType(type, sessionId)

    fun allTasksFlow(sessionId: String): Flow<List<TaskEntity>> =
        taskDao.getAllTasks(sessionId)

    suspend fun addTask(title: String, type: String, subjectId: String, dueDate: Long) {
        taskDao.insertTask(TaskEntity(title = title, type = type, subjectId = subjectId, dueDate = dueDate))
    }

    suspend fun toggleTaskComplete(taskId: String) { taskDao.toggleComplete(taskId) }
    suspend fun deleteTask(taskId: String) { taskDao.deleteTask(taskId) }

    // ── Timetable ────────────────────────────────────────────────────────────
    fun timetableByDayFlow(dayOfWeek: Int, sessionId: String): Flow<List<TimetableEntity>> =
        timetableDao.getEntriesByDay(dayOfWeek, sessionId)

    fun fullTimetableFlow(sessionId: String): Flow<List<TimetableEntity>> =
        timetableDao.getAllEntries(sessionId)

    fun getClassesPerDayCount(sessionId: String): Flow<Map<Int, Int>> =
        timetableDao.getClassesPerDayCount(sessionId)

    fun getTimetableForSubject(subjectId: String): Flow<List<TimetableEntity>> =
        timetableDao.getEntriesBySubject(subjectId)

    suspend fun addTimetableEntry(subjectId: String, dayOfWeek: Int, startTime: String) {
        timetableDao.insertEntry(TimetableEntity(subjectId = subjectId, dayOfWeek = dayOfWeek, startTime = startTime))
    }

    suspend fun addTimetableEntryWithSubject(
        dayOfWeek: Int, startTime: String,
        subjectCode: String, subjectName: String, sessionId: String
    ) {
        val subjects   = subjectDao.getSubjectsBySessionOnce(sessionId)
        var subjectId  = subjects.find { it.code.trim().equals(subjectCode.trim(), ignoreCase = true) }?.id
        if (subjectId == null) {
            subjectId = java.util.UUID.randomUUID().toString()
            subjectDao.insertSubject(SubjectEntity(id = subjectId, name = subjectName, code = subjectCode, sessionId = sessionId))
        }
        timetableDao.insertEntry(TimetableEntity(subjectId = subjectId, dayOfWeek = dayOfWeek, startTime = startTime))
    }

    suspend fun removeTimetableEntry(entryId: String) { timetableDao.deleteEntry(entryId) }

    /** One-shot fetch of the full timetable for a session — used for export. */
    suspend fun getAllTimetableEntriesOnce(sessionId: String): List<TimetableEntity> =
        timetableDao.getAllEntriesOnce(sessionId)

    /**
     * Imports a timetable into [sessionId] from pre-parsed data.
     * For each (code, name) pair in [subjects], creates/reuses a SubjectEntity.
     * Then inserts all [slots] linked to those subjects.
     * Existing entries for the same session are NOT cleared first — duplicates
     * are handled by OnConflictStrategy.REPLACE on the primary key, which is
     * a UUID, so each import always produces fresh entries (no silent deduplication).
     */
    suspend fun importTimetable(
        sessionId: String,
        subjects: List<Pair<String, String>>,         // (code, name)
        slots: List<Triple<String, Int, String>>      // (subjectCode, dayOfWeek, time)
    ) {
        // 1. Build a code → subjectId map (reuse existing subjects where code matches)
        val existingSubjects = subjectDao.getSubjectsBySessionOnce(sessionId)
        val codeToId = existingSubjects.associate { it.code.trim().lowercase() to it.id }.toMutableMap()

        subjects.forEach { (code, name) ->
            val key = code.trim().lowercase()
            if (key !in codeToId) {
                val newId = java.util.UUID.randomUUID().toString()
                subjectDao.insertSubject(SubjectEntity(id = newId, name = name, code = code, sessionId = sessionId))
                codeToId[key] = newId
            }
        }

        // 2. Insert timetable slots linked to the resolved subject IDs
        val entities = slots.mapNotNull { (code, day, time) ->
            val subjectId = codeToId[code.trim().lowercase()] ?: return@mapNotNull null
            TimetableEntity(subjectId = subjectId, dayOfWeek = day, startTime = time)
        }
        timetableDao.insertEntries(entities)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    /** Returns (startOfMonthMs, endOfMonthMs) for a "YYYY-MM" string. */
    fun monthBoundsFor(monthYearText: String): Pair<Long, Long> {
        val parts = monthYearText.split("-")
        val year  = parts[0].toInt()
        val month = parts[1].toInt() - 1  // Calendar months are 0-indexed

        val cal = Calendar.getInstance()
        cal.set(year, month, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        return Pair(start, end)
    }
}
