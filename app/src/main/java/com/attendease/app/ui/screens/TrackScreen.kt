package com.attendease.app.ui.screens

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
import com.attendease.app.ui.theme.*
import com.attendease.app.ui.viewmodel.TodayClassUiState
import com.attendease.app.ui.viewmodel.TrackViewModel
import com.attendease.app.utils.getAppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackScreen(viewModel: TrackViewModel = getAppViewModel()) {
    val todayClasses by viewModel.todayClassesUiState.collectAsState()

    val attendedCount = todayClasses.count { it.status == "PRESENT" }
    val absentCount = todayClasses.count { it.status == "ABSENT" }
    val cancelledCount = todayClasses.count { it.status == "CANCELLED" }
    val proxyCount = todayClasses.count { it.status == "PROXY" }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                // ── Header ─────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF0E0E1E), MaterialTheme.colorScheme.background)
                            )
                        )
                        .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp)
                ) {
                    Text("Today's Attendance", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                    Text("Today", style = MaterialTheme.typography.bodyMedium, color = NeonTeal)

                    Spacer(Modifier.height(14.dp))

                    // Mark day buttons
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
                            Text("Mark Day Off", fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.markDayAbsent() },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, AttendRedAlpha20),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = AttendRedAlpha20,
                                contentColor = AttendRed
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.PersonOff, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Mark Absent", fontSize = 13.sp)
                        }
                    }
                }
            }

            item {
                // ── Daily Stats Row ───────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DailyStat("Attended", attendedCount, AttendGreen, AttendGreenAlpha20, Modifier.weight(1f))
                    DailyStat("Absent", absentCount, AttendRed, AttendRedAlpha20, Modifier.weight(1f))
                    DailyStat("Cancelled", cancelledCount, AttendGrey, AttendGreyAlpha20, Modifier.weight(1f))
                    DailyStat("Proxy", proxyCount, PurpleAccent, PurpleAlpha20, Modifier.weight(1f))
                }
            }

            item {
                Text(
                    "Today's Classes",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(todayClasses, key = { it.timetableId }) { session ->
                ClassCard(
                    session = session,
                    onStatusChange = { status -> viewModel.markAttendance(session.subjectId, status) }
                )
            }
        }
    }
}

@Composable
private fun DailyStat(label: String, count: Int, color: Color, bgColor: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$count", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun ClassCard(
    session: TodayClassUiState,
    onStatusChange: (String) -> Unit
) {
    val pct = session.attendancePct
    val totalCount = session.totalClasses
    val attendedCount = kotlin.math.round(pct * totalCount).toInt()
    
    val needed = kotlin.math.ceil((0.75f * totalCount - attendedCount) / 0.25f).toInt()
    val showWarning = totalCount > 0 && pct < 0.75f && needed > 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 7.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(
            1.dp,
            if (showWarning) AttendRed.copy(0.4f) else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top: Code + Time + Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = NeonTealAlpha20,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                session.subjectCode,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonTeal
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Filled.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(session.time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(session.subjectName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    Text(session.room, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // Percentage
                val pctColor = when {
                    pct >= 0.85f -> NeonTeal
                    pct >= 0.75f -> AttendGreen
                    pct >= 0.60f -> AttendOrange
                    else -> AttendRed
                }
                Text(
                    "${(pct * 100).toInt()}%",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = pctColor
                )
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
            Spacer(Modifier.height(12.dp))

            // ── Action pill buttons ────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AttendanceButton("Present", Icons.Filled.Check, AttendGreen, session.status == "PRESENT", Modifier.weight(1f)) {
                    onStatusChange("PRESENT")
                }
                AttendanceButton("Absent", Icons.Filled.Close, AttendRed, session.status == "ABSENT", Modifier.weight(1f)) {
                    onStatusChange("ABSENT")
                }
                AttendanceButton("Cancel", Icons.Filled.Remove, AttendGrey, session.status == "CANCELLED", Modifier.weight(1f)) {
                    onStatusChange("CANCELLED")
                }
                AttendanceButton("Proxy", Icons.Filled.PersonSearch, PurpleAccent, session.status == "PROXY", Modifier.weight(1f)) {
                    onStatusChange("PROXY")
                }
            }

            // ── Warning block if < 75% ─────────────────────────
            if (showWarning) {
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
                    Text(
                        "Attendance below 75%. You need $needed more classes.",
                        style = MaterialTheme.typography.labelSmall,
                        color = AttendRed
                    )
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
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (isSelected) color else color.copy(alpha = 0.12f))
                .border(1.5.dp, if (isSelected) color else color.copy(0.3f), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else color,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) color else MaterialTheme.colorScheme.outlineVariant
        )
    }
}
