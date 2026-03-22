package com.attendease.app.ui.screens

import androidx.compose.foundation.*
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
import com.attendease.app.ui.components.AddClassSheet
import com.attendease.app.ui.theme.*
import com.attendease.app.ui.viewmodel.TimetableUiState
import com.attendease.app.ui.viewmodel.TimetableViewModel
import com.attendease.app.utils.getAppViewModel

// Cycles through distinct colors per subject code for visual variety
private val subjectColors = listOf(NeonTeal, Magenta, Yellow, BlueAccent, PurpleAccent, AttendOrange)
private fun subjectColor(code: String): Color =
    subjectColors[(code.hashCode() and 0x7FFFFFFF) % subjectColors.size]

val timetableDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@Composable
fun TimetableScreen(
    onBack: () -> Unit = {},
    viewModel: TimetableViewModel = getAppViewModel()
) {
    var selectedDay by remember { mutableStateOf("Mon") }
    var showAddClass by remember { mutableStateOf(false) }

    val entries by viewModel.getTimetableForDay(selectedDay).collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Top Bar ───────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Timetable", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                Text("Configure your weekly schedule", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(
                onClick = { /* Save action */ },
                colors = ButtonDefaults.textButtonColors(contentColor = NeonTeal)
            ) {
                Text("Save", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        // ── Day Chips ─────────────────────────────────────────
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(vertical = 10.dp)
        ) {
            item {
                timetableDays.forEach { day ->
                    val isSelected = day == selectedDay
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) NeonTeal else MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, if (isSelected) NeonTeal else MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                            .clickable { selectedDay = day }
                            .padding(horizontal = 20.dp, vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            day,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF003D35) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                }
            }
        }

        // ── Class List for selected day ───────────────────────
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (entries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.EventBusy, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(52.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("No classes on $selectedDay", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            } else {
                items(items = entries, key = { it.id }) { entry ->
                    TimetableClassCard(
                        entry = entry,
                        onDelete = { viewModel.removeClass(entry.id) }
                    )
                }
            }

            // Add Class button
            item {
                TextButton(
                    onClick = { showAddClass = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = NeonTeal, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Class", color = NeonTeal, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ── Week Overview ─────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(top = 12.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)) {
                Text(
                    "Week Overview",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    timetableDays.forEach { day ->
                        DayOverviewItem(
                            day = day,
                            selectedDay = selectedDay,
                            viewModel = viewModel,
                            onSelect = { selectedDay = day }
                        )
                    }
                }
            }
        }
    }

    if (showAddClass) {
        AddClassSheet(
            selectedDay = selectedDay,
            onDismiss = { showAddClass = false }
        )
    }
}

@Composable
private fun DayOverviewItem(
    day: String,
    selectedDay: String,
    viewModel: TimetableViewModel,
    onSelect: () -> Unit
) {
    val countsMap by viewModel.classesPerDayCount.collectAsState()
    val dayInt = when (day) { "Mon" -> 1; "Tue" -> 2; "Wed" -> 3; "Thu" -> 4; "Fri" -> 5; "Sat" -> 6; "Sun" -> 7; else -> 1 }
    val count = countsMap[dayInt] ?: 0
    val isSelected = day == selectedDay

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) NeonTealAlpha20 else Color.Transparent)
            .clickable { onSelect() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            day,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) NeonTeal else MaterialTheme.colorScheme.outlineVariant,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "$count",
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isSelected) NeonTeal else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
private fun TimetableClassCard(
    entry: TimetableUiState,
    onDelete: () -> Unit
) {
    val color = subjectColor(entry.subjectCode)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 0.dp, end = 16.dp, top = 0.dp, bottom = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left colored stripe
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(72.dp)
                    .background(color, RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
            )
            Spacer(Modifier.width(14.dp))
            // Icon
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(color.copy(0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Book, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = color.copy(0.15f), shape = RoundedCornerShape(5.dp)) {
                        Text(
                            entry.subjectCode,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = color,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Filled.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(entry.time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outlineVariant)
                }
                Spacer(Modifier.height(3.dp))
                Text(entry.subjectName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Text(entry.room, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Remove", tint = AttendRed.copy(0.7f), modifier = Modifier.size(18.dp))
            }
        }
    }
}
