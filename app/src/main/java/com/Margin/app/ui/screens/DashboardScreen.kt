package com.Margin.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Margin.app.ui.components.AddTaskSheet
import com.Margin.app.ui.components.CreateSessionSheet
import com.Margin.app.ui.theme.*
import com.Margin.app.ui.viewmodel.SessionViewModel
import com.Margin.app.ui.viewmodel.TaskListState
import com.Margin.app.ui.viewmodel.TaskViewModel
import com.Margin.app.utils.getAppViewModel

data class DashboardTaskOverview(val label: String, val count: Int, val colorType: String)

data class DashboardSubjectOverview(
    val id: String,
    val name: String,
    val code: String,
    val teacher: String,
    val totalClasses: Int,
    val attendedClasses: Int,
    val percentage: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    isDarkTheme: Boolean = true,
    onToggleTheme: () -> Unit = {},
    onOpenSessions: () -> Unit = {},
    onOpenAssignments: () -> Unit = {},
    onOpenPresentations: () -> Unit = {},
    onOpenPracticals: () -> Unit = {},
    onOpenTimetable: () -> Unit = {},
    sessionViewModel: SessionViewModel = getAppViewModel(),
    taskViewModel: TaskViewModel = getAppViewModel()
) {
    var selectedPeriod by remember { mutableStateOf(0) } // 0=Month, 1=Semester
    var showCreateSession by remember { mutableStateOf(false) }
    var showAddTask by remember { mutableStateOf(false) }

    val activeSession by sessionViewModel.activeSession.collectAsState()
    val subjects by sessionViewModel.activeSubjects.collectAsState()

    val assignmentsState by taskViewModel.getTasks("ASSIGNMENT").collectAsState()
    val presentationsState by taskViewModel.getTasks("PRESENTATION").collectAsState()
    val practicalsState by taskViewModel.getTasks("PRACTICAL").collectAsState()

    val assignments = (assignmentsState as? TaskListState.Success)?.tasks ?: emptyList()
    val presentations = (presentationsState as? TaskListState.Success)?.tasks ?: emptyList()
    val practicals = (practicalsState as? TaskListState.Success)?.tasks ?: emptyList()

    val pendingAssignments = assignments.count { !it.isCompleted }
    val pendingPresentations = presentations.count { !it.isCompleted }
    val pendingPracticals = practicals.count { !it.isCompleted }

    val computedPendingTasks = listOf(
        DashboardTaskOverview(label = "Assignments", count = pendingAssignments, colorType = "blue"),
        DashboardTaskOverview(label = "Presentations", count = pendingPresentations, colorType = "pink"),
        DashboardTaskOverview(label = "Practical Files", count = pendingPracticals, colorType = "yellow")
    ).filter { it.count > 0 }

    val isLoadingTasks = assignmentsState is TaskListState.Loading || 
                         presentationsState is TaskListState.Loading || 
                         practicalsState is TaskListState.Loading

    val overallAttendance by sessionViewModel.overallAttendanceProgress.collectAsState()
    val totalClasses by sessionViewModel.totalTrackedClasses.collectAsState()
    val presentClasses by sessionViewModel.totalPresentClasses.collectAsState()
    val subjectList by sessionViewModel.dashboardSubjects.collectAsState()

    val currentSemesterName = activeSession?.name ?: "No Active Session"

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                // ── Header ────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Margin", 
                            style = MaterialTheme.typography.headlineLarge, 
                            color = NeonTeal, 
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text("$currentSemesterName Overview", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Sun / Moon theme toggle
                        IconButton(onClick = onToggleTheme) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode",
                                tint = NeonTeal
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(NeonTealAlpha20, CircleShape)
                                .border(1.dp, NeonTeal, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = NeonTeal, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }

            item {
                // ── Big Progress + Toggle Card ─────────────────────
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Circular progress
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(110.dp)) {
                                CircularProgressIndicator(
                                    progress = { overallAttendance / 100f },
                                    modifier = Modifier.size(110.dp),
                                    color = NeonTeal,
                                    strokeWidth = 8.dp,
                                    trackColor = MaterialTheme.colorScheme.outline,
                                    strokeCap = StrokeCap.Round
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        String.format(java.util.Locale.US, "%.2f%%", overallAttendance),
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = NeonTeal
                                    )
                                    Text("Overall", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Spacer(Modifier.width(20.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Total Attendance", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(12.dp))
                                // Segmented control
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(4.dp)
                                ) {
                                    listOf("Month", "Semester").forEachIndexed { idx, label ->
                                        val selected = selectedPeriod == idx
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(9.dp))
                                                .background(if (selected) NeonTeal else Color.Transparent)
                                                .clickable { selectedPeriod = idx }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                label,
                                                fontSize = 12.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selected) Color(0xFF003D35) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                                // Stats row
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    MiniStat("Classes", "$totalClasses", NeonTeal)
                                    MiniStat("Present", "$presentClasses", AttendGreen)
                                }
                            }
                        }
                    }
                }
            }

            item {
                // ── Current Session Card ───────────────────────────
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .clickable { onOpenSessions() }
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(NeonTealAlpha20, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = NeonTeal, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Current Session", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(currentSemesterName, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                        }
                        Surface(
                            color = NeonTealAlpha20,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Active", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall, color = NeonTeal)
                        }
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(16.dp))
                    }
                }
            }

            item {
                // ── Pending Work ─────────────────────────────────
                Spacer(Modifier.height(8.dp))
                Text(
                    "Pending Work",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    if (isLoadingTasks) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = NeonTeal, modifier = Modifier.size(24.dp))
                        }
                    } else if (computedPendingTasks.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No pending tasks!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    } else {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            computedPendingTasks.forEachIndexed { idx, task ->
                                val (color, alpha, icon) = when (task.colorType) {
                                    "pink" -> Triple(Magenta, MagentaAlpha20, Icons.Filled.Slideshow)
                                    "blue" -> Triple(BlueAccent, BlueAlpha20, Icons.Filled.Assignment)
                                    else -> Triple(Yellow, YellowAlpha20, Icons.Filled.MenuBook)
                                }
                                val taskNav: () -> Unit = when (task.colorType) {
                                    "pink" -> onOpenPresentations
                                    "blue" -> onOpenAssignments
                                    else   -> onOpenPracticals
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { taskNav() }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(alpha, RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Text(task.label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(color, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${task.count}", color = MaterialTheme.colorScheme.background, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(16.dp))
                                }
                                if (idx < computedPendingTasks.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                                }
                            }
                        }
                    }
                }
            }

            item {
                // ── Subject List ─────────────────────────────────
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Subjects", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                    TextButton(onClick = { onOpenTimetable() }) {
                        Text("Timetable →", color = NeonTeal, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            items(subjectList, key = { it.id }) { subject ->
                SubjectProgressRow(subject)
            }
        }

        // ── FAB ────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmallFloatingActionButton(
                onClick = { showAddTask = true },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = Yellow,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Task")
            }
            ExtendedFloatingActionButton(
                onClick = { showCreateSession = true },
                containerColor = NeonTeal,
                contentColor = Color(0xFF003D35),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.CloudUpload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save to Cloud", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showCreateSession) {
        CreateSessionSheet(onDismiss = { showCreateSession = false })
    }
    if (showAddTask) {
        AddTaskSheet(onDismiss = { showAddTask = false })
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color) {
    Column {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SubjectProgressRow(subject: DashboardSubjectOverview) {
    val pct = subject.percentage
    val barColor = when {
        pct >= 85f -> NeonTeal
        pct >= 60f -> AttendOrange
        else -> AttendRed
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(subject.code, style = MaterialTheme.typography.labelMedium, color = NeonTeal)
                    Text(subject.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                }
                Text(
                    String.format(java.util.Locale.US, "%.2f%%", pct),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = barColor
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { pct / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.outline,
                strokeCap = StrokeCap.Round
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "${subject.attendedClasses}/${subject.totalClasses} classes",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}
