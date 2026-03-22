package com.attendease.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import com.attendease.app.ui.theme.*
import com.attendease.app.ui.viewmodel.SessionViewModel
import com.attendease.app.ui.viewmodel.TaskViewModel
import com.attendease.app.utils.getAppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskSheet(
    onDismiss: () -> Unit,
    taskViewModel: TaskViewModel = getAppViewModel(),
    sessionViewModel: SessionViewModel = getAppViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var taskTitle by remember { mutableStateOf("") }
    var taskDescription by remember { mutableStateOf("") }
    
    var selectedType by remember { mutableStateOf("") }
    var typeExpanded by remember { mutableStateOf(false) }

    var selectedSubjectCode by remember { mutableStateOf("") }
    var selectedSubjectName by remember { mutableStateOf("") }
    var subjectExpanded by remember { mutableStateOf(false) }

    var reminderEnabled by remember { mutableStateOf(false) }
    
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            reminderEnabled = false
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var dueDate by remember { mutableStateOf(System.currentTimeMillis() + 86400000) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDate)
    val formatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    val taskTypes = listOf("Assignment", "Presentation", "Practical File", "Study Goal", "Other")

    val subjects by sessionViewModel.activeSubjects.collectAsState()
    val subjectPairs = subjects.map { it.code to it.name }
    
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(modifier = Modifier.padding(vertical = 14.dp)) {
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(YellowAlpha20, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Task, contentDescription = null, tint = Yellow, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Add Task", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                    Text("Track a pending assignment or deliverable", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            // Task title
            OutlinedTextField(
                value = taskTitle,
                onValueChange = { taskTitle = it },
                label = { Text("Task Title", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                placeholder = { Text("e.g. OS Assignment 3", color = MaterialTheme.colorScheme.outlineVariant) },
                leadingIcon = { Icon(Icons.Filled.EditNote, contentDescription = null, tint = Yellow) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Yellow,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    cursorColor = Yellow,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Subject dropdown
            ExposedDropdownMenuBox(
                expanded = subjectExpanded,
                onExpandedChange = { subjectExpanded = !subjectExpanded }
            ) {
                OutlinedTextField(
                    value = if (selectedSubjectCode.isEmpty()) "" else "$selectedSubjectCode – $selectedSubjectName",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Subject", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    placeholder = { Text("Select a subject", color = MaterialTheme.colorScheme.outlineVariant) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) },
                    leadingIcon = { Icon(Icons.Filled.Book, contentDescription = null, tint = Yellow) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Yellow,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                ExposedDropdownMenu(
                    expanded = subjectExpanded,
                    onDismissRequest = { subjectExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    subjectPairs.forEach { (code, name) ->
                        DropdownMenuItem(
                            text = { Text("$code – $name", color = MaterialTheme.colorScheme.onBackground) },
                            onClick = { selectedSubjectCode = code; selectedSubjectName = name; subjectExpanded = false }
                        )
                    }
                }
            }

            // Type dropdown
            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = !typeExpanded }
            ) {
                OutlinedTextField(
                    value = selectedType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Task Type", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    placeholder = { Text("Select type", color = MaterialTheme.colorScheme.outlineVariant) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    leadingIcon = { Icon(Icons.Filled.Category, contentDescription = null, tint = Yellow) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Yellow,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    taskTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type, color = MaterialTheme.colorScheme.onBackground) },
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
                label = { Text("Due Date", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null, tint = Yellow) },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Date", tint = MaterialTheme.colorScheme.outlineVariant)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Reminder toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Alarm, contentDescription = null, tint = Magenta, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Enable Reminder", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                    Text("Remind me before deadline", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outlineVariant)
                }
                Switch(
                    checked = reminderEnabled,
                    onCheckedChange = { isChecked -> 
                        reminderEnabled = isChecked
                        if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF3D0020),
                        checkedTrackColor = Magenta,
                        uncheckedTrackColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            Spacer(Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) { Text("Cancel") }
                Button(
                    onClick = {
                        if (taskTitle.isNotBlank() && selectedType.isNotBlank() && selectedSubjectCode.isNotBlank()) {
                            taskViewModel.addTask(
                                context = context,
                                title = taskTitle,
                                typeString = selectedType,
                                subjectCode = selectedSubjectCode,
                                dueDate = dueDate,
                                hasReminder = reminderEnabled
                            )
                            onDismiss()
                        }
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTeal, contentColor = Color(0xFF003D35))
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Task", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dueDate = it }
                    showDatePicker = false
                }) {
                    Text("OK", color = Yellow)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
