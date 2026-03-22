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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attendease.app.ui.components.CreateSessionSheet
import com.attendease.app.ui.theme.*
import com.attendease.app.ui.viewmodel.SessionUiState
import com.attendease.app.ui.viewmodel.SessionViewModel
import com.attendease.app.utils.getAppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionManagementScreen(
    onBack: () -> Unit = {},
    viewModel: SessionViewModel = getAppViewModel()
) {
    val sessions by viewModel.sessionsList.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()

    var showCreateSheet by remember(sessions.isEmpty()) { mutableStateOf(sessions.isEmpty()) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                // ── TopBar ─────────────────────────────────────────
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
                        Text("Select Session", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                        Text("Switch or create a semester", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                // ── Action Buttons ──────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { showCreateSheet = true },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonTeal, contentColor = Color(0xFF003D35))
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Create New", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {},
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant, containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Import Sheet")
                    }
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
                    session = session,
                    isActive = session.id == activeSession?.id,
                    totalSessionsCount = sessions.size,
                    onSelect = { 
                        viewModel.setActiveSession(session.id) 
                        onBack()
                    },
                    onDelete = { viewModel.deleteSession(session.id) }
                )
            }
        }
    }

    if (showCreateSheet) {
        CreateSessionSheet(onDismiss = { showCreateSheet = false })
    }
}

@Composable
private fun SessionCard(
    session: SessionUiState,
    isActive: Boolean,
    totalSessionsCount: Int,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val pct = if (session.totalClasses > 0)
        session.attendedClasses.toFloat() / session.totalClasses else 0f
    val borderColor = if (isActive) NeonTeal else MaterialTheme.colorScheme.outline

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 7.dp)
            .clickable { onSelect() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(if (isActive) 1.5.dp else 1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        session.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isActive) NeonTeal else MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Book, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${session.subjectCount} subjects",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (session.startedDaysAgo > 0) {
                            Text("  •  ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outlineVariant)
                            Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(
                                "Started ${session.startedDaysAgo} days ago",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                // Active checkmark & Delete
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (totalSessionsCount > 1 && !isActive) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = AttendRed, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isActive) NeonTeal else MaterialTheme.colorScheme.surface,
                                CircleShape
                            )
                            .border(1.5.dp, if (isActive) NeonTeal else MaterialTheme.colorScheme.outline, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isActive) {
                            Icon(Icons.Filled.Check, contentDescription = "Active", tint = Color(0xFF003D35), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // Progress bar + stats
            if (session.totalClasses > 0) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${session.attendedClasses}/${session.totalClasses} classes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${(pct * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (pct >= 0.75f) NeonTeal else AttendRed,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { pct },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                    color = if (pct >= 0.75f) NeonTeal else AttendRed,
                    trackColor = MaterialTheme.colorScheme.outline,
                    strokeCap = StrokeCap.Round
                )
            } else {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonTealAlpha20)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = NeonTeal, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Not started yet", style = MaterialTheme.typography.labelSmall, color = NeonTeal)
                }
            }
        }
    }
}
