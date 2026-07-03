package com.Margin.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Margin.app.data.local.AppDatabase
import com.Margin.app.data.local.AttendanceRepository
import com.Margin.app.data.local.UserPreferencesRepository
import com.Margin.app.data.local.entity.SessionEntity
import com.Margin.app.data.local.entity.SubjectEntity
import com.Margin.app.ui.screens.DashboardSubjectOverview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class SessionUiState(
    val id: String,
    val name: String,
    val subjectCount: Int,
    val startedDaysAgo: Int,
    val isActive: Boolean,
    val isCompleted: Boolean = false,
    val attendedClasses: Int = 0,
    val totalClasses: Int = 0
)

data class AnalyticsUiState(
    val totalBunks: Int = 0,
    val mostBunkedSubjectName: String? = null,
    val heaviestWorkloadSubjectName: String? = null,
    val safeBunksLeft: Int = 0      // floor((attended - 0.75*total) / 0.75), 0 if negative
)

class SessionViewModel(
    private val repository: AttendanceRepository,
    private val userPrefs: UserPreferencesRepository? = null,
    private val database: AppDatabase? = null
) : ViewModel() {

    // ── Month toggle ───────────────────────────────────────────────────────────
    private val _isMonthView = MutableStateFlow(false)
    val isMonthView: StateFlow<Boolean> = _isMonthView.asStateFlow()

    fun toggleMonthView() { _isMonthView.value = !_isMonthView.value }
    fun setMonthView(value: Boolean) { _isMonthView.value = value }

    // ── Sessions ───────────────────────────────────────────────────────────────
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
                        flowOf(SessionUiState(session.id, session.name, 0, maxOf(0, daysAgo), session.isActive, session.isCompleted))
                    } else {
                        repository.getRecordsForSubjects(subjects.map { it.id }).map { records ->
                            val attended = records.count {
                                it.status == "PRESENT" || it.status == "PROXY" || it.status == "DUTY"
                            }
                            val total    = records.count {
                                it.status == "PRESENT" || it.status == "PROXY" || it.status == "DUTY" ||
                                it.status == "BUNK"    || it.status == "EXCUSED" || it.status == "DUTY_ABSENT"
                            }
                            SessionUiState(session.id, session.name, subjects.size, maxOf(0, daysAgo),
                                session.isActive, session.isCompleted, attended, total)
                        }
                    }
                }
            }
            combine(sessionFlows) { it.toList() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Active session subjects ────────────────────────────────────────────────
    val activeSubjects: StateFlow<List<SubjectEntity>> = activeSession.flatMapLatest { session ->
        if (session == null) flowOf(emptyList()) else repository.subjectsFlow(session.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Emits the list of month strings (e.g. ["2025-03", "2025-04"]) that have
     * already been locked for the active session. Used by the MonthPickerDialog
     * to mark and disable already-locked months.
     */
    val lockedMonthsForActiveSession: StateFlow<List<String>> =
        activeSession.flatMapLatest { session ->
            if (session == null) flowOf(emptyList())
            else repository.lockedMonthsFlow(session.id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Dashboard subjects (live records + monthly summaries combined) ─────────
    val dashboardSubjects: StateFlow<List<DashboardSubjectOverview>> =
        combine(activeSubjects, _isMonthView) { subjects, monthView -> Pair(subjects, monthView) }
            .flatMapLatest { (subjects, monthView) ->
                if (subjects.isEmpty()) flowOf(emptyList())
                else {
                    val subjectIds = subjects.map { it.id }
                    combine(
                        repository.getRecordsForSubjects(subjectIds),
                        repository.getMonthlySummariesForSubjects(subjectIds)
                    ) { records, summaries ->
                        val filteredRecords = if (monthView) {
                            val (start, end) = currentMonthBounds()
                            records.filter { it.date in start..end }
                        } else records

                        subjects.map { subject ->
                            // Live records
                            val liveRecs  = filteredRecords.filter { it.subjectId == subject.id }
                            val liveAtt   = liveRecs.count {
                                it.status == "PRESENT" || it.status == "PROXY" || it.status == "DUTY"
                            }
                            val liveTotal = liveRecs.count {
                                it.status == "PRESENT" || it.status == "PROXY" || it.status == "DUTY" ||
                                it.status == "BUNK"    || it.status == "EXCUSED" || it.status == "DUTY_ABSENT"
                            }

                            // Compressed monthly summaries (excluded from month-view — they are historical)
                            val subSummaries = if (!monthView) summaries.filter { it.subjectId == subject.id } else emptyList()
                            val sumAtt   = subSummaries.sumOf { it.attendedCount }
                            val sumTotal = subSummaries.sumOf { it.totalCount }

                            val totalAtt   = liveAtt   + sumAtt
                            val totalTotal = liveTotal + sumTotal
                            val pct = if (totalTotal == 0) 0f else (totalAtt.toFloat() / totalTotal.toFloat()) * 100f

                            DashboardSubjectOverview(subject.id, subject.name, subject.code, "", totalTotal, totalAtt, pct)
                        }
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val overallAttendanceProgress: StateFlow<Float> = dashboardSubjects.map { overviews ->
        val total    = overviews.sumOf { it.totalClasses }
        val attended = overviews.sumOf { it.attendedClasses }
        if (total == 0) 0f else (attended.toFloat() / total.toFloat()) * 100f
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val totalTrackedClasses: StateFlow<Int> = dashboardSubjects.map { it.sumOf { s -> s.totalClasses } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalPresentClasses: StateFlow<Int> = dashboardSubjects.map { it.sumOf { s -> s.attendedClasses } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ── Analytics StateFlow ────────────────────────────────────────────────────
    val analyticsState: StateFlow<AnalyticsUiState> =
        combine(activeSubjects, activeSession) { subjects, session -> Pair(subjects, session) }
            .flatMapLatest { (subjects, session) ->
                if (subjects.isEmpty() || session == null) flowOf(AnalyticsUiState())
                else {
                    val subjectIds = subjects.map { it.id }
                    combine(
                        repository.getRecordsForSubjects(subjectIds),
                        repository.getMonthlySummariesForSubjects(subjectIds),
                        repository.totalBunksFromSummaries(subjectIds),
                        repository.mostBunkedSubjectFromSummaries(subjectIds)
                    ) { liveRecords, summaries, summaryBunks, mostBunkedRaw ->

                        // Live bunks from raw records
                        val liveBunks = liveRecords.count { it.status == "BUNK" }
                        val totalBunks = summaryBunks + liveBunks

                        // Wall of Shame: combine summary bunks + live bunks per subject
                        val liveBunksBySubject = liveRecords.filter { it.status == "BUNK" }
                            .groupBy { it.subjectId }
                            .mapValues { it.value.size }

                        val summaryBunksBySubject = summaries
                            .groupBy { it.subjectId }
                            .mapValues { e -> e.value.sumOf { it.bunkCount } }

                        val allSubjectIds = (liveBunksBySubject.keys + summaryBunksBySubject.keys).toSet()
                        val mostBunkedSubjectId = allSubjectIds.maxByOrNull { id ->
                            (liveBunksBySubject[id] ?: 0) + (summaryBunksBySubject[id] ?: 0)
                        }
                        val mostBunkedName = subjects.find { it.id == mostBunkedSubjectId }?.name

                        // Grand totals for safe bunks calc
                        val liveAtt   = liveRecords.count {
                            it.status == "PRESENT" || it.status == "PROXY" || it.status == "DUTY"
                        }
                        val liveTotal = liveRecords.count {
                            it.status == "PRESENT" || it.status == "PROXY" || it.status == "DUTY" ||
                            it.status == "BUNK"    || it.status == "EXCUSED" || it.status == "DUTY_ABSENT"
                        }
                        val sumAtt   = summaries.sumOf { it.attendedCount }
                        val sumTotal = summaries.sumOf { it.totalCount }
                        val grandAtt   = liveAtt   + sumAtt
                        val grandTotal = liveTotal  + sumTotal

                        // Safe bunks = floor((attended - 0.75 * total) / 0.75), min 0
                        val safeBunks = if (grandTotal == 0) 0
                        else maxOf(0, kotlin.math.floor((grandAtt - 0.75 * grandTotal) / 0.75).toInt())

                        AnalyticsUiState(
                            totalBunks                = totalBunks,
                            mostBunkedSubjectName     = mostBunkedName,
                            heaviestWorkloadSubjectName = null, // fetched on demand below
                            safeBunksLeft             = safeBunks
                        )
                    }.map { state ->
                        // Heaviest workload is a one-shot query (not a Flow in TaskDao)
                        val sessionId = session.id
                        val hwSubjectId = repository.heaviestWorkloadSubjectId(sessionId)
                        val hwName = subjects.find { it.id == hwSubjectId }?.name
                        state.copy(heaviestWorkloadSubjectName = hwName)
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsUiState())

    // ── Actions ───────────────────────────────────────────────────────────────
    fun createSession(name: String, startDate: Long, subjects: List<Pair<String, String>>) {
        viewModelScope.launch { repository.createSessionWithSubjects(name, startDate, subjects) }
    }

    fun createSessionWithTimetable(
        name: String, 
        startDate: Long, 
        subjects: List<Pair<String, String>>,
        slots: List<Triple<String, Int, String>>
    ) {
        viewModelScope.launch {
            val sessionId = repository.createSessionWithSubjects(name, startDate, subjects)
            if (slots.isNotEmpty()) {
                repository.importTimetable(sessionId, subjects, slots)
            }
        }
    }

    fun setActiveSession(sessionId: String) {
        viewModelScope.launch {
            repository.setActiveSession(sessionId)
            userPrefs?.saveActiveSessionId(sessionId)
        }
    }

    fun endSession(sessionId: String) {
        viewModelScope.launch {
            repository.endSession(sessionId)
            // Clear DataStore so cold boot routes to session picker
            userPrefs?.clearActiveSessionId()
        }
    }

    fun lockMonth(sessionId: String, monthYearText: String) {
        viewModelScope.launch {
            // Guard: don't re-lock a month that already has a summary
            if (repository.isMonthLocked(sessionId, monthYearText)) return@launch
            val subjects = repository.getSubjectsBySessionOnce(sessionId)
            if (subjects.isNotEmpty()) {
                repository.lockMonth(sessionId, subjects.map { it.id }, monthYearText)
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            database?.vacuumAndCheckpoint()
        }
    }

    // ── Calendar helpers ──────────────────────────────────────────────────────
    private fun currentMonthBounds(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0);      cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59);      cal.set(Calendar.MILLISECOND, 999)

        return Pair(start, cal.timeInMillis)
    }
}
