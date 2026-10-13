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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Margin.app.domain.AttendanceMath
import com.Margin.app.ui.components.CreateSessionSheet
import com.Margin.app.ui.components.ExtraDutySheet
import com.Margin.app.ui.components.InteractiveCalendarSheet
import com.Margin.app.ui.components.MarginTopBar
import com.Margin.app.ui.components.SemesterOverviewSheet
import com.Margin.app.ui.components.SwapClassSheet
import com.Margin.app.ui.theme.*
import com.Margin.app.ui.viewmodel.AttendanceViewModel
import com.Margin.app.ui.viewmodel.TodayClassUi
import com.Margin.app.utils.getAppViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class WeekDayUi(
    val dayName: String,
    val dayNumber: Int,
    val dateMs: Long,
    val isSelected: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackScreen(
    viewModel: AttendanceViewModel = getAppViewModel()
) {
    val todayClasses by viewModel.todayClasses.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val analytics by viewModel.analytics.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    var showSemesterSheet by remember { mutableStateOf(false) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var swapTargetClass by remember { mutableStateOf<TodayClassUi?>(null) }
    var dutySubjectId by remember { mutableStateOf<String?>(null) }
    var dutySubjectName by remember { mutableStateOf("") }

    val formattedHeaderDate = remember(selectedDate) {
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date(selectedDate))
    }

    val termWeek = remember(activeSession, selectedDate) {
        val start = activeSession?.startDate ?: selectedDate
        val diff = selectedDate - start
        val week = if (diff < 0) 1 else (TimeUnit.MILLISECONDS.toDays(diff) / 7) + 1
        week.coerceAtLeast(1)
    }

    // 7-day strip (Monday to Sunday) for the selected week
    val weekDays = remember(selectedDate) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = selectedDate
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
        }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val offsetToMon = if (dayOfWeek == Calendar.SUNDAY) -6 else Calendar.MONDAY - dayOfWeek
        cal.add(Calendar.DAY_OF_MONTH, offsetToMon)

        val days = mutableListOf<WeekDayUi>()
        val dayFmt = SimpleDateFormat("EEE", Locale.getDefault())
        for (i in 0 until 7) {
            val ms = cal.timeInMillis
            val name = dayFmt.format(Date(ms)).uppercase(Locale.getDefault())
            val num = cal.get(Calendar.DAY_OF_MONTH)
            days.add(
                WeekDayUi(
                    dayName = name,
                    dayNumber = num,
                    dateMs = ms,
                    isSelected = ms == selectedDate
                )
            )
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        days
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            // Unified Top Bar
            item {
                MarginTopBar(
                    screenTitle = "Track",
                    semesterName = activeSession?.name,
                    onSemesterClick = { showSemesterSheet = true },
                    onProfileClick = { showSemesterSheet = true }
                )
            }

            // Date Header & Quick Actions (Clean, no overlapping square boxes)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Clickable Date with inline calendar icon & subtitle
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showDatePicker = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = formattedHeaderDate,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )
                                Icon(
                                    imageVector = Icons.Filled.CalendarMonth,
                                    contentDescription = "Pick Date",
                                    tint = NeonTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${todayClasses.size} classes • Week $termWeek",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        // Right: Quick Actions (Day Off / Absent)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isDayOff = todayClasses.isNotEmpty() && todayClasses.all { it.status == "CANCELLED" }
                            val isAllAbsent = todayClasses.isNotEmpty() && todayClasses.all { it.status == "ABSENT" }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDayOff) NeonTealAlpha20 else SlatePill)
                                    .border(
                                        1.dp,
                                        if (isDayOff) NeonTeal else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.markDayOff() }
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isDayOff) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = NeonTeal,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Text(
                                        text = "DAY OFF",
                                        fontSize = 11.sp,
                                        fontWeight = if (isDayOff) FontWeight.ExtraBold else FontWeight.Bold,
                                        color = if (isDayOff) NeonTeal else TextSecondary
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isAllAbsent) Color(0xFFEA580C) else Color(0x22EA580C))
                                    .border(
                                        1.dp,
                                        if (isAllAbsent) Color(0xFFF97316) else Color(0x44EA580C),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.markDayAbsent() }
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isAllAbsent) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Text(
                                        text = "ABSENT",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isAllAbsent) Color.White else Color(0xFFFB923C)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 7-Day Horizontal Strip (Light & Minimal, no claustrophobic card borders on unselected days)
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(weekDays) { day ->
                        val isSelected = day.isSelected
                        Box(
                            modifier = Modifier
                                .width(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) NeonTeal else Color.Transparent)
                                .clickable { viewModel.selectDate(day.dateMs) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = day.dayName.take(3),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF0C131D) else TextMuted
                                )

                                Text(
                                    text = "${day.dayNumber}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) Color(0xFF0C131D) else TextPrimary
                                )

                                // Indicator dot
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color(0xFF0C131D) else NeonTeal.copy(alpha = 0.6f))
                                )
                            }
                        }
                    }
                }
            }

            // TIMETABLE LEDGER Section Header (Integrated Aggregate, no redundant floating card box)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TIMETABLE LEDGER",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = NeonTeal,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.1f", analytics.overallPercentage)}% Aggregate",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonTeal
                        )
                        Text("•", color = TextMuted, fontSize = 11.sp)
                        Text(
                            text = "${analytics.safeBunksLeft} safe",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Ledger Class Cards (Clean single-box container, breathable and minimal)
            if (todayClasses.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EventAvailable,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(42.dp)
                            )
                            Text(
                                text = "No classes scheduled for this day.",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(todayClasses, key = { it.timetableId }) { cls ->
                    LedgerClassCard(
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

    if (showDatePicker) {
        InteractiveCalendarSheet(
            onDismiss = { showDatePicker = false },
            viewModel = viewModel,
            onDateSelected = { dateMs ->
                viewModel.selectDate(dateMs)
            }
        )
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
private fun LedgerClassCard(
    cls: TodayClassUi,
    onStatusChange: (String) -> Unit,
    onOpenDuty: () -> Unit,
    onSwap: () -> Unit
) {
    val pct = cls.attendancePct
    val isSafe = cls.total == 0 || pct >= 75f
    val isCancelled = cls.status == "CANCELLED"
    val isAbsent = cls.status == "ABSENT"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, if (isAbsent) Color(0x66EA580C) else SlatePillBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Top Row: Time • Room • Swap & Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${cls.time} • ${cls.room}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = onSwap)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SwapHoriz,
                            contentDescription = "Swap",
                            tint = NeonTeal,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Swap",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonTeal
                        )
                    }
                }

                // Right Badge
                val badgeText = when {
                    isCancelled -> "Cancelled (No count)"
                    isAbsent -> "Marked Absent"
                    cls.total == 0 -> "No Records"
                    cls.attendancePct >= 75f -> String.format(Locale.US, "%.1f%% • +%d safe", pct, cls.safeBunks)
                    else -> String.format(Locale.US, "%.1f%% • %d bunks left", pct, cls.safeBunks)
                }

                val badgeBg = when {
                    isCancelled -> SlatePill
                    isAbsent -> Color(0x33EA580C)
                    cls.total == 0 -> SlatePill
                    isSafe -> NeonTealAlpha20
                    else -> AttendRedAlpha20
                }

                val badgeColor = when {
                    isCancelled -> TextSecondary
                    isAbsent -> Color(0xFFFB923C)
                    cls.total == 0 -> TextSecondary
                    isSafe -> NeonTeal
                    else -> AttendRed
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Course Name (CS Code)
            Text(
                text = "${cls.subjectName} (${cls.subjectCode})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(Modifier.height(10.dp))

            // Slim Progress Bar
            LinearProgressIndicator(
                progress = {
                    if (cls.total == 0) 1f else (pct / 100f).coerceIn(0f, 1f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (isSafe) NeonTeal else AttendRed,
                trackColor = Color(0xFF1E2D3E),
            )

            Spacer(Modifier.height(12.dp))

            // Action Buttons: Clean flat pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ClassActionPill(
                    label = "Present",
                    isSelected = cls.status == "PRESENT",
                    activeColor = NeonTeal,
                    modifier = Modifier.weight(1f),
                    onClick = { onStatusChange("PRESENT") }
                )

                ClassActionPill(
                    label = "Bunk",
                    isSelected = cls.status == "BUNK",
                    activeColor = AttendRed,
                    modifier = Modifier.weight(1f),
                    onClick = { onStatusChange("BUNK") }
                )

                ClassActionPill(
                    label = "Proxy",
                    isSelected = cls.status == "PROXY",
                    activeColor = PurpleAccent,
                    modifier = Modifier.weight(1f),
                    onClick = { onStatusChange("PROXY") }
                )

                ClassActionPill(
                    label = if (isCancelled) "Cancelled" else if (isAbsent) "Absent" else "OD / Org",
                    isSelected = cls.status == "DUTY" || isCancelled || isAbsent,
                    activeColor = when {
                        isCancelled -> TextSecondary
                        isAbsent -> Color(0xFFFB923C)
                        else -> Yellow
                    },
                    modifier = Modifier.weight(1f),
                    onClick = {
                        when {
                            isCancelled -> onStatusChange("NONE")
                            isAbsent -> onStatusChange("NONE")
                            else -> onOpenDuty()
                        }
                    }
                )
            }

            // Subtle inline hint
            if (cls.note.isNotBlank() || isAbsent || (cls.total > 0 && !isCancelled)) {
                Spacer(Modifier.height(10.dp))
                val bannerMessage = when {
                    cls.note.isNotBlank() -> cls.note
                    isAbsent -> "Marked absent (does not count towards bunk bank)"
                    !isSafe -> "Need ${cls.neededToRecover} consecutive classes to reach 75%"
                    cls.safeBunks > 0 -> "Safe buffer: can bunk ${cls.safeBunks} class${if (cls.safeBunks > 1) "es" else ""} safely"
                    else -> "Currently on 75.0% margin line"
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lightbulb,
                        contentDescription = null,
                        tint = if (isAbsent) Color(0xFFFB923C) else if (isSafe) NeonTeal else AttendRed,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = bannerMessage,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ClassActionPill(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) activeColor else SlatePill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color(0xFF0C131D) else TextSecondary
        )
    }
}
