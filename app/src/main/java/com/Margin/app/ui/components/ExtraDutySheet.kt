package com.Margin.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Margin.app.data.local.entity.AttendanceRecordEntity
import com.Margin.app.ui.theme.*
import com.Margin.app.ui.viewmodel.TrackViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Bottom sheet for logging and managing "Extra / Duty" attendance entries.
 *
 * Features:
 *  - Log a DUTY (counts as Present) record on any picked date
 *  - Log a DUTY_ABSENT (counts as Absent) record on any picked date
 *  - Live list of all existing DUTY / DUTY_ABSENT records for this subject
 *  - Delete button per row to remove individual entries
 *
 * @param subjectId    The subject to manage duty records for
 * @param subjectName  Display name shown in the sheet header
 * @param viewModel    TrackViewModel providing duty record flows and actions
 * @param onDismiss    Called when the sheet should close
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtraDutySheet(
    subjectId: String,
    subjectName: String,
    viewModel: TrackViewModel,
    onDismiss: () -> Unit
) {
    // Observe live duty records for this subject
    val dutyRecords by viewModel.dutyRecordsFlow(subjectId).collectAsState(initial = emptyList())

    // Date picker state — defaults to today
    var showDatePicker  by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = todayMidnight()
    )

    // Normalise the picker selection to midnight for clean storage
    val selectedDateMs: Long = remember(datePickerState.selectedDateMillis) {
        val raw = datePickerState.selectedDateMillis ?: todayMidnight()
        Calendar.getInstance().apply {
            timeInMillis = raw
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val selectedDateLabel = remember(selectedDateMs) {
        SimpleDateFormat("EEE, MMM dd yyyy", Locale.getDefault()).format(Date(selectedDateMs))
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("OK", color = NeonTeal)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        ) { DatePicker(state = datePickerState) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(MaterialTheme.colorScheme.outline, CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Extra / Duty Attendance",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        subjectName,
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonTeal
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
            Spacer(Modifier.height(16.dp))

            // ── Date Selector ────────────────────────────────────────────
            Text(
                "Log entry for date",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = { showDatePicker = true },
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonTeal.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonTeal),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(selectedDateLabel, fontSize = 14.sp)
            }

            Spacer(Modifier.height(16.dp))

            // ── Log Buttons ──────────────────────────────────────────────
            Text(
                "Select type and log",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // DUTY — counts as Present
                Button(
                    onClick = {
                        viewModel.addDutyRecord(subjectId, selectedDateMs, "DUTY")
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AttendGreen.copy(alpha = 0.15f),
                        contentColor   = AttendGreen
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AttendGreen.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Duty", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("(+Present)", fontSize = 9.sp, color = AttendGreen.copy(alpha = 0.7f))
                    }
                }

                // DUTY_ABSENT — counts as Absent
                Button(
                    onClick = {
                        viewModel.addDutyRecord(subjectId, selectedDateMs, "DUTY_ABSENT")
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AttendOrange.copy(alpha = 0.15f),
                        contentColor   = AttendOrange
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AttendOrange.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Filled.RemoveCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Duty Absent", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("(+Absent)", fontSize = 9.sp, color = AttendOrange.copy(alpha = 0.7f))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
            Spacer(Modifier.height(12.dp))

            // ── Existing Duty Records List ────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Logged entries",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (dutyRecords.isNotEmpty()) {
                    Surface(
                        shape = CircleShape,
                        color = NeonTealAlpha20
                    ) {
                        Text(
                            "${dutyRecords.size}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonTeal,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (dutyRecords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.EventAvailable,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No duty entries yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(dutyRecords, key = { it.id }) { record ->
                        DutyRecordRow(
                            record   = record,
                            onDelete = { viewModel.deleteDutyRecord(record.id) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun DutyRecordRow(
    record: AttendanceRecordEntity,
    onDelete: () -> Unit
) {
    val isDuty  = record.status == "DUTY"
    val color   = if (isDuty) AttendGreen else AttendOrange
    val bgColor = if (isDuty) AttendGreenAlpha20 else AttendOrange.copy(alpha = 0.12f)
    val label   = if (isDuty) "Duty (Present)" else "Duty Absent"
    val icon    = if (isDuty) Icons.Filled.AddCircle else Icons.Filled.RemoveCircle
    val dateLabel = remember(record.date) {
        SimpleDateFormat("EEE, MMM dd yyyy", Locale.getDefault()).format(Date(record.date))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
            Text(
                dateLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(
            onClick  = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Filled.DeleteOutline,
                contentDescription = "Remove",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun todayMidnight(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
