package com.Margin.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Margin.app.data.local.AttendanceRepository
import com.Margin.app.data.local.entity.SessionEntity
import com.Margin.app.data.local.entity.SubjectEntity
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
                            val attendedClasses = records.count { it.status == "PRESENT" || it.status == "PROXY" }
                            val totalClasses = records.count { it.status == "PRESENT" || it.status == "ABSENT" || it.status == "PROXY" }
                            SessionUiState(
                                id = session.id,
                                name = session.name,
                                subjectCount = subjects.size,
                                startedDaysAgo = maxOf(0, daysAgo),
                                isActive = session.isActive,
                                attendedClasses = attendedClasses,
                                totalClasses = totalClasses
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

    val dashboardSubjects: StateFlow<List<com.Margin.app.ui.screens.DashboardSubjectOverview>> = activeSubjects.flatMapLatest { subjects ->
        if (subjects.isEmpty()) flowOf(emptyList())
        else repository.getRecordsForSubjects(subjects.map { it.id }).map { records ->
            subjects.map { subject ->
                val subjectRecords = records.filter { it.subjectId == subject.id }
                val attended = subjectRecords.count { it.status == "PRESENT" || it.status == "PROXY" }
                val total = subjectRecords.count { it.status == "PRESENT" || it.status == "ABSENT" || it.status == "PROXY" }
                val pct = if (total == 0) 0f else (attended.toFloat() / total.toFloat()) * 100f

                com.Margin.app.ui.screens.DashboardSubjectOverview(
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
        if (total == 0) 0f else (attended.toFloat() / total.toFloat()) * 100f
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
