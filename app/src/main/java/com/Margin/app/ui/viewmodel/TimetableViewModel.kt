package com.attendease.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendease.app.data.local.AttendanceRepository
import com.attendease.app.data.local.entity.SubjectEntity
import com.attendease.app.data.local.entity.TimetableEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TimetableUiState(
    val id: String,
    val subjectId: String,
    val subjectCode: String,
    val subjectName: String,
    val time: String,
    val room: String
)

class TimetableViewModel(
    private val repository: AttendanceRepository
) : ViewModel() {

    private val activeSessionId = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            repository.sessionsFlow.collect { sessions ->
                activeSessionId.value = sessions.firstOrNull { it.isActive }?.id
            }
        }
    }

    // Used by AddClassSheet to populate dropdown
    val subjects: StateFlow<List<SubjectEntity>> = activeSessionId.flatMapLatest { sessionId ->
        if (sessionId == null) flowOf(emptyList()) else repository.subjectsFlow(sessionId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val classesPerDayCount: StateFlow<Map<Int, Int>> = activeSessionId.flatMapLatest { sessionId ->
        if (sessionId == null) flowOf(emptyMap()) else repository.getClassesPerDayCount(sessionId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun getTimetableForDay(dayStr: String): StateFlow<List<TimetableUiState>> {
        val dayNum = dayStringToInt(dayStr)
        return activeSessionId.flatMapLatest { sessionId ->
            if (sessionId == null) flowOf(emptyList()) else {
                val rawFlow = repository.timetableByDayFlow(dayNum, sessionId)
                combine(rawFlow, subjects) { entries, subs ->
                    entries.mapNotNull { entry ->
                        val subject = subs.find { it.id == entry.subjectId } ?: return@mapNotNull null
                        TimetableUiState(
                            id = entry.id,
                            subjectId = subject.id,
                            subjectCode = subject.code,
                            subjectName = subject.name,
                            time = entry.startTime,
                            room = "TBD" // Minimal schema does not include room
                        )
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun addClass(dayStr: String, subjectCode: String, subjectName: String, time: String) {
        viewModelScope.launch {
            val sessionId = activeSessionId.value ?: return@launch
            repository.addTimetableEntryWithSubject(dayStringToInt(dayStr), time, subjectCode, subjectName, sessionId)
        }
    }

    fun removeClass(entryId: String) {
        viewModelScope.launch {
            repository.removeTimetableEntry(entryId)
        }
    }

    private fun dayStringToInt(day: String): Int = when (day) {
        "Mon" -> 1 "Tue" -> 2 "Wed" -> 3 "Thu" -> 4 "Fri" -> 5 "Sat" -> 6 "Sun" -> 7 else -> 1
    }
}
