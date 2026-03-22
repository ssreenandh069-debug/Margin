package com.Margin.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.viewModelScope
import com.Margin.app.data.local.AttendanceRepository
import com.Margin.app.receivers.TaskAlarmReceiver
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.os.Build

data class TaskUiState(
    val id: String,
    val title: String,
    val subjectName: String,
    val subjectCode: String,
    val type: String,
    val dueDate: String,
    val isOverdue: Boolean,
    val isCompleted: Boolean,
    val hasReminder: Boolean
)

sealed interface TaskListState {
    object Loading : TaskListState
    data class Success(val tasks: List<TaskUiState>) : TaskListState
    object Empty : TaskListState
}

class TaskViewModel(
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

    private val subjectsFlow = activeSessionId.flatMapLatest { sessionId ->
        if (sessionId == null) flowOf(emptyList())
        else repository.subjectsFlow(sessionId)
    }

    private val cachedFlows = mutableMapOf<String, StateFlow<TaskListState>>()

    fun getTasks(type: String): StateFlow<TaskListState> {
        val typeUpper = type.uppercase()
        return cachedFlows.getOrPut(typeUpper) {
            activeSessionId.flatMapLatest { sessionId ->
                if (sessionId == null) flowOf(TaskListState.Loading) else {
                    val rawTasksFlow = if (typeUpper == "ALL") {
                        repository.allTasksFlow(sessionId)
                    } else {
                        repository.tasksFlow(typeUpper, sessionId)
                    }

                    combine(rawTasksFlow, subjectsFlow) { tasks, subjects ->
                        val now = System.currentTimeMillis()
                        val mapped = tasks.mapNotNull { task ->
                            val subject = subjects.find { it.id == task.subjectId } ?: return@mapNotNull null
                            
                            val isOverdue = task.dueDate < now
                            
                            TaskUiState(
                                id = task.id,
                                title = task.title,
                                subjectName = subject.name,
                                subjectCode = subject.code,
                                type = task.type,
                                dueDate = "Due soon", // simplified mapping for demo
                                isOverdue = isOverdue,
                                isCompleted = task.isCompleted,
                                hasReminder = false
                            )
                        }
                        if (mapped.isEmpty()) TaskListState.Empty else TaskListState.Success(mapped)
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TaskListState.Loading)
        }
    }

    val pendingWorkCounts = activeSessionId.flatMapLatest { sessionId ->
        if (sessionId == null) flowOf(emptyMap()) else {
            repository.allTasksFlow(sessionId).map { allTasks ->
                allTasks.filter { !it.isCompleted }
                    .groupBy { it.type }
                    .mapValues { it.value.size }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun toggleTaskComplete(taskId: String) {
        viewModelScope.launch {
            repository.toggleTaskComplete(taskId)
        }
    }

    fun addTask(context: Context? = null, title: String, typeString: String, subjectCode: String, dueDate: Long = System.currentTimeMillis() + 86400000, hasReminder: Boolean = false) {
        viewModelScope.launch {
            val sessionId = activeSessionId.value ?: return@launch
            val subjects = repository.getSubjectsBySessionOnce(sessionId)
            val subjectStr = subjectCode.split(" ").firstOrNull() ?: ""
            val actualSubject = subjects.find { it.code == subjectStr }
            val subjectId = actualSubject?.id ?: return@launch
            val subjectName = actualSubject.name

            val type = when {
                typeString.contains("Assign", ignoreCase = true) -> "ASSIGNMENT"
                typeString.contains("Present", ignoreCase = true) -> "PRESENTATION"
                else -> "PRACTICAL"
            }
            repository.addTask(title, type, subjectId, dueDate) // we need to save hasReminder in DB if schema supports it, for now we schedule it instantly

            if (hasReminder && context != null) {
                scheduleTaskReminder(context, title, subjectName, dueDate - 86400000)
            }
        }
    }

    private fun scheduleTaskReminder(context: Context, title: String, subjectName: String, alarmTime: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return
        }

        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("SUBJECT_NAME", subjectName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent)
        } catch (e: SecurityException) {
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, alarmTime, 600000L, pendingIntent)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
        }
    }
}
