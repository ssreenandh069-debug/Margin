package com.Margin.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.Margin.app.ui.theme.*
import com.Margin.app.ui.viewmodel.TimetableViewModel
import com.Margin.app.utils.getAppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClassSheet(
    selectedDay: String = "Mon",
    onDismiss: () -> Unit,
    viewModel: TimetableViewModel = getAppViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var subjectCode by remember { mutableStateOf("") }
    var subjectName by remember { mutableStateOf("") }
    var time                by remember { mutableStateOf("") }
    var room                by remember { mutableStateOf("") }
    var subjectExpanded     by remember { mutableStateOf(false) }

    val subjectsList by viewModel.subjects.collectAsState()
    val subjectPairs = subjectsList.map { it.code to it.name }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(modifier = Modifier.padding(vertical = 14.dp)) {
                Box(modifier = Modifier.width(44.dp).height(4.dp).background(MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp)))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).background(NeonTealAlpha20, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Class, contentDescription = null, tint = NeonTeal, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Add Class", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                    Text("Adding to $selectedDay", style = MaterialTheme.typography.bodySmall, color = NeonTeal)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            // Subject Code Dropdown/Input
            ExposedDropdownMenuBox(
                expanded = subjectExpanded,
                onExpandedChange = { subjectExpanded = !subjectExpanded }
            ) {
                OutlinedTextField(
                    value = subjectCode,
                    onValueChange = { 
                        subjectCode = it
                        subjectExpanded = true 
                        // Auto-clear name if typing fresh so it doesn't leave stray matched names
                        if (subjectPairs.none { p -> p.first == it }) subjectName = ""
                    },
                    label = { Text("Subject Code", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    placeholder = { Text("e.g. CS101", color = MaterialTheme.colorScheme.outlineVariant) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) },
                    leadingIcon = { Icon(Icons.Filled.Book, contentDescription = null, tint = NeonTeal) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonTeal, unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground, unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                
                val filtered = subjectPairs.filter { it.first.contains(subjectCode, ignoreCase = true) || it.second.contains(subjectCode, ignoreCase = true) }
                if (filtered.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = subjectExpanded,
                        onDismissRequest = { subjectExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        filtered.forEach { (c, n) ->
                            DropdownMenuItem(
                                text = { Text("$c – $n", color = MaterialTheme.colorScheme.onBackground) },
                                onClick = { 
                                    subjectCode = c
                                    subjectName = n
                                    subjectExpanded = false 
                                }
                            )
                        }
                    }
                }
            }

            // Subject Name
            OutlinedTextField(
                value = subjectName,
                onValueChange = { subjectName = it },
                label = { Text("Subject Name", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                placeholder = { Text("e.g. Intro to Computer Science", color = MaterialTheme.colorScheme.outlineVariant) },
                leadingIcon = { Icon(Icons.Filled.Abc, contentDescription = null, tint = NeonTeal) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonTeal, unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground, unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    cursorColor = NeonTeal,
                    focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Time row — displayed as a styled text field for simplicity
            OutlinedTextField(
                value = time,
                onValueChange = { time = it },
                label = { Text("Time (e.g. 09:00 AM)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                placeholder = { Text("09:00 AM", color = MaterialTheme.colorScheme.outlineVariant) },
                leadingIcon = { Icon(Icons.Filled.AccessTime, contentDescription = null, tint = NeonTeal) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonTeal, unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground, unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    cursorColor = NeonTeal,
                    focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Room
            OutlinedTextField(
                value = room,
                onValueChange = { room = it },
                label = { Text("Room / Location", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                placeholder = { Text("e.g. Lab A", color = MaterialTheme.colorScheme.outlineVariant) },
                leadingIcon = { Icon(Icons.Filled.Room, contentDescription = null, tint = NeonTeal) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonTeal, unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground, unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    cursorColor = NeonTeal,
                    focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

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
                        if (subjectCode.isNotBlank() && subjectName.isNotBlank() && time.isNotBlank()) {
                            viewModel.addClass(selectedDay, subjectCode, subjectName, time)
                        }
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTeal, contentColor = Color(0xFF003D35))
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Class", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
