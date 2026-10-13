package com.Margin.app.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Margin.app.domain.AttendanceMath
import com.Margin.app.ui.components.MarginTopBar
import com.Margin.app.ui.components.SemesterOverviewSheet
import com.Margin.app.ui.components.SquadSyncSheet
import com.Margin.app.ui.theme.*
import com.Margin.app.ui.viewmodel.AttendanceViewModel
import com.Margin.app.ui.viewmodel.SubjectOverviewUi
import com.Margin.app.utils.getAppViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

enum class AttendanceScope { SEMESTER, MONTH }

@Composable
fun InsightsScreen(
    viewModel: AttendanceViewModel = getAppViewModel()
) {
    val context = LocalContext.current
    val activeSession by viewModel.activeSession.collectAsState()
    val subjectOverviews by viewModel.subjectOverviews.collectAsState()
    val analytics by viewModel.analytics.collectAsState()

    var showSemesterSheet by remember { mutableStateOf(false) }
    var showSquadSync by remember { mutableStateOf(false) }
    var squadSyncInitialTab by remember { mutableIntStateOf(0) }

    // Attendance Period Selector
    var selectedScope by remember { mutableStateOf(AttendanceScope.SEMESTER) }
    var monthOffset by remember { mutableIntStateOf(0) }

    val monthCalendar = remember(monthOffset) {
        Calendar.getInstance().apply {
            add(Calendar.MONTH, monthOffset)
        }
    }

    val monthLabel = remember(monthCalendar) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(monthCalendar.time)
    }

    val monthRange = remember(monthOffset) {
        val cal = Calendar.getInstance().apply {
            add(Calendar.MONTH, monthOffset)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        Pair(start, end)
    }

    val monthRecords by viewModel.recordsInRangeFlow(monthRange.first, monthRange.second).collectAsState(initial = emptyList())

    val activeSubjectIds = remember(subjectOverviews) { subjectOverviews.map { it.id }.toSet() }
    val monthRecordsFiltered = remember(monthRecords, activeSubjectIds) {
        val base = if (activeSubjectIds.isEmpty()) monthRecords
                   else monthRecords.filter { it.subjectId in activeSubjectIds }
        val slotted = base.filter { it.slotId != null }
            .groupBy { Pair(it.date, it.slotId) }
            .mapValues { it.value.last() }
            .values
        val nonSlotted = base.filter { it.slotId == null }
        slotted + nonSlotted
    }

    val monthAttended = remember(monthRecordsFiltered) {
        monthRecordsFiltered.count { it.status == "PRESENT" || it.status == "PROXY" || it.status == "DUTY" }
    }

    val monthTotal = remember(monthRecordsFiltered) {
        monthRecordsFiltered.count {
            it.status == "PRESENT" || it.status == "PROXY" || it.status == "DUTY" ||
            it.status == "BUNK"    || it.status == "EXCUSED" || it.status == "DUTY_ABSENT" ||
            it.status == "ABSENT"
        }
    }

    val monthBunks = remember(monthRecordsFiltered) {
        monthRecordsFiltered.count { it.status == "BUNK" }
    }

    val monthPct = remember(monthAttended, monthTotal) {
        AttendanceMath.calculatePercentage(monthAttended, monthTotal)
    }

    val monthSafeBunks = remember(monthAttended, monthTotal) {
        AttendanceMath.calculateSafeBunks(monthAttended, monthTotal)
    }

    val weeksIn = remember(activeSession) {
        val start = activeSession?.startDate ?: System.currentTimeMillis()
        val diffMs = System.currentTimeMillis() - start
        val weeks = TimeUnit.MILLISECONDS.toDays(diffMs) / 7
        if (weeks < 1) 1 else weeks + 1
    }

    val overallPct = analytics.overallPercentage

    // Sort subjects by attendance risk: deficit first, then lowest percentage
    val sortedSubjects = remember(subjectOverviews) {
        subjectOverviews.sortedWith(
            compareByDescending<SubjectOverviewUi> { it.neededToRecover > 0 }
                .thenBy { it.percentage }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Unified Top Bar
            item {
                MarginTopBar(
                    screenTitle = "Insights",
                    semesterName = activeSession?.name,
                    onSemesterClick = { showSemesterSheet = true },
                    onProfileClick = { showSemesterSheet = true }
                )
            }

            // Active Term Header Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    border = BorderStroke(1.dp, SlatePillBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NeonTealAlpha20),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DateRange,
                                    contentDescription = null,
                                    tint = NeonTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = activeSession?.name ?: "Current Term",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(NeonTeal)
                                    )
                                }
                                Text(
                                    text = "Active Term • $weeksIn Weeks in",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        IconButton(
                            onClick = { showSemesterSheet = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SlatePill)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = "Edit Term",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Attendance Overview Card (Redone: No fluff text, includes Semester vs Particular Month buttons)
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
                        // Scope Toggle Buttons: [ Entire Semester ] [ That Month ]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SlatePill)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedScope == AttendanceScope.SEMESTER) NeonTeal else Color.Transparent)
                                    .clickable { selectedScope = AttendanceScope.SEMESTER }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Entire Semester",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedScope == AttendanceScope.SEMESTER) Color(0xFF0C131D) else TextSecondary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedScope == AttendanceScope.MONTH) NeonTeal else Color.Transparent)
                                    .clickable { selectedScope = AttendanceScope.MONTH }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Particular Month",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedScope == AttendanceScope.MONTH) Color(0xFF0C131D) else TextSecondary
                                )
                            }
                        }

                        // Month browser if Month scope is selected
                        if (selectedScope == AttendanceScope.MONTH) {
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { monthOffset-- },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ChevronLeft,
                                        contentDescription = "Previous Month",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Text(
                                    text = monthLabel,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )

                                IconButton(
                                    onClick = { if (monthOffset < 0) monthOffset++ },
                                    enabled = monthOffset < 0,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ChevronRight,
                                        contentDescription = "Next Month",
                                        tint = if (monthOffset < 0) TextSecondary else TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Selected Stats
                        val activePct = if (selectedScope == AttendanceScope.SEMESTER) overallPct else monthPct
                        val activeAttended = if (selectedScope == AttendanceScope.SEMESTER) analytics.totalAttended else monthAttended
                        val activeTotal = if (selectedScope == AttendanceScope.SEMESTER) analytics.totalClasses else monthTotal
                        val activeSafeBunks = if (selectedScope == AttendanceScope.SEMESTER) analytics.safeBunksLeft else monthSafeBunks
                        val activeBunks = if (selectedScope == AttendanceScope.SEMESTER) analytics.totalBunks else monthBunks
                        val isSafe = activeTotal == 0 || activePct >= 75f

                        // Main Big Percentage Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = String.format(Locale.US, "%.1f%%", activePct),
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSafe) NeonTeal else AttendRed
                                )
                                Text(
                                    text = if (selectedScope == AttendanceScope.SEMESTER) "Semester Aggregate" else "$monthLabel Attendance",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSafe) NeonTealAlpha20 else AttendRedAlpha20)
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = if (isSafe) "✓ Safe Margin" else "! Deficit Focus",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSafe) NeonTeal else AttendRed
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Slim Progress Bar
                        LinearProgressIndicator(
                            progress = {
                                if (activeTotal == 0) 1f else (activePct / 100f).coerceIn(0f, 1f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (isSafe) NeonTeal else AttendRed,
                            trackColor = Color(0xFF1E2D3E),
                        )

                        Spacer(Modifier.height(16.dp))

                        // 3-Metric Summary Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Box 1: Attended
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SlatePill)
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text("ATTENDED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                    Spacer(Modifier.height(4.dp))
                                    Text("$activeAttended / $activeTotal", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("classes", fontSize = 10.sp, color = TextMuted)
                                }
                            }

                            // Box 2: Bunks Buffer
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SlatePill)
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text("BUNKS BUFFER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                    Spacer(Modifier.height(4.dp))
                                    Text("$activeSafeBunks Passes", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (activeSafeBunks > 0) NeonTeal else AttendRed)
                                    Text("safe buffer", fontSize = 10.sp, color = TextMuted)
                                }
                            }

                            // Box 3: Total Bunks
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SlatePill)
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text("SKIPPED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                    Spacer(Modifier.height(4.dp))
                                    Text("$activeBunks Bunks", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (activeBunks > 0) AttendOrange else TextSecondary)
                                    Text("recorded", fontSize = 10.sp, color = TextMuted)
                                }
                            }
                        }
                    }
                }
            }

            // Bunk Leaderboard Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BarChart,
                            contentDescription = null,
                            tint = AttendRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Bunk Leaderboard",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = "TERM TALLY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Bunk Leaderboard Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    border = BorderStroke(1.dp, SlatePillBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (sortedSubjects.isEmpty()) {
                            Text(
                                text = "No subjects added yet. Add subjects to see margins.",
                                color = TextMuted,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            sortedSubjects.take(4).forEachIndexed { index, sub ->
                                val pct = sub.percentage
                                val isDanger = pct < 75f
                                val isCaution = pct in 75f..79.9f
                                val statusLabel = when {
                                    isDanger -> "${pct.toInt()}% DANGER"
                                    isCaution -> "${pct.toInt()}% CAUTION"
                                    else -> "${pct.toInt()}% SAFE"
                                }
                                val statusColor = when {
                                    isDanger -> AttendRed
                                    isCaution -> AttendOrange
                                    else -> NeonTeal
                                }

                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isDanger) AttendRedAlpha20 else SlatePill),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${index + 1}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDanger) AttendRed else TextSecondary
                                                )
                                            }

                                            Text(
                                                text = sub.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(statusColor.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = statusLabel,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = statusColor
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    // Progress Bar
                                    LinearProgressIndicator(
                                        progress = { (pct / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = statusColor,
                                        trackColor = Color(0xFF1E2D3E),
                                    )

                                    Spacer(Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${sub.total - sub.attended} classes skipped",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )

                                        Text(
                                            text = if (isDanger) "Must attend next ${sub.neededToRecover}"
                                            else "Safe margin: +${sub.safeBunks} pass",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isDanger) AttendRed else NeonTeal
                                        )
                                    }
                                }
                            }
                        }

                        // Most bunked highlight
                        analytics.mostBunkedSubjectName?.let { mostBunked ->
                            HorizontalDivider(color = SlatePillBorder, thickness = 0.5.dp)
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
                                        imageVector = Icons.Filled.Hotel,
                                        contentDescription = null,
                                        tint = AttendOrange,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Top Bunk Course: $mostBunked",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }

                                Text(
                                    text = "${analytics.totalBunks} skips",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AttendOrange
                                )
                            }
                        }
                    }
                }
            }

            // Semester Tools Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Build,
                            contentDescription = null,
                            tint = NeonTeal,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Semester Tools",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = "PARAMETERS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Semester Tools List
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    border = BorderStroke(1.dp, SlatePillBorder)
                ) {
                    Column {
                        // Tool 1: Edit Target Margin
                        SemesterToolItem(
                            icon = Icons.Filled.GpsFixed,
                            title = "Edit Target Margin",
                            subtitle = "Currently anchored at university standard: 75.0%",
                            onClick = { showSemesterSheet = true }
                        )

                        HorizontalDivider(color = SlatePillBorder, thickness = 0.5.dp)

                        // Tool 2: Export Timetable / Squad Code
                        SemesterToolItem(
                            icon = Icons.Filled.Share,
                            title = "Export Timetable / Squad Code",
                            subtitle = "Generate and share your Squad Code with classmates",
                            actionIcon = Icons.Filled.Share,
                            onClick = {
                                squadSyncInitialTab = 0
                                showSquadSync = true
                            }
                        )

                        HorizontalDivider(color = SlatePillBorder, thickness = 0.5.dp)

                        // Tool 3: Import Timetable / Squad Sync
                        SemesterToolItem(
                            icon = Icons.Filled.Group,
                            title = "Import Timetable / Squad Sync",
                            subtitle = "Enter or paste a classmate's Squad Code to sync schedule",
                            actionIcon = Icons.Filled.FileDownload,
                            onClick = {
                                squadSyncInitialTab = 1
                                showSquadSync = true
                            }
                        )

                        HorizontalDivider(color = SlatePillBorder, thickness = 0.5.dp)

                        // Tool 4: Manage Semester
                        SemesterToolItem(
                            icon = Icons.Filled.School,
                            title = "Manage / Switch Term",
                            subtitle = "Switch active semester, archive, or configure term dates",
                            onClick = { showSemesterSheet = true }
                        )
                    }
                }
            }
        }
    }

    if (showSemesterSheet) {
        SemesterOverviewSheet(
            onDismiss = { showSemesterSheet = false },
            viewModel = viewModel,
            onCreateNewSemester = { /* handled in sheet */ }
        )
    }

    if (showSquadSync) {
        SquadSyncSheet(
            onDismiss = { showSquadSync = false },
            viewModel = viewModel,
            initialTab = squadSyncInitialTab
        )
    }
}

@Composable
private fun SemesterToolItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.ChevronRight,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SlatePill),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NeonTeal,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }

        Icon(
            imageVector = actionIcon,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}
