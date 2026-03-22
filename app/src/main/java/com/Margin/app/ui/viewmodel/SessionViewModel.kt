package com.attendease.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendease.app.data.local.AttendanceRepository
import com.attendease.app.data.local.entity.SessionEntity
import com.attendease.app.data.local.entity.SubjectEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SessionUiState(
    val id: String,
    val name: String,
    val subjectCount: Int,
    val startedDaysAgo: Int,
    val isActive: Boolean,
    val attendedClasses: Int = 0,
    val totalClasses: Int = 0
)

class SessionViewModel(
    private val repository: AttendanceRepository
) : ViewModel() {

    val activeSession: StateFlow<SessionEntity?> = repository.sessionsFlow.map { sessions ->
        sessions.firstOrNull { it.isActive }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val sessionsList: StateFlow<List<SessionUiState>> = repository.sessionsFlow.flatMapLatest { sessions ->
        if (sessions.isEmpty()) flowOf(emptyList())
        else {
            val sessionFlows = sessions.map { session ->
                val daysAgo = ((System.currentTimeMillis() - session.startDate) / 86400000L).toInt()
                repository.subjectsFlow(session.id).flatMapLatest { subjects ->
                    if (subjects.isEmpty()) {
                        flowOf(
                            SessionUiState(
                                id = session.id,
                                name = session.name,
                                subjectCount = 0,
                                startedDaysAgo = maxOf(0, daysAgo),
                                isActive = session.isActive,
                                attendedClasses = 0,
                                totalClasses = 0
                            )
                        )
                    } else {
                        repository.getRecordsForSubjects(subjects.map { it.id }).map { records ->
                            val validRecords = records.filter { it.status in listOf("PRESENT", "ABSENT", "PROXY") }
                            SessionUiState(
                                id = session.id,
                                name = session.name,
                                subjectCount = subjects.size,
                                startedDaysAgo = maxOf(0, daysAgo),
                                isActive = session.isActive,
                                attendedClasses = validRecords.count { it.status == "PRESENT" || it.status == "PROXY" },
                                totalClasses = validRecords.size
                            )
                        }
                    }
                }
            }
            combine(sessionFlows) { it.toList() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Used by Dashboard to show overall subjects list
    val activeSubjects: StateFlow<List<SubjectEntity>> = activeSession.flatMapLatest { session ->
        if (session == null) flowOf(emptyList()) else repository.subjectsFlow(session.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardSubjects: StateFlow<List<com.attendease.app.ui.screens.DashboardSubjectOverview>> = activeSubjects.flatMapLatest { subjects ->
        if (subjects.isEmpty()) flowOf(emptyList())
        else repository.getRecordsForSubjects(subjects.map { it.id }).map { records ->
            subjects.map { subject ->
                val validRecords = records.filter { it.subjectId == subject.id && it.status in listOf("PRESENT", "ABSENT", "PROXY") }
                val total = validRecords.size
                val attended = validRecords.count { it.status == "PRESENT" || it.status == "PROXY" }
                val pct = if (total == 0) 0f else attended.toFloat() / total.toFloat()

                com.attendease.app.ui.screens.DashboardSubjectOverview(
                    id = subject.id,
                    name = subject.name,
                    code = subject.code,
                    teacher = "",
                    totalClasses = total,
                    attendedClasses = attended,
                    percentage = pct
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val overallAttendanceProgress: StateFlow<Float> = dashboardSubjects.map { overviews ->
        val total = overviews.sumOf { it.totalClasses }
        val attended = overviews.sumOf { it.attendedClasses }
        if (total == 0) 0f else attended.toFloat() / total.toFloat()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val totalTrackedClasses: StateFlow<Int> = dashboardSubjects.map { overviews ->
        overviews.sumOf { it.totalClasses }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalPresentClasses: StateFlow<Int> = dashboardSubjects.map { overviews ->
        overviews.sumOf { it.attendedClasses }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun createSession(name: String, startDate: Long, subjects: List<Pair<String, String>>) {
        viewModelScope.launch {
            repository.createSessionWithSubjects(name, startDate, subjects)
        }
    }

    fun setActiveSession(sessionId: String) {
        viewModelScope.launch {
            repository.setActiveSession(sessionId)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
        }
    }
}
