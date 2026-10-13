package com.Margin.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Margin.app.data.local.AttendanceRepository
import com.Margin.app.data.local.entity.SubjectEntity
import com.Margin.app.data.local.entity.TaskEntity
import com.Margin.app.data.local.entity.TaskType
import com.Margin.app.domain.AttendanceMath
import com.Margin.app.reminders.TaskReminderManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TaskUi(
    val id: String,
    val title: String,
    val subjectId: String,
    val subjectCode: String,
    val subjectName: String,
    val type: TaskType,
    val dueDateMs: Long,
    val dueDateFormatted: String,
    val isCompleted: Boolean,
    val isOverdue: Boolean,
    val hasReminder: Boolean
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TaskViewModel(
    private val repository: AttendanceRepository
) : ViewModel() {

    private val activeSession = repository.sessionsFlow.map { sessions ->
        sessions.firstOrNull { it.isActive } ?: sessions.firstOrNull { !it.isCompleted }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeSessionId: StateFlow<String?> = activeSession.map { it?.id }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val subjects: StateFlow<List<SubjectEntity>> = activeSessionId.flatMapLatest { sessionId ->
        if (sessionId == null) flowOf(emptyList()) else repository.subjectsFlow(sessionId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<TaskUi>> = combine(
        activeSessionId.flatMapLatest { sessionId ->
            if (sessionId == null) flowOf(emptyList()) else repository.allTasksFlow(sessionId)
        },
        subjects
    ) { tasks, subs ->
        val now = System.currentTimeMillis()
        tasks.map { task ->
            val sub = subs.find { it.id == task.subjectId }
            val code = sub?.code ?: "SUB"
            val name = sub?.name ?: "Subject"
            val isOverdue = !task.isCompleted && task.dueDate < now
            val formattedDue = AttendanceMath.formatRelativeDueDate(task.dueDate)
            val parsedType = TaskType.fromString(task.type)

            TaskUi(
                id = task.id,
                title = task.title,
                subjectId = task.subjectId,
                subjectCode = code,
                subjectName = name,
                type = parsedType,
                dueDateMs = task.dueDate,
                dueDateFormatted = formattedDue,
                isCompleted = task.isCompleted,
                isOverdue = isOverdue,
                hasReminder = task.hasReminder
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingWorkCounts: StateFlow<Map<String, Int>> = allTasks.map { tasks ->
        tasks.filter { !it.isCompleted }
            .groupBy { it.type.name }
            .mapValues { it.value.size }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun addTask(
        context: Context,
        title: String,
        type: String,
        subjectId: String,
        dueDate: Long,
        hasReminder: Boolean
    ) {
        viewModelScope.launch {
            val task = repository.addTask(title, type, subjectId, dueDate, hasReminder)
            if (hasReminder) {
                val subjectName = subjects.value.find { it.id == subjectId }?.name
                TaskReminderManager.scheduleTaskAlarms(context, task, subjectName)
            }
        }
    }

    fun toggleTaskComplete(context: Context, task: TaskUi) {
        viewModelScope.launch {
            repository.toggleTaskComplete(task.id)
            if (!task.isCompleted) {
                TaskReminderManager.cancelTaskAlarms(context, task.id)
            } else if (task.hasReminder) {
                val entity = TaskEntity(
                    id = task.id,
                    subjectId = task.subjectId,
                    title = task.title,
                    type = task.type.name,
                    dueDate = task.dueDateMs,
                    isCompleted = false,
                    hasReminder = true
                )
                TaskReminderManager.scheduleTaskAlarms(context, entity, task.subjectName)
            }
        }
    }

    fun deleteTask(context: Context, task: TaskUi) {
        viewModelScope.launch {
            TaskReminderManager.cancelTaskAlarms(context, task.id)
            repository.deleteTask(task.id)
        }
    }
}
