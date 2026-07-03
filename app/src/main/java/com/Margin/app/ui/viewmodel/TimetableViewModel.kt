package com.Margin.app.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Margin.app.data.local.AttendanceRepository
import com.Margin.app.data.local.entity.SubjectEntity
import com.Margin.app.utils.TimetableExportManager
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

/** One-shot UI event emitted after an import completes (success or failure). */
sealed interface ImportResult {
    data class Success(val sessionName: String, val subjectCount: Int, val slotCount: Int) : ImportResult
    data class Failure(val reason: String) : ImportResult
}

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

    // ── Export ────────────────────────────────────────────────────────────────

    /**
     * Serialises the active session's full timetable to a JSON file and fires
     * an Intent.ACTION_SEND chooser so the user can share it.
     *
     * Does nothing if there is no active session.
     */
    fun exportTimetable(context: Context) {
        viewModelScope.launch {
            val sessionId = activeSessionId.value ?: return@launch

            // Fetch the active session entity for its name
            val session = repository.getActiveSession() ?: return@launch
            val subjects = repository.getSubjectsBySessionOnce(sessionId)
            val slots    = repository.getAllTimetableEntriesOnce(sessionId)

            if (subjects.isEmpty() && slots.isEmpty()) return@launch

            val json = TimetableExportManager.toJson(session.name, subjects, slots)
            val uri  = TimetableExportManager.writeToShareableFile(context, json, session.name)
            val intent = TimetableExportManager.buildShareIntent(uri, session.name)

            // startActivity requires a non-Activity context flag on some older APIs;
            // using FLAG_ACTIVITY_NEW_TASK is the safe pattern from a ViewModel.
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    // ── Import ────────────────────────────────────────────────────────────────

    /** Emits the result of the most recent import operation. Null means idle. */
    private val _importResult = MutableStateFlow<ImportResult?>(null)
    val importResult: StateFlow<ImportResult?> = _importResult.asStateFlow()

    /** Call after the UI has consumed the import result to clear the state. */
    fun clearImportResult() { _importResult.value = null }

    /**
     * Parses [uri] (from a file-picker result) and imports the timetable into
     * the currently active session.  Emits [ImportResult] which the UI shows
     * as a Snackbar/Dialog.
     */
    fun importTimetableFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val sessionId = activeSessionId.value
            if (sessionId == null) {
                _importResult.value = ImportResult.Failure("No active semester. Create a semester first, then import.")
                return@launch
            }

            val parsed = try {
                val stream = context.contentResolver.openInputStream(uri)
                    ?: return@launch run {
                        _importResult.value = ImportResult.Failure("Could not open the selected file.")
                    }
                stream.use { TimetableExportManager.parseFromStream(it) }
            } catch (e: Exception) {
                ImportResult.Failure("Failed to read file: ${e.message}")
            }

            when (parsed) {
                is TimetableExportManager.ParseResult.Failure -> {
                    _importResult.value = ImportResult.Failure(parsed.reason)
                }
                is TimetableExportManager.ParseResult.Success -> {
                    repository.importTimetable(sessionId, parsed.subjects, parsed.slots)
                    _importResult.value = ImportResult.Success(
                        sessionName  = parsed.sessionName,
                        subjectCount = parsed.subjects.size,
                        slotCount    = parsed.slots.size
                    )
                }
            }
        }
    }

    private fun dayStringToInt(day: String): Int = when (day) {
        "Mon" -> 1 "Tue" -> 2 "Wed" -> 3 "Thu" -> 4 "Fri" -> 5 "Sat" -> 6 "Sun" -> 7 else -> 1
    }
}
