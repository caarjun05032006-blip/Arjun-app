package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActivityItem
import com.example.data.model.ActivitySession
import com.example.ui.components.MetricCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activities by viewModel.allActivities.collectAsState()
    val sessions by viewModel.allActivitySessions.collectAsState()

    val activeActivityId by viewModel.activeStopwatchActivityId.collectAsState()
    val elapsedSeconds by viewModel.stopwatchElapsedSeconds.collectAsState()
    val isRunning by viewModel.isStopwatchRunning.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showManualSessionDialog by remember { mutableStateOf<ActivityItem?>(null) }
    var selectedActivityForStats by remember { mutableStateOf<ActivityItem?>(null) }
    var showStopDialog by remember { mutableStateOf(false) }
    var stopSessionNotes by remember { mutableStateOf("") }

    val activeActivity = activities.find { it.id == activeActivityId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity Time Tracker", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.testTag("btn_create_activity_top")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create Activity")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = TealAccent,
                modifier = Modifier.testTag("fab_create_activity")
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Activity", tint = Color.White)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
        ) {
            // Live Stopwatch Banner if active
            if (activeActivityId != null && activeActivity != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "ACTIVE STOPWATCH • ${activeActivity.name.uppercase()}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            val hrs = elapsedSeconds / 3600
                            val mins = (elapsedSeconds % 3600) / 60
                            val secs = elapsedSeconds % 60
                            Text(
                                text = "%02d:%02d:%02d".format(hrs, mins, secs),
                                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isRunning) {
                                    Button(
                                        onClick = { viewModel.pauseStopwatch() },
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                                        modifier = Modifier.testTag("btn_pause_stopwatch")
                                    ) {
                                        Icon(Icons.Default.Pause, contentDescription = "Pause", tint = Slate900)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Pause", color = Slate900)
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.resumeStopwatch() },
                                        colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                                        modifier = Modifier.testTag("btn_resume_stopwatch")
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Resume")
                                    }
                                }

                                Button(
                                    onClick = { showStopDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.testTag("btn_stop_stopwatch")
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = "Stop")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Stop & Save")
                                }
                            }
                        }
                    }
                }
            }

            // CRITICAL: Empty state when no activities exist
            if (activities.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("empty_activities_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No Activities Created Yet",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "All activities are custom and created manually by you. Tap below to create your first activity (e.g., Audit, Study, Gym, Reading, Project Work).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = { showCreateDialog = true },
                                modifier = Modifier.testTag("btn_create_first_activity")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create Your First Activity")
                            }
                        }
                    }
                }
            } else {
                // Activity List with live start button
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "My Activities (${activities.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        TextButton(onClick = { showCreateDialog = true }) {
                            Text("+ New Activity")
                        }
                    }
                }

                items(activities, key = { it.id }) { act ->
                    val actSessions = sessions.filter { it.activityId == act.id }
                    val totalSec = actSessions.sumOf { it.durationSeconds }
                    val isThisRunning = activeActivityId == act.id && isRunning

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedActivityForStats = act }
                            .testTag("activity_card_${act.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(TealAccent)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = act.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Badge { Text(act.category, fontSize = 9.sp) }
                                }
                                Text(
                                    text = "%dh %dm".format(totalSec / 3600, (totalSec % 3600) / 60),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TealAccent
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${actSessions.size} session(s) recorded • Tap to view complete statistics",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TextButton(onClick = { showManualSessionDialog = act }) {
                                        Text("+ Manual Log", fontSize = 11.sp)
                                    }
                                    TextButton(onClick = { selectedActivityForStats = act }) {
                                        Text("View Stats", fontSize = 11.sp)
                                    }
                                }

                                if (isThisRunning) {
                                    Button(
                                        onClick = { viewModel.pauseStopwatch() },
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp), tint = Slate900)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Pause", fontSize = 11.sp, color = Slate900)
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.startStopwatch(act.id) },
                                        modifier = Modifier.height(34.dp).testTag("start_stopwatch_${act.id}")
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Start", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Detailed Statistics for selected activity
            if (selectedActivityForStats != null) {
                val act = selectedActivityForStats!!
                val actSessions = sessions.filter { it.activityId == act.id }

                val now = System.currentTimeMillis()
                val oneDay = 24 * 3600 * 1000L
                val oneWeek = 7 * oneDay
                val oneMonth = 30 * oneDay
                val oneQuarter = 90 * oneDay
                val oneYear = 365 * oneDay

                val todaySec = actSessions.filter { it.startTime >= now - oneDay }.sumOf { it.durationSeconds }
                val weekSec = actSessions.filter { it.startTime >= now - oneWeek }.sumOf { it.durationSeconds }
                val monthSec = actSessions.filter { it.startTime >= now - oneMonth }.sumOf { it.durationSeconds }
                val quarterSec = actSessions.filter { it.startTime >= now - oneQuarter }.sumOf { it.durationSeconds }
                val yearSec = actSessions.filter { it.startTime >= now - oneYear }.sumOf { it.durationSeconds }

                val avgSec = if (actSessions.isNotEmpty()) actSessions.map { it.durationSeconds }.average().toLong() else 0L
                val longestSec = actSessions.maxOfOrNull { it.durationSeconds } ?: 0L

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("COMPLETE TIME STATISTICS", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        text = act.name,
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = TealAccent
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteActivity(act); selectedActivityForStats = null }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Activity", tint = MaterialTheme.colorScheme.error)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Today", style = MaterialTheme.typography.labelSmall)
                                    Text("%dh %dm".format(todaySec / 3600, (todaySec % 3600) / 60), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("This Week", style = MaterialTheme.typography.labelSmall)
                                    Text("%dh %dm".format(weekSec / 3600, (weekSec % 3600) / 60), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("This Month", style = MaterialTheme.typography.labelSmall)
                                    Text("%dh %dm".format(monthSec / 3600, (monthSec % 3600) / 60), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("This Quarter", style = MaterialTheme.typography.labelSmall)
                                    Text("%dh %dm".format(quarterSec / 3600, (quarterSec % 3600) / 60), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("This Year", style = MaterialTheme.typography.labelSmall)
                                    Text("%dh %dm".format(yearSec / 3600, (yearSec % 3600) / 60), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Total Sessions", style = MaterialTheme.typography.labelSmall)
                                    Text("${actSessions.size}", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Average Session: %dh %dm".format(avgSec / 3600, (avgSec % 3600) / 60), style = MaterialTheme.typography.bodySmall)
                                Text("Longest: %dh %dm".format(longestSec / 3600, (longestSec % 3600) / 60), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateActivityDialog(
            onDismiss = { showCreateDialog = false },
            onSave = { name, cat ->
                viewModel.createActivity(name, cat, "#14B8A6")
                showCreateDialog = false
            }
        )
    }

    if (showManualSessionDialog != null) {
        val act = showManualSessionDialog!!
        ManualSessionDialog(
            activity = act,
            onDismiss = { showManualSessionDialog = null },
            onSave = { mins, notes ->
                viewModel.addManualActivitySession(act.id, act.name, mins, notes)
                showManualSessionDialog = null
            }
        )
    }

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("Save Completed Activity Session") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val hrs = elapsedSeconds / 3600
                    val mins = (elapsedSeconds % 3600) / 60
                    val secs = elapsedSeconds % 60
                    Text("Duration: %02d:%02d:%02d".format(hrs, mins, secs), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    OutlinedTextField(
                        value = stopSessionNotes,
                        onValueChange = { stopSessionNotes = it },
                        label = { Text("Session Notes (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.stopStopwatch(save = true, notes = stopSessionNotes)
                    showStopDialog = false
                    stopSessionNotes = ""
                }) {
                    Text("Save Session")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.stopStopwatch(save = false)
                    showStopDialog = false
                    stopSessionNotes = ""
                }) {
                    Text("Discard")
                }
            }
        )
    }
}

@Composable
fun CreateActivityDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, category: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Professional") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Custom Activity") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Activity Name (e.g. Audit, Research, Gym)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_activity_name")
                )
                Text("Category / Group:", style = MaterialTheme.typography.labelSmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Professional", "Learning", "Health", "Creative", "Personal").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 9.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) onSave(name, category)
                },
                modifier = Modifier.testTag("save_activity_btn")
            ) {
                Text("Create Activity")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ManualSessionDialog(
    activity: ActivityItem,
    onDismiss: () -> Unit,
    onSave: (minutes: Long, notes: String) -> Unit
) {
    var minutesStr by remember { mutableStateOf("60") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Past Session: ${activity.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = minutesStr,
                    onValueChange = { minutesStr = it },
                    label = { Text("Duration (Minutes)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_manual_session_minutes")
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val mins = minutesStr.toLongOrNull() ?: 0L
                if (mins > 0) onSave(mins, notes)
            }) { Text("Save Session") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
