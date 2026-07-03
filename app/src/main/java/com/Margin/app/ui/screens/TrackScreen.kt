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
import com.Margin.app.ui.components.ExtraDutySheet
import com.Margin.app.ui.theme.*
import com.Margin.app.ui.viewmodel.TodayClassUiState
import com.Margin.app.ui.viewmodel.TrackViewModel
import com.Margin.app.utils.getAppViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackScreen(viewModel: TrackViewModel = getAppViewModel()) {
    val todayClasses      by viewModel.todayClassesUiState.collectAsState()
    val isCompleted       by viewModel.activeSessionIsCompleted.collectAsState()
    val selectedDate      by viewModel.selectedDate.collectAsState()

    var showDatePicker    by remember { mutableStateOf(false) }
    val datePickerState   = rememberDatePickerState(initialSelectedDateMillis = selectedDate)

    // Duty sheet state — null means closed; non-null holds the subject being edited
    var dutySheetSubjectId   by remember { mutableStateOf<String?>(null) }
    var dutySheetSubjectName by remember { mutableStateOf("") }

    val dateLabel = remember(selectedDate) {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis == todayCal.timeInMillis) "Today"
        else SimpleDateFormat("EEE, MMM dd yyyy", Locale.getDefault()).format(Date(selectedDate))
    }

    val attendedCount  = todayClasses.count { it.status == "PRESENT" }
    val bunkCount      = todayClasses.count { it.status == "BUNK" }
    val excusedCount   = todayClasses.count { it.status == "EXCUSED" }
    val cancelledCount = todayClasses.count { it.status == "CANCELLED" }
    val proxyCount     = todayClasses.count { it.status == "PROXY" }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.selectDate(it) }
                    showDatePicker = false
                }) { Text("OK", color = NeonTeal) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        ) { DatePicker(state = datePickerState) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {

            // ── Archived Banner ───────────────────────────────────
            if (isCompleted) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AttendOrange.copy(alpha = 0.15f))
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = AttendOrange, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "This semester is archived. Attendance tracking is locked.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AttendOrange,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            item {
                // ── Header ─────────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFF0E0E1E), MaterialTheme.colorScheme.background))
                        )
                        .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp)
                ) {
                    // Title row with calendar button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Attendance Track", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(dateLabel, style = MaterialTheme.typography.bodyMedium, color = NeonTeal)
                                if (dateLabel != "Today") {
                                    Spacer(Modifier.width(8.dp))
                                    TextButton(
                                        onClick = { viewModel.resetToToday() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("← Today", color = MaterialTheme.colorScheme.outlineVariant, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                        // Time Machine calendar icon
                        IconButton(onClick = { showDatePicker = true }) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(NeonTealAlpha20, CircleShape)
                                    .border(1.dp, NeonTeal, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.CalendarMonth, contentDescription = "Pick Date", tint = NeonTeal, modifier = Modifier.size(22.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Day action buttons (disabled when archived)
                    if (!isCompleted) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.markDayOff() },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.EventBusy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Day Off", fontSize = 13.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.markDayExcused() },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, AttendOrange.copy(alpha = 0.4f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = AttendOrange.copy(alpha = 0.12f),
                                    contentColor = AttendOrange
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.PersonOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Excused", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            item {
                // ── Daily Stats ────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DailyStat("Present",   attendedCount,  AttendGreen,   AttendGreenAlpha20,  Modifier.weight(1f))
                    DailyStat("Bunk",      bunkCount,      AttendRed,     AttendRedAlpha20,    Modifier.weight(1f))
                    DailyStat("Excused",   excusedCount,   AttendOrange,  AttendOrange.copy(alpha = 0.15f), Modifier.weight(1f))
                    DailyStat("Proxy",     proxyCount,     PurpleAccent,  PurpleAlpha20,       Modifier.weight(1f))
                    DailyStat("Cancelled", cancelledCount, AttendGrey,    AttendGreyAlpha20,   Modifier.weight(1f))
                }
            }

            item {
                Text(
                    if (dateLabel == "Today") "Today's Classes" else "Classes on $dateLabel",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (todayClasses.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("No classes scheduled", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }

            items(todayClasses, key = { it.timetableId }) { cls ->
                ClassCard(
                    cls        = cls,
                    isArchived = isCompleted,
                    onStatusChange = { status -> viewModel.markAttendance(cls.subjectId, status) },
                    onOpenDuty     = {
                        dutySheetSubjectId   = cls.subjectId
                        dutySheetSubjectName = cls.subjectName
                    }
                )
            }
        }

        // Duty sheet — rendered outside the LazyColumn so it can overlay freely
        dutySheetSubjectId?.let { sid ->
            ExtraDutySheet(
                subjectId   = sid,
                subjectName = dutySheetSubjectName,
                viewModel   = viewModel,
                onDismiss   = { dutySheetSubjectId = null }
            )
        }
    }
}

@Composable
private fun DailyStat(label: String, count: Int, color: Color, bgColor: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(6.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$count", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f), maxLines = 1)
        }
    }
}

@Composable
private fun ClassCard(
    cls: TodayClassUiState,
    isArchived: Boolean,
    onStatusChange: (String) -> Unit,
    onOpenDuty: () -> Unit
) {
    val pct     = cls.attendancePct
    val needed  = kotlin.math.ceil((0.75f * cls.total - cls.attended) / 0.25f).toInt()
    val showWarn = cls.total > 0 && pct < 75f && needed > 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 7.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, if (showWarn) AttendRed.copy(0.4f) else MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = NeonTealAlpha20, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                cls.subjectCode,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelMedium, color = NeonTeal
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Filled.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(cls.time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(cls.subjectName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                }
                val pctColor = when { pct >= 85f -> NeonTeal; pct >= 75f -> AttendGreen; pct >= 60f -> AttendOrange; else -> AttendRed }
                Text(
                    String.format(java.util.Locale.US, "%.1f%%", pct),
                    fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = pctColor
                )
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
            Spacer(Modifier.height(12.dp))

            // Action buttons — disabled when archived
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AttendanceButton("Present", Icons.Filled.Check,        AttendGreen,  cls.status == "PRESENT",   isArchived, Modifier.weight(1f)) { onStatusChange("PRESENT") }
                AttendanceButton("Bunk",    Icons.Filled.Close,        AttendRed,    cls.status == "BUNK",      isArchived, Modifier.weight(1f)) { onStatusChange("BUNK") }
                AttendanceButton("Cancel",  Icons.Filled.Remove,       AttendGrey,   cls.status == "CANCELLED", isArchived, Modifier.weight(1f)) { onStatusChange("CANCELLED") }
                AttendanceButton("Proxy",   Icons.Filled.PersonSearch,  PurpleAccent, cls.status == "PROXY",     isArchived, Modifier.weight(1f)) { onStatusChange("PROXY") }
                // Duty button — always tappable (not a toggling status, opens a sheet)
                AttendanceButton(
                    label      = "Duty",
                    icon       = Icons.Filled.Stars,
                    color      = Yellow,
                    isSelected = cls.status == "DUTY",
                    disabled   = false,   // duty logging is always allowed
                    modifier   = Modifier.weight(1f),
                    onClick    = onOpenDuty
                )
            }

            if (showWarn) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AttendRedAlpha20)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = AttendRed, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Below 75%. Need $needed more classes.", style = MaterialTheme.typography.labelSmall, color = AttendRed)
                }
            }
        }
    }
}

@Composable
private fun AttendanceButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    isSelected: Boolean,
    disabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val effectiveColor = if (disabled) MaterialTheme.colorScheme.outlineVariant else color
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (isSelected) effectiveColor else effectiveColor.copy(alpha = 0.12f))
                .border(1.5.dp, if (isSelected) effectiveColor else effectiveColor.copy(0.3f), CircleShape)
                .then(if (!disabled) Modifier.clickable(onClick = onClick) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else effectiveColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) effectiveColor else MaterialTheme.colorScheme.outlineVariant
        )
    }
}
