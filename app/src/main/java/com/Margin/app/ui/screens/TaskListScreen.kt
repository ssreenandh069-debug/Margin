package com.Margin.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Margin.app.data.local.entity.TaskType
import com.Margin.app.ui.components.AddTaskSheet
import com.Margin.app.ui.theme.*
import com.Margin.app.ui.viewmodel.AttendanceViewModel
import com.Margin.app.ui.viewmodel.SubjectOverviewUi
import com.Margin.app.ui.viewmodel.TaskUi
import com.Margin.app.ui.viewmodel.TaskViewModel
import com.Margin.app.utils.getAppViewModel
import java.util.Locale

fun taskAccentColor(type: TaskType): Color = when (type) {
    TaskType.ASSIGNMENT   -> BlueAccent
    TaskType.PRESENTATION -> Magenta
    TaskType.PRACTICAL    -> Yellow
    TaskType.STUDY_GOAL   -> AttendGreen
    TaskType.OTHER        -> AttendGrey
}

enum class StatusFilter { ALL, PENDING, COMPLETED }

@Composable
fun TaskListScreen(
    initialType: TaskType? = null,
    onBack: (() -> Unit)? = null,
    viewModel: TaskViewModel = getAppViewModel(),
    attendanceViewModel: AttendanceViewModel = getAppViewModel()
) {
    val context = LocalContext.current
    val allTasks by viewModel.allTasks.collectAsState()
    val subjects by attendanceViewModel.subjectOverviews.collectAsState()
    val analytics by attendanceViewModel.analytics.collectAsState()

    var selectedMainTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(initialType) }
    var selectedStatus by remember { mutableStateOf(StatusFilter.ALL) }
    var showAddSheet by remember { mutableStateOf(false) }

    val filteredTasks = allTasks.filter { task ->
        val matchesCategory = selectedCategory == null || task.type == selectedCategory
        val matchesStatus = when (selectedStatus) {
            StatusFilter.ALL -> true
            StatusFilter.PENDING -> !task.isCompleted
            StatusFilter.COMPLETED -> task.isCompleted
        }
        val matchesSearch = searchQuery.isBlank() ||
            task.title.contains(searchQuery, true) ||
            task.subjectName.contains(searchQuery, true) ||
            task.subjectCode.contains(searchQuery, true)

        matchesCategory && matchesStatus && matchesSearch
    }

    val pendingCount = allTasks.count { !it.isCompleted }
    val completedCount = allTasks.count { it.isCompleted }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (selectedMainTab == 0) "Reminders & Tasks" else "Subject Attendance",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            if (selectedMainTab == 0) "$pendingCount pending  •  $completedCount completed"
                            else "${subjects.size} subjects  •  1-tap + and - control",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Main Tab Switcher
            item {
                TabRow(
                    selectedTabIndex = selectedMainTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = NeonTeal,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedMainTab]),
                            color = NeonTeal
                        )
                    },
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 10.dp)
                ) {
                    Tab(
                        selected = selectedMainTab == 0,
                        onClick = { selectedMainTab = 0 },
                        text = {
                            Text(
                                "Tasks ($pendingCount)",
                                fontWeight = if (selectedMainTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedMainTab == 1,
                        onClick = { selectedMainTab = 1 },
                        text = {
                            Text(
                                "All Subjects (${subjects.size})",
                                fontWeight = if (selectedMainTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            if (selectedMainTab == 0) {
                // ── TAB 0: DELIVERABLES & TASKS ──────────────────────────────────
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("Search assignments, labs...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        singleLine = true
                    )
                }

                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 10.dp)
                    ) {
                        item {
                            FilterPill("All", selectedStatus == StatusFilter.ALL) { selectedStatus = StatusFilter.ALL }
                        }
                        item {
                            FilterPill("Pending", selectedStatus == StatusFilter.PENDING) { selectedStatus = StatusFilter.PENDING }
                        }
                        item {
                            FilterPill("Completed", selectedStatus == StatusFilter.COMPLETED) { selectedStatus = StatusFilter.COMPLETED }
                        }
                    }
                }

                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        item {
                            FilterPill("All Types", selectedCategory == null) { selectedCategory = null }
                        }
                        items(TaskType.entries) { type ->
                            FilterPill(type.displayName, selectedCategory == type) {
                                selectedCategory = if (selectedCategory == type) null else type
                            }
                        }
                    }
                }

                if (filteredTasks.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.AssignmentTurnedIn, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(10.dp))
                                Text("No tasks found", color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                } else {
                    items(filteredTasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onToggle = { viewModel.toggleTaskComplete(context, task) },
                            onDelete = { viewModel.deleteTask(context, task) }
                        )
                    }
                }
            } else {
                // ── TAB 1: ALL SUBJECTS ATTENDANCE CONTROLS ─────────────────────
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Semester Attendance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${analytics.totalAttended} of ${analytics.totalClasses} classes attended • ${analytics.totalBunks} bunks",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (analytics.overallPercentage >= 75f) {
                                    Text("Safe Bunks: ${analytics.safeBunksLeft}", color = AttendGreen, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                } else {
                                    Text("Need ${analytics.classesNeeded} classes to 75%", color = AttendRed, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }

                            val overallColor = if (analytics.overallPercentage >= 75f) NeonTeal else AttendRed
                            Text(
                                String.format(Locale.US, "%.1f%%", analytics.overallPercentage),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = overallColor
                            )
                        }
                    }
                }

                if (subjects.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.School, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(10.dp))
                                Text("No subjects found", color = MaterialTheme.colorScheme.outlineVariant)
                                Text("Add subjects or import timetable via Squad Sync", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                } else {
                    items(subjects, key = { it.id }) { sub ->
                        SubjectAttendanceCard(
                            subject = sub,
                            isMostBunked = sub.name == analytics.mostBunkedSubjectName || sub.code == analytics.mostBunkedSubjectName,
                            isHeaviestWorkload = sub.name == analytics.heaviestWorkloadSubjectName || sub.code == analytics.heaviestWorkloadSubjectName,
                            onIncrementPresent = { attendanceViewModel.incrementPresent(sub.id) },
                            onIncrementBunk = { attendanceViewModel.incrementBunk(sub.id) },
                            onUndo = { attendanceViewModel.decrementAttendance(sub.id) }
                        )
                    }
                }
            }
        }

        // FAB only in Tasks view
        if (selectedMainTab == 0) {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 20.dp),
                containerColor = NeonTeal,
                contentColor = Color(0xFF003D35),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Task")
            }
        }
    }

    if (showAddSheet) {
        AddTaskSheet(onDismiss = { showAddSheet = false }, taskViewModel = viewModel)
    }
}

@Composable
private fun FilterPill(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) NeonTeal else MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, if (isSelected) NeonTeal else MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color(0xFF003D35) else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SubjectAttendanceCard(
    subject: SubjectOverviewUi,
    isMostBunked: Boolean,
    isHeaviestWorkload: Boolean,
    onIncrementPresent: () -> Unit,
    onIncrementBunk: () -> Unit,
    onUndo: () -> Unit
) {
    val barColor = when {
        subject.percentage >= 75f -> AttendGreen
        subject.percentage >= 65f -> AttendOrange
        else -> AttendRed
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, if (subject.percentage < 75f && subject.total > 0) AttendRed.copy(0.4f) else MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(color = NeonTealAlpha20, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            subject.code,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonTeal,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    String.format(Locale.US, "%.1f%%", subject.percentage),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = barColor
                )
            }

            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { (subject.percentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surface,
                strokeCap = StrokeCap.Round
            )

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${subject.attended} / ${subject.total} attended",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                if (subject.percentage >= 75f) {
                    Text(
                        "Safe margin: ${subject.safeBunks} bunks",
                        color = AttendGreen,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                } else if (subject.total > 0) {
                    Text(
                        "Need: ${subject.neededToRecover} consecutive",
                        color = AttendRed,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Badges (Wall of Shame / Heavy Workload)
            if (isMostBunked || isHeaviestWorkload) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isMostBunked) {
                        Surface(color = AttendOrange.copy(0.15f), shape = RoundedCornerShape(6.dp)) {
                            Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = AttendOrange, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Wall of Shame", color = AttendOrange, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (isHeaviestWorkload) {
                        Surface(color = Magenta.copy(0.15f), shape = RoundedCornerShape(6.dp)) {
                            Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = Magenta, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Heavy Load", color = Magenta, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
            Spacer(Modifier.height(8.dp))

            // Stepper buttons (+ Present, - Bunk, Undo)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onIncrementBunk,
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = AttendRed.copy(0.12f),
                        contentColor = AttendRed
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Bunk", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("- Bunk", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onIncrementPresent,
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AttendGreen.copy(0.2f),
                        contentColor = AttendGreen
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Present", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("+ Present", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = onUndo,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo Last Log",
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: TaskUi,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = taskAccentColor(task.type)

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, if (task.isOverdue) AttendRed.copy(0.4f) else MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (task.isCompleted) NeonTeal else Color.Transparent)
                    .border(1.5.dp, if (task.isCompleted) NeonTeal else MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF003D35), modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.onBackground,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = accent.copy(0.15f), shape = RoundedCornerShape(4.dp)) {
                        Text(task.subjectCode, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp), style = MaterialTheme.typography.labelSmall, color = accent, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(task.subjectName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        task.dueDateFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (task.isOverdue) AttendRed else MaterialTheme.colorScheme.outlineVariant,
                        fontWeight = if (task.isOverdue) FontWeight.Bold else FontWeight.Normal
                    )
                    if (task.hasReminder) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Filled.Alarm, contentDescription = "Alarm", tint = Magenta, modifier = Modifier.size(12.dp))
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = AttendRed.copy(0.7f), modifier = Modifier.size(20.dp))
            }
        }
    }
}
