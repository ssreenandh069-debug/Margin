package com.Margin.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Margin.app.data.local.entity.TaskType
import com.Margin.app.ui.components.AddTaskSheet
import com.Margin.app.ui.components.MarginTopBar
import com.Margin.app.ui.components.SemesterOverviewSheet
import com.Margin.app.ui.theme.*
import com.Margin.app.ui.viewmodel.AttendanceViewModel
import com.Margin.app.ui.viewmodel.TaskUi
import com.Margin.app.ui.viewmodel.TaskViewModel
import com.Margin.app.utils.getAppViewModel

@Composable
fun TaskListScreen(
    viewModel: TaskViewModel = getAppViewModel(),
    attendanceViewModel: AttendanceViewModel = getAppViewModel()
) {
    val context = LocalContext.current
    val allTasks by viewModel.allTasks.collectAsState()
    val activeSession by attendanceViewModel.activeSession.collectAsState()

    var selectedCategoryFilter by remember { mutableStateOf<TaskType?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showSemesterSheet by remember { mutableStateOf(false) }

    val pendingCount = allTasks.count { !it.isCompleted }
    val urgentCount = allTasks.count { !it.isCompleted && (it.isOverdue || it.dueDateMs - System.currentTimeMillis() < 86400000L * 2) }

    val filteredTasks = remember(allTasks, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) allTasks
        else allTasks.filter { it.type == selectedCategoryFilter }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Unified Top Bar
            item {
                MarginTopBar(
                    screenTitle = "Tasks",
                    semesterName = activeSession?.name,
                    onSemesterClick = { showSemesterSheet = true },
                    onProfileClick = { showSemesterSheet = true }
                )
            }

            // Submissions Overview Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    border = BorderStroke(1.dp, SlatePillBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Checklist,
                                    contentDescription = null,
                                    tint = NeonTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Submissions Overview",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeonTealAlpha20)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "ACTIVE TERM",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NeonTeal
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Metric Split Boxes
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Queue Box
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SlatePill)
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "QUEUE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextMuted,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = "$pendingCount",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = TextPrimary
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "Pending",
                                            fontSize = 13.sp,
                                            color = TextSecondary,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Urgent Box
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (urgentCount > 0) AttendRedAlpha20 else SlatePill)
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "URGENT",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (urgentCount > 0) AttendRed else TextMuted,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = "$urgentCount",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (urgentCount > 0) AttendRed else TextPrimary
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "Due Soon",
                                            fontSize = 13.sp,
                                            color = if (urgentCount > 0) AttendRed else TextSecondary,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Filter Chips: [All] [Assignments] [Lab Journals] [Projects]
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterPillChip(
                            label = "All (${allTasks.size})",
                            isSelected = selectedCategoryFilter == null,
                            onClick = { selectedCategoryFilter = null }
                        )
                    }

                    item {
                        FilterPillChip(
                            label = "Assignments",
                            isSelected = selectedCategoryFilter == TaskType.ASSIGNMENT,
                            onClick = { selectedCategoryFilter = TaskType.ASSIGNMENT }
                        )
                    }

                    item {
                        FilterPillChip(
                            label = "Lab Journals",
                            isSelected = selectedCategoryFilter == TaskType.PRACTICAL,
                            onClick = { selectedCategoryFilter = TaskType.PRACTICAL }
                        )
                    }

                    item {
                        FilterPillChip(
                            label = "Presentations",
                            isSelected = selectedCategoryFilter == TaskType.PRESENTATION,
                            onClick = { selectedCategoryFilter = TaskType.PRESENTATION }
                        )
                    }

                    item {
                        FilterPillChip(
                            label = "Study Goals",
                            isSelected = selectedCategoryFilter == TaskType.STUDY_GOAL,
                            onClick = { selectedCategoryFilter = TaskType.STUDY_GOAL }
                        )
                    }
                }
            }

            // Task Cards List
            if (filteredTasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.TaskAlt,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "No tasks in this category",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
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

            // Add Work / Assignment Button
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = { showAddSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonTeal,
                            contentColor = Color(0xFF0C131D)
                        )
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Add Work / Assignment",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddTaskSheet(
            onDismiss = { showAddSheet = false },
            taskViewModel = viewModel
        )
    }

    if (showSemesterSheet) {
        SemesterOverviewSheet(
            onDismiss = { showSemesterSheet = false },
            viewModel = attendanceViewModel,
            onCreateNewSemester = { /* handeled inside */ }
        )
    }
}

@Composable
private fun FilterPillChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) SlatePillBorder else SlatePill)
            .border(1.dp, if (isSelected) NeonTeal else SlatePillBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) TextPrimary else TextSecondary
        )
    }
}

@Composable
private fun TaskCard(
    task: TaskUi,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val isDueSoon = task.isOverdue || task.dueDateMs - System.currentTimeMillis() < 86400000L * 2

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, SlatePillBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Top Row: Code + Due Date + Category Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Subject Code
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SlatePill)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = task.subjectCode,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }

                    // Due Date Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDueSoon) AttendRedAlpha20 else SlatePill)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccessTime,
                                contentDescription = null,
                                tint = if (isDueSoon) AttendRed else TextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = task.dueDateFormatted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDueSoon) AttendRed else TextSecondary
                            )
                        }
                    }
                }

                // Category Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SlatePill)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = task.type.displayName.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Task Title
            Text(
                text = task.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (task.isCompleted) TextMuted else TextPrimary,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
            )

            Spacer(Modifier.height(4.dp))

            // Subject Name
            Text(
                text = task.subjectName,
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(Modifier.height(14.dp))

            // Action Row: Delete & Mark Done
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete Task",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Mark Done Pill Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (task.isCompleted) SlatePill else NeonTeal)
                        .border(
                            1.dp,
                            if (task.isCompleted) SlatePillBorder else NeonTeal,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable(onClick = onToggle)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (task.isCompleted) Icons.Filled.Check else Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = if (task.isCompleted) NeonTeal else Color(0xFF0C131D),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (task.isCompleted) "Completed" else "Mark Done",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (task.isCompleted) TextSecondary else Color(0xFF0C131D)
                        )
                    }
                }
            }
        }
    }
}
