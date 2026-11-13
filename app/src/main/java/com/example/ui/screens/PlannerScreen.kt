package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.model.PlannedTask
import com.example.ui.components.MetricCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.allPlannedTasks.collectAsState()
    val sleepRecords by viewModel.allSleepRecords.collectAsState()
    val screenRecords by viewModel.allScreenTimeRecords.collectAsState()
    val activitySessions by viewModel.allActivitySessions.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    var showAddTaskDialog by remember { mutableStateOf(false) }

    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todayTasks = tasks.filter { it.date == todayStr }

    // Synthesized Actual Timeline for today:
    // 1. Sleep
    val todaySleep = sleepRecords.filter { it.date == todayStr }
    // 2. Activity Sessions today
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }
    val startOfDay = cal.timeInMillis
    val todaySessions = activitySessions.filter { it.startTime >= startOfDay }
    // 3. Screen Time
    val todayScreen = screenRecords.filter { it.date == todayStr }

    // Analysis metrics
    val completedCount = todayTasks.count { it.isCompleted }
    val totalCount = todayTasks.size
    val pendingCount = totalCount - completedCount
    val completionPct = if (totalCount > 0) (completedCount * 100) / totalCount else 0

    val totalActivityMinutes = todaySessions.sumOf { it.durationSeconds } / 60
    val totalSleepMinutes = todaySleep.sumOf { it.durationMinutes }
    val totalScreenMinutes = todayScreen.sumOf { it.durationMinutes }
    val trackedMinutes = totalActivityMinutes + totalSleepMinutes + totalScreenMinutes
    val untrackedMinutes = (1440 - trackedMinutes).coerceAtLeast(0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Planner: Planned vs Actual", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddTaskDialog = true },
                        modifier = Modifier.testTag("btn_add_task_top")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Task")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskDialog = true },
                containerColor = PrimaryBlue,
                modifier = Modifier.testTag("fab_add_task")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task", tint = Color.White)
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
            // Planned vs Actual Comparison Card
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
                            Text("TODAY'S PRODUCTIVITY & TIMELINE", style = MaterialTheme.typography.labelSmall)
                            Badge(containerColor = PrimaryBlue) { Text("$completionPct% Completed", modifier = Modifier.padding(2.dp)) }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Planned Tasks", style = MaterialTheme.typography.labelSmall)
                                Text("$totalCount tasks", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Completed", style = MaterialTheme.typography.labelSmall)
                                Text("$completedCount done", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ProfitGreen)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Untracked Day", style = MaterialTheme.typography.labelSmall)
                                Text("%dh %dm".format(untrackedMinutes / 60, untrackedMinutes % 60), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }

            // LAYER 1: Planned Schedule
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("1. Planned Tasks Schedule", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = PrimaryBlue)
                    TextButton(onClick = { showAddTaskDialog = true }) { Text("+ Plan Task") }
                }
            }

            if (todayTasks.isEmpty()) {
                item {
                    Text("No planned tasks for today. Tap + to add tasks.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(todayTasks, key = { it.id }) { task ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = task.isCompleted,
                                onCheckedChange = { viewModel.toggleTaskCompletion(task) },
                                modifier = Modifier.testTag("task_check_${task.id}")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (task.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    val priorityColor = when (task.priority) {
                                        "High" -> LossRed
                                        "Medium" -> AccentGold
                                        else -> ProfitGreen
                                    }
                                    Badge(containerColor = priorityColor) { Text(task.priority, fontSize = 9.sp, color = Color.White) }
                                }
                                if (task.startTime.isNotBlank() || task.endTime.isNotBlank()) {
                                    Text(
                                        text = "Time: ${task.startTime} - ${task.endTime}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                if (task.notes.isNotBlank()) {
                                    Text(task.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { viewModel.deleteTask(task) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // LAYER 2: Automatically Generated Actual Timeline
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "2. Automatically Generated Actual Timeline",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TealAccent
                )
                Text(
                    "Synthesized accurately from Sleep Tracker, Stopwatch Activity Tracker, and Screen Time without fabricated data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Actual Sleep Blocks
            if (todaySleep.isNotEmpty()) {
                items(todaySleep) { s ->
                    ActualTimelineCard(
                        category = "SLEEP",
                        title = "Sleep & Rest Session",
                        timeRange = "${s.sleepTime} - ${s.wakeTime}",
                        durationStr = "%dh %dm".format(s.durationMinutes / 60, s.durationMinutes % 60),
                        icon = Icons.Default.Bedtime,
                        color = IndigoAccent
                    )
                }
            }

            // Actual Stopwatch Activity Sessions
            if (todaySessions.isNotEmpty()) {
                items(todaySessions) { sess ->
                    val startStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(sess.startTime))
                    val endStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(sess.endTime))
                    ActualTimelineCard(
                        category = "ACTIVITY",
                        title = sess.activityName + if (sess.notes.isNotBlank()) " (${sess.notes})" else "",
                        timeRange = "$startStr - $endStr",
                        durationStr = "%dh %dm".format(sess.durationSeconds / 3600, (sess.durationSeconds % 3600) / 60),
                        icon = Icons.Default.Timer,
                        color = TealAccent
                    )
                }
            }

            // Actual Top Screen Time Apps
            if (todayScreen.isNotEmpty()) {
                items(todayScreen.take(4)) { sc ->
                    ActualTimelineCard(
                        category = "SCREEN TIME",
                        title = sc.appName + " (${sc.category})",
                        timeRange = "Usage Today",
                        durationStr = "%dh %dm".format(sc.durationMinutes / 60, sc.durationMinutes % 60),
                        icon = Icons.Default.PhoneAndroid,
                        color = PurpleAccent
                    )
                }
            }
        }
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onSave = { title, start, end, prio, notes ->
                viewModel.addPlannedTask(todayStr, title, start, end, prio, notes)
                showAddTaskDialog = false
            }
        )
    }
}

@Composable
fun ActualTimelineCard(
    category: String,
    title: String,
    timeRange: String,
    durationStr: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.width(6.dp))
                        Badge(containerColor = color.copy(alpha = 0.2f)) { Text(category, fontSize = 8.sp, color = color) }
                    }
                    Text(timeRange, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }
            Text(durationStr, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = color)
        }
    }
}

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, startTime: String, endTime: String, priority: String, notes: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("10:30") }
    var priority by remember { mutableStateOf("Medium") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Planned Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Name") },
                    modifier = Modifier.fillMaxWidth().testTag("input_task_title")
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start (HH:mm)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End (HH:mm)") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Text("Priority:", style = MaterialTheme.typography.labelSmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("High", "Medium", "Low").forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p, fontSize = 9.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) onSave(title, startTime, endTime, priority, notes)
                },
                modifier = Modifier.testTag("btn_save_task")
            ) {
                Text("Add Task")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
