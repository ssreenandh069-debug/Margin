package com.Margin.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Margin.app.data.local.entity.TimetableEntity
import com.Margin.app.domain.AttendanceMath
import com.Margin.app.ui.components.AddClassSheet
import com.Margin.app.ui.components.MarginTopBar
import com.Margin.app.ui.components.SemesterOverviewSheet
import com.Margin.app.ui.components.SquadSyncSheet
import com.Margin.app.ui.theme.*
import com.Margin.app.ui.viewmodel.AttendanceViewModel
import com.Margin.app.ui.viewmodel.SubjectOverviewUi
import com.Margin.app.utils.getAppViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

enum class SubjectMarginFilter { ALL, NEEDS_CARE, SAFE }

@Composable
fun TimetableScreen(
    viewModel: AttendanceViewModel = getAppViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    val activeSession by viewModel.activeSession.collectAsState()
    val subjects by viewModel.activeSubjects.collectAsState()
    val subjectOverviews by viewModel.subjectOverviews.collectAsState()

    var selectedDayOfWeek by remember { mutableIntStateOf(1) } // 1=Mon … 6=Sat, 7=Sun
    var showAddClass by remember { mutableStateOf(false) }
    var showSquadSync by remember { mutableStateOf(false) }
    var showSemesterSheet by remember { mutableStateOf(false) }

    var selectedMarginFilter by remember { mutableStateOf(SubjectMarginFilter.ALL) }

    val timetableForDay by viewModel.getTimetableForDay(selectedDayOfWeek).collectAsState()

    val weekDays = listOf(
        Triple("MON", 1, "Monday"),
        Triple("TUE", 2, "Tuesday"),
        Triple("WED", 3, "Wednesday"),
        Triple("THU", 4, "Thursday"),
        Triple("FRI", 5, "Friday"),
        Triple("SAT", 6, "Saturday")
    )

    val currentDayInfo = weekDays.find { it.second == selectedDayOfWeek } ?: weekDays.first()

    val needsCareCount = subjectOverviews.count { it.percentage < 75f && it.total > 0 }
    val safeCount = subjectOverviews.count { it.percentage >= 75f || it.total == 0 }

    val filteredSubjects = remember(subjectOverviews, selectedMarginFilter) {
        when (selectedMarginFilter) {
            SubjectMarginFilter.ALL -> subjectOverviews
            SubjectMarginFilter.NEEDS_CARE -> subjectOverviews.filter { it.percentage < 75f && it.total > 0 }
            SubjectMarginFilter.SAFE -> subjectOverviews.filter { it.percentage >= 75f || it.total == 0 }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importTimetableFromUri(context, it) { success, message ->
                scope.launch { snackbarHost.showSnackbar(message) }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        containerColor = DarkBg
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(DarkBg)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 36.dp)
            ) {
                // Unified Top Bar
                item {
                    MarginTopBar(
                        screenTitle = "Timetable",
                        semesterName = activeSession?.name,
                        onSemesterClick = { showSemesterSheet = true },
                        onProfileClick = { showSemesterSheet = true }
                    )
                }

                // Week Range Header (< Week X • Date Range >)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SlatePill)
                                .clickable {
                                    selectedDayOfWeek = if (selectedDayOfWeek > 1) selectedDayOfWeek - 1 else 6
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ChevronLeft,
                                contentDescription = "Previous Day",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DateRange,
                                contentDescription = null,
                                tint = NeonTeal,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Weekly Routine • Term Schedule",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SlatePill)
                                .clickable {
                                    selectedDayOfWeek = if (selectedDayOfWeek < 6) selectedDayOfWeek + 1 else 1
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = "Next Day",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // 6-Day Strip (Mon to Sat)
                item {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(weekDays) { day ->
                            val isSelected = day.second == selectedDayOfWeek
                            Box(
                                modifier = Modifier
                                    .width(50.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) NeonTeal else CardDark)
                                    .border(
                                        1.dp,
                                        if (isSelected) NeonTeal else SlatePillBorder,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { selectedDayOfWeek = day.second }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = day.first,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFF0C131D) else TextMuted
                                    )
                                    Text(
                                        text = "Day ${day.second}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) Color(0xFF0C131D) else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Routine Header: "[DayName] Routine [X Classes]"
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${currentDayInfo.third} Routine",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SlatePill)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "${timetableForDay.size} Classes",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                            }
                        }

                        Text(
                            text = if (timetableForDay.isNotEmpty()) "Scheduled" else "Off Day",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (timetableForDay.isNotEmpty()) NeonTeal else TextMuted
                        )
                    }
                }

                // Routine Timeline List
                if (timetableForDay.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardDark),
                            border = BorderStroke(1.dp, SlatePillBorder)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Filled.EventBusy,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "No classes scheduled on ${currentDayInfo.third}",
                                        fontSize = 13.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(timetableForDay, key = { it.id }) { entry ->
                        val sub = subjects.find { it.id == entry.subjectId }
                        val overview = subjectOverviews.find { it.id == entry.subjectId }

                        RoutineTimelineCard(
                            entry = entry,
                            code = sub?.code ?: "SUB",
                            name = sub?.name ?: "Subject",
                            overview = overview,
                            onDelete = { viewModel.removeClass(entry.id) }
                        )
                    }
                }

                // Add Extra Class / Reschedule Button
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Button(
                            onClick = { showAddClass = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SlatePill,
                                contentColor = NeonTeal
                            ),
                            border = BorderStroke(1.dp, SlatePillBorder)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Add Extra Class / Reschedule",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Subject Margins Section Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Subject Margins (${subjectOverviews.size} Courses)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Text(
                            text = "GOAL: 75% MIN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Filter Chips Row: [All (X)] [Needs Care (Y)] [Safe (Z)]
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterPillChip(
                            label = "All (${subjectOverviews.size})",
                            isSelected = selectedMarginFilter == SubjectMarginFilter.ALL,
                            onClick = { selectedMarginFilter = SubjectMarginFilter.ALL }
                        )

                        FilterPillChip(
                            label = "Needs Care ($needsCareCount)",
                            isSelected = selectedMarginFilter == SubjectMarginFilter.NEEDS_CARE,
                            onClick = { selectedMarginFilter = SubjectMarginFilter.NEEDS_CARE }
                        )

                        FilterPillChip(
                            label = "Safe ($safeCount)",
                            isSelected = selectedMarginFilter == SubjectMarginFilter.SAFE,
                            onClick = { selectedMarginFilter = SubjectMarginFilter.SAFE }
                        )
                    }
                }

                // Subject Cards List
                items(filteredSubjects, key = { it.id }) { sub ->
                    SubjectMarginCard(
                        sub = sub,
                        onIncrementPresent = { viewModel.incrementPresent(sub.id) },
                        onIncrementBunk = { viewModel.incrementBunk(sub.id) },
                        onDecrement = { viewModel.decrementAttendance(sub.id) }
                    )
                }
            }
        }
    }

    if (showAddClass) {
        AddClassSheet(
            selectedDayInt = selectedDayOfWeek,
            onDismiss = { showAddClass = false },
            viewModel = viewModel
        )
    }

    if (showSquadSync) {
        SquadSyncSheet(
            onDismiss = { showSquadSync = false },
            viewModel = viewModel
        )
    }

    if (showSemesterSheet) {
        SemesterOverviewSheet(
            onDismiss = { showSemesterSheet = false },
            viewModel = viewModel,
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
private fun RoutineTimelineCard(
    entry: TimetableEntity,
    code: String,
    name: String,
    overview: SubjectOverviewUi?,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, SlatePillBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Mint Accent Line
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(NeonTeal)
            )

            Spacer(Modifier.width(14.dp))

            // Time Block
            Column(
                modifier = Modifier.width(60.dp)
            ) {
                Text(
                    text = entry.startTime,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonTeal
                )
                Text(
                    text = if (entry.room.isNotBlank()) entry.room else "Room TBD",
                    fontSize = 11.sp,
                    color = TextMuted,
                    maxLines = 1
                )
            }

            Spacer(Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1
                    )
                }

                Spacer(Modifier.height(3.dp))

                val subDetail = if (overview != null && overview.total > 0) {
                    if (overview.percentage >= 75f) "${overview.safeBunks} safe bunks left"
                    else "Need ${overview.neededToRecover} consecutive classes"
                } else {
                    code
                }

                Text(
                    text = "$code • $subDetail",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            // Delete Icon
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Remove Slot",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SubjectMarginCard(
    sub: SubjectOverviewUi,
    onIncrementPresent: () -> Unit,
    onIncrementBunk: () -> Unit,
    onDecrement: () -> Unit
) {
    val pct = sub.percentage
    val isDanger = sub.total > 0 && pct < 75f
    val isBorderline = sub.total > 0 && pct in 75f..78.9f
    val isComfortable = sub.total > 0 && pct >= 79f

    val statusColor = when {
        isDanger -> AttendRed
        isBorderline -> Yellow
        else -> NeonTeal
    }

    val statusTag = when {
        isDanger -> "Deficit"
        isBorderline -> "Borderline"
        isComfortable -> "Comfortable"
        else -> "New"
    }

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
            // Code • Tag and Attended Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sub.code,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = sub.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale.US, "%.1f%%", pct),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor
                    )
                    Text(
                        text = "${sub.attended}/${sub.total} attended",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = {
                    if (sub.total == 0) 1f else (pct / 100f).coerceIn(0f, 1f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = statusColor,
                trackColor = Color(0xFF1E2D3E),
            )

            Spacer(Modifier.height(10.dp))

            // Footer row with guidance notice and 1-tap quick adjustment buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isDanger) {
                        Text(
                            text = "! Attend next ${sub.neededToRecover} classes to reach 75%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AttendRed
                        )
                    } else if (sub.safeBunks > 0) {
                        Text(
                            text = "✓ ${sub.safeBunks} bunks safe",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonTeal
                        )
                    } else {
                        Text(
                            text = "0 bunks remaining (on margin)",
                            fontSize = 11.sp,
                            color = Yellow
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = statusTag,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }

                // Steppers: Present (+), Bunk (-), Undo
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeonTealAlpha20)
                            .clickable(onClick = onIncrementPresent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Present", tint = NeonTeal, modifier = Modifier.size(15.dp))
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(AttendRedAlpha20)
                            .clickable(onClick = onIncrementBunk),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "Add Bunk", tint = AttendRed, modifier = Modifier.size(15.dp))
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(SlatePill)
                            .clickable(onClick = onDecrement),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = TextMuted, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
