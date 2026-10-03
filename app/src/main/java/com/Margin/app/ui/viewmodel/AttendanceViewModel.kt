package com.Margin.app.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Margin.app.data.local.AttendanceRepository
import com.Margin.app.data.local.UserPreferencesRepository
import com.Margin.app.data.local.entity.SessionEntity
import com.Margin.app.data.local.entity.SubjectEntity
import com.Margin.app.data.local.entity.TimetableEntity
import com.Margin.app.domain.AttendanceMath
import com.Margin.app.utils.TimetableExportManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class TodayClassUi(
    val timetableId: String,
    val subjectId: String,
    val subjectCode: String,
    val subjectName: String,
    val time: String,
    val room: String,
    val status: String,
    val attendancePct: Float,
    val attended: Int,
    val total: Int,
    val safeBunks: Int,
    val neededToRecover: Int,
    val note: String = ""
)

data class SubjectOverviewUi(
    val id: String,
    val code: String,
    val name: String,
    val attended: Int,
    val total: Int,
    val percentage: Float,
    val safeBunks: Int,
    val neededToRecover: Int
)

data class SemesterAnalyticsUi(
    val overallPercentage: Float = 0f,
    val totalAttended: Int = 0,
    val totalClasses: Int = 0,
    val totalBunks: Int = 0,
    val safeBunksLeft: Int = 0,
    val classesNeeded: Int = 0,
    val mostBunkedSubjectName: String? = null,
    val heaviestWorkloadSubjectName: String? = null
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AttendanceViewModel(
    private val repository: AttendanceRepository,
    private val userPrefs: UserPreferencesRepository
) : ViewModel() {

    // ── Time Machine Selected Date ────────────────────────────────────────────
    private val _selectedDate = MutableStateFlow(todayMidnight())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    fun selectDate(dateMs: Long) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = dateMs
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
        }
        _selectedDate.value = cal.timeInMillis
    }

    fun resetToToday() { _selectedDate.value = todayMidnight() }

    // ── Active Session ────────────────────────────────────────────────────────
    val allSessions: StateFlow<List<SessionEntity>> = repository.sessionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSession: StateFlow<SessionEntity?> = allSessions.map { sessions ->
        sessions.firstOrNull { it.isActive }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeSessionId: StateFlow<String?> = activeSession.map { it?.id }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ── Subjects ─────────────────────────────────────────────────────────────
    val activeSubjects: StateFlow<List<SubjectEntity>> = activeSessionId.flatMapLatest { sessionId ->
        if (sessionId == null) flowOf(emptyList()) else repository.subjectsFlow(sessionId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Timetable & Today's Schedule ──────────────────────────────────────────
    private val selectedDayOfWeek: Flow<Int> = _selectedDate.map { dateMs ->
        val calDay = Calendar.getInstance().apply { timeInMillis = dateMs }.get(Calendar.DAY_OF_WEEK)
        if (calDay == Calendar.SUNDAY) 7 else calDay - 1 // 1=Mon … 7=Sun
    }

    private val todayTimetableFlow = combine(activeSessionId, selectedDayOfWeek) { sessionId, dow ->
        Pair(sessionId, dow)
    }.flatMapLatest { (sessionId, dow) ->
        if (sessionId == null) flowOf(emptyList())
        else repository.timetableByDayFlow(dow, sessionId)
    }

    private val selectedDateRecordsFlow = _selectedDate.flatMapLatest { date ->
        repository.recordsForDateFlow(date)
    }

    private val allRecordsFlow = activeSubjects.flatMapLatest { subjects ->
        if (subjects.isEmpty()) flowOf(emptyList())
        else repository.recordsForSubjectsFlow(subjects.map { it.id })
    }

    val todayClasses: StateFlow<List<TodayClassUi>> = combine(
        todayTimetableFlow,
        activeSubjects,
        selectedDateRecordsFlow,
        allRecordsFlow
    ) { timetable, subjects, dateRecords, allRecords ->
        val sortedTimetable = timetable.sortedBy { AttendanceMath.parseTimeToMinutes(it.startTime) }
        sortedTimetable.map { entry ->
            val record = dateRecords.find { (it.slotId == entry.id) || (it.slotId == null && it.subjectId == entry.subjectId) }
            val status = record?.status ?: "NONE"
            val note = record?.note ?: ""

            val effectiveSubjectId = record?.subjectId ?: entry.subjectId
            val subject = subjects.find { it.id == effectiveSubjectId } ?: subjects.find { it.id == entry.subjectId }
            val code = subject?.code ?: "SUB"
            val name = subject?.name ?: "Subject"

            val subjectRecords = allRecords.filter { it.subjectId == effectiveSubjectId }
            val attended = subjectRecords.count {
                it.status == "PRESENT" || it.status == "PROXY" || it.status == "DUTY"
            }
            val total = subjectRecords.count {
                it.status == "PRESENT" || it.status == "PROXY" || it.status == "DUTY" ||
                it.status == "BUNK"    || it.status == "EXCUSED" || it.status == "DUTY_ABSENT"
            }

            val pct = AttendanceMath.calculatePercentage(attended, total)
            val safeBunks = AttendanceMath.calculateSafeBunks(attended, total)
            val needed = AttendanceMath.calculateClassesNeeded(attended, total)

            TodayClassUi(
                timetableId = entry.id,
                subjectId = effectiveSubjectId,
                subjectCode = code,
                subjectName = name,
                time = entry.startTime,
                room = entry.room.ifBlank { "TBD" },
                status = status,
                attendancePct = pct,
                attended = attended,
                total = total,
                safeBunks = safeBunks,
                neededToRecover = needed,
                note = note
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Subject Overviews (for Dashboard / Semester Hub) ──────────────────────
    val subjectOverviews: StateFlow<List<SubjectOverviewUi>> = combine(
        activeSubjects,
        allRecordsFlow
    ) { subjects, records ->
        subjects.map { subject ->
            val subRecs = records.filter { it.subjectId == subject.id }
            val attended = subRecs.count {
                it.status == "PRESENT" || it.status == "PROXY" || it.status == "DUTY"
            }
            val total = subRecs.count {
                it.status == "PRESENT" || it.status == "PROXY" || it.status == "DUTY" ||
                it.status == "BUNK"    || it.status == "EXCUSED" || it.status == "DUTY_ABSENT"
            }
            val pct = AttendanceMath.calculatePercentage(attended, total)
            val safeBunks = AttendanceMath.calculateSafeBunks(attended, total)
            val needed = AttendanceMath.calculateClassesNeeded(attended, total)

            SubjectOverviewUi(
                id = subject.id,
                code = subject.code,
                name = subject.name,
                attended = attended,
                total = total,
                percentage = pct,
                safeBunks = safeBunks,
                neededToRecover = needed
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Analytics ─────────────────────────────────────────────────────────────
    val analytics: StateFlow<SemesterAnalyticsUi> = combine(
        subjectOverviews,
        allRecordsFlow,
        activeSession
    ) { overviews, records, session ->
        if (session == null || overviews.isEmpty()) {
            SemesterAnalyticsUi()
        } else {
            val totalAtt = overviews.sumOf { it.attended }
            val totalTot = overviews.sumOf { it.total }
            val overallPct = AttendanceMath.calculatePercentage(totalAtt, totalTot)
            val overallSafeBunks = AttendanceMath.calculateSafeBunks(totalAtt, totalTot)
            val overallNeeded = AttendanceMath.calculateClassesNeeded(totalAtt, totalTot)
            val totalBunks = records.count { it.status == "BUNK" }

            val bunksBySubject = records.filter { it.status == "BUNK" }
                .groupBy { it.subjectId }
                .mapValues { it.value.size }
            val mostBunkedId = bunksBySubject.maxByOrNull { it.value }?.key
            val mostBunkedName = overviews.find { it.id == mostBunkedId }?.name

            val heaviestSubjectId = repository.heaviestWorkloadSubjectId(session.id)
            val heaviestName = overviews.find { it.id == heaviestSubjectId }?.name

            SemesterAnalyticsUi(
                overallPercentage = overallPct,
                totalAttended = totalAtt,
                totalClasses = totalTot,
                totalBunks = totalBunks,
                safeBunksLeft = overallSafeBunks,
                classesNeeded = overallNeeded,
                mostBunkedSubjectName = mostBunkedName,
                heaviestWorkloadSubjectName = heaviestName
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SemesterAnalyticsUi())

    // ── Weekly Timetable Cached Flows (Zero recomposition leak) ───────────────
    private val cachedDayFlows = mutableMapOf<Int, StateFlow<List<TimetableEntity>>>()

    fun getTimetableForDay(dayOfWeek: Int): StateFlow<List<TimetableEntity>> {
        return cachedDayFlows.getOrPut(dayOfWeek) {
            activeSessionId.flatMapLatest { sessionId ->
                if (sessionId == null) flowOf(emptyList())
                else repository.timetableByDayFlow(dayOfWeek, sessionId).map { list ->
                    list.sortedBy { AttendanceMath.parseTimeToMinutes(it.startTime) }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        }
    }

    // ── User Actions ──────────────────────────────────────────────────────────
    fun markAttendance(subjectId: String, slotId: String, currentStatus: String, newStatus: String, note: String = "") {
        viewModelScope.launch {
            val finalStatus = if (currentStatus == newStatus) "NONE" else newStatus
            repository.markAttendance(subjectId, slotId, _selectedDate.value, finalStatus, note)
        }
    }

    fun incrementPresent(subjectId: String) {
        viewModelScope.launch {
            repository.incrementPresent(subjectId, _selectedDate.value)
        }
    }

    fun incrementBunk(subjectId: String) {
        viewModelScope.launch {
            repository.incrementBunk(subjectId, _selectedDate.value)
        }
    }

    fun decrementAttendance(subjectId: String) {
        viewModelScope.launch {
            repository.decrementAttendance(subjectId)
        }
    }

    fun swapClass(oldSubjectId: String, newSubjectId: String, slotId: String, status: String, note: String) {
        viewModelScope.launch {
            repository.swapClassSubject(oldSubjectId, newSubjectId, slotId, _selectedDate.value, status, note)
        }
    }

    fun logSeminar(note: String) {
        viewModelScope.launch {
            val classes = todayClasses.value
            classes.forEach { cls ->
                repository.markAttendance(cls.subjectId, cls.timetableId, _selectedDate.value, "EXCUSED", note)
            }
        }
    }

    fun markDayOff() {
        viewModelScope.launch {
            val classes = todayClasses.value
            val allCancelled = classes.isNotEmpty() && classes.all { it.status == "CANCELLED" }
            val target = if (allCancelled) "NONE" else "CANCELLED"
            classes.forEach {
                repository.markAttendance(it.subjectId, it.timetableId, _selectedDate.value, target)
            }
        }
    }

    fun markDayExcused() {
        viewModelScope.launch {
            val classes = todayClasses.value
            val allExcused = classes.isNotEmpty() && classes.all { it.status == "EXCUSED" }
            val target = if (allExcused) "NONE" else "EXCUSED"
            classes.forEach {
                repository.markAttendance(it.subjectId, it.timetableId, _selectedDate.value, target)
            }
        }
    }

    fun dutyRecordsFlow(subjectId: String) = repository.dutyRecordsFlow(subjectId)

    fun addDutyRecord(subjectId: String, dateMs: Long, status: String) {
        viewModelScope.launch {
            repository.addDutyRecord(subjectId, dateMs, status)
        }
    }

    fun deleteDutyRecord(recordId: String) {
        viewModelScope.launch {
            repository.deleteRecord(recordId)
        }
    }

    fun createSession(
        name: String,
        startDate: Long,
        subjects: List<Pair<String, String>>,
        slots: List<Triple<String, Int, String>> = emptyList()
    ) {
        viewModelScope.launch {
            val sessionId = repository.createSessionWithSubjects(name, startDate, subjects, activate = true)
            userPrefs.saveActiveSessionId(sessionId)
            if (slots.isNotEmpty()) {
                repository.importTimetable(sessionId, subjects, slots)
            }
        }
    }

    fun setActiveSession(sessionId: String) {
        viewModelScope.launch {
            repository.setActiveSession(sessionId)
            userPrefs.saveActiveSessionId(sessionId)
        }
    }

    fun endSession(sessionId: String) {
        viewModelScope.launch {
            repository.endSession(sessionId)
            userPrefs.clearActiveSessionId()
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            val current = activeSessionId.value
            if (current == sessionId) {
                userPrefs.clearActiveSessionId()
            }
        }
    }

    fun addClass(dayOfWeek: Int, startTime: String, room: String, subjectCode: String, subjectName: String) {
        viewModelScope.launch {
            val sessionId = activeSessionId.value ?: return@launch
            repository.addTimetableEntryWithSubject(dayOfWeek, startTime, room, subjectCode, subjectName, sessionId)
        }
    }

    fun removeClass(entryId: String) {
        viewModelScope.launch {
            repository.removeTimetableEntry(entryId)
        }
    }

    fun exportTimetable(context: Context) {
        viewModelScope.launch {
            val sessionId = activeSessionId.value ?: return@launch
            val session = repository.getActiveSession() ?: return@launch
            val subjects = repository.getSubjectsBySessionOnce(sessionId)
            val slots = repository.getAllTimetableEntriesOnce(sessionId)
            if (subjects.isEmpty() && slots.isEmpty()) return@launch

            val json = TimetableExportManager.toJson(session.name, subjects, slots)
            val uri = TimetableExportManager.writeToShareableFile(context, json, session.name)
            val intent = TimetableExportManager.buildShareIntent(uri, session.name)
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun importTimetableFromUri(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val sessionId = activeSessionId.value
            if (sessionId == null) {
                onResult(false, "No active semester to import into.")
                return@launch
            }
            try {
                val stream = context.contentResolver.openInputStream(uri)
                if (stream == null) {
                    onResult(false, "Could not open file.")
                    return@launch
                }
                val parseResult = stream.use { TimetableExportManager.parseFromStream(it) }
                when (parseResult) {
                    is TimetableExportManager.ParseResult.Success -> {
                        repository.importTimetable(sessionId, parseResult.subjects, parseResult.slots)
                        onResult(true, "Imported ${parseResult.slots.size} classes successfully!")
                    }
                    is TimetableExportManager.ParseResult.Failure -> {
                        onResult(false, parseResult.reason)
                    }
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Import failed")
            }
        }
    }

    fun getSquadCode(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val sessionId = activeSessionId.value ?: return@launch
            val session = repository.getActiveSession() ?: return@launch
            val subjects = repository.getSubjectsBySessionOnce(sessionId)
            val slots = repository.getAllTimetableEntriesOnce(sessionId)
            val code = TimetableExportManager.encodeToSquadCode(session.name, subjects, slots)
            onReady(code)
        }
    }

    fun importFromSquadCode(rawCode: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val sessionId = activeSessionId.value
            if (sessionId == null) {
                onResult(false, "No active semester to import into.")
                return@launch
            }
            when (val res = TimetableExportManager.parseFromString(rawCode)) {
                is TimetableExportManager.ParseResult.Success -> {
                    repository.importTimetable(sessionId, res.subjects, res.slots)
                    onResult(true, "Synced ${res.slots.size} classes with Squad!")
                }
                is TimetableExportManager.ParseResult.Failure -> {
                    onResult(false, res.reason)
                }
            }
        }
    }

    private fun todayMidnight(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
