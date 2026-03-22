package com.attendease.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attendease.app.ui.theme.*
import com.attendease.app.ui.viewmodel.TimetableViewModel
import com.attendease.app.utils.getAppViewModel

@Composable
fun ScheduleScreen(
    viewModel: TimetableViewModel = getAppViewModel()
) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri")
    var selectedDay by remember { mutableStateOf(2) } // Wednesday default
    
    val currentDayStr = days.getOrNull(selectedDay) ?: "Wed"
    val scheduleFlow = remember(currentDayStr) {
        viewModel.getTimetableForDay(currentDayStr)
    }
    val schedule by scheduleFlow.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column {
            // ── Header ─────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp)
            ) {
                Text("Schedule", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                Text("Semester 1 Timetable", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // ── Day selector ──────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                itemsIndexed(days) { idx, day ->
                    val isSelected = idx == selectedDay
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) NeonTeal else MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, if (isSelected) NeonTeal else MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                            .clickable { selectedDay = idx }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            day,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF003D35) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // ── Timetable ──────────────────────────────────────
            if (schedule.isNotEmpty()) {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(schedule, key = { it.id }) { entry ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Time column
                            Column(
                                modifier = Modifier.width(60.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    entry.time,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NeonTeal
                                )
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(60.dp)
                                        .background(MaterialTheme.colorScheme.outline)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            // Card
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(NeonTealAlpha20, RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Book, contentDescription = null, tint = NeonTeal, modifier = Modifier.size(22.dp))
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(entry.subjectName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                                        Spacer(Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(color = NeonTealAlpha20, shape = RoundedCornerShape(4.dp)) {
                                                Text(entry.subjectCode, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall, color = NeonTeal)
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Icon(Icons.Filled.Room, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(11.dp))
                                            Spacer(Modifier.width(2.dp))
                                            Text(entry.room, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outlineVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Event, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("No classes scheduled", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}
