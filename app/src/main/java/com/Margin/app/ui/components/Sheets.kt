package com.Margin.app.ui.components

import android.Manifest
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.Margin.app.data.local.entity.AttendanceRecordEntity
import com.Margin.app.data.local.entity.TaskType
import com.Margin.app.domain.AttendanceMath
import com.Margin.app.ui.theme.*
import com.Margin.app.ui.viewmodel.AttendanceViewModel
import com.Margin.app.ui.viewmodel.TaskViewModel
import com.Margin.app.utils.TimetableExportManager
import java.text.SimpleDateFormat
import java.util.*

// ── ADD TASK SHEET ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskSheet(
    onDismiss: () -> Unit,
    taskViewModel: TaskViewModel
) {
    val context = LocalContext.current
    val subjects by taskViewModel.subjects.collectAsState()

    var taskTitle by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TaskType.ASSIGNMENT) }
    var typeExpanded by remember { mutableStateOf(false) }

    var selectedSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: "") }
    var subjectExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(subjects) {
        if (selectedSubjectId.isBlank() && subjects.isNotEmpty()) {
            selectedSubjectId = subjects.first().id
        }
    }

    var reminderEnabled by remember { mutableStateOf(false) }
    var dueDate by remember { mutableStateOf(System.currentTimeMillis() + 86400000) }
    var showDatePicker by remember { mutableStateOf(false) }
    val formatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> if (!isGranted) reminderEnabled = false }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).background(NeonTealAlpha20, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Task, contentDescription = null, tint = NeonTeal, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Add Task", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text("Track assignments, labs, and deadlines", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            OutlinedTextField(
                value = taskTitle,
                onValueChange = { taskTitle = it },
                label = { Text("Task Title") },
                placeholder = { Text("e.g. Operating Systems Lab 3") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Subject Dropdown
            ExposedDropdownMenuBox(
                expanded = subjectExpanded,
                onExpandedChange = { subjectExpanded = !subjectExpanded }
            ) {
                val currentSub = subjects.find { it.id == selectedSubjectId }
                OutlinedTextField(
                    value = if (currentSub != null) "${currentSub.code} – ${currentSub.name}" else "Select Subject",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Subject") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = subjectExpanded,
                    onDismissRequest = { subjectExpanded = false }
                ) {
                    subjects.forEach { sub ->
                        DropdownMenuItem(
                            text = { Text("${sub.code} – ${sub.name}") },
                            onClick = { selectedSubjectId = sub.id; subjectExpanded = false }
                        )
                    }
                }
            }

            // Task Type Dropdown
            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = !typeExpanded }
            ) {
                OutlinedTextField(
                    value = selectedType.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Task Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false }
                ) {
                    TaskType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayName) },
                            onClick = { selectedType = type; typeExpanded = false }
                        )
                    }
                }
            }

            // Due Date
            OutlinedTextField(
                value = formatter.format(Date(dueDate)),
                onValueChange = {},
                readOnly = true,
                label = { Text("Due Date") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.EditCalendar, contentDescription = "Pick Date", tint = NeonTeal)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Reminder Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Remind Me Before Due Date", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                    Text("Alarms on the day before and due date", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = reminderEnabled,
                    onCheckedChange = { isChecked ->
                        reminderEnabled = isChecked
                        if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Cancel") }

                Button(
                    onClick = {
                        if (taskTitle.isNotBlank() && selectedSubjectId.isNotBlank()) {
                            taskViewModel.addTask(
                                context = context,
                                title = taskTitle.trim(),
                                type = selectedType.name,
                                subjectId = selectedSubjectId,
                                dueDate = dueDate,
                                hasReminder = reminderEnabled
                            )
                            onDismiss()
                        }
                    },
                    enabled = taskTitle.isNotBlank() && selectedSubjectId.isNotBlank(),
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTeal, contentColor = Color(0xFF003D35))
                ) { Text("Save Task", fontWeight = FontWeight.Bold) }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = AttendanceMath.localToUtcMidnight(dueDate)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        dueDate = AttendanceMath.utcToLocalMidnight(it)
                    }
                    showDatePicker = false
                }) { Text("OK", color = NeonTeal) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}

// ── ADD CLASS SHEET ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClassSheet(
    selectedDayInt: Int = 1,
    onDismiss: () -> Unit,
    viewModel: AttendanceViewModel
) {
    val subjects by viewModel.activeSubjects.collectAsState()
    var subjectCode by remember { mutableStateOf("") }
    var subjectName by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("09:00 AM") }
    var room by remember { mutableStateOf("") }
    var subjectExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Add Class to Timetable", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)

            ExposedDropdownMenuBox(
                expanded = subjectExpanded,
                onExpandedChange = { subjectExpanded = !subjectExpanded }
            ) {
                OutlinedTextField(
                    value = subjectCode,
                    onValueChange = { subjectCode = it; subjectExpanded = true },
                    label = { Text("Subject Code") },
                    placeholder = { Text("e.g. CS101") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                val filtered = subjects.filter { it.code.contains(subjectCode, true) || it.name.contains(subjectCode, true) }
                if (filtered.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = subjectExpanded,
                        onDismissRequest = { subjectExpanded = false }
                    ) {
                        filtered.forEach { s ->
                            DropdownMenuItem(
                                text = { Text("${s.code} – ${s.name}") },
                                onClick = {
                                    subjectCode = s.code
                                    subjectName = s.name
                                    subjectExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = subjectName,
                onValueChange = { subjectName = it },
                label = { Text("Subject Name") },
                placeholder = { Text("e.g. Operating Systems") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = time,
                onValueChange = { time = it },
                label = { Text("Start Time") },
                placeholder = { Text("09:00 AM") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = room,
                onValueChange = { room = it },
                label = { Text("Room / Lab (Optional)") },
                placeholder = { Text("e.g. Room 302 or Lab 2") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Cancel") }

                Button(
                    onClick = {
                        if (subjectCode.isNotBlank() && subjectName.isNotBlank() && time.isNotBlank()) {
                            viewModel.addClass(selectedDayInt, time.trim(), room.trim(), subjectCode.trim(), subjectName.trim())
                            onDismiss()
                        }
                    },
                    enabled = subjectCode.isNotBlank() && subjectName.isNotBlank() && time.isNotBlank(),
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTeal, contentColor = Color(0xFF003D35))
                ) { Text("Save Class", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// ── CREATE SEMESTER SHEET ─────────────────────────────────────────────────────

data class SubjectDraft(val code: String = "", val name: String = "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSessionSheet(
    onDismiss: () -> Unit,
    viewModel: AttendanceViewModel
) {
    val context = LocalContext.current
    var sessionName by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var subjectCount by remember { mutableStateOf(5) }
    var subjectsList by remember { mutableStateOf(List(5) { SubjectDraft() }) }
    var importedSlots by remember { mutableStateOf<List<Triple<String, Int, String>>>(emptyList()) }

    var showDatePicker by remember { mutableStateOf(false) }
    val formatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val stream = context.contentResolver.openInputStream(it) ?: return@let
                val res = stream.use { s -> TimetableExportManager.parseFromStream(s) }
                when (res) {
                    is TimetableExportManager.ParseResult.Success -> {
                        sessionName = res.sessionName
                        subjectCount = res.subjects.size
                        subjectsList = res.subjects.map { SubjectDraft(it.first, it.second) }
                        importedSlots = res.slots
                        Toast.makeText(context, "Loaded ${res.subjects.size} subjects from JSON", Toast.LENGTH_SHORT).show()
                    }
                    is TimetableExportManager.ParseResult.Failure -> {
                        Toast.makeText(context, "Import failed: ${res.reason}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Create Semester", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text("Define semester subjects & timetable", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = {
                    importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                }) {
                    Icon(Icons.Filled.FileDownload, contentDescription = "Import JSON", tint = NeonTeal)
                }
            }

            OutlinedTextField(
                value = sessionName,
                onValueChange = { sessionName = it },
                label = { Text("Semester / Term Name") },
                placeholder = { Text("e.g. Semester 4 (2025)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = formatter.format(Date(startDate)),
                onValueChange = {},
                readOnly = true,
                label = { Text("Semester Start Date") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = NeonTeal)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Subjects: $subjectCount", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Row {
                    IconButton(onClick = {
                        if (subjectCount > 1) {
                            subjectCount--
                            subjectsList = subjectsList.take(subjectCount)
                        }
                    }) { Icon(Icons.Filled.RemoveCircleOutline, contentDescription = "Fewer", tint = NeonTeal) }
                    IconButton(onClick = {
                        if (subjectCount < 15) {
                            subjectCount++
                            subjectsList = subjectsList + SubjectDraft()
                        }
                    }) { Icon(Icons.Filled.AddCircleOutline, contentDescription = "More", tint = NeonTeal) }
                }
            }

            subjectsList.forEachIndexed { idx, sub ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sub.code,
                        onValueChange = { newCode ->
                            subjectsList = subjectsList.toMutableList().apply { set(idx, sub.copy(code = newCode)) }
                        },
                        placeholder = { Text("Code (CS101)") },
                        modifier = Modifier.weight(0.35f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = sub.name,
                        onValueChange = { newName ->
                            subjectsList = subjectsList.toMutableList().apply { set(idx, sub.copy(name = newName)) }
                        },
                        placeholder = { Text("Subject Name") },
                        modifier = Modifier.weight(0.65f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }
            }

            val isValid = sessionName.isNotBlank() && subjectsList.all { it.code.isNotBlank() && it.name.isNotBlank() }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Cancel") }

                Button(
                    onClick = {
                        if (isValid) {
                            val payload = subjectsList.map { Pair(it.code.trim(), it.name.trim()) }
                            viewModel.createSession(sessionName.trim(), startDate, payload, importedSlots)
                            onDismiss()
                        }
                    },
                    enabled = isValid,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTeal, contentColor = Color(0xFF003D35))
                ) { Text("Create", fontWeight = FontWeight.Bold) }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = AttendanceMath.localToUtcMidnight(startDate)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        startDate = AttendanceMath.utcToLocalMidnight(it)
                    }
                    showDatePicker = false
                }) { Text("OK", color = NeonTeal) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}

// ── EXTRA DUTY SHEET ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtraDutySheet(
    subjectId: String,
    subjectName: String,
    viewModel: AttendanceViewModel,
    onDismiss: () -> Unit
) {
    val dutyRecords by viewModel.dutyRecordsFlow(subjectId).collectAsState(emptyList())
    var selectedDateMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val formatter = remember { SimpleDateFormat("EEE, MMM dd yyyy", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Duty & Extra Attendance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text(subjectName, style = MaterialTheme.typography.bodySmall, color = NeonTeal)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Close") }
            }

            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = NeonTeal, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(formatter.format(Date(selectedDateMs)), color = MaterialTheme.colorScheme.onBackground)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        viewModel.addDutyRecord(subjectId, selectedDateMs, "DUTY")
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AttendGreen.copy(0.2f), contentColor = AttendGreen)
                ) {
                    Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Duty (+Present)", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        viewModel.addDutyRecord(subjectId, selectedDateMs, "DUTY_ABSENT")
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AttendRed.copy(0.2f), contentColor = AttendRed)
                ) {
                    Icon(Icons.Filled.RemoveCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Duty Absent", fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            Text("Logged Entries (${dutyRecords.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)

            if (dutyRecords.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    Text("No duty entries logged yet", color = MaterialTheme.colorScheme.outlineVariant)
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dutyRecords.forEach { rec ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                val isPresent = rec.status == "DUTY"
                                Text(
                                    if (isPresent) "Duty (+Present)" else "Duty Absent",
                                    color = if (isPresent) AttendGreen else AttendRed,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(formatter.format(Date(rec.date)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outlineVariant)
                            }
                            IconButton(onClick = { viewModel.deleteDutyRecord(rec.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = AttendRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = AttendanceMath.localToUtcMidnight(selectedDateMs)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDateMs = AttendanceMath.utcToLocalMidnight(it)
                    }
                    showDatePicker = false
                }) { Text("OK", color = NeonTeal) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}

// ── SEMESTER OVERVIEW & MANAGEMENT SHEET ──────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemesterOverviewSheet(
    onDismiss: () -> Unit,
    viewModel: AttendanceViewModel,
    onCreateNewSemester: () -> Unit
) {
    val activeSession by viewModel.activeSession.collectAsState()
    val allSessions by viewModel.allSessions.collectAsState()
    val analytics by viewModel.analytics.collectAsState()
    val subjects by viewModel.subjectOverviews.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(activeSession?.name ?: "No Active Semester", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = NeonTeal)
                    Text("Overall Performance & Margin", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    color = NeonTealAlpha20,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { onCreateNewSemester(); onDismiss() }
                ) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = NeonTeal, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New Term", color = NeonTeal, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Big Circular Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(96.dp)) {
                        CircularProgressIndicator(
                            progress = { (analytics.overallPercentage / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.size(96.dp),
                            color = if (analytics.overallPercentage >= 75f) NeonTeal else AttendRed,
                            strokeWidth = 8.dp,
                            strokeCap = StrokeCap.Round
                        )
                        Text(
                            String.format(Locale.US, "%.1f%%", analytics.overallPercentage),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Total: ${analytics.totalAttended} / ${analytics.totalClasses} classes", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Total Bunks: ${analytics.totalBunks}", style = MaterialTheme.typography.bodySmall, color = AttendRed)
                        if (analytics.overallPercentage >= 75f) {
                            Text("Safe Bunks: ${analytics.safeBunksLeft}", color = AttendGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text("Need ${analytics.classesNeeded} classes to 75%", color = AttendRed, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                        if (!analytics.mostBunkedSubjectName.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = AttendOrange, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Wall of Shame: ${analytics.mostBunkedSubjectName}", color = AttendOrange, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (!analytics.heaviestWorkloadSubjectName.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = Magenta, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Heavy Load: ${analytics.heaviestWorkloadSubjectName}", color = Magenta, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Subject Breakdown
            Text("Subject Margins & Quick Adjust", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            subjects.forEach { s ->
                val barColor = when { s.percentage >= 75f -> AttendGreen; s.percentage >= 65f -> AttendOrange; else -> AttendRed }
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${s.code} – ${s.name}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text(String.format(Locale.US, "%.1f%%", s.percentage), fontWeight = FontWeight.Bold, color = barColor)
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (s.percentage / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                            color = barColor,
                            strokeCap = StrokeCap.Round
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${s.attended}/${s.total} attended", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outlineVariant)
                            if (s.percentage >= 75f) {
                                Text("Safe to bunk: ${s.safeBunks}", color = AttendGreen, style = MaterialTheme.typography.labelSmall)
                            } else {
                                Text("Need: ${s.neededToRecover} more", color = AttendRed, style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.incrementBunk(s.id) },
                                modifier = Modifier.weight(1f).height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = AttendRed.copy(0.12f),
                                    contentColor = AttendRed
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Bunk", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("- Bunk", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.incrementPresent(s.id) },
                                modifier = Modifier.weight(1f).height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AttendGreen.copy(0.2f),
                                    contentColor = AttendGreen
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Present", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("+ Present", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            IconButton(
                                onClick = { viewModel.decrementAttendance(s.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Switch Semester Section
            if (allSessions.size > 1) {
                Text("Switch Semester", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                allSessions.forEach { sess ->
                    val isCurr = sess.id == activeSession?.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isCurr) NeonTealAlpha20 else MaterialTheme.colorScheme.surface)
                            .clickable { viewModel.setActiveSession(sess.id); onDismiss() }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(sess.name, fontWeight = if (isCurr) FontWeight.Bold else FontWeight.Normal, color = if (isCurr) NeonTeal else MaterialTheme.colorScheme.onBackground)
                        if (isCurr) {
                            Icon(Icons.Filled.Check, contentDescription = "Active", tint = NeonTeal, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

// ── SQUAD SYNC SHEET ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SquadSyncSheet(
    onDismiss: () -> Unit,
    viewModel: AttendanceViewModel
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val activeSession by viewModel.activeSession.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var squadCode by remember { mutableStateOf("") }
    var inputCode by remember { mutableStateOf("") }
    var isCopied by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.getSquadCode { code -> squadCode = code }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(38.dp).background(NeonTealAlpha20, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Group, contentDescription = null, tint = NeonTeal, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Squad Sync", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text("1-Tap timetable sharing with your friends", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = NeonTeal,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = NeonTeal
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Share Schedule", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Join / Import", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            if (selectedTab == 0) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(activeSession?.name ?: "Current Timetable", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = NeonTeal)
                        Text(
                            "Share this Squad Code with your classmates so they can sync this exact timetable in seconds!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = squadCode,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Your Squad Code") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            maxLines = 3
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(squadCode))
                            isCopied = true
                            Toast.makeText(context, "Squad Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(if (isCopied) Icons.Filled.Check else Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isCopied) "Copied!" else "Copy Code", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Timetable Squad Code — ${activeSession?.name}")
                                putExtra(Intent.EXTRA_TEXT, "Here's our class timetable Squad Code for Margin! Copy this code and open Margin -> Sync:\n\n$squadCode")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Squad Code via…").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonTeal, contentColor = Color(0xFF003D35))
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share via Apps", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Paste Squad Code", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = NeonTeal)
                        Text(
                            "Paste the Squad Code your friend sent you to import all subjects and timetable slots.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = inputCode,
                            onValueChange = { inputCode = it },
                            placeholder = { Text("Paste MARGIN:... code here") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            maxLines = 4
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            val clip = clipboardManager.getText()?.text
                            if (!clip.isNullOrBlank()) {
                                inputCode = clip.trim()
                            } else {
                                Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Paste", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            if (inputCode.isNotBlank()) {
                                viewModel.importFromSquadCode(inputCode.trim()) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    if (success) onDismiss()
                                }
                            }
                        },
                        enabled = inputCode.isNotBlank(),
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonTeal, contentColor = Color(0xFF003D35))
                    ) {
                        Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sync Timetable", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── SWAP CLASS SHEET ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwapClassSheet(
    slotId: String,
    currentSubjectId: String,
    currentSubjectCode: String,
    currentSubjectName: String,
    time: String,
    viewModel: AttendanceViewModel,
    onDismiss: () -> Unit
) {
    val subjects by viewModel.activeSubjects.collectAsState()
    var selectedSubjectId by remember { mutableStateOf(currentSubjectId) }
    var subjectExpanded by remember { mutableStateOf(false) }
    var selectedStatus by remember { mutableStateOf("PRESENT") }
    var note by remember { mutableStateOf("Substitute class") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(38.dp).background(NeonTealAlpha20, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = null, tint = NeonTeal, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Swap / Substitute Class", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text("Scheduled: $currentSubjectCode • $time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Text("Which subject took this period instead?", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)

            ExposedDropdownMenuBox(
                expanded = subjectExpanded,
                onExpandedChange = { subjectExpanded = !subjectExpanded }
            ) {
                val chosenSub = subjects.find { it.id == selectedSubjectId }
                OutlinedTextField(
                    value = chosenSub?.let { "${it.code} – ${it.name}" } ?: "Select Substitute Subject",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Actual Subject") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = subjectExpanded,
                    onDismissRequest = { subjectExpanded = false }
                ) {
                    subjects.forEach { s ->
                        DropdownMenuItem(
                            text = { Text("${s.code} – ${s.name}") },
                            onClick = {
                                selectedSubjectId = s.id
                                subjectExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note / Reason") },
                placeholder = { Text("e.g. Swapped with Dr. Smith, extra lab, etc.") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Cancel") }

                Button(
                    onClick = {
                        viewModel.swapClass(
                            oldSubjectId = currentSubjectId,
                            newSubjectId = selectedSubjectId,
                            slotId = slotId,
                            status = selectedStatus,
                            note = note.trim()
                        )
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTeal, contentColor = Color(0xFF003D35))
                ) { Text("Save Swap", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// ── LOG SEMINAR SHEET ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogSeminarSheet(
    onDismiss: () -> Unit,
    viewModel: AttendanceViewModel
) {
    var seminarTitle by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(38.dp).background(PurpleAlpha20, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Log Seminar / Campus Event", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text("Excuses today's attendance without penalizing bunk margins", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            OutlinedTextField(
                value = seminarTitle,
                onValueChange = { seminarTitle = it },
                label = { Text("Seminar / Event Name") },
                placeholder = { Text("e.g. AI Guest Lecture, Hackathon, College Fest") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Cancel") }

                Button(
                    onClick = {
                        val title = if (seminarTitle.isNotBlank()) seminarTitle.trim() else "Seminar / Event"
                        viewModel.logSeminar(title)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent, contentColor = Color.White)
                ) { Text("Apply Seminar", fontWeight = FontWeight.Bold) }
            }
        }
    }
}
