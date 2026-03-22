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
    val status: String, // PRESENT/ABSENT/NONE etc.
    val attendancePct: Float = 0.85f, // Computed locally for now based on dummy logic, ideally from DB
    val totalClasses: Int = 0
)

class TrackViewModel(
    private val repository: AttendanceRepository
) : ViewModel() {

    // Helper: midnight of today
    private val todayMillis: Long
        get() = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    // 1=Sun, 2=Mon... 7=Sat -> Domain expects 1=Mon...7=Sun
    private val todayDayOfWeek: Int
        get() {
            val calDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            return if (calDay == Calendar.SUNDAY) 7 else calDay - 1
        }

    private val activeSessionId = MutableStateFlow<String?>(null)

    init {
        // Find active session
        viewModelScope.launch {
            repository.sessionsFlow.collect { sessions ->
                activeSessionId.value = sessions.firstOrNull { it.isActive }?.id
            }
        }
    }

    // Load subjects for the active session
    private val subjectsFlow = activeSessionId.flatMapLatest { sessionId ->
        if (sessionId == null) flowOf(emptyList())
        else repository.subjectsFlow(sessionId)
    }

    // Load timetable for today
    private val timetableFlow = activeSessionId.flatMapLatest { sessionId ->
        if (sessionId == null) flowOf(emptyList())
        else repository.timetableByDayFlow(todayDayOfWeek, sessionId)
    }

    // Load today's attendance records
    private val attendanceRecordsFlow = repository.todayRecordsFlow(todayMillis)

    private val allSubjectRecordsFlow = subjectsFlow.flatMapLatest { subjects ->
        if (subjects.isEmpty()) flowOf(emptyList())
        else repository.getRecordsForSubjects(subjects.map { it.id })
    }

    // Merge them into UiState
    val todayClassesUiState: StateFlow<List<TodayClassUiState>> = combine(
        timetableFlow,
        subjectsFlow,
        attendanceRecordsFlow,
        allSubjectRecordsFlow
    ) { timetable, subjects, todayRecords, allRecords ->
        timetable.mapNotNull { entry ->
            val subject = subjects.find { it.id == entry.subjectId } ?: return@mapNotNull null
            val record = todayRecords.find { it.subjectId == subject.id }
            val status = record?.status ?: "NONE"
            
            val validRecords = allRecords.filter { it.subjectId == subject.id && it.status in listOf("PRESENT", "ABSENT", "PROXY") }
            val total = validRecords.size
            val attended = validRecords.count { it.status == "PRESENT" || it.status == "PROXY" }
            val computedPct = if (total == 0) 0f else attended.toFloat() / total.toFloat()

            TodayClassUiState(
                timetableId = entry.id,
                subjectId = subject.id,
                subjectCode = subject.code,
                subjectName = subject.name,
                time = entry.startTime,
                room = "TBD", // Room is not in my minimal timetable schema, defaulting for demo
                status = status,
                attendancePct = computedPct,
                totalClasses = total
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── User Actions ─────────────────────────────────────────────────────────

    fun markAttendance(subjectId: String, status: String) {
        viewModelScope.launch {
            // Un-toggle logic: if it's already the requested status, flip it to NONE
            val currentStatus = todayClassesUiState.value.find { it.subjectId == subjectId }?.status
            val finalStatus = if (currentStatus == status) "NONE" else status

            repository.markAttendance(subjectId, todayMillis, finalStatus)
        }
    }

    fun markDayOff() {
        viewModelScope.launch {
            val classes = todayClassesUiState.value
            val allCancelled = classes.all { it.status == "CANCELLED" }
            val target = if (allCancelled) "NONE" else "CANCELLED"
            classes.forEach { 
                repository.markAttendance(it.subjectId, todayMillis, target)
            }
        }
    }

    fun markDayAbsent() {
        viewModelScope.launch {
            val classes = todayClassesUiState.value
            val allAbsent = classes.all { it.status == "ABSENT" }
            val target = if (allAbsent) "NONE" else "ABSENT"
            classes.forEach { 
                repository.markAttendance(it.subjectId, todayMillis, target)
            }
        }
    }
}
