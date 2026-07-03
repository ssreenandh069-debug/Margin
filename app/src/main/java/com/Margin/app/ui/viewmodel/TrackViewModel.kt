package com.Margin.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Margin.app.data.local.AttendanceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class TodayClassUiState(
    val timetableId: String,
    val subjectId: String,
    val subjectCode: String,
    val subjectName: String,
    val time: String,
    val room: String,
    val status: String,      // PRESENT | BUNK | EXCUSED | CANCELLED | PROXY | NONE
    val attendancePct: Float = 0f,
    val attended: Int = 0,
    val total: Int = 0
)

class TrackViewModel(
    private val repository: AttendanceRepository
) : ViewModel() {

    // ── Time Machine: selected date (defaults to today) ───────────────────────
    private val _selectedDate = MutableStateFlow(todayMidnight())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    fun selectDate(dateMs: Long) {
        // Normalise to midnight of selected date
        val cal = Calendar.getInstance().apply {
            timeInMillis = dateMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        _selectedDate.value = cal.timeInMillis
    }

    fun resetToToday() { _selectedDate.value = todayMidnight() }

    // ── Active session tracker ────────────────────────────────────────────────
    private val activeSessionFlow = repository.sessionsFlow.map { sessions ->
        sessions.firstOrNull { it.isActive }
    }

    val activeSessionIsCompleted: StateFlow<Boolean> = activeSessionFlow.map { it?.isCompleted ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val activeSessionId: StateFlow<String?> = activeSessionFlow.map { it?.id }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Day of week derived from selected date (1=Mon … 7=Sun domain)
    private val selectedDayOfWeek: Flow<Int> = _selectedDate.map { dateMs ->
        val calDay = Calendar.getInstance().apply { timeInMillis = dateMs }.get(Calendar.DAY_OF_WEEK)
        if (calDay == Calendar.SUNDAY) 7 else calDay - 1
    }

    // Subjects for active session
    private val subjectsFlow = activeSessionId.flatMapLatest { sessionId ->
        if (sessionId == null) flowOf(emptyList()) else repository.subjectsFlow(sessionId)
    }

    // Timetable entries for selected day
    private val timetableFlow = combine(activeSessionId, selectedDayOfWeek) { sessionId, dow ->
        Pair(sessionId, dow)
    }.flatMapLatest { (sessionId, dow) ->
        if (sessionId == null) flowOf(emptyList())
        else repository.timetableByDayFlow(dow, sessionId)
    }

    // Attendance records for the selected date
    private val selectedDateRecordsFlow = _selectedDate.flatMapLatest { date ->
        repository.todayRecordsFlow(date)
    }

    // All records for selected session's subjects (for cumulative % calc)
    private val allSubjectRecordsFlow = subjectsFlow.flatMapLatest { subjects ->
        if (subjects.isEmpty()) flowOf(emptyList())
        else repository.getRecordsForSubjects(subjects.map { it.id })
    }

    // Merge into UI state
    val todayClassesUiState: StateFlow<List<TodayClassUiState>> = combine(
        timetableFlow,
        subjectsFlow,
        selectedDateRecordsFlow,
        allSubjectRecordsFlow
    ) { timetable, subjects, dateRecords, allRecords ->
        timetable.mapNotNull { entry ->
            val subject = subjects.find { it.id == entry.subjectId } ?: return@mapNotNull null
            val record  = dateRecords.find { it.subjectId == subject.id }
            val status  = record?.status ?: "NONE"

            // Cumulative attendance across ALL records
            // DUTY counts as attended (same as PRESENT); DUTY_ABSENT counts against total only
            val subjectRecords = allRecords.filter { it.subjectId == subject.id }
            val attended = subjectRecords.count {
                it.status == "PRESENT" || it.status == "PROXY" || it.status == "DUTY"
            }
            val total    = subjectRecords.count {
                it.status == "PRESENT" || it.status == "PROXY" || it.status == "DUTY"  ||
                it.status == "BUNK"    || it.status == "EXCUSED" || it.status == "DUTY_ABSENT"
            }
            val pct = if (total == 0) 0f else (attended.toFloat() / total.toFloat()) * 100f

            TodayClassUiState(
                timetableId  = entry.id,
                subjectId    = subject.id,
                subjectCode  = subject.code,
                subjectName  = subject.name,
                time         = entry.startTime,
                room         = "TBD",
                status       = status,
                attendancePct = pct,
                attended     = attended,
                total        = total
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── User Actions ─────────────────────────────────────────────────────────

    /** Single class manual mark — ABSENT = BUNK (intentional skipping) */
    fun markAttendance(subjectId: String, status: String) {
        viewModelScope.launch {
            val currentStatus = todayClassesUiState.value.find { it.subjectId == subjectId }?.status
            val finalStatus   = if (currentStatus == status) "NONE" else status
            repository.markAttendance(subjectId, _selectedDate.value, finalStatus)
        }
    }

    /** Mark all today's classes as CANCELLED (day off — doesn't affect percentage) */
    fun markDayOff() {
        viewModelScope.launch {
            val classes     = todayClassesUiState.value
            val allCancelled = classes.all { it.status == "CANCELLED" }
            val target      = if (allCancelled) "NONE" else "CANCELLED"
            classes.forEach { repository.markAttendance(it.subjectId, _selectedDate.value, target) }
        }
    }

    /** Mark all today's classes as EXCUSED (sick/legitimate — counts against %, not a BUNK) */
    fun markDayExcused() {
        viewModelScope.launch {
            val classes    = todayClassesUiState.value
            val allExcused = classes.all { it.status == "EXCUSED" }
            val target     = if (allExcused) "NONE" else "EXCUSED"
            classes.forEach { repository.markAttendance(it.subjectId, _selectedDate.value, target) }
        }
    }

    // ── Duty / Extra Attendance ───────────────────────────────────────────────

    /**
     * Returns a Flow of all DUTY/DUTY_ABSENT records for [subjectId].
     * Observed by ExtraDutySheet to render the live list of duty entries.
     */
    fun dutyRecordsFlow(subjectId: String) = repository.dutyRecordsFlow(subjectId)

    /**
     * Logs a new DUTY or DUTY_ABSENT record on [dateMs] for [subjectId].
     * Unlike markAttendance(), this always inserts a new row (multiple duty
     * records on different dates for the same subject are intentional).
     */
    fun addDutyRecord(subjectId: String, dateMs: Long, status: String) {
        viewModelScope.launch {
            repository.addDutyRecord(subjectId, dateMs, status)
        }
    }

    /** Remove a previously logged duty record by its UUID. */
    fun deleteDutyRecord(recordId: String) {
        viewModelScope.launch {
            repository.deleteDutyRecord(recordId)
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────
    private fun todayMidnight(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
