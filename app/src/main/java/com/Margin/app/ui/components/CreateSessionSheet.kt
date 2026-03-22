package com.attendease.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.attendease.app.ui.theme.*
import com.attendease.app.ui.viewmodel.SessionViewModel
import com.attendease.app.utils.getAppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SubjectInput(val code: String = "", val name: String = "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSessionSheet(
    onDismiss: () -> Unit,
    viewModel: SessionViewModel = getAppViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var sessionName by remember { mutableStateOf("") }
    var subjectCount by remember { mutableStateOf(6f) }
    var subjectInputs by remember { mutableStateOf(List(6) { SubjectInput() }) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf(System.currentTimeMillis()) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate)
    val formatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

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
                .padding(start = 24.dp, end = 24.dp, bottom = 40.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header text logic
            Column {
                Text("Create Semester", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                Text(
                    "Add a new academic term and define its subjects.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Session name field
            OutlinedTextField(
                value = sessionName,
                onValueChange = { sessionName = it },
                label = { Text("Semester Name", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                placeholder = { Text("e.g. Semester 2, 2024", color = MaterialTheme.colorScheme.outlineVariant) },
                leadingIcon = { Icon(Icons.Filled.EditNote, contentDescription = null, tint = NeonTeal) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonTeal,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    cursorColor = NeonTeal,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            // Start Date
            OutlinedTextField(
                value = formatter.format(Date(startDate)),
                onValueChange = {},
                readOnly = true,
                label = { Text("Start Date", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null, tint = NeonTeal) },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Date", tint = MaterialTheme.colorScheme.outlineVariant)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Subject Count Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Number of Subjects", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${subjectCount.toInt()}", fontWeight = FontWeight.Bold, color = NeonTeal)
                }
                Slider(
                    value = subjectCount,
                    onValueChange = { newValue -> 
                        val newInt = newValue.toInt()
                        if (newInt != subjectCount.toInt()) {
                            subjectInputs = List(newInt) { i -> subjectInputs.getOrNull(i) ?: SubjectInput() }
                        }
                        subjectCount = newValue 
                    },
                    valueRange = 1f..15f,
                    steps = 13,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonTeal,
                        activeTrackColor = NeonTeal,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            Text("Subjects", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                subjectInputs.forEachIndexed { index, input ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = input.code,
                            onValueChange = { newCode ->
                                subjectInputs = subjectInputs.toMutableList().apply { set(index, input.copy(code = newCode)) }
                            },
                            placeholder = { Text("Code (e.g. CS101)", color = MaterialTheme.colorScheme.outlineVariant) },
                            modifier = Modifier.weight(0.35f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonTeal,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                cursorColor = NeonTeal,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = input.name,
                            onValueChange = { newName ->
                                subjectInputs = subjectInputs.toMutableList().apply { set(index, input.copy(name = newName)) }
                            },
                            placeholder = { Text("Name (e.g. Data structures)", color = MaterialTheme.colorScheme.outlineVariant) },
                            modifier = Modifier.weight(0.65f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonTeal,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                cursorColor = NeonTeal,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            singleLine = true
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) { Text("Cancel") }
                val canCreate = sessionName.isNotBlank() && subjectInputs.all { it.code.isNotBlank() && it.name.isNotBlank() }
                Button(
                    onClick = {
                        if (canCreate) {
                            val payload = subjectInputs.map { Pair(it.code, it.name) }
                            viewModel.createSession(sessionName, startDate, payload)
                            onDismiss()
                        }
                    },
                    enabled = canCreate,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonTeal, contentColor = Color(0xFF003D35),
                        disabledContainerColor = NeonTeal.copy(alpha = 0.5f),
                        disabledContentColor = Color(0xFF003D35).copy(alpha = 0.5f)
                    )
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Create", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { startDate = it }
                    showDatePicker = false
                }) {
                    Text("OK", color = NeonTeal)
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
