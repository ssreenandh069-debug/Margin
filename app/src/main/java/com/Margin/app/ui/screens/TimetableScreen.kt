package com.Margin.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.Margin.app.ui.components.AddClassSheet
import com.Margin.app.ui.components.SquadSyncSheet
import com.Margin.app.ui.theme.*
import com.Margin.app.ui.viewmodel.AttendanceViewModel
import com.Margin.app.utils.getAppViewModel
import kotlinx.coroutines.launch

val weekDays = listOf("Mon" to 1, "Tue" to 2, "Wed" to 3, "Thu" to 4, "Fri" to 5, "Sat" to 6, "Sun" to 7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    onBack: (() -> Unit)? = null,
    viewModel: AttendanceViewModel = getAppViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    var selectedDayPair by remember { mutableStateOf(weekDays.first()) }
    var showAddClass by remember { mutableStateOf(false) }
    var showSquadSync by remember { mutableStateOf(false) }

    val entries by viewModel.getTimetableForDay(selectedDayPair.second).collectAsState()
    val subjects by viewModel.activeSubjects.collectAsState()

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
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Weekly Timetable", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text("Schedule and class locations", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Surface(
                    color = NeonTealAlpha20,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.clickable { showSquadSync = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Group, contentDescription = "Squad Sync", tint = NeonTeal, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Squad Sync", color = NeonTeal, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                IconButton(onClick = {
                    importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                }) {
                    Icon(Icons.Filled.FileDownload, contentDescription = "Import", tint = MaterialTheme.colorScheme.outlineVariant)
                }

                IconButton(onClick = { viewModel.exportTimetable(context) }) {
                    Icon(Icons.Filled.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            // Day Selector Chips (Mon - Sun)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                items(weekDays) { pair ->
                    val isSelected = pair == selectedDayPair
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) NeonTeal else MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, if (isSelected) NeonTeal else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .clickable { selectedDayPair = pair }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            pair.first,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF003D35) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Class Cards List
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (entries.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.EventBusy, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(10.dp))
                                Text("No classes on ${selectedDayPair.first}", color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = { showSquadSync = true },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonTeal)
                                ) {
                                    Icon(Icons.Filled.Group, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Sync Timetable with Squad Code", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                } else {
                    items(entries, key = { it.id }) { entry ->
                        val sub = subjects.find { it.id == entry.subjectId }
                        TimetableSlotCard(
                            entry = entry,
                            code = sub?.code ?: "SUB",
                            name = sub?.name ?: "Subject",
                            onDelete = { viewModel.removeClass(entry.id) }
                        )
                    }
                }

                item {
                    TextButton(
                        onClick = { showAddClass = true },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = NeonTeal, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Class for ${selectedDayPair.first}", color = NeonTeal, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showAddClass) {
        AddClassSheet(
            selectedDayInt = selectedDayPair.second,
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
}

@Composable
private fun TimetableSlotCard(
    entry: TimetableEntity,
    code: String,
    name: String,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(NeonTealAlpha20, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Class, contentDescription = null, tint = NeonTeal, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = NeonTealAlpha20, shape = RoundedCornerShape(4.dp)) {
                        Text(code, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = NeonTeal, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Filled.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(entry.startTime, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outlineVariant)
                    if (entry.room.isNotBlank()) {
                        Text("  •  ${entry.room}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Close, contentDescription = "Delete", tint = AttendRed.copy(0.7f), modifier = Modifier.size(18.dp))
            }
        }
    }
}
