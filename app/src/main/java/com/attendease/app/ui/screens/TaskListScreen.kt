package com.attendease.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attendease.app.data.TaskType
import com.attendease.app.ui.components.AddTaskSheet
import com.attendease.app.ui.theme.*
import com.attendease.app.ui.viewmodel.TaskListState
import com.attendease.app.ui.viewmodel.TaskUiState
import com.attendease.app.ui.viewmodel.TaskViewModel
import com.attendease.app.utils.getAppViewModel

// Resolves accent color from TaskType
fun taskAccentColor(type: TaskType): Color = when (type) {
    TaskType.ASSIGNMENT   -> BlueAccent
    TaskType.PRESENTATION -> Magenta
    TaskType.PRACTICAL    -> Yellow
}
fun taskAccentAlpha(type: TaskType): Color = when (type) {
    TaskType.ASSIGNMENT   -> BlueAlpha20
    TaskType.PRESENTATION -> MagentaAlpha20
    TaskType.PRACTICAL    -> YellowAlpha20
}

enum class TaskFilter { ALL, PENDING, COMPLETED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    taskType: TaskType = TaskType.ASSIGNMENT,
    onBack: () -> Unit = {},
    viewModel: TaskViewModel = getAppViewModel()
) {
    val accent      = taskAccentColor(taskType)
    val accentAlpha = taskAccentAlpha(taskType)

    val title = when (taskType) {
        TaskType.ASSIGNMENT   -> "Assignments"
        TaskType.PRESENTATION -> "Presentations"
        TaskType.PRACTICAL    -> "Practical Files"
    }

    val typeString = taskType.name // "ASSIGNMENT", "PRESENTATION", "PRACTICAL"
    val tasksState by viewModel.getTasks(typeString).collectAsState()
    val tasks = (tasksState as? TaskListState.Success)?.tasks ?: emptyList()

    var searchQuery  by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(TaskFilter.ALL) }
    var showAddSheet by remember { mutableStateOf(false) }

    val pending   = tasks.count { !it.isCompleted }
    val completed = tasks.count { it.isCompleted }

    val displayed = tasks
        .filter { task ->
            (searchQuery.isBlank() ||
                task.title.contains(searchQuery, true) ||
                task.subjectName.contains(searchQuery, true))
        }
        .filter { task ->
            when (activeFilter) {
                TaskFilter.ALL       -> true
                TaskFilter.PENDING   -> !task.isCompleted
                TaskFilter.COMPLETED -> task.isCompleted
            }
        }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                // ── TopBar ─────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 12.dp, top = 20.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                        Text(
                            "$pending pending  •  $completed completed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showAddSheet = true }) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(accentAlpha, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add Task", tint = accent, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            item {
                // ── Search Bar ─────────────────────────────────────
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    placeholder = { Text("Search tasks...", color = MaterialTheme.colorScheme.outlineVariant) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        cursorColor = accent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            item {
                // ── Filter Chips ───────────────────────────────────
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    item { FilterChipItem("All",       TaskFilter.ALL,       activeFilter, accent) { activeFilter = TaskFilter.ALL }       }
                    item { FilterChipItem("Pending",   TaskFilter.PENDING,   activeFilter, accent) { activeFilter = TaskFilter.PENDING }   }
                    item { FilterChipItem("Completed", TaskFilter.COMPLETED, activeFilter, accent) { activeFilter = TaskFilter.COMPLETED } }
                    item {
                        // Sort chip (decorative)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Sort, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Sort", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item {
                AnimatedContent(
                    targetState = tasksState,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tasks_state_anim"
                ) { state ->
                    when {
                        state is TaskListState.Loading -> {
                            Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = accent)
                            }
                        }
                        state is TaskListState.Empty || displayed.isEmpty() -> {
                            Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.TaskAlt, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(52.dp))
                                    Spacer(Modifier.height(12.dp))
                                    Text("No tasks here!", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                        else -> {
                            Column {
                                displayed.forEach { task ->
                                    TaskCard(
                                        task = task,
                                        accent = accent,
                                        accentAlpha = accentAlpha,
                                        onToggleComplete = { viewModel.toggleTaskComplete(task.id) },
                                        onDelete = { viewModel.deleteTask(task.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── FAB ────────────────────────────────────────────────
        FloatingActionButton(
            onClick = { showAddSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp),
            containerColor = accent,
            contentColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Task", modifier = Modifier.size(28.dp))
        }
    }

    if (showAddSheet) {
        AddTaskSheet(onDismiss = { showAddSheet = false })
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    filter: TaskFilter,
    activeFilter: TaskFilter,
    accent: Color,
    onClick: () -> Unit
) {
    val selected = filter == activeFilter
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) accent else MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, if (selected) accent else MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun TaskCard(
    task: TaskUiState,
    accent: Color,
    accentAlpha: Color,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.dp, if (task.isCompleted) MaterialTheme.colorScheme.outline else accent.copy(0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox circle
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (task.isCompleted) accent else Color.Transparent)
                    .border(1.5.dp, if (task.isCompleted) accent else MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable { onToggleComplete() },
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.width(12.dp))

            // Middle content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.onBackground,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = accentAlpha, shape = RoundedCornerShape(5.dp)) {
                        Text(
                            task.subjectCode,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(task.subjectName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(task.dueDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outlineVariant)
                    if (task.isOverdue && !task.isCompleted) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = AttendRedAlpha20,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(0.5.dp, AttendRed.copy(0.4f))
                        ) {
                            Text(
                                "Overdue",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = AttendRed,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Right side actions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (task.hasReminder) Icons.Filled.Notifications else Icons.Filled.NotificationsNone,
                    contentDescription = "Reminder",
                    tint = if (task.hasReminder) accent else MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(20.dp)
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = AttendRed.copy(0.7f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
