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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Margin.app.domain.AttendanceMath
import com.Margin.app.ui.components.CreateSessionSheet
import com.Margin.app.ui.components.ExtraDutySheet
import com.Margin.app.ui.components.LogSeminarSheet
import com.Margin.app.ui.components.SemesterOverviewSheet
import com.Margin.app.ui.components.SwapClassSheet
import com.Margin.app.ui.theme.*
import com.Margin.app.ui.viewmodel.AttendanceViewModel
import com.Margin.app.ui.viewmodel.TodayClassUi
import com.Margin.app.utils.getAppViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackScreen(
    viewModel: AttendanceViewModel = getAppViewModel()
) {
    val todayClasses by viewModel.todayClasses.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val allSessions by viewModel.allSessions.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }

    var showSemesterSheet by remember { mutableStateOf(false) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var showSeminarSheet by remember { mutableStateOf(false) }
    var swapTargetClass by remember { mutableStateOf<TodayClassUi?>(null) }

    var dutySubjectId by remember { mutableStateOf<String?>(null) }
    var dutySubjectName by remember { mutableStateOf("") }

    val dateLabel = remember(selectedDate) {
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
        }
        if (selectedDate == todayCal.timeInMillis) "Today"
        else SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).format(Date(selectedDate))
    }

    val attendedCount  = todayClasses.count { it.status == "PRESENT" || it.status == "DUTY" }
    val bunkCount      = todayClasses.count { it.status == "BUNK" }
    val excusedCount   = todayClasses.count { it.status == "EXCUSED" }
    val proxyCount     = todayClasses.count { it.status == "PROXY" }
    val cancelledCount = todayClasses.count { it.status == "CANCELLED" }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = AttendanceMath.localToUtcMidnight(selectedDate)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        viewModel.selectDate(AttendanceMath.utcToLocalMidnight(it))
                    }
                    showDatePicker = false
                }) { Text("OK", color = NeonTeal) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color(0xFF0E0E1E), MaterialTheme.colorScheme.background)))
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)
                ) {
                    // Semester Pill
                    Surface(
                        color = NeonTealAlpha20,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clickable {
                            if (allSessions.isEmpty()) showCreateSheet = true
                            else showSemesterSheet = true
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.School, contentDescription = null, tint = NeonTeal, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                activeSession?.name ?: "Tap to Set Semester",
                                color = NeonTeal,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = NeonTeal, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Track Attendance", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(dateLabel, color = NeonTeal, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                if (dateLabel != "Today") {
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "• Return to Today",
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.clickable { viewModel.resetToToday() }
                                    )
                                }
                            }
                        }

                        IconButton(onClick = { showDatePicker = true }) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(NeonTealAlpha20, CircleShape)
                                    .border(1.dp, NeonTeal, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.CalendarMonth, contentDescription = "Date", tint = NeonTeal, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Day Off / Excused / Seminar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.markDayOff() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(Icons.Filled.EventBusy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Day Off", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.markDayExcused() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = AttendOrange.copy(0.12f), contentColor = AttendOrange)
                        ) {
                            Icon(Icons.Filled.PersonOff, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Excused", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { showSeminarSheet = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = PurpleAccent.copy(0.12f), contentColor = PurpleAccent)
                        ) {
                            Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Seminar", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Quick Stats
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatPill("Present", attendedCount, AttendGreen, AttendGreenAlpha20, Modifier.weight(1f))
                    StatPill("Bunk", bunkCount, AttendRed, AttendRedAlpha20, Modifier.weight(1f))
                    StatPill("Excused", excusedCount, AttendOrange, AttendOrange.copy(0.15f), Modifier.weight(1f))
                    StatPill("Proxy", proxyCount, PurpleAccent, PurpleAlpha20, Modifier.weight(1f))
                    StatPill("Off", cancelledCount, AttendGrey, AttendGreyAlpha20, Modifier.weight(1f))
                }
            }

            // Class Cards Header
            item {
                Text(
                    if (dateLabel == "Today") "Today's Schedule" else "Classes on $dateLabel",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (todayClasses.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.EventAvailable, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("No classes scheduled for $dateLabel", color = MaterialTheme.colorScheme.outlineVariant, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else {
                items(todayClasses, key = { it.timetableId }) { cls ->
                    TodayClassCard(
                        cls = cls,
                        onStatusChange = { newStatus ->
                            viewModel.markAttendance(cls.subjectId, cls.timetableId, cls.status, newStatus)
                        },
                        onOpenDuty = {
                            dutySubjectId = cls.subjectId
                            dutySubjectName = cls.subjectName
                        },
                        onSwap = {
                            swapTargetClass = cls
                        }
                    )
                }
            }
        }
    }

    if (showSemesterSheet) {
        SemesterOverviewSheet(
            onDismiss = { showSemesterSheet = false },
            viewModel = viewModel,
            onCreateNewSemester = { showCreateSheet = true }
        )
    }

    if (showCreateSheet) {
        CreateSessionSheet(
            onDismiss = { showCreateSheet = false },
            viewModel = viewModel
        )
    }

    if (showSeminarSheet) {
        LogSeminarSheet(
            onDismiss = { showSeminarSheet = false },
            viewModel = viewModel
        )
    }

    swapTargetClass?.let { target ->
        SwapClassSheet(
            slotId = target.timetableId,
            currentSubjectId = target.subjectId,
            currentSubjectCode = target.subjectCode,
            currentSubjectName = target.subjectName,
            time = target.time,
            viewModel = viewModel,
            onDismiss = { swapTargetClass = null }
        )
    }

    dutySubjectId?.let { sid ->
        ExtraDutySheet(
            subjectId = sid,
            subjectName = dutySubjectName,
            viewModel = viewModel,
            onDismiss = { dutySubjectId = null }
        )
    }
}

@Composable
private fun StatPill(label: String, count: Int, color: Color, bg: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Column(modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$count", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(0.8f))
        }
    }
}

@Composable
private fun TodayClassCard(
    cls: TodayClassUi,
    onStatusChange: (String) -> Unit,
    onOpenDuty: () -> Unit,
    onSwap: () -> Unit
) {
    val pct = cls.attendancePct
    val isWarning = cls.total > 0 && pct < 75f

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, if (isWarning) AttendRed.copy(0.4f) else MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = NeonTealAlpha20, shape = RoundedCornerShape(6.dp)) {
                            Text(cls.subjectCode, modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = NeonTeal, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Filled.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(cls.time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outlineVariant)
                        if (cls.room != "TBD") {
                            Text("  •  ${cls.room}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(cls.subjectName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                }

                val pctColor = when { pct >= 80f -> NeonTeal; pct >= 75f -> AttendGreen; pct >= 65f -> AttendOrange; else -> AttendRed }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onSwap, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.SwapHoriz, contentDescription = "Swap Class", tint = NeonTeal, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        String.format(Locale.US, "%.1f%%", pct),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = pctColor
                    )
                }
            }

            if (cls.note.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(PurpleAlpha20)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(cls.note, color = PurpleAccent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                }
            }

            // Margin recovery / safe bunks helper banner
            if (cls.total > 0) {
                Spacer(Modifier.height(8.dp))
                if (isWarning) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(AttendRedAlpha20).padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = AttendRed, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Below 75%. Need ${cls.neededToRecover} consecutive classes.", color = AttendRed, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                } else if (cls.safeBunks > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(AttendGreenAlpha20).padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Shield, contentDescription = null, tint = AttendGreen, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Safe margin: can bunk ${cls.safeBunks} class${if (cls.safeBunks > 1) "es" else ""}.", color = AttendGreen, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
            Spacer(Modifier.height(10.dp))

            // Action Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ActionBtn("Present", Icons.Filled.Check, AttendGreen, cls.status == "PRESENT", Modifier.weight(1f)) { onStatusChange("PRESENT") }
                ActionBtn("Bunk", Icons.Filled.Close, AttendRed, cls.status == "BUNK", Modifier.weight(1f)) { onStatusChange("BUNK") }
                ActionBtn("Off", Icons.Filled.Remove, AttendGrey, cls.status == "CANCELLED", Modifier.weight(1f)) { onStatusChange("CANCELLED") }
                ActionBtn("Proxy", Icons.Filled.PersonSearch, PurpleAccent, cls.status == "PROXY", Modifier.weight(1f)) { onStatusChange("PROXY") }
                ActionBtn("Duty", Icons.Filled.Stars, Yellow, cls.status == "DUTY", Modifier.weight(1f), onClick = onOpenDuty)
            }
        }
    }
}

@Composable
private fun ActionBtn(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    isSelected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (isSelected) color else color.copy(0.12f))
                .border(1.5.dp, if (isSelected) color else color.copy(0.25f), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = if (isSelected) Color.White else color, modifier = Modifier.size(18.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (isSelected) color else MaterialTheme.colorScheme.outlineVariant)
    }
}
