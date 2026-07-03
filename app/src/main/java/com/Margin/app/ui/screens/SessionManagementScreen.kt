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
import com.Margin.app.ui.components.CreateSessionSheet
import com.Margin.app.ui.theme.*
import com.Margin.app.ui.viewmodel.SessionUiState
import com.Margin.app.ui.viewmodel.SessionViewModel
import com.Margin.app.utils.getAppViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionManagementScreen(
    onBack: () -> Unit = {},
    viewModel: SessionViewModel = getAppViewModel()
) {
    val sessions      by viewModel.sessionsList.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val lockedMonths  by viewModel.lockedMonthsForActiveSession.collectAsState()

    var showCreateSheet by remember(sessions.isEmpty()) { mutableStateOf(sessions.isEmpty()) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sessions", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                        Text("Switch, archive, or create semesters", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                Button(
                    onClick = { showCreateSheet = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTeal, contentColor = Color(0xFF003D35))
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Create New Semester", fontWeight = FontWeight.Bold)
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Your Sessions",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(sessions, key = { it.id }) { session ->
                SessionCard(
                    session            = session,
                    isActive           = session.id == activeSession?.id,
                    totalSessionsCount = sessions.size,
                    // Only pass lockedMonths for the active session; archived sessions
                    // show all months as locked anyway (session.isCompleted)
                    lockedMonths       = if (session.id == activeSession?.id) lockedMonths else emptyList(),
                    onSelect           = { viewModel.setActiveSession(session.id); onBack() },
                    onDelete           = { viewModel.deleteSession(session.id) },
                    onEndSemester      = { viewModel.endSession(session.id) },
                    onLockMonth        = { monthText -> viewModel.lockMonth(session.id, monthText) }
                )
            }
        }
    }

    if (showCreateSheet) {
        CreateSessionSheet(onDismiss = { showCreateSheet = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionCard(
    session: SessionUiState,
    isActive: Boolean,
    totalSessionsCount: Int,
    lockedMonths: List<String>,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onEndSemester: () -> Unit,
    onLockMonth: (String) -> Unit
) {
    val pct         = if (session.totalClasses > 0) session.attendedClasses.toFloat() / session.totalClasses else 0f
    val borderColor = when {
        session.isCompleted -> AttendOrange
        isActive            -> NeonTeal
        else                -> MaterialTheme.colorScheme.outline
    }

    var showEndConfirm  by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }

    // Month picker for Lock Month
    if (showMonthPicker) {
        MonthPickerDialog(
            lockedMonths = lockedMonths,
            onDismiss    = { showMonthPicker = false },
            onConfirm    = { monthText -> onLockMonth(monthText); showMonthPicker = false }
        )
    }

    // End Semester confirm dialog
    if (showEndConfirm) {
        AlertDialog(
            onDismissRequest = { showEndConfirm = false },
            icon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = AttendOrange) },
            title = { Text("End Semester?", fontWeight = FontWeight.Bold) },
            text  = { Text("This archives \"${session.name}\" and locks all attendance tracking. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { onEndSemester(); showEndConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AttendOrange)
                ) { Text("End Semester") }
            },
            dismissButton = {
                TextButton(onClick = { showEndConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 7.dp)
            .then(if (!session.isCompleted) Modifier.clickable { onSelect() } else Modifier),
        shape  = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(if (isActive) 1.5.dp else 1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            session.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = when {
                                session.isCompleted -> AttendOrange
                                isActive            -> NeonTeal
                                else                -> MaterialTheme.colorScheme.onBackground
                            },
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(8.dp))
                        if (session.isCompleted) {
                            Surface(color = AttendOrange.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                                Text("Archived", modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall, color = AttendOrange)
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Book, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${session.subjectCount} subjects", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (session.startedDaysAgo > 0) {
                            Text("  •  ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outlineVariant)
                            Text("${session.startedDaysAgo}d ago", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                // Status indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (totalSessionsCount > 1 && !isActive && !session.isCompleted) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = AttendRed, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                when { session.isCompleted -> AttendOrange; isActive -> NeonTeal; else -> MaterialTheme.colorScheme.surface },
                                CircleShape
                            )
                            .border(1.5.dp, borderColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            session.isCompleted -> Icon(Icons.Filled.Lock, contentDescription = "Archived", tint = Color.White, modifier = Modifier.size(18.dp))
                            isActive            -> Icon(Icons.Filled.Check, contentDescription = "Active",   tint = Color(0xFF003D35), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // Progress bar
            if (session.totalClasses > 0) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${session.attendedClasses}/${session.totalClasses} classes",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${(pct * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (pct >= 0.75f) NeonTeal else AttendRed,
                        fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { pct },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                    color = if (pct >= 0.75f) NeonTeal else AttendRed,
                    trackColor = MaterialTheme.colorScheme.outline,
                    strokeCap = StrokeCap.Round
                )
            }

            // Action buttons for non-archived sessions
            if (!session.isCompleted) {
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Lock Month
                    OutlinedButton(
                        onClick = { showMonthPicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        Icon(Icons.Filled.Compress, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Lock Month", fontSize = 12.sp)
                    }
                    // End Semester (only shown if this is active or the only session)
                    if (isActive) {
                        OutlinedButton(
                            onClick = { showEndConfirm = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, AttendOrange.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AttendOrange)
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("End Semester", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Month picker dialog for the "Lock Month" soft-lock feature.
 *
 * Generates only PAST months (excludes the current month and any future months —
 * you can't lock a month that isn't over yet). Already-locked months are shown
 * with a check-circle badge and cannot be re-selected.
 *
 * @param lockedMonths  Set of month strings already locked (e.g. "2025-03").
 */
@Composable
private fun MonthPickerDialog(
    lockedMonths: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    // Build list of past-only months (never current or future).
    // We go back from the PREVIOUS month (index 1) up to 12 months ago.
    val monthOptions = remember {
        val fmt      = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val labelFmt = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val cal      = Calendar.getInstance()
        // Step back by 1 first — current month is excluded (it's not over yet)
        cal.add(Calendar.MONTH, -1)
        // Collect up to 12 past months
        (0 until 12).map {
            val text  = fmt.format(cal.time)
            val label = labelFmt.format(cal.time)
            cal.add(Calendar.MONTH, -1)
            Pair(text, label)
        }
        // Result is newest-first; we keep that order (most recent at top)
    }

    // Default selection: the most recent unlocked month, or the top option
    val firstUnlocked = monthOptions.firstOrNull { (value, _) -> value !in lockedMonths }
    var selected by remember { mutableStateOf(firstUnlocked?.first ?: monthOptions.firstOrNull()?.first ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon  = { Icon(Icons.Filled.Compress, contentDescription = null, tint = NeonTeal) },
        title = { Text("Lock Month", fontWeight = FontWeight.Bold) },
        text  = {
            Column {
                Text(
                    "Snapshot this month's attendance into a summary. " +
                    "Your original daily records are preserved — this is a soft lock.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                if (monthOptions.isEmpty()) {
                    Text(
                        "No past months available to lock.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    monthOptions.forEach { (value, label) ->
                        val isLocked    = value in lockedMonths
                        val isSelected  = selected == value
                        val canSelect   = !isLocked

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .then(
                                    if (canSelect) Modifier.clickable { selected = value }
                                    else Modifier  // Locked months are not clickable
                                )
                                .background(
                                    when {
                                        isLocked   -> MaterialTheme.colorScheme.surface
                                        isSelected -> NeonTealAlpha20
                                        else       -> Color.Transparent
                                    }
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isLocked) {
                                // Show a filled check-circle for already-locked months
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Already locked",
                                    tint = NeonTeal.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                RadioButton(
                                    selected = isSelected,
                                    onClick  = { selected = value },
                                    colors   = RadioButtonDefaults.colors(selectedColor = NeonTeal)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isLocked)
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    else
                                        MaterialTheme.colorScheme.onBackground
                                )
                                if (isLocked) {
                                    Text(
                                        "Already locked",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonTeal.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val allLocked = monthOptions.all { (v, _) -> v in lockedMonths }
            Button(
                onClick  = { if (selected.isNotEmpty()) onConfirm(selected) },
                enabled  = selected.isNotEmpty() && !allLocked && selected !in lockedMonths,
                colors   = ButtonDefaults.buttonColors(containerColor = NeonTeal, contentColor = Color(0xFF003D35))
            ) { Text("Lock", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
